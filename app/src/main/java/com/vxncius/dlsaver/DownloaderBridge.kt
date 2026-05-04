package com.vxncius.dlsaver

import com.chaquo.python.Python
import org.json.JSONArray
import org.json.JSONObject

object DownloaderBridge {
    fun search(query: String, page: Int): Pair<List<SearchResultItem>, Boolean> {
        val module = Python.getInstance().getModule("downloader")
        val resultJson = module.callAttr("search", query, page).toString()
        val json = JSONObject(resultJson)
        val items = json.getJSONArray("items").toSearchResults()
        return items to json.getBoolean("has_more")
    }

    fun inspectUrl(url: String): SearchResultItem {
        val module = Python.getInstance().getModule("downloader")
        val resultJson = module.callAttr("inspect_url", url).toString()
        return JSONObject(resultJson).toSearchResult()
    }

    fun listPlaylist(url: String, page: Int, pageSize: Int = 20): PlaylistPage {
        val module = Python.getInstance().getModule("downloader")
        val resultJson = module.callAttr("list_playlist", url, page, pageSize).toString()
        val json = JSONObject(resultJson)
        val title = json.optString("title").ifBlank { "Playlist" }
        val items = (json.optJSONArray("items") ?: JSONArray()).toSearchResults()
        val hasMore = json.optBoolean("has_more", false)
        return PlaylistPage(title = title, items = items, hasMore = hasMore)
    }

    fun download(
        url: String,
        tempDir: String,
        ytDlpPath: String,
        ffmpegPath: String,
        aria2cPath: String,
        audioOnly: Boolean,
        callback: PythonProgressCallback
    ): DownloadResult {
        val module = Python.getInstance().getModule("downloader")
        val resultJson = module.callAttr(
            "run_download",
            url,
            tempDir,
            ytDlpPath,
            ffmpegPath,
            aria2cPath,
            audioOnly,
            callback
        ).toString()

        val json = JSONObject(resultJson)
        return DownloadResult(
            success = json.getBoolean("success"),
            exitCode = json.getInt("exit_code"),
            log = json.getString("log"),
            tempDir = json.getString("temp_dir"),
            files = json.getJSONArray("files").toStringList()
        )
    }
}

private fun JSONArray.toSearchResults(): List<SearchResultItem> {
    return buildList {
        for (index in 0 until length()) {
            add(getJSONObject(index).toSearchResult())
        }
    }
}

private fun JSONObject.toSearchResult(): SearchResultItem {
    return SearchResultItem(
        id = optString("id"),
        title = optString("title"),
        author = optString("author"),
        resultType = optString("result_type").ifBlank { optString("resultType") },
        extra = optString("extra").ifBlank {
            optString("duration").ifBlank { optString("views") }
        },
        url = optString("url"),
        thumbnailUrl = optString("thumbnail_url").ifBlank {
            optString("thumbnail_src").ifBlank { optString("thumbnail") }
        }
    )
}

private fun JSONArray.toStringList(): List<String> {
    return buildList {
        for (index in 0 until length()) {
            add(getString(index))
        }
    }
}
