package com.vxncius.dlsaver

import android.content.Context
import java.io.File

object BinaryInstaller {
    fun installBundledBinaries(context: Context): BinaryPaths {
        val assetManager = context.assets
        val binDir = File(context.filesDir, "bin").apply {
            mkdirs()
        }

        val assetNames = runCatching { assetManager.list("bin").orEmpty().toList() }
            .getOrDefault(emptyList())

        var ytDlpPath = ""
        var ffmpegPath = ""
        var ffprobePath = ""
        var aria2cPath = ""

        assetNames.forEach { assetName ->
            if (assetName.endsWith(".txt")) {
                return@forEach
            }

            val targetFile = File(binDir, assetName)
            assetManager.open("bin/$assetName").use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.setReadable(true, false)
            targetFile.setExecutable(true, false)

            when {
                assetName == "ffmpeg" || assetName.startsWith("ffmpeg-") -> {
                    ffmpegPath = targetFile.absolutePath
                }
                assetName == "ffprobe" || assetName.startsWith("ffprobe-") -> {
                    ffprobePath = targetFile.absolutePath
                }
                assetName == "aria2c" || assetName.startsWith("aria2c-") -> {
                    aria2cPath = targetFile.absolutePath
                }
                assetName == "yt-dlp" || assetName.startsWith("yt-dlp-") -> {
                    ytDlpPath = targetFile.absolutePath
                }
            }
        }

        return BinaryPaths(
            ytDlp = ytDlpPath,
            ffmpeg = ffmpegPath,
            ffprobe = ffprobePath,
            aria2c = aria2cPath,
            tempRoot = File(context.cacheDir, "downloads_work").apply { mkdirs() }.absolutePath
        )
    }
}
