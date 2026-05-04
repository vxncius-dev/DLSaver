package com.vxncius.dlsaver

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class DLSaverApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        NotificationHelper.ensureUpdateChannel(this)
        if (!isMainProcess()) return
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        DownloadStateStore.setBinaries(BinaryInstaller.installBundledBinaries(this))
        DownloadsLibraryObserver.start(this)
        UpdateManager.start(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        MediaPlayback.trimMemory(level)
        UiArtworkCache.trimMemory(level)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        MediaPlayback.trimMemory(android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
        UiArtworkCache.clear()
    }

    private fun isMainProcess(): Boolean {
        val currentProcess = currentProcessName() ?: return true
        return currentProcess == packageName
    }

    private fun currentProcessName(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName()
        }

        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return null
        val currentPid = Process.myPid()
        return activityManager.runningAppProcesses
            ?.firstOrNull { it.pid == currentPid }
            ?.processName
    }
}
