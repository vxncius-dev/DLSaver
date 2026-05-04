package com.vxncius.dlsaver

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.text.DateFormat
import java.util.Date

object DownloadFileActions {
    fun open(context: Context, item: ExistingDownloadItem): Boolean {
        val targetUri = openableUri(context, item) ?: return false
        val mimeType = mimeType(context, item)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(targetUri, mimeType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrElse { error ->
            if (error is ActivityNotFoundException) false else false
        }
    }

    fun share(context: Context, item: ExistingDownloadItem): Boolean {
        val targetUri = openableUri(context, item) ?: return false
        val mimeType = mimeType(context, item)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, targetUri)
            clipData = android.content.ClipData.newUri(context.contentResolver, item.name, targetUri)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {
            context.startActivity(Intent.createChooser(intent, "Compartilhar").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }

    fun share(context: Context, items: List<ExistingDownloadItem>): Boolean {
        val targets = items.mapNotNull { item -> openableUri(context, item)?.let { uri -> item to uri } }
        if (targets.isEmpty()) return false
        if (targets.size == 1) return share(context, targets.first().first)

        val uris = ArrayList(targets.map { it.second })
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = commonMimeType(context, items)
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            clipData = android.content.ClipData.newUri(context.contentResolver, targets.first().first.name, targets.first().second).apply {
                targets.drop(1).forEach { (item, uri) ->
                    addItem(android.content.ClipData.Item(uri))
                }
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {
            context.startActivity(Intent.createChooser(intent, "Compartilhar").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }

    fun details(context: Context, item: ExistingDownloadItem): LocalDownloadDetails {
        val rawUri = item.sourceUrl.toUriOrNull()
        return when (rawUri?.scheme) {
            "content" -> {
                val projection = arrayOf(
                    android.provider.MediaStore.MediaColumns.DISPLAY_NAME,
                    android.provider.MediaStore.MediaColumns.SIZE,
                    android.provider.MediaStore.MediaColumns.MIME_TYPE
                )
                context.contentResolver.query(rawUri, projection, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.SIZE)
                    val mimeIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.MIME_TYPE)
                    if (cursor.moveToFirst()) {
                        return LocalDownloadDetails(
                            name = cursor.getString(nameIndex).orEmpty().ifBlank { item.name },
                            mimeType = cursor.getString(mimeIndex).orEmpty(),
                            sizeBytes = if (cursor.isNull(sizeIndex)) 0L else cursor.getLong(sizeIndex).coerceAtLeast(0L),
                            modifiedAt = item.modifiedAt,
                            location = rawUri.toString()
                        )
                    }
                }
                LocalDownloadDetails(
                    name = item.name,
                    mimeType = mimeType(context, item),
                    sizeBytes = 0L,
                    modifiedAt = item.modifiedAt,
                    location = rawUri.toString()
                )
            }

            "file" -> {
                val file = rawUri.path?.let(::File)
                LocalDownloadDetails(
                    name = item.name,
                    mimeType = mimeType(context, item),
                    sizeBytes = file?.length()?.coerceAtLeast(0L) ?: 0L,
                    modifiedAt = item.modifiedAt,
                    location = file?.absolutePath.orEmpty()
                )
            }

            else -> LocalDownloadDetails(
                name = item.name,
                mimeType = mimeType(context, item),
                sizeBytes = 0L,
                modifiedAt = item.modifiedAt,
                location = item.sourceUrl
            )
        }
    }

    fun formatDetails(details: LocalDownloadDetails): String {
        return buildString {
            appendLine("Nome: ${details.name}")
            appendLine("Tipo: ${details.mimeType.ifBlank { "desconhecido" }}")
            appendLine("Tamanho: ${formatSize(details.sizeBytes)}")
            if (details.modifiedAt > 0L) {
                appendLine("Modificado: ${DateFormat.getDateTimeInstance().format(Date(details.modifiedAt))}")
            }
        }
    }

    private fun openableUri(context: Context, item: ExistingDownloadItem): Uri? {
        val rawUri = item.sourceUrl.toUriOrNull() ?: return null
        return when (rawUri.scheme) {
            "content" -> rawUri
            "file" -> rawUri.path?.let { path ->
                runCatching {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(path)
                    )
                }.getOrNull()
            }

            else -> null
        }
    }

    fun rename(context: Context, item: ExistingDownloadItem, newName: String): Boolean {
        val cleaned = newName.trim()
        if (cleaned.isBlank() || cleaned == item.name) return false

        val uri = item.sourceUrl.toUriOrNull() ?: return false
        return when (uri.scheme) {
            "content" -> {
                val values = ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, cleaned)
                }
                context.contentResolver.update(uri, values, null, null) > 0
            }

            "file" -> {
                val sourceFile = uri.path?.let(::File) ?: return false
                val targetFile = File(sourceFile.parentFile, cleaned)
                if (targetFile.exists()) return false
                sourceFile.renameTo(targetFile)
            }

            else -> false
        }
    }

    fun delete(context: Context, item: ExistingDownloadItem): Boolean {
        val uri = item.sourceUrl.toUriOrNull() ?: return false
        return when (uri.scheme) {
            "content" -> deleteContentUri(context, uri, item.name)
            "file" -> uri.path?.let { safeDeleteFile(File(it)) } == true
            else -> false
        }
    }

    fun delete(context: Context, items: List<ExistingDownloadItem>): Int {
        return items.count { delete(context, it) }
    }

    private fun deleteContentUri(context: Context, uri: Uri, displayName: String): Boolean {
        val resolver = context.contentResolver
        val directDeleted = runCatching { resolver.delete(uri, null, null) > 0 }
            .getOrElse { error ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestScopedDelete(context, uri, error)
                } else {
                    false
                }
            }
        if (directDeleted) return true

        if (deleteResolvedFilePath(context, uri)) {
            runCatching { resolver.delete(uri, null, null) }
            return true
        }

        val refreshedUri = findDownloadUriByName(context, displayName)
            ?: return requestScopedDelete(context, uri, IllegalStateException("MediaStore item not refreshed"))
        if (refreshedUri == uri) {
            return requestScopedDelete(context, uri, IllegalStateException("MediaStore delete returned 0"))
        }
        return runCatching { resolver.delete(refreshedUri, null, null) > 0 }
            .getOrElse { error ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestScopedDelete(context, refreshedUri, error)
                } else {
                    false
                }
            } || requestScopedDelete(context, refreshedUri, IllegalStateException("MediaStore delete returned 0"))
    }

    private fun safeDeleteFile(file: File): Boolean {
        if (!file.exists()) return true
        return runCatching { file.delete() || !file.exists() }.getOrDefault(false)
    }

    private fun requestScopedDelete(context: Context, uri: Uri, error: Throwable): Boolean {
        val activity = context.findActivity() ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return runCatching {
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                activity.startIntentSenderForResult(
                    pendingIntent.intentSender,
                    0,
                    null,
                    0,
                    0,
                    0
                )
                true
            }.getOrDefault(false)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && error is android.app.RecoverableSecurityException) {
            return runCatching {
                activity.startIntentSenderForResult(
                    error.userAction.actionIntent.intentSender,
                    0,
                    null,
                    0,
                    0,
                    0
                )
                true
            }.getOrDefault(false)
        }
        return false
    }

    private fun deleteResolvedFilePath(context: Context, uri: Uri): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) return false
        val path = queryFilePath(context, uri) ?: return false
        val file = File(path)
        if (!file.exists()) return false
        return safeDeleteFile(file)
    }

    private fun queryFilePath(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        return runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun findDownloadUriByName(context: Context, displayName: String): Uri? {
        if (displayName.isBlank() || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH}=? AND ${MediaStore.Files.FileColumns.DISPLAY_NAME}=?"
        val selectionArgs = arrayOf(android.os.Environment.DIRECTORY_DOWNLOADS + "/DLSaver/", displayName)
        return context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            if (cursor.moveToFirst()) {
                android.content.ContentUris.withAppendedId(collection, cursor.getLong(idIndex))
            } else {
                null
            }
        }
    }

    private fun mimeType(context: Context, item: ExistingDownloadItem): String {
        val uri = item.sourceUrl.toUriOrNull()
        val contentMime = if (uri?.scheme == "content") {
            context.contentResolver.getType(uri).orEmpty()
        } else {
            ""
        }
        if (contentMime.isNotBlank()) return contentMime
        return when (item.kind) {
            DownloadKind.AUDIO -> "audio/*"
            DownloadKind.VIDEO -> "video/*"
        }
    }

    private fun commonMimeType(context: Context, items: List<ExistingDownloadItem>): String {
        val mimes = items.map { mimeType(context, it) }
        return when {
            mimes.all { it.startsWith("audio/") } -> "audio/*"
            mimes.all { it.startsWith("video/") } -> "video/*"
            else -> "*/*"
        }
    }

    private fun formatSize(sizeBytes: Long): String {
        if (sizeBytes <= 0L) return "desconhecido"
        val units = listOf("B", "KB", "MB", "GB")
        var value = sizeBytes.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.lastIndex) {
            value /= 1024
            unitIndex++
        }
        return if (unitIndex == 0) {
            "${value.toLong()} ${units[unitIndex]}"
        } else {
            String.format("%.1f %s", value, units[unitIndex])
        }
    }
}

data class LocalDownloadDetails(
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val modifiedAt: Long,
    val location: String
)

private fun String.toUriOrNull(): Uri? = runCatching { Uri.parse(this) }.getOrNull()

private fun Context.findActivity(): android.app.Activity? {
    var current: Context = this
    while (true) {
        when (current) {
            is android.app.Activity -> return current
            is android.content.ContextWrapper -> current = current.baseContext
            else -> return null
        }
    }
}
