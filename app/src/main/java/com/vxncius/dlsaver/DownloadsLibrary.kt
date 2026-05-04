package com.vxncius.dlsaver

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object DownloadsLibrary {
    fun queryAppDownloads(context: Context): List<ExistingDownloadItem> {
        cleanupResidualArtifacts(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            queryMediaStore(context)
        } else {
            queryLegacy()
        }
    }

    private fun queryMediaStore(context: Context): List<ExistingDownloadItem> {
        val resolver = context.contentResolver
        val items = mutableListOf<ExistingDownloadItem>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.RELATIVE_PATH,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )
        val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH}=?"
        val selectionArgs = arrayOf(Environment.DIRECTORY_DOWNLOADS + "/DLSaver/")
        val collection = MediaStore.Files.getContentUri("external")

        resolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeTypeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val modifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex)
                val mimeType = cursor.getString(mimeTypeIndex).orEmpty()
                val modifiedAt = cursor.getLong(modifiedIndex) * 1000L
                val kind = mimeTypeToKind(mimeType, name)
                items += ExistingDownloadItem(
                    name = name,
                    sourceUrl = ContentUris.withAppendedId(collection, id).toString(),
                    modifiedAt = modifiedAt,
                    kind = kind,
                    mimeType = mimeType
                )
            }
        }

        return items.distinctBy { it.name }
    }

    private fun cleanupResidualArtifacts(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cleanupMediaStoreResiduals(context)
        }
        cleanupLegacyResiduals()
    }

    private fun cleanupMediaStoreResiduals(context: Context) {
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.IS_PENDING
        )
        val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH}=?"
        val selectionArgs = arrayOf(Environment.DIRECTORY_DOWNLOADS + "/DLSaver/")

        resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val modifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val pendingIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.IS_PENDING)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex).orEmpty()
                val modifiedAt = cursor.getLong(modifiedIndex) * 1000L
                val isPending = pendingIndex >= 0 && cursor.getInt(pendingIndex) == 1
                if (!isPending && !shouldDeleteResidual(name, modifiedAt)) continue
                if (isPending && !isExpiredPending(modifiedAt)) continue
                val itemUri = ContentUris.withAppendedId(collection, cursor.getLong(idIndex))
                runCatching { resolver.delete(itemUri, null, null) }
            }
        }
    }

    private fun cleanupLegacyResiduals() {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetDir = File(downloadsDir, "DLSaver")
        if (!targetDir.exists()) return

        targetDir.listFiles()
            .orEmpty()
            .filter { it.isFile && shouldDeleteResidual(it.name, it.lastModified()) }
            .forEach { file ->
                runCatching { file.delete() }
            }
    }

    private fun queryLegacy(): List<ExistingDownloadItem> {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetDir = File(downloadsDir, "DLSaver")
        if (!targetDir.exists()) return emptyList()

        return targetDir.listFiles()
            .orEmpty()
            .filter { it.isFile }
            .sortedByDescending { it.lastModified() }
            .map { file ->
                val mimeType = guessLegacyMime(file.name)
                val kind = mimeTypeToKind(mimeType, file.name)
                ExistingDownloadItem(
                    name = file.name,
                    sourceUrl = file.toURI().toString(),
                    modifiedAt = file.lastModified(),
                    kind = kind,
                    mimeType = mimeType
                )
            }
    }

    private fun mimeTypeToKind(mimeType: String, fileName: String): DownloadKind {
        // MediaStore pode rotular alguns audios (ex: opus em .webm) como video/webm.
        // Para o DLSaver, a extensao costuma ser mais confiavel para diferenciar audio/video.
        val ext = fileName.substringAfterLast('.', "").lowercase()
        when (ext) {
            "mp3", "m4a", "aac", "wav", "ogg", "opus", "flac", "webm" -> return DownloadKind.AUDIO
            "mp4", "mkv", "mov", "avi" -> return DownloadKind.VIDEO
        }

        val normalizedMime = mimeType.lowercase()
        if (normalizedMime.startsWith("audio/")) return DownloadKind.AUDIO
        if (normalizedMime.startsWith("video/")) return DownloadKind.VIDEO

        return DownloadKind.VIDEO
    }

    private fun guessLegacyMime(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "m4a" -> "audio/mp4"
            "mp3" -> "audio/mpeg"
            "aac" -> "audio/aac"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "opus" -> "audio/ogg"
            "flac" -> "audio/flac"
            "webm" -> "audio/webm"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            else -> ""
        }
    }

    private fun shouldDeleteResidual(fileName: String, modifiedAt: Long): Boolean {
        val lowerName = fileName.lowercase()
        val isResidual = lowerName.endsWith(".part") ||
            ".part-frag" in lowerName ||
            ".pending" in lowerName ||
            lowerName.endsWith(".ytdl") ||
            lowerName.endsWith(".temp")
        if (!isResidual) return false

        val ageMs = System.currentTimeMillis() - modifiedAt.coerceAtLeast(0L)
        return ageMs >= RESIDUAL_GRACE_PERIOD_MS
    }

    private fun isExpiredPending(modifiedAt: Long): Boolean {
        val ageMs = System.currentTimeMillis() - modifiedAt.coerceAtLeast(0L)
        return ageMs >= PENDING_GRACE_PERIOD_MS
    }

    private const val RESIDUAL_GRACE_PERIOD_MS = 12L * 60L * 60L * 1000L
    private const val PENDING_GRACE_PERIOD_MS = 2L * 60L * 60L * 1000L
}
