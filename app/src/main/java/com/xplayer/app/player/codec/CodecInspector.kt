package com.xplayer.app.player.codec

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build

data class CodecCapability(
    val mimeType: String,
    val isHardwareAccelerated: Boolean,
    val codecName: String,
    val maxResolution: String,
    val profiles: List<String>
)

data class CodecInspectionResult(
    val deviceModel: String,
    val socManufacturer: String,
    val androidVersion: String,
    val supportsH264: Boolean,
    val supportsHevc10Bit: Boolean,
    val supportsAv1: Boolean,
    val supportsVp9: Boolean,
    val supportsDolbyVision: Boolean,
    val dolbyVisionProfiles: List<String>,
    val supportsHdr10: Boolean,
    val hardwareDecoders: List<CodecCapability>
)

object CodecInspector {

    fun inspect(): CodecInspectionResult {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val allCodecInfos = codecList.codecInfos.filter { !it.isEncoder }

        var h264Supported = false
        var hevc10BitSupported = false
        var av1Supported = false
        var vp9Supported = false
        var dvSupported = false
        val dvProfiles = mutableListOf<String>()
        var hdr10Supported = false
        val hwDecoders = mutableListOf<CodecCapability>()

        for (info in allCodecInfos) {
            val isHw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                info.isHardwareAccelerated
            } else {
                !info.name.startsWith("OMX.google.") && !info.name.startsWith("c2.android.")
            }

            for (mime in info.supportedTypes) {
                val caps = runCatching { info.getCapabilitiesForType(mime) }.getOrNull() ?: continue
                val profileNames = mutableListOf<String>()

                caps.profileLevels?.forEach { pl ->
                    when (mime) {
                        MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION -> {
                            dvSupported = true
                            profileNames.add("Profile ${pl.profile}")
                        }
                        MediaFormat.MIMETYPE_VIDEO_HEVC -> {
                            if (pl.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10 ||
                                pl.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10 ||
                                pl.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus
                            ) {
                                hevc10BitSupported = true
                                hdr10Supported = true
                                profileNames.add("Main 10")
                            }
                        }
                    }
                }

                when (mime) {
                    MediaFormat.MIMETYPE_VIDEO_AVC -> h264Supported = true
                    MediaFormat.MIMETYPE_VIDEO_AV1 -> av1Supported = true
                    MediaFormat.MIMETYPE_VIDEO_VP9 -> vp9Supported = true
                }

                if (isHw && mime.startsWith("video/")) {
                    val videoCaps = caps.videoCapabilities
                    val maxRes = if (videoCaps != null) {
                        "${videoCaps.supportedWidths.upper}x${videoCaps.supportedHeights.upper}"
                    } else "Unknown"

                    hwDecoders.add(
                        CodecCapability(
                            mimeType = mime,
                            isHardwareAccelerated = true,
                            codecName = info.name,
                            maxResolution = maxRes,
                            profiles = profileNames.distinct()
                        )
                    )
                }
            }
        }

        return CodecInspectionResult(
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            socManufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER else Build.HARDWARE,
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            supportsH264 = h264Supported,
            supportsHevc10Bit = hevc10BitSupported,
            supportsAv1 = av1Supported,
            supportsVp9 = vp9Supported,
            supportsDolbyVision = dvSupported,
            dolbyVisionProfiles = dvProfiles.distinct(),
            supportsHdr10 = hdr10Supported,
            hardwareDecoders = hwDecoders.distinctBy { it.codecName to it.mimeType }
        )
    }
}
