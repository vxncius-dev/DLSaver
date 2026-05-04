package com.vxncius.dlsaver

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

object DownloadRepository {
    private val downloadSemaphore = Semaphore(4)

    suspend fun download(
        url: String,
        tempDir: String,
        ytDlpPath: String,
        ffmpegPath: String,
        aria2cPath: String,
        audioOnly: Boolean,
        callback: PythonProgressCallback
    ): DownloadResult = withContext(Dispatchers.IO) {
        downloadSemaphore.withPermit {
            DownloadEngines.current.download(
                url = url,
                tempDir = tempDir,
                ytDlpPath = ytDlpPath,
                ffmpegPath = ffmpegPath,
                aria2cPath = aria2cPath,
                audioOnly = audioOnly,
                callback = callback
            )
        }
    }
}
