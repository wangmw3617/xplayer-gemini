package com.xplayer.app.data.source

import java.io.File
import java.io.Serializable

/**
 * Unified media source abstraction.
 * V1: LocalFileSource, NetworkUrlSource
 * V2 Reserved: WebDavSource, SmbSource, EmbySource
 */
sealed interface MediaSource : Serializable {
    val id: String
    val title: String
    val playbackUri: String

    data class LocalFileSource(
        val path: String,
        override val title: String,
        val sizeBytes: Long = 0L,
        val modifiedTime: Long = 0L,
        val externalSubtitles: List<SubtitleFile> = emptyList()
    ) : MediaSource {
        override val id: String get() = path
        override val playbackUri: String get() = path
        val file: File get() = File(path)
    }

    data class NetworkUrlSource(
        val url: String,
        override val title: String,
        val headers: Map<String, String> = emptyMap()
    ) : MediaSource {
        override val id: String get() = url
        override val playbackUri: String get() = url
    }
}

data class SubtitleFile(
    val path: String,
    val name: String,
    val extension: String
) : Serializable
