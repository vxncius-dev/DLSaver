package com.vxncius.dlsaver

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.UUID
import java.util.Locale
import java.net.URI

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val uiState: StateFlow<DownloadUiState> = DownloadStateStore.uiState
    private var searchJob: Job? = null
    private val searchCache = linkedMapOf<String, CachedSearchPage>()

    init {
        DownloadStateStore.setSimultaneousDownloadsLimit(
            AppPreferences.getSimultaneousDownloadsLimit(getApplication())
        )
        DownloadStateStore.restoreJobs(DownloadJobPersistence.loadJobs(getApplication()))
        refreshExistingDownloads()
        viewModelScope.launch {
            DownloadScheduler.kick(getApplication())
        }
    }

    fun updateInput(value: String) {
        DownloadStateStore.setInput(value)
    }

    fun handleIncomingSharedText(value: String) {
        val normalized = value.trim()
        if (normalized.isBlank()) return
        DownloadStateStore.navigateTo(AppScreen.HOME)
        DownloadStateStore.setInput(normalized)
        prepareInputForListing()
    }

    fun navigateTo(screen: AppScreen) {
        DownloadStateStore.navigateTo(screen)
        if (screen == AppScreen.DOWNLOADED_OR_PROGRESS_LIST_ITEMS) {
            refreshExistingDownloads()
        }
    }

    fun search(resetPage: Boolean = true) {
        val state = uiState.value
        if (state.isSearching) return
        if (!resetPage && !state.canLoadMore) return

        val query = if (resetPage) {
            state.input.trim()
        } else {
            state.lastSearchQuery.ifBlank { state.input.trim() }
        }
        if (query.isBlank()) {
            DownloadStateStore.searchFailed("Digite uma URL ou termo de pesquisa.")
            return
        }
        if (looksLikeUrl(query)) {
            if (looksLikePlaylistUrl(query)) {
                openPlaylist(query, resetPage = true)
            } else {
                loadDirectLink(query)
            }
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (resetPage) {
                DownloadStateStore.setLastSearchQuery(query)
            }
            DownloadStateStore.searchStarted(clearResults = resetPage)
            runCatching {
                withContext(Dispatchers.IO) {
                    if (resetPage) {
                        val cached = searchCache[query]
                        if (cached != null && cached.items.isNotEmpty()) {
                            return@withContext Triple(cached.page, cached.items, cached.hasMore)
                        }

                        val pages = mutableListOf<Pair<List<SearchResultItem>, Boolean>>()
                        for (pageIndex in 0 until SEARCH_PREFETCH_PAGES) {
                            pages += DownloadEngines.current.search(query, pageIndex)
                            yield()
                            if (pages.last().first.isEmpty() || !pages.last().second) break
                        }
                        val merged = mergeSearchResults(pages.flatMap { it.first })
                        val lastPage = (pages.size - 1).coerceAtLeast(0)
                        val hasMore = pages.lastOrNull()?.second == true
                        rememberSearchCache(query, CachedSearchPage(lastPage, merged, hasMore))
                        Triple(lastPage, merged, hasMore)
                    } else {
                        val targetPage = state.searchPage + 1
                        val (results, hasMore) = DownloadEngines.current.search(query, targetPage)
                        val merged = mergeSearchResults(state.searchResults + results)
                        rememberSearchCache(query, CachedSearchPage(targetPage, merged, hasMore))
                        Triple(targetPage, results, hasMore)
                    }
                }
            }.onSuccess { (page, results, hasMore) ->
                DownloadStateStore.searchFinished(page, results, hasMore)
            }.onFailure { error ->
                DownloadStateStore.searchFailed(error.stackTraceToString())
            }
        }
    }

    fun prepareInputForListing() {
        val value = uiState.value.input.trim()
        if (value.isBlank()) {
            DownloadStateStore.searchFailed("Digite uma URL ou termo de pesquisa.")
            return
        }
        if (looksLikeUrl(value)) {
            if (looksLikePlaylistUrl(value)) {
                openPlaylist(value, resetPage = true)
            } else {
                loadDirectLink(value)
            }
        } else {
            search(resetPage = true)
        }
        DownloadStateStore.setInput("")
    }

    fun loadMoreListing() {
        val state = uiState.value
        if (state.isSearching) return
        if (!state.canLoadMore) return

        if (state.listingMode == ListingMode.PLAYLIST && state.listingSourceUrl.isNotBlank()) {
            openPlaylist(state.listingSourceUrl, resetPage = false)
        } else {
            search(resetPage = false)
        }
    }

    fun openPlaylist(url: String, resetPage: Boolean = true) {
        val state = uiState.value
        if (state.isSearching) return
        if (!resetPage && !state.canLoadMore) return

        val targetPage = if (resetPage) 0 else state.searchPage + 1
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            DownloadStateStore.playlistListingStarted(sourceUrl = url, clearResults = resetPage)
            runCatching {
                withContext(Dispatchers.IO) {
                    val page = DownloadEngines.current.listPlaylist(url, targetPage, pageSize = 20)
                    Triple(targetPage, page, page.hasMore)
                }
            }.onSuccess { (pageIndex, page, hasMore) ->
                DownloadStateStore.playlistListingFinished(
                    page = pageIndex,
                    title = page.title,
                    results = page.items,
                    canLoadMore = hasMore
                )
            }.onFailure { error ->
                DownloadStateStore.searchFailed(error.stackTraceToString())
            }
        }
    }

    fun setSelectionMode(enabled: Boolean) {
        DownloadStateStore.setSelectionMode(enabled)
    }

    fun toggleSelectAllVisible() {
        DownloadStateStore.toggleSelectAllVisible()
    }

    fun toggleSelected(url: String) {
        DownloadStateStore.toggleSelected(url)
    }

    fun downloadSelected(kind: DownloadKind): DownloadEnqueueResult {
        val state = uiState.value
        val selected = state.searchResults.filter { it.url in state.selectedUrls }
        if (selected.isEmpty()) {
            return DownloadEnqueueResult(
                status = DownloadEnqueueStatus.DUPLICATE_ACTIVE,
                title = "Nenhum item selecionado",
                ignoredCount = 0,
                enqueuedCount = 0
            )
        }

        var enqueued = 0
        var ignored = 0
        selected.forEach { item ->
            val result = downloadResult(item, kind)
            if (result.enqueued) enqueued++ else ignored++
        }
        viewModelScope.launch {
            DownloadStateStore.setSelectionMode(false)
        }
        return DownloadEnqueueResult(
            status = if (enqueued > 0) DownloadEnqueueStatus.ENQUEUED else DownloadEnqueueStatus.DUPLICATE_ACTIVE,
            title = if (enqueued > 0) "$enqueued item(ns) adicionados" else "Nenhum item novo para baixar",
            ignoredCount = ignored,
            enqueuedCount = enqueued
        )
    }

    fun downloadResult(item: SearchResultItem, kind: DownloadKind, videoMinHeight: Int = 0): DownloadEnqueueResult {
        val sanitizedTitle = sanitizeDownloadTitle(item.title)
        return startDownload(
            url = item.url,
            title = sanitizedTitle,
            thumbnailUrl = if (kind == DownloadKind.AUDIO) "" else item.thumbnailUrl.ifBlank {
                if (item.resultType == "video" && item.id.isNotBlank()) {
                    "https://i.ytimg.com/vi/${item.id}/hqdefault.jpg"
                } else {
                    ""
                }
            },
            kind = kind,
            videoMinHeight = if (kind == DownloadKind.VIDEO) videoMinHeight else 0
        )
    }

    fun refreshExistingDownloads() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val items = withContext(Dispatchers.IO) {
                val downloads = DownloadsLibrary.queryAppDownloads(context)
                LocalThumbnailStore.sync(context, downloads, maxItemsToProcess = Int.MAX_VALUE)
            }
            DownloadStateStore.setExistingDownloads(items)
        }
    }

    fun setSimultaneousDownloadsLimit(value: Int) {
        AppPreferences.setSimultaneousDownloadsLimit(getApplication(), value)
        DownloadStateStore.setSimultaneousDownloadsLimit(value)
        viewModelScope.launch {
            DownloadScheduler.kick(getApplication())
        }
    }

    fun playExistingAudio(item: ExistingDownloadItem) {
        if (item.kind != DownloadKind.AUDIO) return
        viewModelScope.launch {
            MediaPlayback.playFromLibrary(
                context = getApplication(),
                items = uiState.value.existingDownloads,
                startItem = item
            )
        }
    }

    fun playExistingVideo(item: ExistingDownloadItem) {
        if (item.kind != DownloadKind.VIDEO) return
        viewModelScope.launch {
            MediaPlayback.playExternalVideo(
                context = getApplication(),
                uri = android.net.Uri.parse(item.sourceUrl),
                title = item.name
            )
        }
    }

    fun needsLegacyStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) return false
        return ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED
    }

    fun needsMediaReadPermissions(): Boolean {
        val context = getApplication<Application>()
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            }
            else -> false
        }
    }

    fun hasLegacyStoragePermission(): Boolean = !needsLegacyStoragePermission()

    fun hasMediaReadPermissions(): Boolean = !needsMediaReadPermissions()

    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun retryJob(jobId: String) {
        val context = getApplication<Application>()
        DownloadJobPersistence.removeJob(context, jobId)
        DownloadStateStore.retryJob(jobId)
        viewModelScope.launch {
            DownloadScheduler.kick(context)
        }
    }

    fun cancelJob(jobId: String) {
        val context = getApplication<Application>()
        val job = uiState.value.downloadJobs.firstOrNull { it.id == jobId } ?: return
        DownloadJobPersistence.removeJob(context, jobId)
        if (job.status == DownloadJobStatus.RUNNING || job.status == DownloadJobStatus.EXPORTING) {
            startForegroundService(
                context,
                DownloadForegroundService.createCancelIntent(context, jobId)
            )
        }
        DownloadStateStore.cancelJob(jobId)
        viewModelScope.launch {
            DownloadScheduler.kick(context)
        }
    }

    private fun startDownload(
        url: String,
        title: String,
        thumbnailUrl: String,
        kind: DownloadKind,
        videoMinHeight: Int = 0
    ): DownloadEnqueueResult {
        val context = getApplication<Application>()
        val duplicateStatus = duplicateStatusFor(url, title, kind)
        if (duplicateStatus != null) {
            val message = when (duplicateStatus) {
                DownloadEnqueueStatus.DUPLICATE_ACTIVE -> "Este download já está na fila ou em andamento"
                DownloadEnqueueStatus.DUPLICATE_EXISTING -> "Arquivo já existe na biblioteca"
                else -> "Download duplicado ignorado"
            }
            DownloadStateStore.setStatus(message)
            return DownloadEnqueueResult(duplicateStatus, title, ignoredCount = 1)
        }
        val jobId = UUID.randomUUID().toString()
        DownloadStateStore.enqueueDownload(jobId, title, url, thumbnailUrl, kind, videoMinHeight)
        DownloadJobPersistence.upsertJob(
            context,
            DownloadStateStore.uiState.value.downloadJobs.first { it.id == jobId }
        )
        viewModelScope.launch {
            DownloadScheduler.kick(context)
        }
        return DownloadEnqueueResult(
            status = DownloadEnqueueStatus.ENQUEUED,
            title = title,
            enqueuedCount = 1
        )
    }

    private fun loadDirectLink(url: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            DownloadStateStore.directLinkListingStarted(url)
            runCatching {
                withContext(Dispatchers.IO) {
                    DownloadEngines.current.inspectUrl(url)
                }
            }.onSuccess { item ->
                DownloadStateStore.showDirectLinkAsList(
                    item.copy(
                        id = item.id.ifBlank { url },
                        title = item.title.ifBlank { url },
                        author = item.author.ifBlank { "Link direto" },
                        resultType = item.resultType.ifBlank { "video" },
                        url = item.url.ifBlank { url }
                    )
                )
            }.onFailure {
                DownloadStateStore.showDirectLinkAsList(
                    SearchResultItem(
                        id = url,
                        title = url,
                        author = "Link direto",
                        resultType = "url",
                        extra = "",
                        url = url
                    )
                )
            }
        }
    }

    private fun looksLikeUrl(value: String): Boolean {
        val lower = value.lowercase(Locale.ROOT)
        return lower.startsWith("http://") || lower.startsWith("https://")
    }

    private fun looksLikePlaylistUrl(value: String): Boolean {
        val lower = value.lowercase(Locale.ROOT)
        if (!lower.contains("youtube.") && !lower.contains("youtu.be")) return false
        return runCatching {
            val uri = URI(value)
            val query = uri.rawQuery.orEmpty()
            query.split("&")
                .mapNotNull { part ->
                    val idx = part.indexOf('=')
                    if (idx <= 0) return@mapNotNull null
                    part.substring(0, idx) to part.substring(idx + 1)
                }
                .any { (k, v) -> k == "list" && v.isNotBlank() }
        }.getOrDefault(lower.contains("list=") || lower.contains("/playlist"))
    }

    private fun duplicateStatusFor(url: String, title: String, kind: DownloadKind): DownloadEnqueueStatus? {
        val state = uiState.value
        val activeDuplicate = state.downloadJobs.any { job ->
            job.kind == kind &&
                job.status != DownloadJobStatus.COMPLETED &&
                job.sourceUrl == url
        }
        if (activeDuplicate) return DownloadEnqueueStatus.DUPLICATE_ACTIVE

        val existingDuplicate = state.existingDownloads.any { existing ->
            existing.kind == kind &&
                existing.sourceUrl == url &&
                existing.sourceUrl.startsWith("http")
        }
        if (existingDuplicate) return DownloadEnqueueStatus.DUPLICATE_EXISTING

        return null
    }

    private fun rememberSearchCache(query: String, page: CachedSearchPage) {
        searchCache[query] = page
        while (searchCache.size > MAX_SEARCH_CACHE_QUERIES) {
            val oldest = searchCache.keys.firstOrNull() ?: break
            searchCache.remove(oldest)
        }
    }

    private fun mergeSearchResults(items: List<SearchResultItem>): List<SearchResultItem> {
        return buildList {
            items.forEach { item ->
                if (item.url.isNotBlank() && none { it.url == item.url }) add(item)
            }
        }
    }

    private data class CachedSearchPage(
        val page: Int,
        val items: List<SearchResultItem>,
        val hasMore: Boolean
    )

    companion object {
        private const val SEARCH_PREFETCH_PAGES = 5
        private const val MAX_SEARCH_CACHE_QUERIES = 8
    }
}
