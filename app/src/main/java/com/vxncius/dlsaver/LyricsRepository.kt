package com.vxncius.dlsaver

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.Normalizer
import kotlin.math.abs

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class LyricsPayload(
    val syncedLines: List<LyricLine> = emptyList(),
    val plainLyrics: String = ""
) {
    val hasLyrics: Boolean
        get() = syncedLines.isNotEmpty() || plainLyrics.isNotBlank()
}

data class LyricsSearchChoice(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val albumName: String,
    val durationSeconds: Long,
    val payload: LyricsPayload
)

object LyricsRepository {
    private const val baseUrl = "https://lrclib.net/api/get"
    private const val searchUrl = "https://lrclib.net/api/search"
    private const val userAgent = "DLSaver/1.0.7"

    suspend fun load(
        context: Context,
        rawTitle: String,
        rawArtist: String,
        displayTitle: String,
        displayArtist: String,
        durationMs: Long
    ): LyricsPayload? {
        val primaryTitle = displayTitle.ifBlank { rawTitle }.trim()
        if (primaryTitle.isBlank()) return null

        val cacheFile = cacheFileFor(
            context = context,
            rawTitle = rawTitle,
            rawArtist = rawArtist,
            displayTitle = displayTitle,
            displayArtist = displayArtist,
            durationMs = durationMs
        )
        readCache(cacheFile)?.let { return it }

        val remote = fetchRemote(
            rawTitle = rawTitle,
            rawArtist = rawArtist,
            displayTitle = displayTitle,
            displayArtist = displayArtist,
            durationMs = durationMs
        ) ?: return null
        writeCache(cacheFile, remote)
        return remote
    }

    suspend fun searchManual(query: String, durationMs: Long): List<LyricsSearchChoice> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
            val targetDurationSeconds = (durationMs / 1000L).coerceAtLeast(0L)
            val candidates = buildCandidates(
                rawTitle = cleanQuery,
                rawArtist = "",
                displayTitle = cleanQuery,
                displayArtist = ""
            )
            val durations = buildList {
                if (targetDurationSeconds > 0L) add(targetDurationSeconds)
                add(0L)
            }.distinct()
            val choices = linkedMapOf<String, Pair<LyricsSearchChoice, Int>>()

            for (candidate in candidates) {
                for (durationSeconds in durations) {
                    val url = buildString {
                        append(searchUrl)
                        append("?track_name=").append(Uri.encode(candidate.trackName))
                        if (candidate.artistName.isNotBlank()) {
                            append("&artist_name=").append(Uri.encode(candidate.artistName))
                        }
                        if (durationSeconds > 0L) {
                            append("&duration=").append(durationSeconds)
                        }
                    }
                    collectManualSearchChoices(
                        response = requestJsonArray(url),
                        candidate = candidate,
                        targetDurationSeconds = targetDurationSeconds,
                        output = choices
                    )
                }
            }

            for (durationSeconds in durations) {
                val url = buildString {
                    append(searchUrl)
                    append("?track_name=").append(Uri.encode(cleanQuery))
                    if (durationSeconds > 0L) {
                        append("&duration=").append(durationSeconds)
                    }
                }
                collectManualSearchChoices(
                    response = requestJsonArray(url),
                    candidate = LyricsCandidate(cleanQuery, ""),
                    targetDurationSeconds = targetDurationSeconds,
                    output = choices
                )
            }

            val artistOnlyUrl = buildString {
                append(searchUrl)
                append("?artist_name=").append(Uri.encode(cleanQuery))
            }
            collectManualSearchChoices(
                response = requestJsonArray(artistOnlyUrl),
                candidate = LyricsCandidate(cleanQuery, cleanQuery),
                targetDurationSeconds = targetDurationSeconds,
                output = choices
            )

            choices.values
                .sortedWith(compareByDescending<Pair<LyricsSearchChoice, Int>> { it.second }
                    .thenBy { it.first.trackName.lowercase() })
                .map { it.first }
        }
    }

    private fun collectManualSearchChoices(
        response: JSONArray?,
        candidate: LyricsCandidate,
        targetDurationSeconds: Long,
        output: MutableMap<String, Pair<LyricsSearchChoice, Int>>
    ) {
        if (response == null) return
        for (index in 0 until response.length()) {
            val item = response.optJSONObject(index) ?: continue
            val payload = responseToPayload(item) ?: continue
            val choice = LyricsSearchChoice(
                id = item.optLong("id", index.toLong()),
                trackName = item.optString("trackName").ifBlank { item.optString("name") },
                artistName = item.optString("artistName"),
                albumName = item.optString("albumName"),
                durationSeconds = item.optLong("duration", 0L),
                payload = payload
            )
            val key = if (choice.id > 0L) {
                "id:${choice.id}"
            } else {
                listOf(choice.trackName, choice.artistName, choice.durationSeconds.toString())
                    .joinToString("|") { normalizeForCompare(it) }
            }
            val score = scoreSearchResult(item, candidate, targetDurationSeconds)
            val current = output[key]
            if (current == null || score > current.second) {
                output[key] = choice to score
            }
        }
    }

    suspend fun saveManual(
        context: Context,
        rawTitle: String,
        rawArtist: String,
        displayTitle: String,
        displayArtist: String,
        durationMs: Long,
        payload: LyricsPayload
    ) {
        val cacheFile = cacheFileFor(
            context = context,
            rawTitle = rawTitle,
            rawArtist = rawArtist,
            displayTitle = displayTitle,
            displayArtist = displayArtist,
            durationMs = durationMs
        )
        writeCache(cacheFile, payload)
    }

    private suspend fun fetchRemote(
        rawTitle: String,
        rawArtist: String,
        displayTitle: String,
        displayArtist: String,
        durationMs: Long
    ): LyricsPayload? {
        return withContext(Dispatchers.IO) {
            val candidates = buildCandidates(
                rawTitle = rawTitle,
                rawArtist = rawArtist,
                displayTitle = displayTitle,
                displayArtist = displayArtist
            )

            val durations = buildList {
                if (durationMs > 0L) add((durationMs / 1000L).coerceAtLeast(1L))
                add(0L)
            }.distinct()

            for (candidate in candidates) {
                for (durationSeconds in durations) {
                    val payload = requestLyrics(candidate, durationSeconds)
                    if (payload != null) return@withContext payload
                }
            }

            val targetDurationSeconds = (durationMs / 1000L).coerceAtLeast(0L)
            val searched = searchLyrics(candidates, targetDurationSeconds)
            if (searched != null) {
                return@withContext searched
            }

            null
        }
    }

    private fun requestLyrics(candidate: LyricsCandidate, durationSeconds: Long): LyricsPayload? {
        val url = buildString {
            append(baseUrl)
            append("?track_name=").append(Uri.encode(candidate.trackName))
            if (candidate.artistName.isNotBlank()) {
                append("&artist_name=").append(Uri.encode(candidate.artistName))
            }
            if (durationSeconds > 0L) {
                append("&duration=").append(durationSeconds)
            }
        }

        val response = requestJson(url) ?: return null
        return responseToPayload(response)
    }

    private fun searchLyrics(
        candidates: List<LyricsCandidate>,
        targetDurationSeconds: Long
    ): LyricsPayload? {
        var bestResult: SearchLyricsResult? = null

        candidates.forEach { candidate ->
            val url = buildString {
                append(searchUrl)
                append("?track_name=").append(Uri.encode(candidate.trackName))
                if (candidate.artistName.isNotBlank()) {
                    append("&artist_name=").append(Uri.encode(candidate.artistName))
                }
                if (targetDurationSeconds > 0L) {
                    append("&duration=").append(targetDurationSeconds)
                }
            }

            val response = requestJsonArray(url) ?: return@forEach
            for (index in 0 until response.length()) {
                val item = response.optJSONObject(index) ?: continue
                val payload = responseToPayload(item) ?: continue
                val result = SearchLyricsResult(
                    payload = payload,
                    score = scoreSearchResult(item, candidate, targetDurationSeconds)
                )
                if (bestResult == null || result.score > bestResult!!.score) {
                    bestResult = result
                }
            }
        }

        return bestResult?.payload
    }

    private fun requestJson(url: String): JSONObject? {
        return requestText(url)?.let { body ->
            runCatching { JSONObject(body) }.getOrNull()
        }
    }

    private fun requestJsonArray(url: String): JSONArray? {
        return requestText(url)?.let { body ->
            runCatching { JSONArray(body) }.getOrNull()
        }
    }

    private fun requestText(url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7_000
            readTimeout = 7_000
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "application/json")
        }

        return try {
            val code = connection.responseCode
            if (code !in 200..299) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun readCache(file: File): LyricsPayload? {
        if (!file.exists()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val json = JSONObject(file.readText())
                val linesJson = json.optJSONArray("syncedLines") ?: JSONArray()
                val lines = buildList {
                    for (i in 0 until linesJson.length()) {
                        val item = linesJson.optJSONObject(i) ?: continue
                        val text = item.optString("text").trim()
                        val timeMs = item.optLong("timeMs")
                        if (text.isNotBlank()) {
                            add(LyricLine(timeMs = timeMs, text = text))
                        }
                    }
                }
                LyricsPayload(
                    syncedLines = lines,
                    plainLyrics = json.optString("plainLyrics").trim()
                ).takeIf { it.hasLyrics }
            }.getOrNull()
        }
    }

    private suspend fun writeCache(file: File, payload: LyricsPayload) {
        withContext(Dispatchers.IO) {
            runCatching {
                file.parentFile?.mkdirs()
                val json = JSONObject().apply {
                    put("plainLyrics", payload.plainLyrics)
                    put("syncedLines", JSONArray().apply {
                        payload.syncedLines.forEach { line ->
                            put(JSONObject().apply {
                                put("timeMs", line.timeMs)
                                put("text", line.text)
                            })
                        }
                    })
                }
                file.writeText(json.toString())
            }
        }
    }

    private fun cacheFileFor(
        context: Context,
        rawTitle: String,
        rawArtist: String,
        displayTitle: String,
        displayArtist: String,
        durationMs: Long
    ): File {
        val key = listOf(
            rawTitle.trim(),
            rawArtist.trim(),
            displayTitle.trim(),
            displayArtist.trim(),
            durationMs.toString()
        ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-1").digest(key.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
        return File(File(context.filesDir, ".lyrics"), "$digest.json")
    }

    private fun responseToPayload(json: JSONObject): LyricsPayload? {
        val payload = LyricsPayload(
            syncedLines = parseLrc(json.optString("syncedLyrics")),
            plainLyrics = json.optString("plainLyrics").trim()
        )
        return payload.takeIf { it.hasLyrics }
    }

    private fun buildCandidates(
        rawTitle: String,
        rawArtist: String,
        displayTitle: String,
        displayArtist: String
    ): List<LyricsCandidate> {
        val variants = linkedSetOf<LyricsCandidate>()

        fun addCandidate(track: String, artist: String) {
            val cleanTrack = cleanTrackCandidate(track)
            val cleanArtist = cleanArtistCandidate(artist)
            if (cleanTrack.isBlank()) return
            variants += LyricsCandidate(cleanTrack, cleanArtist)
        }

        fun addSplitCandidates(value: String, fallbackArtist: String) {
            splitLooseArtistTitle(value).forEach { (left, right) ->
                val resolvedArtist = fallbackArtist.ifBlank { left }
                addCandidate(right, resolvedArtist)
                addCandidate(right, left)
                addCandidate(left, right)
            }
        }

        addCandidate(displayTitle, displayArtist)
        addCandidate(rawTitle, rawArtist)
        if (displayTitle.isNotBlank() && displayArtist.isNotBlank()) {
            addCandidate(displayArtist, displayTitle)
        }
        if (rawTitle.isNotBlank() && rawArtist.isNotBlank()) {
            addCandidate(rawArtist, rawTitle)
        }

        addSplitCandidates(rawTitle, rawArtist)
        addSplitCandidates(displayTitle, displayArtist)
        addSplitCandidates(rawArtist, rawTitle)
        addSplitCandidates(displayArtist, displayTitle)

        val expanded = variants.toList().flatMap { candidate ->
            buildList {
                add(candidate)
                val strippedTrack = stripDecorators(candidate.trackName)
                val strippedArtist = stripDecorators(candidate.artistName)
                if (strippedTrack != candidate.trackName || strippedArtist != candidate.artistName) {
                    add(LyricsCandidate(strippedTrack, strippedArtist))
                }
                if (candidate.artistName.isNotBlank()) {
                    add(LyricsCandidate(candidate.trackName, ""))
                    if (strippedTrack != candidate.trackName) {
                        add(LyricsCandidate(strippedTrack, ""))
                    }
                }
            }
        }

        return expanded
            .map { LyricsCandidate(it.trackName.trim(), it.artistName.trim()) }
            .filter { it.trackName.isNotBlank() }
            .distinct()
    }

    private fun cleanTrackCandidate(value: String): String {
        return stripDecorators(
            value
                .substringBeforeLast('.', value)
                .replace(Regex("""[_]+"""), " ")
                .replace(Regex("""\s{2,}"""), " ")
                .trim()
        )
    }

    private fun cleanArtistCandidate(value: String): String {
        return stripDecorators(
            value
                .replace(Regex("""[_]+"""), " ")
                .replace(Regex("""\s{2,}"""), " ")
                .trim()
        )
    }

    private fun stripDecorators(value: String): String {
        if (value.isBlank()) return ""
        return value
            .replace(Regex("""\[[^\]]*]"""), " ")
            .replace(Regex("""\((?i)(official|audio|video|lyrics?|lyric video|visualizer|remaster(ed)?|live|ao vivo|feat\.?|ft\.?|prod\.?|from .*?)\)"""), " ")
            .replace(Regex("""(?i)\b(official|audio|video|lyrics?|lyric video|visualizer|remaster(ed)?|live|ao vivo|feat\.?|ft\.?|prod\.?)\b"""), " ")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
            .trim('-', '_', '.', ' ')
    }

    private fun splitPossibleArtistTitle(value: String): List<Pair<String, String>> {
        val text = value.trim()
        if (text.isBlank()) return emptyList()

        val delimiters = listOf(" - ", " – ", " — ", " | ", " / ")
        return delimiters.mapNotNull { delimiter ->
            val idx = text.indexOf(delimiter)
            if (idx <= 0) return@mapNotNull null
            val left = text.substring(0, idx).trim()
            val right = text.substring(idx + delimiter.length).trim()
            if (left.isBlank() || right.isBlank()) return@mapNotNull null
            left to right
        }
    }

    private fun splitLooseArtistTitle(value: String): List<Pair<String, String>> {
        val text = value.trim()
        if (text.isBlank()) return emptyList()

        val delimiters = listOf(" - ", " – ", " — ", " | ", " / ")
        return delimiters.mapNotNull { delimiter ->
            val idx = text.indexOf(delimiter)
            if (idx <= 0) return@mapNotNull null
            val left = text.substring(0, idx).trim()
            val right = text.substring(idx + delimiter.length).trim()
            if (left.isBlank() || right.isBlank()) return@mapNotNull null
            left to right
        }
    }

    private fun scoreSearchResult(
        json: JSONObject,
        candidate: LyricsCandidate,
        targetDurationSeconds: Long
    ): Int {
        val track = normalizeForCompare(json.optString("trackName").ifBlank { json.optString("name") })
        val artist = normalizeForCompare(json.optString("artistName"))
        val candidateTrack = normalizeForCompare(candidate.trackName)
        val candidateArtist = normalizeForCompare(candidate.artistName)
        val duration = json.optLong("duration")

        var score = 0
        if (track == candidateTrack) score += 120
        else if (track.contains(candidateTrack) || candidateTrack.contains(track)) score += 70

        if (candidateArtist.isNotBlank()) {
            if (artist == candidateArtist) score += 90
            else if (artist.contains(candidateArtist) || candidateArtist.contains(artist)) score += 45
        }

        if (targetDurationSeconds > 0L && duration > 0L) {
            val diff = abs(duration - targetDurationSeconds)
            score += when {
                diff <= 2L -> 40
                diff <= 5L -> 20
                diff <= 10L -> 8
                else -> 0
            }
        }
        return score
    }

    private fun normalizeForCompare(value: String): String {
        return Normalizer.normalize(stripDecorators(value).lowercase(), Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")
            .replace(Regex("""[^\p{L}\p{Nd}]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun parseLrc(raw: String): List<LyricLine> {
        if (raw.isBlank()) return emptyList()
        val timeRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?]""")
        return buildList {
            raw.lineSequence().forEach { line ->
                val matches = timeRegex.findAll(line).toList()
                if (matches.isEmpty()) return@forEach
                val text = timeRegex.replace(line, "").trim()
                if (text.isBlank()) return@forEach

                matches.forEach { match ->
                    val minutes = match.groupValues[1].toLongOrNull() ?: return@forEach
                    val seconds = match.groupValues[2].toLongOrNull() ?: return@forEach
                    val fractionRaw = match.groupValues[3]
                    val millis = when (fractionRaw.length) {
                        0 -> 0L
                        1 -> fractionRaw.toLongOrNull()?.times(100L) ?: 0L
                        2 -> fractionRaw.toLongOrNull()?.times(10L) ?: 0L
                        else -> fractionRaw.take(3).toLongOrNull() ?: 0L
                    }
                    add(LyricLine(timeMs = minutes * 60_000L + seconds * 1_000L + millis, text = text))
                }
            }
        }.sortedBy { it.timeMs }
    }
}

private data class LyricsCandidate(
    val trackName: String,
    val artistName: String
)

private data class SearchLyricsResult(
    val payload: LyricsPayload,
    val score: Int
)
