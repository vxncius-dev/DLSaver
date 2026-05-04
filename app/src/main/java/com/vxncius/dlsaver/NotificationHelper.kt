package com.vxncius.dlsaver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    const val CHANNEL_ID = "downloads_progress_v3_silent"
    const val UPDATE_CHANNEL_ID = "updates_v1_silent"
    private const val GROUP_KEY_ACTIVE = "com.vxncius.dlsaver.downloads.active"
    private const val GROUP_KEY_HISTORY = "com.vxncius.dlsaver.downloads.history"
    private const val SUMMARY_NOTIFICATION_ID = 999

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.download_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.download_channel_description)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun ensureUpdateChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            UPDATE_CHANNEL_ID,
            "Atualizações",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Avisos silenciosos sobre novas versões do aplicativo"
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun notificationIdFor(jobId: String): Int {
        return 1000 + (jobId.hashCode().ushr(1) % 10_000)
    }

    fun activeNotificationIdFor(jobId: String): Int = notificationIdFor(jobId)

    fun historyNotificationIdFor(jobId: String): Int = 50_000 + notificationIdFor(jobId)

    fun buildProgressNotification(
        context: Context,
        title: String,
        status: String,
        progress: Int,
        thumbnailUrl: String,
        contentIntent: PendingIntent
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(status)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setOngoing(true)
            .setProgress(100, progress.coerceIn(0, 100), false)
            .setSubText(null)
            .setGroup(GROUP_KEY_ACTIVE)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun buildCompletedNotification(
        context: Context,
        title: String,
        contentIntent: PendingIntent
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText("Download concluído")
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setOngoing(false)
            .setProgress(0, 0, false)
            .setGroup(GROUP_KEY_HISTORY)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .build()
    }

    fun buildFailedNotification(
        context: Context,
        title: String,
        contentIntent: PendingIntent
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText("Falha no download")
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setOngoing(false)
            .setProgress(0, 0, false)
            .setGroup(GROUP_KEY_HISTORY)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .build()
    }

    fun buildUpdateAvailableNotification(
        context: Context,
        title: String,
        content: String,
        contentIntent: PendingIntent
    ): Notification {
        return NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setOngoing(false)
            .setGroup(GROUP_KEY_HISTORY)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .build()
    }

    fun showSummary(context: Context) {
        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle("Downloads")
            .setContentText("Historico de downloads")
            .setSilent(true)
            .setGroup(GROUP_KEY_HISTORY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(SUMMARY_NOTIFICATION_ID, summaryNotification)
    }

    fun show(context: Context, notificationId: Int, notification: Notification) {
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun showUpdateAvailable(
        context: Context,
        title: String,
        content: String,
        openIntent: PendingIntent
    ) {
        show(
            context = context,
            notificationId = 900_001,
            notification = buildUpdateAvailableNotification(
                context = context,
                title = title,
                content = content,
                contentIntent = openIntent
            )
        )
    }

    fun cancel(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}
