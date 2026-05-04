package com.vxncius.dlsaver

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class SavedPlaybackQueueItem(
    val uri: Uri,
    val title: String,
    val artist: String,
    val mimeType: String,
    val kind: DownloadKind
)

data class SavedPlaybackSession(
    val uri: Uri,
    val title: String,
    val artist: String,
    val mimeType: String,
    val kind: DownloadKind,
    val positionMs: Long,
    val savedAtMs: Long,
    val queue: List<SavedPlaybackQueueItem> = emptyList(),
    val currentIndex: Int = 0
)

object PlaybackSessionStore {
    private const val PREFS_NAME = "dlsaver_playback_session"
    private const val KEY_SESSION = "session"
    private const val MAX_SESSION_AGE_MS = 24L * 60L * 60L * 1000L

    fun save(context: Context, state: PlaybackUiState) {
        val uri = state.mediaUri ?: return
        val kind = if (state.isVideo) DownloadKind.VIDEO else DownloadKind.AUDIO
        save(
            context = context,
            state = state,
            queue = listOf(
                SavedPlaybackQueueItem(
                    uri = uri,
                    title = state.title,
                    artist = state.artist,
                    mimeType = if (kind == DownloadKind.VIDEO) "video/*" else "audio/*",
                    kind = kind
                )
            ),
            currentIndex = 0
        )
    }

    fun save(
        context: Context,
        state: PlaybackUiState,
        queue: List<SavedPlaybackQueueItem>,
        currentIndex: Int
    ) {
        val uri = state.mediaUri ?: return
        val kind = if (state.isVideo) DownloadKind.VIDEO else DownloadKind.AUDIO
        val safeQueue = queue.ifEmpty {
            listOf(
                SavedPlaybackQueueItem(
                    uri = uri,
                    title = state.title,
                    artist = state.artist,
                    mimeType = if (kind == DownloadKind.VIDEO) "video/*" else "audio/*",
                    kind = kind
                )
            )
        }
        val json = JSONObject().apply {
            put("uri", uri.toString())
            put("title", state.title)
            put("artist", state.artist)
            put("mimeType", if (kind == DownloadKind.VIDEO) "video/*" else "audio/*")
            put("kind", kind.name)
            put("positionMs", state.positionMs.coerceAtLeast(0L))
            put("savedAtMs", System.currentTimeMillis())
            put("currentIndex", currentIndex.coerceIn(0, (safeQueue.size - 1).coerceAtLeast(0)))
            put(
                "queue",
                JSONArray().apply {
                    safeQueue.forEach { item ->
                        put(JSONObject().apply {
                            put("uri", item.uri.toString())
                            put("title", item.title)
                            put("artist", item.artist)
                            put("mimeType", item.mimeType)
                            put("kind", item.kind.name)
                        })
                    }
                }
            )
        }
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SESSION, json.toString())
            .apply()
    }

    fun load(context: Context): SavedPlaybackSession? {
        val raw = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SESSION, null)
            ?: return null
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val savedAt = json.optLong("savedAtMs", 0L)
        if (savedAt <= 0L || System.currentTimeMillis() - savedAt > MAX_SESSION_AGE_MS) {
            clear(context)
            return null
        }
        val uri = json.optString("uri").takeIf { it.isNotBlank() }?.let(Uri::parse) ?: return null
        val fallbackKind = runCatching { DownloadKind.valueOf(json.optString("kind")) }.getOrDefault(DownloadKind.AUDIO)
        val fallbackItem = SavedPlaybackQueueItem(
            uri = uri,
            title = json.optString("title").ifBlank { "Midia" },
            artist = json.optString("artist"),
            mimeType = json.optString("mimeType"),
            kind = fallbackKind
        )
        val queue = buildList {
            json.optJSONArray("queue")?.let { array ->
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val itemUri = item.optString("uri").takeIf { it.isNotBlank() }?.let(Uri::parse) ?: continue
                    add(
                        SavedPlaybackQueueItem(
                            uri = itemUri,
                            title = item.optString("title").ifBlank { "Midia" },
                            artist = item.optString("artist"),
                            mimeType = item.optString("mimeType"),
                            kind = runCatching {
                                DownloadKind.valueOf(item.optString("kind"))
                            }.getOrDefault(fallbackKind)
                        )
                    )
                }
            }
            if (isEmpty()) add(fallbackItem)
        }
        val restoredIndex = json.optInt("currentIndex", queue.indexOfFirst { it.uri == uri })
            .coerceIn(0, (queue.size - 1).coerceAtLeast(0))
        return SavedPlaybackSession(
            uri = uri,
            title = json.optString("title").ifBlank { "Midia" },
            artist = json.optString("artist"),
            mimeType = json.optString("mimeType"),
            kind = fallbackKind,
            positionMs = json.optLong("positionMs", 0L).coerceAtLeast(0L),
            savedAtMs = savedAt,
            queue = queue,
            currentIndex = restoredIndex
        )
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SESSION)
            .apply()
    }
}
