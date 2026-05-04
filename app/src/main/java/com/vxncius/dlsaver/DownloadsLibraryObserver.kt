package com.vxncius.dlsaver

import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Environment
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object DownloadsLibraryObserver {
    private var contentObserver: ContentObserver? = null
    private var fileObserver: FileObserver? = null
    private var refreshJob: Job? = null
    private var refreshScope: CoroutineScope? = null

    fun start(context: Context) {
        stop(context)
        val appContext = context.applicationContext
        refreshScope = CoroutineScope(Dispatchers.Main.immediate)
        refresh(appContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    scheduleRefresh(appContext)
                }

                override fun onChange(selfChange: Boolean, uri: android.net.Uri?) {
                    scheduleRefresh(appContext)
                }
            }
            appContext.contentResolver.registerContentObserver(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
            contentObserver = observer
        } else {
            val targetDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "DLSaver"
            ).apply { mkdirs() }
            val observer = object : FileObserver(targetDir.absolutePath, ALL_EVENTS) {
                override fun onEvent(event: Int, path: String?) {
                    if (event and (CREATE or MOVED_TO or CLOSE_WRITE or DELETE or MOVED_FROM or DELETE_SELF) != 0) {
                        scheduleRefresh(appContext)
                    }
                }
            }
            observer.startWatching()
            fileObserver = observer
        }
    }

    fun stop(context: Context) {
        contentObserver?.let {
            context.applicationContext.contentResolver.unregisterContentObserver(it)
        }
        contentObserver = null
        fileObserver?.stopWatching()
        fileObserver = null
        refreshJob?.cancel()
        refreshJob = null
        refreshScope?.cancel()
        refreshScope = null
    }

    private fun refresh(context: Context) {
        refreshJob?.cancel()
        refreshJob = refreshScope?.launch {
            val items = withContext(Dispatchers.IO) {
                val downloads = DownloadsLibrary.queryAppDownloads(context)
                LocalThumbnailStore.sync(context, downloads)
            }
            DownloadStateStore.setExistingDownloads(items)
        }
    }

    private fun scheduleRefresh(context: Context) {
        refreshJob?.cancel()
        refreshJob = refreshScope?.launch {
            delay(350)
            refresh(context)
        }
    }
}
