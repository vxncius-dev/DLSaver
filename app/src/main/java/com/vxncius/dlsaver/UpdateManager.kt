package com.vxncius.dlsaver

import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class UpdateRelease(
    val versionCode: Int,
    val versionName: String,
    val apkPath: String,
    val apkUrl: String = "",
    val notes: String = "",
    val publishedAt: String = "",
    val sha256: String = "",
    val sizeBytes: Long = 0L,
    val abi: String = "",
    val abis: List<String> = emptyList()
)

data class UpdateManifest(
    val appId: String,
    val latest: UpdateRelease,
    val releases: List<UpdateRelease>
)

data class UpdateUiState(
    val checking: Boolean = false,
    val downloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val manifest: UpdateManifest? = null,
    val lastCheckedAt: Long = 0L,
    val message: String = "",
    val error: String = ""
)

sealed class UpdateInstallResult {
    data object InstalledIntentLaunched : UpdateInstallResult()
    data object RequiresInstallPermission : UpdateInstallResult()
    data class Failed(val reason: String) : UpdateInstallResult()
}

object UpdateManager {
    private const val BUCKET = "dlsaver.firebasestorage.app"
    private const val MANIFEST_URL = "https://raw.githubusercontent.com/vxncius-dev/DLSaver/main/releases/android/manifest.json"
    private const val PREFS_NAME = "dlsaver_update_state"
    private const val KEY_LAST_NOTIFIED_VERSION = "last_notified_version"
    private const val POLL_INTERVAL_MS = 60_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var pollingJob: Job? = null

    fun start(context: Context) {
        if (pollingJob != null) return
        pollingJob = scope.launch {
            refresh(context.applicationContext)
        }.also { job ->
            job.invokeOnCompletion {
                pollingJob = null
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    suspend fun refresh(context: Context) {
        val appContext = context.applicationContext
        _state.update { it.copy(checking = true, error = "") }
        val manifest = runCatching {
            withContext(Dispatchers.IO) {
                fetchManifest()
            }
        }.getOrElse { error ->
            _state.update {
                it.copy(
                    checking = false,
                    lastCheckedAt = System.currentTimeMillis(),
                    error = error.message.orEmpty().ifBlank { error::class.java.simpleName }
                )
            }
            return
        }

        _state.update {
            it.copy(
                checking = false,
                manifest = manifest,
                lastCheckedAt = System.currentTimeMillis(),
                error = ""
            )
        }

        maybeNotifyUpdate(appContext, manifest.latest)
    }

    suspend fun downloadAndInstall(context: Context, release: UpdateRelease): UpdateInstallResult {
        if (!canRequestInstallPackages(context)) {
            return UpdateInstallResult.RequiresInstallPermission
        }

        val appContext = context.applicationContext
        val targetFile = File(appContext.cacheDir, "updates/${release.versionCode}/DLSaver-${release.versionName}.apk")
        targetFile.parentFile?.mkdirs()

        _state.update {
            it.copy(
                downloading = true,
                downloadProgress = 0f,
                message = "Baixando ${release.versionName}..."
            )
        }

        return runCatching {
            withContext(Dispatchers.IO) {
                downloadToFile(
                    sourceUrl = release.downloadUrl(),
                    targetFile = targetFile,
                    onProgress = { progress ->
                        _state.update { state ->
                            state.copy(downloadProgress = progress, message = "Baixando ${release.versionName}... ${(progress * 100).toInt()}%")
                        }
                    }
                )
                validateChecksum(targetFile, release.sha256)
            }
            withContext(Dispatchers.Main) {
                launchInstallIntent(appContext, targetFile)
            }
            _state.update {
                it.copy(
                    downloading = false,
                    downloadProgress = 1f,
                    message = "Instalador aberto para ${release.versionName}"
                )
            }
            UpdateInstallResult.InstalledIntentLaunched
        }.getOrElse { error ->
            _state.update {
                it.copy(
                    downloading = false,
                    downloadProgress = 0f,
                    error = error.message.orEmpty().ifBlank { error::class.java.simpleName },
                    message = ""
                )
            }
            UpdateInstallResult.Failed(error.message.orEmpty().ifBlank { "Falha ao atualizar" })
        }
    }

    private fun fetchManifest(): UpdateManifest {
        val body = fetchText(MANIFEST_URL).trim()
        val (appId, releases) = parseManifest(body)
        val latest = releases.maxByOrNull { it.versionCode }
            ?: error("Manifesto sem releases")
        return UpdateManifest(
            appId = appId,
            latest = latest,
            releases = releases.sortedByDescending { it.versionCode }
        )
    }

    private fun parseManifest(body: String): Pair<String, List<UpdateRelease>> {
        val releases = mutableListOf<UpdateRelease>()
        var appId = BuildConfig.APPLICATION_ID

        if (body.startsWith("[")) {
            val array = JSONArray(body)
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                parseRelease(item)?.let(releases::add)
            }
        } else {
            val root = JSONObject(body)
            appId = root.optString("appId").ifBlank { BuildConfig.APPLICATION_ID }
            val array = root.optJSONArray("releases")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    parseRelease(item)?.let(releases::add)
                }
            }
            if (releases.isEmpty()) {
                root.optJSONObject("latest")?.let { parseRelease(it)?.let(releases::add) }
                root.optJSONObject("release")?.let { parseRelease(it)?.let(releases::add) }
                if (releases.isEmpty()) {
                    parseRelease(root)?.let(releases::add)
                }
            }
        }

        return appId to releases.distinctBy { it.versionCode }
    }

    private fun parseRelease(json: JSONObject): UpdateRelease? {
        val versionCode = json.optInt("versionCode", 0)
        val versionName = json.optString("versionName").ifBlank { versionCode.takeIf { it > 0 }?.toString().orEmpty() }
        val apkPath = json.optString("apkPath").ifBlank { json.optString("apk") }
        val apkUrl = json.optString("apkUrl")
        if (versionCode <= 0 || versionName.isBlank() || apkPath.isBlank() && apkUrl.isBlank()) return null
        return UpdateRelease(
            versionCode = versionCode,
            versionName = versionName,
            apkPath = apkPath,
            apkUrl = apkUrl,
            notes = json.optString("notes"),
            publishedAt = json.optString("publishedAt"),
            sha256 = json.optString("sha256"),
            sizeBytes = json.optLong("sizeBytes", 0L),
            abi = json.optString("abi"),
            abis = json.optJSONArray("abis")?.toStringList().orEmpty()
        )
    }

    private fun maybeNotifyUpdate(context: Context, release: UpdateRelease) {
        if (release.versionCode <= BuildConfig.VERSION_CODE) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastNotified = prefs.getInt(KEY_LAST_NOTIFIED_VERSION, -1)
        if (lastNotified == release.versionCode) return

        val openIntent = PendingIntent.getActivity(
            context,
            0,
            MainActivity.createOpenScreenIntent(context, AppScreen.SETTINGS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        NotificationHelper.showUpdateAvailable(
            context = context,
            title = "Nova versão do DLSaver",
            content = "v${release.versionName} disponível para instalação",
            openIntent = openIntent
        )

        prefs.edit().putInt(KEY_LAST_NOTIFIED_VERSION, release.versionCode).apply()
        _state.update { it.copy(message = "Nova versão ${release.versionName} disponível") }
    }

    private fun launchInstallIntent(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = uri
            setDataAndType(uri, "application/vnd.android.package-archive")
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    private fun canRequestInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    private fun storageObjectUrl(path: String): String {
        val encodedPath = Uri.encode(path)
        return "https://firebasestorage.googleapis.com/v0/b/$BUCKET/o/$encodedPath?alt=media"
    }

    private fun UpdateRelease.downloadUrl(): String {
        val directUrl = apkUrl.ifBlank { apkPath }
        return if (directUrl.startsWith("http://") || directUrl.startsWith("https://")) directUrl else storageObjectUrl(directUrl)
    }

    private fun fetchText(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error("Falha ao ler manifesto: HTTP $code")
            text
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadToFile(
        sourceUrl: String,
        targetFile: File,
        onProgress: (Float) -> Unit
    ) {
        val connection = (URL(sourceUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 30_000
            readTimeout = 60_000
            setRequestProperty("User-Agent", "DLSaver/${BuildConfig.VERSION_NAME}")
        }
        targetFile.parentFile?.mkdirs()
        try {
            val total = connection.contentLengthLong.coerceAtLeast(0L)
            if (connection.responseCode !in 200..299) {
                error("Falha no download: HTTP ${connection.responseCode}")
            }
            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead <= 0) break
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        if (total > 0L) {
                            onProgress((downloaded.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f))
                        }
                    }
                }
            }
            onProgress(1f)
        } finally {
            connection.disconnect()
        }
    }

    private fun validateChecksum(file: File, expectedSha256: String) {
        val expected = expectedSha256.trim()
        if (expected.isBlank()) return
        val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            .joinToString("") { byte -> "%02x".format(byte) }
        if (!digest.equals(expected, ignoreCase = true)) {
            file.delete()
            error("Checksum invalido para o APK")
        }
    }

    private fun JSONObject.optString(key: String, defaultValue: String = ""): String {
        return optString(key).takeIf { it.isNotBlank() } ?: defaultValue
    }

    private fun JSONArray.toStringList(): List<String> {
        return buildList {
            for (index in 0 until length()) {
                optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }
}
