package com.xplayer.app.data.scanner

import com.xplayer.app.data.source.SubtitleFile
import java.io.File

object SubtitleMatcher {
    private val SUBTITLE_EXTENSIONS = setOf("ass", "ssa", "srt", "vtt", "sub")

    /**
     * Find subtitle files in the same directory matching the video filename prefix.
     */
    fun findSubtitlesForVideo(videoFile: File): List<SubtitleFile> {
        val parent = videoFile.parentFile ?: return emptyList()
        val videoBaseName = videoFile.nameWithoutExtension.lowercase()

        val files = parent.listFiles() ?: return emptyList()
        val results = mutableListOf<SubtitleFile>()

        for (file in files) {
            if (!file.isFile) continue
            val ext = file.extension.lowercase()
            if (ext in SUBTITLE_EXTENSIONS) {
                val subBase = file.nameWithoutExtension.lowercase()
                // Matches exact name or name with language tag (e.g., movie.zh.srt or movie.ass)
                if (subBase == videoBaseName || subBase.startsWith(videoBaseName)) {
                    results.add(
                        SubtitleFile(
                            path = file.absolutePath,
                            name = file.name,
                            extension = ext
                        )
                    )
                }
            }
        }
        return results
    }

    fun hasAssSubtitle(subtitles: List<SubtitleFile>): Boolean {
        return subtitles.any { it.extension == "ass" || it.extension == "ssa" }
    }
}
