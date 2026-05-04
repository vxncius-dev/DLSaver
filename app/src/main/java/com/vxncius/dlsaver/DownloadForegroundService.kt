package com.vxncius.dlsaver

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class DownloadForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL_JOB) {
            val cancelJobId = intent.getStringExtra(EXTRA_JOB_ID).orEmpty()
            cancelledJobs += cancelJobId
            runningJobs.remove(cancelJobId)?.cancel()
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val jobId = intent?.getStringExtra(EXTRA_JOB_ID).orEmpty()
        val url = intent?.getStringExtra(EXTRA_URL).orEmpty()
        val title = intent?.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "DLSaver" }
        val thumbnailUrl = intent?.getStringExtra(EXTRA_THUMBNAIL).orEmpty()
        val kind = intent?.getStringExtra(EXTRA_KIND)?.let { DownloadKind.valueOf(it) } ?: DownloadKind.VIDEO
        val activeNotificationId = NotificationHelper.activeNotificationIdFor(jobId)
        val historyNotificationId = NotificationHelper.historyNotificationIdFor(jobId)
        val contentIntent = PendingIntent.getActivity(
            this,
            activeNotificationId,
            MainActivity.createOpenScreenIntent(
                context = this,
                screen = AppScreen.DOWNLOADED_OR_PROGRESS_LIST_ITEMS
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (url.isBlank() || jobId.isBlank()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        cancelledJobs.remove(jobId)

        startForeground(
            activeNotificationId,
            NotificationHelper.buildProgressNotification(
                context = this,
                title = title,
                status = "Preparando download",
                progress = 0,
                thumbnailUrl = thumbnailUrl,
                contentIntent = contentIntent
            )
        )
        NotificationHelper.show(
            this,
            activeNotificationId,
            NotificationHelper.buildProgressNotification(
                context = this,
                title = title,
                status = "Preparando download",
                progress = 0,
                thumbnailUrl = thumbnailUrl,
                contentIntent = contentIntent
            )
        )

        val runningJob = serviceScope.launch {
            val binaries = BinaryInstaller.installBundledBinaries(this@DownloadForegroundService)
            val tempDir = File(binaries.tempRoot, "job_$jobId").apply {
                deleteRecursively()
                mkdirs()
            }
            var lastUiProgress = -1
            var lastStableProgress = 0f
            var lastUiStatus = ""
            var lastUiUpdateAt = 0L

            DownloadStateStore.setBinaries(binaries)

            val callback = PythonProgressCallback { progress, status, logLine ->
                val stableProgress = progress.coerceIn(0f, 1f).coerceAtLeast(lastStableProgress)
                lastStableProgress = stableProgress
                val progressPercent = (stableProgress * 100).toInt().coerceIn(0, 100)
                val cleanedStatus = sanitizeProgressStatus(status)
                val now = System.currentTimeMillis()
                val progressAdvanced =
                    lastUiProgress < 0 ||
                        progressPercent == 100 ||
                        progressPercent - lastUiProgress >= 1
                val enoughTimePassed = now - lastUiUpdateAt >= 500L
                val statusChanged = cleanedStatus != lastUiStatus
                val shouldPushUi =
                    (progressPercent != lastUiProgress && (progressAdvanced || enoughTimePassed)) ||
                        (statusChanged && enoughTimePassed)
                if (shouldPushUi) {
                    val notification = NotificationHelper.buildProgressNotification(
                        context = this@DownloadForegroundService,
                        title = title,
                        status = cleanedStatus,
                        progress = progressPercent,
                        thumbnailUrl = thumbnailUrl,
                        contentIntent = contentIntent
                    )
                    NotificationHelper.show(this@DownloadForegroundService, activeNotificationId, notification)
                    DownloadStateStore.downloadProgress(jobId, stableProgress, cleanedStatus, logLine)
                    lastUiProgress = progressPercent
                    lastUiStatus = cleanedStatus
                    lastUiUpdateAt = now
                }
            }

            runCatching {
                DownloadRepository.download(
                    url = url,
                    tempDir = tempDir.absolutePath,
                    ytDlpPath = binaries.ytDlp,
                    ffmpegPath = binaries.ffmpeg,
                    aria2cPath = binaries.aria2c,
                    audioOnly = kind == DownloadKind.AUDIO,
                    callback = callback
                )
            }.onSuccess { result ->
                if (!isActive || jobId in cancelledJobs) {
                    DownloadStateStore.cancelJob(jobId)
                    DownloadJobPersistence.removeJob(this@DownloadForegroundService, jobId)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    NotificationHelper.cancel(this@DownloadForegroundService, activeNotificationId)
                    return@onSuccess
                }

                if (!result.success) {
                    DownloadStateStore.downloadFailed(jobId, result.log)
                    DownloadJobPersistence.upsertFailedJob(
                        this@DownloadForegroundService,
                        DownloadStateStore.uiState.value.downloadJobs.firstOrNull { it.id == jobId }
                            ?: DownloadJobItem(
                                id = jobId,
                                title = title,
                                sourceUrl = url,
                                thumbnailUrl = thumbnailUrl,
                                kind = kind,
                                status = DownloadJobStatus.FAILED,
                                statusText = "Falha no download",
                                log = result.log
                            )
                    )
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    NotificationHelper.cancel(this@DownloadForegroundService, activeNotificationId)
                    NotificationHelper.show(
                        this@DownloadForegroundService,
                        historyNotificationId,
                        NotificationHelper.buildFailedNotification(
                            context = this@DownloadForegroundService,
                            title = title,
                            contentIntent = contentIntent
                        )
                    )
                    NotificationHelper.showSummary(this@DownloadForegroundService)
                } else {
                    DownloadStateStore.markJobExporting(jobId)
                    DownloadStateStore.uiState.value.downloadJobs.firstOrNull { it.id == jobId }?.let { exportingJob ->
                        DownloadJobPersistence.upsertJob(this@DownloadForegroundService, exportingJob)
                    }
                    val exported = MediaStoreExporter.exportDirectoryToDownloads(
                        context = this@DownloadForegroundService,
                        sourceDir = File(result.tempDir)
                    )
                    ensureActive()
                    if (jobId in cancelledJobs) {
                        DownloadStateStore.cancelJob(jobId)
                        DownloadJobPersistence.removeJob(this@DownloadForegroundService, jobId)
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        NotificationHelper.cancel(this@DownloadForegroundService, activeNotificationId)
                        return@onSuccess
                    }
                    val downloads = LocalThumbnailStore.sync(
                        this@DownloadForegroundService,
                        DownloadsLibrary.queryAppDownloads(this@DownloadForegroundService),
                        maxItemsToProcess = Int.MAX_VALUE
                    )
                    DownloadStateStore.setExistingDownloads(downloads)
                    DownloadStateStore.downloadFinished(
                        jobId = jobId,
                        savedFiles = exported,
                        log = result.log
                    )
                    DownloadJobPersistence.removeJob(this@DownloadForegroundService, jobId)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    NotificationHelper.cancel(this@DownloadForegroundService, activeNotificationId)
                    NotificationHelper.show(
                        this@DownloadForegroundService,
                        historyNotificationId,
                        NotificationHelper.buildCompletedNotification(
                            context = this@DownloadForegroundService,
                            title = title,
                            contentIntent = contentIntent
                        )
                    )
                    NotificationHelper.showSummary(this@DownloadForegroundService)
                }
            }.onFailure { error ->
                val cancelled = error is kotlinx.coroutines.CancellationException
                if (cancelled) {
                    DownloadStateStore.cancelJob(jobId)
                    DownloadJobPersistence.removeJob(this@DownloadForegroundService, jobId)
                } else {
                    val message = error.stackTraceToString()
                    DownloadStateStore.downloadFailed(jobId, message)
                    DownloadJobPersistence.upsertFailedJob(
                        this@DownloadForegroundService,
                        DownloadStateStore.uiState.value.downloadJobs.firstOrNull { it.id == jobId }
                            ?: DownloadJobItem(
                                id = jobId,
                                title = title,
                                sourceUrl = url,
                                thumbnailUrl = thumbnailUrl,
                                kind = kind,
                                status = DownloadJobStatus.FAILED,
                                statusText = "Falha no download",
                                log = message
                            )
                    )
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                NotificationHelper.cancel(this@DownloadForegroundService, activeNotificationId)
                if (error !is kotlinx.coroutines.CancellationException) {
                    NotificationHelper.show(
                        this@DownloadForegroundService,
                        historyNotificationId,
                        NotificationHelper.buildFailedNotification(
                            context = this@DownloadForegroundService,
                            title = title,
                            contentIntent = contentIntent
                        )
                    )
                    NotificationHelper.showSummary(this@DownloadForegroundService)
                }
            }

            runCatching { tempDir.deleteRecursively() }
            runningJobs.remove(jobId)
            cancelledJobs.remove(jobId)
            DownloadJobPersistence.saveJobs(
                this@DownloadForegroundService,
                DownloadStateStore.uiState.value.downloadJobs
            )

            // Puxa o proximo item da fila, se existir.
            serviceScope.launch { DownloadScheduler.kick(this@DownloadForegroundService) }
            stopSelf(startId)
        }
        runningJobs[jobId] = runningJob

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val ACTION_CANCEL_JOB = "cancel_job"
        private const val EXTRA_JOB_ID = "extra_job_id"
        private const val EXTRA_URL = "extra_url"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_THUMBNAIL = "extra_thumbnail"
        private const val EXTRA_KIND = "extra_kind"
        private val runningJobs = ConcurrentHashMap<String, Job>()
        private val cancelledJobs = ConcurrentHashMap.newKeySet<String>()

        private fun sanitizeProgressStatus(status: String): String {
            val normalized = status.trim()
            if (normalized.isBlank()) return "Baixando..."
            val lower = normalized.lowercase()
            if ("warning" in lower || "aviso" in lower) return "Baixando..."
            return normalized
        }
 
        fun createIntent(
            context: Context,
            jobId: String,
            url: String,
            title: String,
            thumbnailUrl: String,
            kind: DownloadKind
        ): Intent {
            return Intent(context, DownloadForegroundService::class.java).apply {
                putExtra(EXTRA_JOB_ID, jobId)
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_THUMBNAIL, thumbnailUrl)
                putExtra(EXTRA_KIND, kind.name)
            }
        }

        fun createCancelIntent(
            context: Context,
            jobId: String
        ): Intent {
            return Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_CANCEL_JOB
                putExtra(EXTRA_JOB_ID, jobId)
            }
        }
    }
}

class PythonProgressCallback(
    private val onProgress: (Float, String, String) -> Unit
) {
    @Suppress("unused")
    fun onProgress(progress: Double, status: String, logLine: String) {
        onProgress(progress.toFloat().coerceIn(0f, 1f), status, logLine)
    }
}
