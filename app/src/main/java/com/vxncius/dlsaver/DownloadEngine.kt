package com.vxncius.dlsaver

interface DownloadEngine {
    fun search(query: String, page: Int): Pair<List<SearchResultItem>, Boolean>

    fun inspectUrl(url: String): SearchResultItem

    fun listPlaylist(url: String, page: Int, pageSize: Int = 20): PlaylistPage

    fun download(
        url: String,
        tempDir: String,
        ytDlpPath: String,
        ffmpegPath: String,
        aria2cPath: String,
        audioOnly: Boolean,
        callback: PythonProgressCallback
    ): DownloadResult
}

object DownloadEngines {
    val current: DownloadEngine = PythonYtDlpDownloadEngine
}

