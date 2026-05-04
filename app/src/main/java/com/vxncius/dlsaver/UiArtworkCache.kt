package com.vxncius.dlsaver

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

object UiArtworkCache {
    private const val maxSidePx = 1024

    private val cache = object : LruCache<String, ImageBitmap>(10 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            return ((value.width * value.height * 4) / 1024).coerceAtLeast(1)
        }
    }

    suspend fun loadEmbeddedArtwork(context: Context, mediaUri: Uri): ImageBitmap? {
        val key = mediaUri.toString()
        cache.get(key)?.let { return it }

        val bitmap = runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, mediaUri)
                val bytes = retriever.embeddedPicture ?: return@runCatching null
                decodeSampledBitmap(bytes, maxSidePx, maxSidePx)?.asImageBitmap()
            } finally {
                retriever.release()
            }
        }.getOrNull() ?: return null

        cache.put(key, bitmap)
        return bitmap
    }

    fun trimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            cache.evictAll()
        } else if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            cache.trimToSize(3 * 1024)
        }
    }

    fun clear() {
        cache.evictAll()
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
}
