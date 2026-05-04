package com.vxncius.dlsaver

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object DownloadStateStore {
    // Em playlists grandes, logs enormes fritam memoria. Mantem bem curto.
    private const val MAX_LOG_CHARS = 12000
    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    fun setInput(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun setStatus(message: String) {
        _uiState.update { it.copy(status = message) }
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(screen = screen) }
    }

    fun directLinkListingStarted(url: String) {
        _uiState.update {
            it.copy(
                screen = AppScreen.LIST_ITEMS_FOR_DOWNLOAD,
                listingMode = ListingMode.DIRECT_URL,
                listingSourceUrl = url,
                listingTitle = "",
                selectionMode = false,
                selectedUrls = emptySet(),
                lastSearchQuery = "",
                isSearching = true,
                status = "Carregando link...",
                searchPage = 0,
                canLoadMore = false,
                searchResults = emptyList()
            )
        }
    }

    fun showDirectLinkAsList(item: SearchResultItem) {
        _uiState.update {
            it.copy(
                screen = AppScreen.LIST_ITEMS_FOR_DOWNLOAD,
                listingMode = ListingMode.DIRECT_URL,
                listingSourceUrl = item.url,
                listingTitle = "",
                selectionMode = false,
                selectedUrls = emptySet(),
                lastSearchQuery = "",
                isSearching = false,
                status = "Link carregado",
                searchPage = 0,
                canLoadMore = false,
                searchResults = listOf(
                    item
                )
            )
        }
    }

    fun setBinaries(paths: BinaryPaths) {
        _uiState.update {
            it.copy(
                bundledYtDlp = paths.ytDlp,
                bundledFfmpeg = paths.ffmpeg
            )
        }
    }

    fun setExistingDownloads(items: List<ExistingDownloadItem>) {
        _uiState.update { it.copy(existingDownloads = items) }
    }

    fun restoreJobs(items: List<DownloadJobItem>) {
        _uiState.update { state ->
            val existingIds = state.downloadJobs.map { it.id }.toSet()
            val merged = items.filterNot { it.id in existingIds } + state.downloadJobs
            state.copy(downloadJobs = merged)
        }
    }

    fun restoreFailedJobs(items: List<DownloadJobItem>) = restoreJobs(items)

    fun setSimultaneousDownloadsLimit(value: Int) {
        _uiState.update { it.copy(simultaneousDownloadsLimit = value.coerceIn(1, 4)) }
    }

    fun searchStarted(clearResults: Boolean = false) {
        _uiState.update {
            it.copy(
                screen = AppScreen.LIST_ITEMS_FOR_DOWNLOAD,
                listingMode = ListingMode.SEARCH,
                listingSourceUrl = "",
                listingTitle = "",
                selectionMode = false,
                selectedUrls = emptySet(),
                isSearching = true,
                status = "Pesquisando...",
                log = "",
                searchResults = if (clearResults) emptyList() else it.searchResults,
                canLoadMore = if (clearResults) false else it.canLoadMore
            )
        }
    }

    fun setLastSearchQuery(value: String) {
        _uiState.update { it.copy(lastSearchQuery = value.trim()) }
    }

    fun searchFinished(
        page: Int,
        results: List<SearchResultItem>,
        canLoadMore: Boolean
    ) {
        _uiState.update { state ->
            val merged = if (page == 0) {
                results
            } else {
                state.searchResults + results.filter { incoming ->
                    state.searchResults.none { it.url == incoming.url }
                }
            }
            state.copy(
                isSearching = false,
                screen = AppScreen.LIST_ITEMS_FOR_DOWNLOAD,
                listingMode = ListingMode.SEARCH,
                listingSourceUrl = "",
                listingTitle = "",
                selectionMode = false,
                selectedUrls = emptySet(),
                status = if (merged.isEmpty()) "Nenhum resultado encontrado" else "Resultados carregados",
                searchPage = page,
                searchResults = merged,
                canLoadMore = canLoadMore
            )
        }
    }

    fun searchFailed(message: String) {
        _uiState.update {
            it.copy(
                isSearching = false,
                status = "Erro na pesquisa",
                log = message
            )
        }
    }

    fun playlistListingStarted(sourceUrl: String, clearResults: Boolean = true) {
        _uiState.update {
            it.copy(
                screen = AppScreen.LIST_ITEMS_FOR_DOWNLOAD,
                listingMode = ListingMode.PLAYLIST,
                listingSourceUrl = sourceUrl,
                listingTitle = if (clearResults) "" else it.listingTitle,
                selectionMode = false,
                selectedUrls = emptySet(),
                isSearching = true,
                status = "Carregando playlist...",
                log = "",
                searchPage = if (clearResults) 0 else it.searchPage,
                searchResults = if (clearResults) emptyList() else it.searchResults,
                canLoadMore = if (clearResults) false else it.canLoadMore
            )
        }
    }

    fun playlistListingFinished(page: Int, title: String, results: List<SearchResultItem>, canLoadMore: Boolean) {
        _uiState.update { state ->
            val merged = if (page == 0) {
                results
            } else {
                state.searchResults + results.filter { incoming ->
                    state.searchResults.none { it.url == incoming.url }
                }
            }
            state.copy(
                isSearching = false,
                screen = AppScreen.LIST_ITEMS_FOR_DOWNLOAD,
                listingMode = ListingMode.PLAYLIST,
                listingSourceUrl = state.listingSourceUrl,
                listingTitle = title.ifBlank { state.listingTitle },
                status = if (merged.isEmpty()) "Playlist vazia" else "Playlist carregada",
                searchPage = page,
                searchResults = merged,
                canLoadMore = canLoadMore
            )
        }
    }

    fun setSelectionMode(enabled: Boolean) {
        _uiState.update {
            it.copy(
                selectionMode = enabled,
                selectedUrls = if (enabled) it.selectedUrls else emptySet()
            )
        }
    }

    fun toggleSelected(url: String) {
        _uiState.update { state ->
            if (!state.selectionMode) return@update state
            val next = state.selectedUrls.toMutableSet()
            if (!next.add(url)) next.remove(url)
            state.copy(selectedUrls = next)
        }
    }

    fun toggleSelectAllVisible() {
        _uiState.update { state ->
            if (!state.selectionMode) return@update state
            val visible = state.searchResults.map { it.url }.toSet()
            if (visible.isEmpty()) return@update state
            val next = state.selectedUrls.toMutableSet()
            val allSelected = visible.all { it in next }
            if (allSelected) {
                next.removeAll(visible)
            } else {
                next.addAll(visible)
            }
            state.copy(selectedUrls = next)
        }
    }

    fun enqueueDownload(
        jobId: String,
        target: String,
        sourceUrl: String,
        thumbnailUrl: String,
        kind: DownloadKind,
        videoMinHeight: Int = 0
    ) {
        _uiState.update {
            val newJob = DownloadJobItem(
                id = jobId,
                title = target,
                sourceUrl = sourceUrl,
                thumbnailUrl = thumbnailUrl,
                kind = kind,
                videoMinHeight = videoMinHeight,
                status = DownloadJobStatus.QUEUED,
                progress = 0f,
                statusText = "Na fila..."
            )
            it.copy(
                isDownloading = it.downloadJobs.any { job -> job.status == DownloadJobStatus.RUNNING || job.status == DownloadJobStatus.EXPORTING },
                progress = 0f,
                currentTarget = target,
                status = "Adicionado na fila",
                log = "",
                savedFiles = emptyList(),
                downloadJobs = listOf(newJob) + it.downloadJobs
            )
        }
    }

    fun markJobRunning(jobId: String) {
        _uiState.update { state ->
            val updated = state.downloadJobs.map { job ->
                if (job.id == jobId) {
                    job.copy(status = DownloadJobStatus.RUNNING, statusText = "Iniciando download...")
                } else {
                    job
                }
            }
            state.copy(
                isDownloading = updated.any { it.status == DownloadJobStatus.RUNNING || it.status == DownloadJobStatus.EXPORTING },
                progress = overallRunningProgress(updated),
                downloadJobs = updated
            )
        }
    }

    fun downloadProgress(jobId: String, progress: Float, status: String, logLine: String) {
        _uiState.update {
            val nextLog = buildString {
                if (it.log.isNotBlank()) {
                    append(it.log)
                    append('\n')
                }
                append(logLine)
            }.takeLast(MAX_LOG_CHARS)

            val updatedJobs = it.downloadJobs.map { job ->
                if (job.id == jobId) {
                    val stableProgress = progress.coerceIn(0f, 1f).coerceAtLeast(job.progress)
                    job.copy(
                        progress = stableProgress,
                        statusText = status,
                        log = buildString {
                            if (job.log.isNotBlank()) {
                                append(job.log)
                                append('\n')
                            }
                            append(logLine)
                        }.takeLast(MAX_LOG_CHARS)
                    )
                } else {
                    job
                }
            }

            it.copy(
                isDownloading = updatedJobs.any { job -> job.status == DownloadJobStatus.RUNNING || job.status == DownloadJobStatus.EXPORTING },
                progress = overallRunningProgress(updatedJobs),
                status = status,
                log = nextLog,
                downloadJobs = updatedJobs
            )
        }
    }

    fun downloadFinished(jobId: String, savedFiles: List<String>, log: String) {
        _uiState.update {
            val updatedJobs = it.downloadJobs.map { job ->
                if (job.id == jobId) {
                    job.copy(
                        status = DownloadJobStatus.COMPLETED,
                        progress = 1f,
                        statusText = "Download concluído",
                        log = log,
                        savedFiles = savedFiles
                    )
                } else {
                    job
                }
            }
            it.copy(
                isDownloading = updatedJobs.any { job -> job.status == DownloadJobStatus.RUNNING || job.status == DownloadJobStatus.EXPORTING },
                progress = if (updatedJobs.any { job -> job.status == DownloadJobStatus.RUNNING || job.status == DownloadJobStatus.EXPORTING }) {
                    overallRunningProgress(updatedJobs)
                } else {
                    1f
                },
                status = if (savedFiles.isEmpty()) {
                    "Download concluído, mas nenhum arquivo foi exportado"
                } else {
                    "Download concluído"
                },
                log = log,
                savedFiles = savedFiles,
                downloadJobs = updatedJobs
            )
        }
    }

    fun markJobExporting(jobId: String) {
        _uiState.update { state ->
            val updatedJobs = state.downloadJobs.map { job ->
                if (job.id == jobId) {
                    job.copy(
                        status = DownloadJobStatus.EXPORTING,
                        progress = job.progress.coerceAtLeast(0.99f),
                        statusText = "Exportando arquivo..."
                    )
                } else {
                    job
                }
            }
            state.copy(
                isDownloading = updatedJobs.any { job -> job.status == DownloadJobStatus.RUNNING || job.status == DownloadJobStatus.EXPORTING },
                progress = overallRunningProgress(updatedJobs),
                status = "Exportando arquivo...",
                downloadJobs = updatedJobs
            )
        }
    }

    fun downloadFailed(jobId: String?, message: String) {
        _uiState.update {
            val updatedJobs = it.downloadJobs.map { job ->
                if (job.id == jobId) {
                    job.copy(
                        status = DownloadJobStatus.FAILED,
                        statusText = "Falha no download",
                        log = message
                    )
                } else {
                    job
                }
            }
            it.copy(
                isDownloading = updatedJobs.any { job -> job.status == DownloadJobStatus.RUNNING || job.status == DownloadJobStatus.EXPORTING },
                progress = overallRunningProgress(updatedJobs),
                status = "Falha no download",
                log = message,
                downloadJobs = updatedJobs
            )
        }
    }

    fun retryJob(jobId: String) {
        _uiState.update { state ->
            val updated = state.downloadJobs.map { job ->
                if (job.id == jobId) {
                    job.copy(
                        status = DownloadJobStatus.QUEUED,
                        progress = 0f,
                        statusText = "Na fila..."
                    )
                } else {
                    job
                }
            }
            state.copy(
                isDownloading = updated.any { it.status == DownloadJobStatus.RUNNING || it.status == DownloadJobStatus.EXPORTING },
                progress = overallRunningProgress(updated),
                downloadJobs = updated
            )
        }
    }

    fun cancelJob(jobId: String) {
        _uiState.update { state ->
            val updated = state.downloadJobs.filterNot { it.id == jobId }
            state.copy(
                isDownloading = updated.any { it.status == DownloadJobStatus.RUNNING || it.status == DownloadJobStatus.EXPORTING },
                progress = overallRunningProgress(updated),
                downloadJobs = updated
            )
        }
    }

    private fun overallRunningProgress(jobs: List<DownloadJobItem>): Float {
        val running = jobs.filter { it.status == DownloadJobStatus.RUNNING || it.status == DownloadJobStatus.EXPORTING }
        if (running.isEmpty()) return 0f
        return running.map { it.progress }.average().toFloat().coerceIn(0f, 1f)
    }
}
