package com.vxncius.dlsaver

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.MessageDigest

object LocalThumbnailStore {
    private const val cacheThumbnailDirectory = ".thumbnails"
    private val syncMutex = Mutex()

    suspend fun sync(
        context: Context,
        items: List<ExistingDownloadItem>,
        maxItemsToProcess: Int = 30
    ): List<ExistingDownloadItem> {
        return syncMutex.withLock {
            syncAppCache(context, items, maxItemsToProcess)
        }
    }

    private fun syncAppCache(
        context: Context,
        items: List<ExistingDownloadItem>,
        maxItemsToProcess: Int
    ): List<ExistingDownloadItem> {
        val thumbDir = File(context.cacheDir, cacheThumbnailDirectory).apply { mkdirs() }
        val thumbItems = items.filter { it.sourceUrl.isNotBlank() }
        val expectedNames = thumbItems.associateBy { thumbnailFileNameFor(it.name) }

        thumbDir.listFiles()
            .orEmpty()
            .filter { it.isFile && it.name !in expectedNames.keys }
            .forEach { it.delete() }

        // Processa apenas os mais recentes por chamada para evitar picos de memoria.
        val processList = if (maxItemsToProcess > 0) {
            thumbItems.sortedByDescending { it.modifiedAt }.take(maxItemsToProcess)
        } else {
            emptyList()
        }

        val thumbnailUrls = mutableMapOf<String, String>()
        // Reaproveita thumbs ja existentes sem reprocessar tudo.
        thumbItems.forEach { item ->
            val targetFile = File(thumbDir, thumbnailFileNameFor(item.name))
            val shouldGenerate = !targetFile.exists() || targetFile.lastModified() < item.modifiedAt
            if (shouldGenerate && item in processList) {
                generateCacheThumb(context, item, targetFile)
            }
            if (targetFile.exists()) {
                thumbnailUrls[item.name] = targetFile.toURI().toString()
            }
        }

        return items.map { item ->
            item.copy(thumbnailUrl = thumbnailUrls[item.name].orEmpty())
        }
    }

    private fun generateCacheThumb(
        context: Context,
        item: ExistingDownloadItem,
        targetFile: File
    ) {
        val bitmap = thumbnailBitmap(context, item.sourceUrl) ?: return
        try {
            targetFile.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun thumbnailBitmap(context: Context, sourceUrl: String): Bitmap? {
        return runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                val uri = Uri.parse(sourceUrl)
                when (uri.scheme?.lowercase()) {
                    "content", "android.resource" -> retriever.setDataSource(context, uri)
                    "file" -> retriever.setDataSource(uri.path)
                    else -> retriever.setDataSource(sourceUrl)
                }

                val embedded = retriever.embeddedPicture
                if (embedded != null && embedded.isNotEmpty()) {
                    return@runCatching decodeSampledBitmap(embedded, 640, 640)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    retriever.getScaledFrameAtTime(
                        1_000_000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        640,
                        360
                    )
                } else {
                    retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.frameAtTime
                }
            } finally {
                retriever.release()
            }
        }.getOrNull()
    }

    private fun decodeSampledBitmap(bytes: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while ((bounds.outWidth / sampleSize) > reqWidth * 2 || (bounds.outHeight / sampleSize) > reqHeight * 2) {
            sampleSize *= 2
        }

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private fun thumbnailFileNameFor(fileName: String): String {
        return sha1(fileName.lowercase()) + ".jpg"
    }

    private fun sha1(value: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(value.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
