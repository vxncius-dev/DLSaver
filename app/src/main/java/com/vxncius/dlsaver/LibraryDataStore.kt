package com.vxncius.dlsaver

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PlaybackStats(
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L
)

object LibraryDataStore {
    private const val PREFS_NAME = "dlsaver_library_data"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_PLAYBACK_STATS = "playback_stats"

    fun isFavorite(context: Context, item: ExistingDownloadItem): Boolean {
        return item.libraryKey() in favorites(context)
    }

    fun setFavorite(context: Context, item: ExistingDownloadItem, favorite: Boolean) {
        val next = if (favorite) {
            favorites(context) + item.libraryKey()
        } else {
            favorites(context) - item.libraryKey()
        }
        context.preferences().edit()
            .putString(KEY_FAVORITES, JSONArray(next.toList()).toString())
            .apply()
    }

    fun favoriteKeys(context: Context): Set<String> = favorites(context)

    fun recordPlaybackStart(context: Context, item: ExistingDownloadItem) {
        val prefs = context.preferences()
        val root = JSONObject(prefs.getString(KEY_PLAYBACK_STATS, "{}").orEmpty().ifBlank { "{}" })
        val key = item.libraryKey()
        val current = root.optJSONObject(key) ?: JSONObject()
        val playCount = current.optInt("playCount", 0) + 1
        root.put(
            key,
            JSONObject()
                .put("name", item.name)
                .put("kind", item.kind.name)
                .put("playCount", playCount)
                .put("lastPlayedAt", System.currentTimeMillis())
        )
        prefs.edit().putString(KEY_PLAYBACK_STATS, root.toString()).apply()
    }

    fun playbackStats(context: Context, item: ExistingDownloadItem): PlaybackStats {
        val root = JSONObject(context.preferences().getString(KEY_PLAYBACK_STATS, "{}").orEmpty().ifBlank { "{}" })
        val json = root.optJSONObject(item.libraryKey()) ?: return PlaybackStats()
        return PlaybackStats(
            playCount = json.optInt("playCount", 0),
            lastPlayedAt = json.optLong("lastPlayedAt", 0L)
        )
    }

    private fun favorites(context: Context): Set<String> {
        val raw = context.preferences().getString(KEY_FAVORITES, "[]").orEmpty()
        val array = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        return buildSet {
            for (index in 0 until array.length()) {
                array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }

    private fun ExistingDownloadItem.libraryKey(): String {
        return sourceUrl.ifBlank { "${kind.name}:$name" }
    }

    private fun Context.preferences() = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
