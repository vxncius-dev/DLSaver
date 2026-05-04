package com.vxncius.dlsaver

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File

object MediaStoreExporter {
    fun exportDirectoryToDownloads(context: Context, sourceDir: File): List<String> {
        if (!sourceDir.exists()) return emptyList()

        val files = sourceDir.walkTopDown()
            .filter { it.isFile }
            .filter { it.extension.lowercase() in exportableExtensions }
            .sortedBy { it.name.lowercase() }
            .toList()

        if (files.isEmpty()) return emptyList()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val existingNames = queryExistingDownloadNames(context).toMutableSet()
            files.map { file ->
                exportToMediaStore(context, file, reserveTargetName(file.name, existingNames))
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            val targetDir = File(downloadsDir, "DLSaver").apply { mkdirs() }
            val existingNames = targetDir.listFiles()
                .orEmpty()
                .filter { it.isFile }
                .map { it.name }
                .toMutableSet()
            files.map { file ->
                exportLegacy(file, targetDir, reserveTargetName(file.name, existingNames))
            }
        }
    }

    private fun exportToMediaStore(context: Context, file: File, targetName: String): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, targetName)
            put(MediaStore.Downloads.MIME_TYPE, guessMime(file))
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DLSaver")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Não foi possível criar item no MediaStore")

        return runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Não foi possível abrir destino no MediaStore")

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            targetName
        }.getOrElse { error ->
            runCatching { resolver.delete(uri, null, null) }
            throw error
        }
    }

    private fun exportLegacy(file: File, targetDir: File, targetName: String): String {
        val targetFile = File(targetDir, targetName)
        file.inputStream().use { input ->
            targetFile.outputStream().use { output -> input.copyTo(output) }
        }
        return targetFile.name
    }

    private fun reserveTargetName(originalFileName: String, existingNames: MutableSet<String>): String {
        val extension = originalFileName.substringAfterLast('.', "")
        val baseName = sanitizeDownloadTitle(fileNameWithoutExtension(originalFileName))
        val sanitizedName = if (extension.isBlank() || originalFileName.lastIndexOf('.') <= 0) {
            baseName
        } else {
            "$baseName.$extension"
        }
        val uniqueName = nextUniqueFileName(sanitizedName, existingNames)
        existingNames += uniqueName
        return uniqueName
    }

    private fun queryExistingDownloadNames(context: Context): Set<String> {
        val result = mutableSetOf<String>()
        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Downloads.DISPLAY_NAME),
            "${MediaStore.Downloads.RELATIVE_PATH}=?",
            arrayOf(Environment.DIRECTORY_DOWNLOADS + "/DLSaver/"),
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                result += cursor.getString(nameIndex)
            }
        }
        return result
    }

    private fun guessMime(file: File): String {
        val extension = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
    }

    private val exportableExtensions = setOf(
        "mp3", "m4a", "aac", "wav", "ogg", "opus", "flac", "webm",
        "mp4", "mkv", "mov", "avi"
    )
}
