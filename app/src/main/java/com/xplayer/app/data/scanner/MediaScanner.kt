package com.xplayer.app.data.scanner

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.xplayer.app.data.local.AppDatabase
import com.xplayer.app.data.local.entity.HdrType
import com.xplayer.app.data.local.entity.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class MediaScanner(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val videoDao = db.videoDao()
    private val blacklistDao = db.folderBlacklistDao()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val videoExtensions = setOf(
        "mp4", "mkv", "mov", "avi", "webm", "flv", "ts", "m2ts", "rmvb", "wmv", "3gp"
    )

    suspend fun scanMedia(): Int = withContext(Dispatchers.IO) {
        if (_isScanning.value) return@withContext 0
        _isScanning.value = true

        try {
            val blacklistedPaths = blacklistDao.getAllBlacklistedPaths().toSet()
            val discoveredFiles = mutableListOf<File>()

            // 1. Query MediaStore
            queryMediaStore(discoveredFiles)

            // 2. Direct scan of common directories if MediaStore misses files
            scanDirectDirectories(discoveredFiles)

            val existingVideos = videoDao.getAllVideos().associateBy { it.path }
            val videoEntitiesToSave = mutableListOf<VideoEntity>()
            val validPaths = mutableListOf<String>()

            for (file in discoveredFiles) {
                if (!file.exists() || !file.isFile || file.length() == 0L) continue
                val path = file.absolutePath
                val parentPath = file.parent ?: ""

                // Ignore hidden directories and blacklisted folders
                if (file.name.startsWith(".") || parentPath.split(File.separator).any { it.startsWith(".") }) {
                    continue
                }
                if (blacklistedPaths.any { parentPath.startsWith(it) }) {
                    continue
                }

                validPaths.add(path)

                val existing = existingVideos[path]
                if (existing != null && existing.dateModified == file.lastModified() && existing.sizeBytes == file.length()) {
                    videoEntitiesToSave.add(existing)
                    continue
                }

                // Extract metadata
                val entity = extractMetadata(file)
                videoEntitiesToSave.add(entity)
            }

            // Save batch
            if (videoEntitiesToSave.isNotEmpty()) {
                videoDao.insertVideos(videoEntitiesToSave)
            }

            // Remove videos no longer existing on disk
            if (validPaths.isNotEmpty()) {
                videoDao.deleteOldVideos(validPaths)
            }

            videoEntitiesToSave.size
        } catch (e: Exception) {
            e.printStackTrace()
            0
        } finally {
            _isScanning.value = false
        }
    }

    private fun queryMediaStore(outFiles: MutableList<File>) {
        val projection = arrayOf(
            MediaStore.Video.Media.DATA
        )
        val selection = null
        val selectionArgs = null
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        val collectionUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        try {
            context.contentResolver.query(collectionUri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataIndex)
                    if (!path.isNullOrEmpty()) {
                        val file = File(path)
                        if (file.exists() && file.extension.lowercase() in videoExtensions) {
                            outFiles.add(file)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scanDirectDirectories(outFiles: MutableList<File>) {
        val root = Environment.getExternalStorageDirectory() ?: return
        val seenPaths = outFiles.map { it.absolutePath }.toMutableSet()

        val directoriesToScan = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            File(root, "Video"),
            File(root, "Videos")
        )

        for (dir in directoriesToScan) {
            if (dir.exists() && dir.isDirectory) {
                scanDirRecursively(dir, outFiles, seenPaths, maxDepth = 4)
            }
        }
    }

    private fun scanDirRecursively(dir: File, outFiles: MutableList<File>, seenPaths: MutableSet<String>, maxDepth: Int) {
        if (maxDepth <= 0 || dir.name.startsWith(".")) return
        val files = dir.listFiles() ?: return

        for (file in files) {
            if (file.name.startsWith(".")) continue
            if (file.isDirectory) {
                scanDirRecursively(file, outFiles, seenPaths, maxDepth - 1)
            } else if (file.isFile && file.extension.lowercase() in videoExtensions) {
                if (seenPaths.add(file.absolutePath)) {
                    outFiles.add(file)
                }
            }
        }
    }

    private fun extractMetadata(file: File): VideoEntity {
        var durationMs = 0L
        var width = 0
        var height = 0
        var codec = ""
        var hdrType = HdrType.NONE

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            codec = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""

            // HDR & Dolby Vision detection
            val nameLower = file.name.lowercase()
            if (nameLower.contains("dovi") || nameLower.contains("dolby") || nameLower.contains("dv")) {
                hdrType = HdrType.DOLBY_VISION
            } else if (nameLower.contains("hdr10+") || nameLower.contains("hdr10plus")) {
                hdrType = HdrType.HDR10_PLUS
            } else if (nameLower.contains("hdr") || nameLower.contains("hdr10")) {
                hdrType = HdrType.HDR10
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hdrType == HdrType.NONE) {
                val colorTransfer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_TRANSFER)?.toIntOrNull()
                if (colorTransfer == 6 /* COLOR_TRANSFER_ST2084 */) {
                    hdrType = HdrType.HDR10
                } else if (colorTransfer == 7 /* COLOR_TRANSFER_HLG */) {
                    hdrType = HdrType.HDR10
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            runCatching { retriever.release() }
        }

        val subtitles = SubtitleMatcher.findSubtitlesForVideo(file)
        val hasAss = SubtitleMatcher.hasAssSubtitle(subtitles)

        return VideoEntity(
            path = file.absolutePath,
            folderPath = file.parent ?: "",
            title = file.nameWithoutExtension,
            durationMs = durationMs,
            sizeBytes = file.length(),
            dateAdded = System.currentTimeMillis(),
            dateModified = file.lastModified(),
            width = width,
            height = height,
            videoCodec = codec,
            hdrType = hdrType,
            hasAssSubtitle = hasAss,
            thumbnailPath = null
        )
    }
}
