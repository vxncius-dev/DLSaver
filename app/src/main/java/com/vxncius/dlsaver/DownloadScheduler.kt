package com.vxncius.dlsaver

import android.content.Context
import androidx.core.content.ContextCompat.startForegroundService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object DownloadScheduler {
    private const val MAX_CONCURRENT_DOWNLOADS = 4
    private val mutex = Mutex()

    suspend fun kick(context: Context) {
        mutex.withLock {
            val state = DownloadStateStore.uiState.value
            val requestedLimit = state.simultaneousDownloadsLimit.coerceIn(1, 4)
            val limit = minOf(requestedLimit, MAX_CONCURRENT_DOWNLOADS)
            val runningCount = state.downloadJobs.count {
                it.status == DownloadJobStatus.RUNNING || it.status == DownloadJobStatus.EXPORTING
            }
            if (runningCount >= limit) return

            val toStart = state.downloadJobs
                .filter { it.status == DownloadJobStatus.QUEUED }
                .take(limit - runningCount)

            if (toStart.isEmpty()) return

            toStart.forEach { job ->
                DownloadStateStore.markJobRunning(job.id)
                DownloadJobPersistence.upsertJob(
                    context,
                    job.copy(status = DownloadJobStatus.RUNNING, statusText = "Iniciando download...")
                )
                startForegroundService(
                    context,
                    DownloadForegroundService.createIntent(
                        context = context,
                        jobId = job.id,
                        url = job.sourceUrl,
                        title = job.title,
                        thumbnailUrl = job.thumbnailUrl,
                        kind = job.kind
                    )
                )
            }
        }
    }
}
