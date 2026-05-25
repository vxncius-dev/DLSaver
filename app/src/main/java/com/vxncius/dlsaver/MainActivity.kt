package com.vxncius.dlsaver

import android.Manifest
import android.content.ClipData
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.DisplayMetrics
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var requestedScreen by mutableStateOf<AppScreen?>(null)
    private var sharedText by mutableStateOf<String?>(null)
    private var openPlayerRequested by mutableStateOf(false)
    private var incomingMedia by mutableStateOf<IncomingMedia?>(null)

    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration).apply {
            fontScale = 1f
            densityDpi = DisplayMetrics.DENSITY_DEVICE_STABLE
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedScreen = extractRequestedScreen(intent)
        sharedText = extractSharedText(intent)
        openPlayerRequested = extractOpenPlayerRequest(intent)
        incomingMedia = extractIncomingMedia(intent)

        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(Color.BLACK),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(Color.BLACK)
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        setContent {
            val stableDensity = DisplayMetrics.DENSITY_DEVICE_STABLE / 160f
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = stableDensity,
                    fontScale = 1f
                )
            ) {
                DLSaverTheme {
                    val viewModel: MainViewModel = viewModel()
                    val state by viewModel.uiState.collectAsState()
                    var downloadPermissionGranted by remember { mutableStateOf(!viewModel.needsLegacyStoragePermission()) }
                    var notificationPermissionGranted by remember { mutableStateOf(viewModel.hasNotificationPermission()) }
                    var mediaPermissionsGranted by remember { mutableStateOf(viewModel.hasMediaReadPermissions()) }
                    var legacyStoragePermissionGranted by remember { mutableStateOf(viewModel.hasLegacyStoragePermission()) }
                    var installPackagesPermissionGranted by remember { mutableStateOf(canRequestInstallPackages()) }
                    var allFilesPermissionGranted by remember { mutableStateOf(canManageAllFiles()) }
                    fun refreshPermissionStates() {
                        downloadPermissionGranted = !viewModel.needsLegacyStoragePermission()
                        notificationPermissionGranted = viewModel.hasNotificationPermission()
                        mediaPermissionsGranted = viewModel.hasMediaReadPermissions()
                        legacyStoragePermissionGranted = viewModel.hasLegacyStoragePermission()
                        installPackagesPermissionGranted = canRequestInstallPackages()
                        allFilesPermissionGranted = canManageAllFiles()
                    }
                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { refreshPermissionStates() }
                    val mediaReadPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { grants ->
                        refreshPermissionStates()
                        if (grants.values.any { it }) {
                            viewModel.refreshExistingDownloads()
                        }
                    }
                    val storagePermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) {
                        refreshPermissionStates()
                        if (it) {
                            viewModel.refreshExistingDownloads()
                        }
                    }
                    var pendingScreen by remember { mutableStateOf(requestedScreen) }
                    var pendingSharedText by remember { mutableStateOf(sharedText) }
                    var pendingOpenPlayer by remember { mutableStateOf(openPlayerRequested) }
                    var pendingMedia by remember { mutableStateOf(incomingMedia) }
                    var pendingPlayerKind by remember { mutableStateOf<DownloadKind?>(null) }

                LaunchedEffect(Unit) {
                    MediaPlayback.prewarm(this@MainActivity)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    if (viewModel.needsLegacyStoragePermission()) {
                        storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !allFilesPermissionGranted) {
                        requestAllFilesAccess()
                    }
                    if (viewModel.needsMediaReadPermissions()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            mediaReadPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_MEDIA_AUDIO,
                                    Manifest.permission.READ_MEDIA_VIDEO
                                )
                            )
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            mediaReadPermissionLauncher.launch(
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                            )
                        }
                    }
                }

                LifecycleResumeEffect(Unit) {
                    refreshPermissionStates()
                    onPauseOrDispose { }
                }

                LaunchedEffect(requestedScreen) {
                    pendingScreen = requestedScreen
                }

                LaunchedEffect(sharedText) {
                    pendingSharedText = sharedText
                }

                LaunchedEffect(openPlayerRequested) {
                    pendingOpenPlayer = openPlayerRequested
                }

                LaunchedEffect(incomingMedia) {
                    pendingMedia = incomingMedia
                }

                LaunchedEffect(pendingScreen) {
                    pendingScreen?.let { screen ->
                        viewModel.navigateTo(screen)
                        requestedScreen = null
                        pendingScreen = null
                    }
                }

                LaunchedEffect(pendingSharedText) {
                    pendingSharedText?.let { text ->
                        viewModel.handleIncomingSharedText(text)
                        sharedText = null
                        pendingSharedText = null
                        clearConsumedExternalIntent()
                    }
                }

                    Surface(modifier = Modifier.fillMaxSize()) {
                        val playbackState by MediaPlayback.uiState.collectAsState()
                        val scope = rememberCoroutineScope()

                    LaunchedEffect(pendingMedia) {
                        val media = pendingMedia ?: return@LaunchedEffect
                        if (media.kind == DownloadKind.AUDIO) {
                            scope.launch {
                                MediaPlayback.playExternalAudio(
                                    context = this@MainActivity,
                                    uri = media.uri,
                                    title = media.displayName
                                )
                            }
                            pendingPlayerKind = DownloadKind.AUDIO
                            openPlayerRequested = true
                        } else {
                            scope.launch {
                                MediaPlayback.playExternalVideo(
                                    context = this@MainActivity,
                                    uri = media.uri,
                                    title = media.displayName
                                )
                            }
                            pendingPlayerKind = DownloadKind.VIDEO
                            openPlayerRequested = true
                        }
                        incomingMedia = null
                        pendingMedia = null
                        clearConsumedExternalIntent()
                    }

                        DLSaverRoot(
                            state = state,
                            playbackUiState = playbackState,
                            openPlayerRequested = pendingOpenPlayer,
                            requestedPlayerKind = pendingPlayerKind,
                            onPlayerRequestConsumed = {
                                openPlayerRequested = false
                                pendingOpenPlayer = false
                                pendingPlayerKind = null
                            },
                            onInputChange = viewModel::updateInput,
                            onListInput = viewModel::prepareInputForListing,
                            onLoadMore = viewModel::loadMoreListing,
                            onNavigate = viewModel::navigateTo,
                            onDownloadResult = viewModel::downloadResult,
                            downloadPermissionGranted = downloadPermissionGranted,
                            notificationPermissionGranted = notificationPermissionGranted,
                            mediaPermissionsGranted = mediaPermissionsGranted,
                            legacyStoragePermissionGranted = legacyStoragePermissionGranted,
                            allFilesPermissionGranted = allFilesPermissionGranted,
                            installPackagesPermissionGranted = installPackagesPermissionGranted,
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !viewModel.hasNotificationPermission()
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onRequestMediaPermission = {
                                if (viewModel.needsMediaReadPermissions()) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        mediaReadPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.READ_MEDIA_AUDIO,
                                                Manifest.permission.READ_MEDIA_VIDEO
                                            )
                                        )
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        mediaReadPermissionLauncher.launch(
                                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                        )
                                    }
                                }
                            },
                            onRequestStoragePermission = {
                                if (viewModel.needsLegacyStoragePermission()) {
                                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                }
                            },
                            onRequestAllFilesPermission = {
                                requestAllFilesAccess()
                            },
                            onRequestInstallPackagesPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                            Uri.parse("package:$packageName")
                                        )
                                    )
                                }
                            },
                            onPlayExistingAudio = viewModel::playExistingAudio,
                            onPlayExistingVideo = viewModel::playExistingVideo,
                            onTogglePlayPause = {
                                scope.launch { MediaPlayback.togglePlayPause(this@MainActivity) }
                            },
                            onSeekTo = { posMs ->
                                scope.launch { MediaPlayback.seekTo(this@MainActivity, posMs) }
                            },
                            onSkipToPrevious = {
                                scope.launch { MediaPlayback.skipToPrevious(this@MainActivity) }
                            },
                            onSkipToNext = {
                                scope.launch { MediaPlayback.skipToNext(this@MainActivity) }
                            },
                            onToggleShuffle = {
                                scope.launch { MediaPlayback.toggleShuffle(this@MainActivity) }
                            },
                            onCycleRepeatMode = {
                                scope.launch { MediaPlayback.cycleRepeatMode(this@MainActivity) }
                            },
                            onStopPlayback = {
                                scope.launch { MediaPlayback.stop(this@MainActivity) }
                            },
                            onRefreshExistingDownloads = viewModel::refreshExistingDownloads,
                            onOpenPlaylist = viewModel::openPlaylist,
                            onSetSelectionMode = viewModel::setSelectionMode,
                            onToggleSelectAll = viewModel::toggleSelectAllVisible,
                            onToggleSelected = viewModel::toggleSelected,
                            onDownloadSelected = viewModel::downloadSelected,
                            onRetryJob = viewModel::retryJob,
                            onCancelJob = viewModel::cancelJob,
                            onExitApp = ::finish
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedScreen = extractRequestedScreen(intent)
        sharedText = extractSharedText(intent)
        openPlayerRequested = extractOpenPlayerRequest(intent)
        incomingMedia = extractIncomingMedia(intent)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN && isFinishing) {
            PlaybackSessionStore.clear(this)
        }
    }

    override fun onDestroy() {
        if (isFinishing && !isChangingConfigurations) {
            PlaybackSessionStore.clear(this)
        }
        super.onDestroy()
    }

    private fun extractRequestedScreen(intent: Intent?): AppScreen? {
        val raw = intent?.getStringExtra(EXTRA_OPEN_SCREEN).orEmpty()
        return AppScreen.entries.firstOrNull { it.name == raw }
    }

    private fun extractSharedText(intent: Intent?): String? {
        val safeIntent = intent ?: return null
        return when (safeIntent.action) {
            Intent.ACTION_SEND -> safeIntent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
            Intent.ACTION_VIEW -> {
                val scheme = safeIntent.data?.scheme?.lowercase()
                if (scheme == "http" || scheme == "https") safeIntent.dataString?.trim() else null
            }
            else -> null
        }?.takeIf { it.isNotBlank() }
    }

    private fun extractOpenPlayerRequest(intent: Intent?): Boolean {
        return intent?.getBooleanExtra(EXTRA_OPEN_PLAYER, false) == true
    }

    private fun extractIncomingMedia(intent: Intent?): IncomingMedia? {
        val safeIntent = intent ?: return null
        val type = safeIntent.resolveType(contentResolver)
            .orEmpty()
            .lowercase()

        val uri: Uri? = when (safeIntent.action) {
            Intent.ACTION_VIEW -> safeIntent.data
            Intent.ACTION_SEND -> extractStreamUri(safeIntent)
            else -> null
        }
        if (uri == null) return null

        val kind = when {
            type.startsWith("audio/") -> DownloadKind.AUDIO
            type.startsWith("video/") -> DownloadKind.VIDEO
            else -> null
        } ?: run {
            // Fallback por extensao
            val last = uri.toString().substringAfterLast('/', "").substringAfterLast(':', "")
            val ext = last.substringAfterLast('.', "").lowercase()
            when (ext) {
                "mp3", "m4a", "aac", "wav", "ogg", "opus", "flac" -> DownloadKind.AUDIO
                "mp4", "mkv", "webm", "mov", "avi" -> DownloadKind.VIDEO
                else -> return null
            }
        }

        return IncomingMedia(
            uri = uri,
            kind = kind,
            displayName = fileNameWithoutExtension(queryDisplayName(uri)).ifBlank { "Arquivo" }
        )
    }

    private fun extractStreamUri(intent: Intent): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let { return it }
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { return it }
        }
        return intent.clipData?.firstUri()
    }

    private fun ClipData.firstUri(): Uri? {
        for (index in 0 until itemCount) {
            getItemAt(index)?.uri?.let { return it }
        }
        return null
    }

    private fun queryDisplayName(uri: Uri): String {
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx).orEmpty() else ""
            } ?: ""
        }.getOrDefault("")
    }

    private fun clearConsumedExternalIntent() {
        val currentIntent = intent ?: return
        val action = currentIntent.action.orEmpty()
        if (action != Intent.ACTION_VIEW && action != Intent.ACTION_SEND) return
        setIntent(Intent(this, MainActivity::class.java))
    }

    private fun canRequestInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    private fun canManageAllFiles(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()
    }

    private fun requestAllFilesAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val appIntent = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:$packageName")
        )
        runCatching {
            startActivity(appIntent)
        }.recoverCatching {
            startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        }
    }

    companion object {
        private const val EXTRA_OPEN_SCREEN = "open_screen"
        private const val EXTRA_OPEN_PLAYER = "open_player"

        fun createOpenScreenIntent(context: Context, screen: AppScreen): Intent {
            return Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_OPEN_SCREEN, screen.name)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        }

        fun createOpenPlayerIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        }
    }
}

private data class IncomingMedia(
    val uri: Uri,
    val kind: DownloadKind,
    val displayName: String
)
