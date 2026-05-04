package com.vxncius.dlsaver

data class SearchResultItem(
    val id: String,
    val title: String,
    val author: String,
    val resultType: String,
    val extra: String,
    val url: String,
    val thumbnailUrl: String = ""
)

enum class ListingMode {
    SEARCH,
    DIRECT_URL,
    PLAYLIST
}

enum class AppScreen {
    HOME,
    LIST_ITEMS_FOR_DOWNLOAD,
    DOWNLOADED_OR_PROGRESS_LIST_ITEMS,
    SETTINGS,
    SETTINGS_DOCUMENT
}

enum class DownloadKind {
    VIDEO,
    AUDIO
}

enum class DownloadJobStatus {
    QUEUED,
    RUNNING,
    EXPORTING,
    COMPLETED,
    FAILED
}

enum class DownloadEnqueueStatus {
    ENQUEUED,
    DUPLICATE_ACTIVE,
    DUPLICATE_EXISTING
}

data class DownloadEnqueueResult(
    val status: DownloadEnqueueStatus,
    val title: String,
    val ignoredCount: Int = 0,
    val enqueuedCount: Int = 0
) {
    val enqueued: Boolean
        get() = status == DownloadEnqueueStatus.ENQUEUED
}

data class DownloadJobItem(
    val id: String,
    val title: String,
    val sourceUrl: String,
    val thumbnailUrl: String = "",
    val kind: DownloadKind,
    val status: DownloadJobStatus,
    val progress: Float = 0f,
    val statusText: String = "",
    val log: String = "",
    val savedFiles: List<String> = emptyList()
)

data class ExistingDownloadItem(
    val name: String,
    val sourceUrl: String = "",
    val modifiedAt: Long = 0L,
    val thumbnailUrl: String = "",
    val kind: DownloadKind = DownloadKind.VIDEO,
    val mimeType: String = ""
)

data class DownloadUiState(
    val screen: AppScreen = AppScreen.HOME,
    val input: String = "",
    val lastSearchQuery: String = "",
    val status: String = "Pronto para pesquisar ou baixar",
    val log: String = "",
    val outputDir: String = "Downloads/DLSaver",
    val bundledYtDlp: String = "",
    val bundledFfmpeg: String = "",
    val isSearching: Boolean = false,
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val currentTarget: String = "",
    val listingMode: ListingMode = ListingMode.SEARCH,
    val listingSourceUrl: String = "",
    val listingTitle: String = "",
    val selectionMode: Boolean = false,
    val selectedUrls: Set<String> = emptySet(),
    val searchPage: Int = 0,
    val canLoadMore: Boolean = false,
    val searchResults: List<SearchResultItem> = emptyList(),
    val savedFiles: List<String> = emptyList(),
    val downloadJobs: List<DownloadJobItem> = emptyList(),
    val existingDownloads: List<ExistingDownloadItem> = emptyList(),
    val simultaneousDownloadsLimit: Int = 2
)

data class PlaylistPage(
    val title: String,
    val items: List<SearchResultItem>,
    val hasMore: Boolean
)

data class DownloadResult(
    val success: Boolean,
    val exitCode: Int,
    val log: String,
    val tempDir: String,
    val files: List<String>
)

data class BinaryPaths(
    val ytDlp: String,
    val ffmpeg: String,
    val ffprobe: String,
    val aria2c: String,
    val tempRoot: String
)
