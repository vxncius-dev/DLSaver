package com.vxncius.dlsaver

object PythonYtDlpDownloadEngine : DownloadEngine {
    override fun search(query: String, page: Int): Pair<List<SearchResultItem>, Boolean> {
        return DownloaderBridge.search(query, page)
    }

    override fun inspectUrl(url: String): SearchResultItem {
        return DownloaderBridge.inspectUrl(url)
    }

    override fun listPlaylist(url: String, page: Int, pageSize: Int): PlaylistPage {
        return DownloaderBridge.listPlaylist(url, page, pageSize)
    }

    override fun listVideoQualities(url: String): List<VideoQualityOption> {
        return DownloaderBridge.listVideoQualities(url)
    }

    override fun previewStreamUrl(url: String): String {
        return DownloaderBridge.previewStreamUrl(url)
    }

    override fun download(
        url: String,
        tempDir: String,
        ytDlpPath: String,
        ffmpegPath: String,
        aria2cPath: String,
        audioOnly: Boolean,
        videoMinHeight: Int,
        callback: PythonProgressCallback
    ): DownloadResult {
        return DownloaderBridge.download(
            url = url,
            tempDir = tempDir,
            ytDlpPath = ytDlpPath,
            ffmpegPath = ffmpegPath,
            aria2cPath = aria2cPath,
            audioOnly = audioOnly,
            videoMinHeight = videoMinHeight,
            callback = callback
        )
    }
}
