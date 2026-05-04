package com.vxncius.dlsaver

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object DownloadJobPersistence {
    private const val PREFS_NAME = "dlsaver_job_history"
    private const val KEY_JOBS = "jobs"
    private const val KEY_FAILED_JOBS = "failed_jobs"

    fun loadJobs(context: Context): List<DownloadJobItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_JOBS, null)
            ?: prefs.getString(KEY_FAILED_JOBS, "[]")
            .orEmpty()
        val json = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        return buildList {
            for (index in 0 until json.length()) {
                val item = json.optJSONObject(index) ?: continue
                val job = item.toDownloadJobItem()
                if (job.id.isNotBlank() && job.sourceUrl.isNotBlank()) {
                    add(
                        when (job.status) {
                            DownloadJobStatus.RUNNING,
                            DownloadJobStatus.EXPORTING -> job.copy(
                                status = DownloadJobStatus.QUEUED,
                                progress = 0f,
                                statusText = "Na fila..."
                            )
                            else -> job
                        }
                    )
                }
            }
        }
    }

    fun loadFailedJobs(context: Context): List<DownloadJobItem> {
        return loadJobs(context).filter { it.status == DownloadJobStatus.FAILED }
    }

    fun saveJobs(context: Context, jobs: List<DownloadJobItem>) {
        val persistent = jobs
            .filter { it.status != DownloadJobStatus.COMPLETED }
            .distinctBy { it.id }
            .take(MAX_PERSISTED_JOBS)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_JOBS, persistent.toJsonString())
            .putString(KEY_FAILED_JOBS, persistent.filter { it.status == DownloadJobStatus.FAILED }.toJsonString())
            .apply()
    }

    fun upsertJob(context: Context, job: DownloadJobItem) {
        val current = loadJobs(context)
            .filterNot { it.id == job.id }
            .toMutableList()
        current.add(0, job)
        saveJobs(context, current)
    }

    fun upsertFailedJob(context: Context, job: DownloadJobItem) {
        upsertJob(context, job.copy(status = DownloadJobStatus.FAILED))
    }

    fun removeJob(context: Context, jobId: String) {
        val next = loadJobs(context).filterNot { it.id == jobId }
        saveJobs(context, next)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_JOBS)
            .remove(KEY_FAILED_JOBS)
            .apply()
    }

    private fun List<DownloadJobItem>.toJsonString(): String {
        return JSONArray().apply {
            forEach { put(it.toJson()) }
        }.toString()
    }

    private fun DownloadJobItem.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("sourceUrl", sourceUrl)
            put("thumbnailUrl", thumbnailUrl)
            put("kind", kind.name)
            put("status", status.name)
            put("progress", progress.toDouble())
            put("statusText", statusText)
            put("log", log)
            put("savedFiles", JSONArray(savedFiles))
        }
    }

    private fun JSONObject.toDownloadJobItem(): DownloadJobItem {
        val savedFilesJson = optJSONArray("savedFiles") ?: JSONArray()
        val savedFiles = buildList {
            for (index in 0 until savedFilesJson.length()) {
                add(savedFilesJson.optString(index))
            }
        }
        return DownloadJobItem(
            id = optString("id"),
            title = optString("title"),
            sourceUrl = optString("sourceUrl"),
            thumbnailUrl = optString("thumbnailUrl"),
            kind = runCatching { DownloadKind.valueOf(optString("kind")) }.getOrDefault(DownloadKind.VIDEO),
            status = runCatching { DownloadJobStatus.valueOf(optString("status")) }.getOrDefault(DownloadJobStatus.FAILED),
            progress = optDouble("progress", 0.0).toFloat(),
            statusText = optString("statusText").ifBlank { "Falha no download" },
            log = optString("log"),
            savedFiles = savedFiles
        )
    }

    private const val MAX_PERSISTED_JOBS = 80
}
