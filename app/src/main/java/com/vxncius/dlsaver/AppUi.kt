package com.vxncius.dlsaver

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.provider.Settings
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Scale
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.palette.graphics.Palette
import androidx.core.view.WindowCompat
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DLSaverRoot(
    state: DownloadUiState,
    playbackUiState: PlaybackUiState,
    openPlayerRequested: Boolean,
    requestedPlayerKind: DownloadKind?,
    onPlayerRequestConsumed: () -> Unit,
    onInputChange: (String) -> Unit,
    onListInput: () -> Unit,
    onLoadMore: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onDownloadResult: (SearchResultItem, DownloadKind) -> DownloadEnqueueResult,
    onOpenPlaylist: (String) -> Unit,
    onSetSelectionMode: (Boolean) -> Unit,
    onToggleSelectAll: () -> Unit,
    onToggleSelected: (String) -> Unit,
    onDownloadSelected: (DownloadKind) -> DownloadEnqueueResult,
    onRetryJob: (String) -> Unit,
    onCancelJob: (String) -> Unit,
    downloadPermissionGranted: Boolean,
    notificationPermissionGranted: Boolean,
    mediaPermissionsGranted: Boolean,
    legacyStoragePermissionGranted: Boolean,
    allFilesPermissionGranted: Boolean,
    installPackagesPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestMediaPermission: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    onRequestAllFilesPermission: () -> Unit,
    onRequestInstallPackagesPermission: () -> Unit,
    onPlayExistingAudio: (ExistingDownloadItem) -> Unit,
    onPlayExistingVideo: (ExistingDownloadItem) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipToPrevious: () -> Unit,
    onSkipToNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onStopPlayback: () -> Unit,
    onRefreshExistingDownloads: () -> Unit,
    onExitApp: () -> Unit
) {
    val context = LocalContext.current
    var dialogItem by remember { mutableStateOf<SearchResultItem?>(null) }
    var multiDownloadSheetOpen by remember { mutableStateOf(false) }
    var duplicateDownloadRequest by remember { mutableStateOf<Pair<SearchResultItem, DownloadKind>?>(null) }
    var libraryMenuItem by remember { mutableStateOf<ExistingDownloadItem?>(null) }
    var playbackMenuItem by remember { mutableStateOf<ExistingDownloadItem?>(null) }
    var renameItem by remember { mutableStateOf<ExistingDownloadItem?>(null) }
    var detailsItem by remember { mutableStateOf<ExistingDownloadItem?>(null) }
    var deleteItem by remember { mutableStateOf<ExistingDownloadItem?>(null) }
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    var videoPlayerOpen by rememberSaveable { mutableStateOf(false) }
    var videoPlayerFullscreen by rememberSaveable { mutableStateOf(false) }
    var pendingPlayerOpenRequest by rememberSaveable { mutableStateOf(false) }
    var pendingRequestedPlayerKind by rememberSaveable { mutableStateOf<DownloadKind?>(null) }
    var lastBackPressAt by remember { mutableStateOf(0L) }
    var homeInputFocusNonce by remember { mutableStateOf(0) }
    var playerMonochromatic by remember { mutableStateOf(AppPreferences.isPlayerMonochromatic(context)) }
    var settingsReturnScreen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
    var settingsDocumentAssetPath by rememberSaveable { mutableStateOf("") }
    var librarySearchActive by rememberSaveable { mutableStateOf(false) }
    var dismissedUpdateVersionCode by rememberSaveable { mutableStateOf(-1) }
    val updateState by UpdateManager.state.collectAsState()
    val updateScope = rememberCoroutineScope()
    val latestUpdate = updateState.manifest?.latest
    val showUpdateDialog = latestUpdate != null &&
        latestUpdate.versionCode > BuildConfig.VERSION_CODE &&
        latestUpdate.versionCode != dismissedUpdateVersionCode
    val currentPlaybackItem = remember(state.existingDownloads, playbackUiState.mediaUri) {
        val currentUri = playbackUiState.mediaUri?.toString().orEmpty()
        state.existingDownloads.firstOrNull { it.sourceUrl == currentUri }
    }

    LaunchedEffect(state.screen) {
        if (state.screen != AppScreen.SETTINGS && state.screen != AppScreen.SETTINGS_DOCUMENT) {
            settingsReturnScreen = state.screen
        }
    }

    LaunchedEffect(openPlayerRequested) {
        if (openPlayerRequested) {
            pendingPlayerOpenRequest = true
            pendingRequestedPlayerKind = requestedPlayerKind
            onPlayerRequestConsumed()
        }
    }

    LaunchedEffect(pendingPlayerOpenRequest, pendingRequestedPlayerKind, playbackUiState.isVideo) {
        if (pendingPlayerOpenRequest) {
            if (pendingRequestedPlayerKind == DownloadKind.VIDEO || playbackUiState.isVideo) {
                videoPlayerOpen = true
                playerOpen = false
            } else {
                playerOpen = true
                videoPlayerOpen = false
            }
            pendingPlayerOpenRequest = false
            pendingRequestedPlayerKind = null
        }
    }

    BackHandler {
        if (videoPlayerOpen) {
            if (videoPlayerFullscreen) {
                videoPlayerFullscreen = false
                return@BackHandler
            }
            videoPlayerOpen = false
            videoPlayerFullscreen = false
            onStopPlayback()
            return@BackHandler
        }
        if (playerOpen) {
            playerOpen = false
            return@BackHandler
        }
        if (state.screen == AppScreen.SETTINGS_DOCUMENT) {
            onNavigate(AppScreen.SETTINGS)
            return@BackHandler
        }
        if (state.screen == AppScreen.SETTINGS) {
            onNavigate(settingsReturnScreen)
            return@BackHandler
        }
        if (state.screen != AppScreen.HOME) {
            onNavigate(AppScreen.HOME)
            return@BackHandler
        }

        val now = System.currentTimeMillis()
        if (now - lastBackPressAt <= 2_000L) {
            onExitApp()
        } else {
            lastBackPressAt = now
            Toast.makeText(context, "Toque de novo para sair", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(playbackUiState.hasMedia) {
        if (!playbackUiState.hasMedia) {
            playerOpen = false
            videoPlayerOpen = false
            videoPlayerFullscreen = false
            pendingPlayerOpenRequest = false
        }
    }

    dialogItem?.let { item ->
        DownloadKindBottomSheet(
            item = item,
            onDismiss = { dialogItem = null },
            onSelect = { kind ->
                val result = onDownloadResult(item, kind)
                Toast.makeText(context, result.toastMessage(), Toast.LENGTH_SHORT).show()
                dialogItem = null
            }
        )
    }

    if (multiDownloadSheetOpen) {
        DownloadKindMultiBottomSheet(
            title = state.listingTitle.ifBlank { "Itens selecionados" },
            count = state.selectedUrls.size,
            thumbnailUrl = state.searchResults.firstOrNull { it.url in state.selectedUrls }?.thumbnailUrl.orEmpty(),
            onDismiss = { multiDownloadSheetOpen = false },
            onSelect = { kind ->
                val result = onDownloadSelected(kind)
                Toast.makeText(context, result.toastMessage(), Toast.LENGTH_SHORT).show()
                multiDownloadSheetOpen = false
            }
        )
    }

    duplicateDownloadRequest?.let { (item, kind) ->
        ConfirmDuplicateDownloadDialog(
            itemTitle = sanitizeDownloadTitle(item.title),
            onDismiss = { duplicateDownloadRequest = null },
            onConfirm = {
                val result = onDownloadResult(item, kind)
                Toast.makeText(context, result.toastMessage(), Toast.LENGTH_SHORT).show()
                duplicateDownloadRequest = null
            }
        )
    }

    libraryMenuItem?.let { item ->
        ExistingDownloadActionsBottomSheet(
            item = item,
            onDismiss = { libraryMenuItem = null },
            onShare = {
                if (!DownloadFileActions.share(context, item)) {
                    Toast.makeText(context, "Não foi possível compartilhar o arquivo", Toast.LENGTH_SHORT).show()
                }
                libraryMenuItem = null
            },
            onRename = {
                renameItem = item
                libraryMenuItem = null
            },
            onDetails = {
                detailsItem = item
                libraryMenuItem = null
            },
            onDelete = {
                deleteItem = item
                libraryMenuItem = null
            }
        )
    }

    renameItem?.let { item ->
        RenameDownloadBottomSheet(
            item = item,
            onDismiss = { renameItem = null },
            onConfirm = { newName ->
                if (!isValidRenamedBaseName(fileNameWithoutExtension(newName))) {
                    Toast.makeText(context, "Nome inválido para renomear", Toast.LENGTH_SHORT).show()
                    return@RenameDownloadBottomSheet
                }
                val renamed = DownloadFileActions.rename(context, item, newName)
                if (renamed) {
                    onRefreshExistingDownloads()
                    Toast.makeText(context, "Arquivo renomeado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Não foi possível renomear", Toast.LENGTH_SHORT).show()
                }
                renameItem = null
            }
        )
    }

    detailsItem?.let { item ->
        DownloadDetailsBottomSheet(
            item = item,
            onDismiss = { detailsItem = null }
        )
    }

    deleteItem?.let { item ->
        ConfirmDeleteDialog(
            item = item,
            onDismiss = { deleteItem = null },
            onConfirm = {
                val deleted = DownloadFileActions.delete(context, item)
                if (deleted) {
                    updateScope.launch {
                        MediaPlayback.removeDeletedItems(context.applicationContext, listOf(item))
                    }
                    onRefreshExistingDownloads()
                    Toast.makeText(context, "Arquivo excluído", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Não foi possível excluir", Toast.LENGTH_SHORT).show()
                }
                deleteItem = null
            }
        )
    }

    if (showUpdateDialog && latestUpdate != null) {
        UpdateAvailableDialog(
            release = latestUpdate,
            downloading = updateState.downloading,
            progress = updateState.downloadProgress,
            onLater = { dismissedUpdateVersionCode = latestUpdate.versionCode },
            onUpdate = {
                updateScope.launch {
                    when (UpdateManager.downloadAndInstall(context.applicationContext, latestUpdate)) {
                        UpdateInstallResult.RequiresInstallPermission -> onRequestInstallPackagesPermission()
                        is UpdateInstallResult.Failed -> {
                            Toast.makeText(context, updateState.error.ifBlank { "Não foi possível baixar a atualização" }, Toast.LENGTH_LONG).show()
                        }
                        else -> Unit
                    }
                }
            }
        )
    }

    val isHome = state.screen == AppScreen.HOME
    val isSettingsDocument = state.screen == AppScreen.SETTINGS_DOCUMENT
    val topBarBackTarget = when (state.screen) {
        AppScreen.SETTINGS -> settingsReturnScreen
        else -> AppScreen.HOME
    }
    val rootModifier = Modifier
        .fillMaxSize()
        .let { base ->
            if (isHome) {
                base.background(Brush.verticalGradient(listOf(Color.Black, Color(0xFF212121))))
            } else if (isSettingsDocument) {
                base.background(Color.White)
            } else {
                base.background(Color.Black)
            }
        }

    Box(modifier = rootModifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
        ) {
            Scaffold(
                containerColor = when {
                    isHome -> Color.Transparent
                    isSettingsDocument -> Color.White
                    else -> Color.Black
                },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    if (
                        !isHome &&
                        !isSettingsDocument &&
                        state.screen != AppScreen.LIST_ITEMS_FOR_DOWNLOAD &&
                        state.screen != AppScreen.DOWNLOADED_OR_PROGRESS_LIST_ITEMS &&
                        state.screen != AppScreen.SETTINGS
                    ) {
                        AppTopBar(title = screenTitle(state.screen), onBack = { onNavigate(topBarBackTarget) })
                    }
                },
                bottomBar = {
                    if (
                        state.screen != AppScreen.LIST_ITEMS_FOR_DOWNLOAD &&
                        state.screen != AppScreen.SETTINGS &&
                        state.screen != AppScreen.SETTINGS_DOCUMENT
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 49.dp)
                                .background(
                                    when {
                                        isHome -> Color.Transparent
                                        isSettingsDocument -> Color.White
                                        else -> Color.Black
                                    }
                                )
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (
                                    playbackUiState.hasMedia &&
                                    !playbackUiState.isVideo &&
                                    !librarySearchActive &&
                                    (state.screen == AppScreen.DOWNLOADED_OR_PROGRESS_LIST_ITEMS || state.screen == AppScreen.SETTINGS)
                                ) {
                                    MiniPlayerBar(
                                        state = playbackUiState,
                                        onOpen = { playerOpen = true },
                                        onTogglePlayPause = onTogglePlayPause,
                                        modifier = Modifier.padding(horizontal = 11.dp),
                                        outerPadding = PaddingValues(start = 4.dp, end = 4.dp)
                                    )
                                }
                                BottomNavBar(current = state.screen, onNavigate = onNavigate)
                            }
                        }
                    }
                }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (state.screen) {
                    AppScreen.HOME -> HomeScreen(
                        state = state,
                        onInputChange = onInputChange,
                        onListInput = onListInput,
                        focusInputNonce = homeInputFocusNonce
                    )

                AppScreen.LIST_ITEMS_FOR_DOWNLOAD -> ResultListScreen(
                    state = state,
                    onLoadMore = onLoadMore,
                    onDownloadClick = { dialogItem = it },
                    onOpenPlaylist = onOpenPlaylist,
                    onSetSelectionMode = onSetSelectionMode,
                    onToggleSelectAll = onToggleSelectAll,
                    onToggleSelected = onToggleSelected,
                    onOpenMultiDownloadSheet = { multiDownloadSheetOpen = true }
                )

                AppScreen.DOWNLOADED_OR_PROGRESS_LIST_ITEMS -> DownloadsScreen(
                    state = state,
                    onNavigateToSearch = {
                        homeInputFocusNonce++
                        onNavigate(AppScreen.HOME)
                    },
                    onOpenItemMenu = { libraryMenuItem = it },
                    onRetryJob = onRetryJob,
                    onCancelJob = onCancelJob,
                    onSearchActiveChange = { librarySearchActive = it },
                    onShareItems = { items ->
                        if (!DownloadFileActions.share(context, items)) {
                            Toast.makeText(context, "Não foi possível compartilhar os arquivos", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDeleteItems = { items ->
                        val deletedCount = DownloadFileActions.delete(context, items)
                        if (deletedCount > 0) {
                            updateScope.launch {
                                MediaPlayback.removeDeletedItems(context.applicationContext, items)
                            }
                            onRefreshExistingDownloads()
                            Toast.makeText(context, "$deletedCount item(ns) excluído(s)", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Não foi possível excluir", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onItemClick = { item ->
                        if (item.kind == DownloadKind.AUDIO) {
                            LibraryDataStore.recordPlaybackStart(context, item)
                            onPlayExistingAudio(item)
                            playerOpen = true
                        } else if (item.kind == DownloadKind.VIDEO) {
                            LibraryDataStore.recordPlaybackStart(context, item)
                            onPlayExistingVideo(item)
                            videoPlayerOpen = true
                        }
                    }
                )

                AppScreen.SETTINGS -> SettingsHubScreen(
                    state = state,
                    playerMonochromatic = playerMonochromatic,
                    downloadPermissionGranted = downloadPermissionGranted,
                    notificationPermissionGranted = notificationPermissionGranted,
                    mediaPermissionsGranted = mediaPermissionsGranted,
                    legacyStoragePermissionGranted = legacyStoragePermissionGranted,
                    allFilesPermissionGranted = allFilesPermissionGranted,
                    installPackagesPermissionGranted = installPackagesPermissionGranted,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onRequestMediaPermission = onRequestMediaPermission,
                    onRequestStoragePermission = onRequestStoragePermission,
                    onRequestAllFilesPermission = onRequestAllFilesPermission,
                    onRequestInstallPackagesPermission = onRequestInstallPackagesPermission,
                    onTogglePlayerMonochromatic = { enabled ->
                        playerMonochromatic = enabled
                        AppPreferences.setPlayerMonochromatic(context, enabled)
                    },
                    onOpenTerms = {
                        settingsDocumentAssetPath = "file:///android_asset/termos_uso.html"
                        onNavigate(AppScreen.SETTINGS_DOCUMENT)
                    },
                    onOpenPrivacy = {
                        settingsDocumentAssetPath = "file:///android_asset/politica_privacidade.html"
                        onNavigate(AppScreen.SETTINGS_DOCUMENT)
                    }
                )

                AppScreen.SETTINGS_DOCUMENT -> SettingsDocumentScreen(
                    assetPath = settingsDocumentAssetPath,
                    onBack = { onNavigate(AppScreen.SETTINGS) }
                )
            }

            if (playbackUiState.hasMedia && state.screen == AppScreen.HOME && !playbackUiState.isVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 15.dp, start = 11.dp, end = 11.dp)
                ) {
                    MiniPlayerBar(
                        state = playbackUiState,
                        onOpen = { playerOpen = true },
                        onTogglePlayPause = onTogglePlayPause
                    )
                }
            }
            }
        }

        }

        // Mantém um fundo sólido enquanto o player de vídeo está aberto (evita "piscada"
        // da lista por trás durante transições de fullscreen/orientação).
        if (videoPlayerOpen) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        }

        if (playerOpen) {
            PlayerBottomSheet(
                state = playbackUiState,
                monochromatic = playerMonochromatic,
                currentItem = currentPlaybackItem,
                onDismiss = { playerOpen = false },
                onOpenItemMenu = { item -> playbackMenuItem = item },
                onTogglePlayPause = onTogglePlayPause,
                onSeekTo = onSeekTo,
                onSkipToPrevious = onSkipToPrevious,
                onSkipToNext = onSkipToNext,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeatMode = onCycleRepeatMode
            )
        }

        if (videoPlayerOpen) {
            VideoPlayerBottomSheet(
                state = playbackUiState,
                currentItem = currentPlaybackItem,
                onDismiss = {
                    videoPlayerOpen = false
                    videoPlayerFullscreen = false
                    onStopPlayback()
                },
                onOpenItemMenu = { item -> playbackMenuItem = item },
                isFullscreen = videoPlayerFullscreen,
                onFullscreenChange = { videoPlayerFullscreen = it },
                onTogglePlayPause = onTogglePlayPause,
                onSeekTo = onSeekTo
            )
        }

        playbackMenuItem?.let { item ->
            ExistingDownloadActionsBottomSheet(
                item = item,
                onDismiss = { playbackMenuItem = null },
                onShare = {
                    if (!DownloadFileActions.share(context, item)) {
                        Toast.makeText(context, "Não foi possível compartilhar o arquivo", Toast.LENGTH_SHORT).show()
                    }
                    playbackMenuItem = null
                },
                onRename = {
                    if (videoPlayerOpen) onStopPlayback()
                    playerOpen = false
                    videoPlayerOpen = false
                    videoPlayerFullscreen = false
                    renameItem = item
                    playbackMenuItem = null
                },
                onDetails = {
                    if (videoPlayerOpen) onStopPlayback()
                    playerOpen = false
                    videoPlayerOpen = false
                    videoPlayerFullscreen = false
                    detailsItem = item
                    playbackMenuItem = null
                },
                onDelete = {
                    if (videoPlayerOpen) onStopPlayback()
                    playerOpen = false
                    videoPlayerOpen = false
                    videoPlayerFullscreen = false
                    deleteItem = item
                    playbackMenuItem = null
                }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    state: DownloadUiState,
    onInputChange: (String) -> Unit,
    onListInput: () -> Unit,
    focusInputNonce: Int
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(focusInputNonce) {
        if (focusInputNonce > 0) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .offset(y = 40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DLSaver",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .padding(top = 14.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), CircleShape)
                    .background(Color.Transparent)
                    .padding(start = 18.dp, end = 8.dp, top = 0.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = state.input,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                keyboardController?.hide()
                            }
                        },
                    placeholder = { Text("Pesquisar para baixar") },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontSize = 16.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onListInput()
                        }
                    ),
                    singleLine = true
                )

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF111111))
                        .clickable {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onListInput()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Pesquisar",
                        tint = Color.White
                    )
                }
            }

            if (state.isSearching) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Listando resultados...")
                }
            }
        }
    }
}

@Composable
private fun ResultListScreen(
    state: DownloadUiState,
    onLoadMore: () -> Unit,
    onDownloadClick: (SearchResultItem) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onSetSelectionMode: (Boolean) -> Unit,
    onToggleSelectAll: () -> Unit,
    onToggleSelected: (String) -> Unit,
    onOpenMultiDownloadSheet: () -> Unit
) {
    val isPlaylistMode = state.listingMode == ListingMode.PLAYLIST
    val allPlaylistItemsSelected = isPlaylistMode &&
        state.searchResults.isNotEmpty() &&
        state.searchResults.all { it.url in state.selectedUrls }
    val listContentPadding = PaddingValues(
        start = 7.dp,
        end = 7.dp,
        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 15.dp,
        bottom = if (isPlaylistMode) 108.dp else 20.dp
    )

    if (state.isSearching && state.searchResults.isEmpty()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = listContentPadding,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item("results_header") {
                Box(modifier = Modifier.padding(horizontal = 10.dp)) {
                    ScreenSectionHeader(
                        title = if (state.listingMode == ListingMode.PLAYLIST) {
                            state.listingTitle.ifBlank { "Playlist" }
                        } else {
                            "Resultados"
                        },
                        subtitle = if (state.listingMode == ListingMode.PLAYLIST) "Carregando..." else "Buscando...",
                        minHeight = 39.dp
                    )
                }
            }
            items(6, key = { idx -> "shimmer_$idx" }) {
                ShimmerResultPlaceholder()
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = listContentPadding,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item("results_header") {
                Box(modifier = Modifier.padding(horizontal = 10.dp)) {
                    if (isPlaylistMode) {
                        PlaylistHeader(
                            title = state.listingTitle.ifBlank { "Playlist" },
                            subtitle = listingResultsSubtitle(
                                count = state.searchResults.size,
                                isLoadingMore = state.isSearching && state.searchResults.isNotEmpty()
                            ),
                            allSelected = allPlaylistItemsSelected,
                            selectedCount = state.selectedUrls.size,
                            onToggleSelectAll = {
                                if (!state.selectionMode) onSetSelectionMode(true)
                                onToggleSelectAll()
                            }
                        )
                    } else {
                        ScreenSectionHeader(
                            title = "Resultados",
                            subtitle = listingResultsSubtitle(
                                count = state.searchResults.size,
                                isLoadingMore = state.isSearching && state.searchResults.isNotEmpty()
                            ),
                            minHeight = 39.dp
                        )
                    }
                }
            }

            itemsIndexed(
                items = state.searchResults,
                key = { _, item -> item.url },
                contentType = { _, item -> item.resultType }
            ) { _, item ->
                if (isPlaylistMode) {
                    SearchResultSelectableRow(
                        item = item,
                        selected = item.url in state.selectedUrls,
                        onToggle = {
                            if (!state.selectionMode) onSetSelectionMode(true)
                            onToggleSelected(item.url)
                        }
                    )
                } else {
                    SearchResultRow(
                        item = item,
                        onDownload = {
                            if (item.resultType == "playlist") {
                                onOpenPlaylist(item.url)
                            } else {
                                onDownloadClick(item)
                            }
                        }
                    )
                }
            }

            if (state.isSearching && state.searchResults.isNotEmpty()) {
                item("loading_more_shimmer") {
                    ShimmerResultPlaceholder()
                }
            }
        }
    }
}

@Composable
private fun DownloadsScreen(
    state: DownloadUiState,
    onNavigateToSearch: () -> Unit,
    onOpenItemMenu: (ExistingDownloadItem) -> Unit,
    onRetryJob: (String) -> Unit,
    onCancelJob: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onShareItems: (List<ExistingDownloadItem>) -> Unit,
    onDeleteItems: (List<ExistingDownloadItem>) -> Unit,
    onItemClick: (ExistingDownloadItem) -> Unit
) {
    var librarySearchOpen by rememberSaveable { mutableStateOf(false) }
    var librarySearchQuery by rememberSaveable { mutableStateOf("") }
    var multiSelectMode by rememberSaveable { mutableStateOf(false) }
    var selectedLocalUris by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var multiMenuExpanded by remember { mutableStateOf(false) }
    var confirmMultiDelete by remember { mutableStateOf(false) }
    val libraryListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val statusTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val activeJobs = state.downloadJobs.filter {
        it.status == DownloadJobStatus.RUNNING || it.status == DownloadJobStatus.EXPORTING
    }
    val queuedJobs = state.downloadJobs.filter { it.status == DownloadJobStatus.QUEUED }
    val failedJobs = state.downloadJobs.filter { it.status == DownloadJobStatus.FAILED }
    val pendingOrFailedNames = state.downloadJobs
        .filter { it.status != DownloadJobStatus.COMPLETED }
        .flatMap { job -> job.savedFiles }
        .map { path -> path.substringAfterLast('/') }
        .toSet()
    val existingDownloads = state.existingDownloads
        .filterNot { it.name in pendingOrFailedNames }
        .filter { item ->
            val query = librarySearchQuery.trim()
            query.isBlank() ||
                item.name.contains(query, ignoreCase = true) ||
                item.mimeType.contains(query, ignoreCase = true)
        }
    val selectedItems = existingDownloads.filter { it.sourceUrl in selectedLocalUris }

    LaunchedEffect(existingDownloads) {
        val visibleUris = existingDownloads.map { it.sourceUrl }.toSet()
        selectedLocalUris = selectedLocalUris.intersect(visibleUris)
        if (selectedLocalUris.isEmpty()) multiSelectMode = false
    }

    LaunchedEffect(librarySearchOpen) {
        onSearchActiveChange(librarySearchOpen)
    }

    DisposableEffect(Unit) {
        onDispose { onSearchActiveChange(false) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = libraryListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 7.dp,
                end = 7.dp,
                top = statusTopPadding + if (librarySearchOpen) 73.dp else 15.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!librarySearchOpen) item("downloads_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .heightIn(min = 39.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScreenSectionHeader(
                        title = if (multiSelectMode) "${selectedItems.size} selecionado(s)" else "Mídia local",
                        subtitle = if (multiSelectMode) {
                            "Escolha uma ação"
                        } else {
                            buildString {
                                append(existingDownloads.size + failedJobs.size)
                                append(" item(ns)")
                                if (activeJobs.isNotEmpty()) {
                                    append("  ")
                                    append(activeJobs.size)
                                    append(" em progresso")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        minHeight = 39.dp
                    )
                    Box {
                        IconButton(
                            onClick = {
                                if (multiSelectMode) multiMenuExpanded = true else librarySearchOpen = true
                            },
                            modifier = Modifier.offset(y = (-5).dp, x = (15).dp)
                        ) {
                            Icon(
                                if (multiSelectMode) Icons.Default.MoreVert else Icons.Default.Search,
                                contentDescription = if (multiSelectMode) "Opções da seleção" else "Pesquisar mídia local",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = multiMenuExpanded,
                            onDismissRequest = { multiMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Compartilhar") },
                                leadingIcon = { Icon(Icons.Default.DownloadForOffline, contentDescription = null) },
                                enabled = selectedItems.isNotEmpty(),
                                onClick = {
                                    multiMenuExpanded = false
                                    onShareItems(selectedItems)
                                    selectedLocalUris = emptySet()
                                    multiSelectMode = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Excluir") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                enabled = selectedItems.isNotEmpty(),
                                onClick = {
                                    multiMenuExpanded = false
                                    confirmMultiDelete = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Cancelar seleção") },
                                leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                                onClick = {
                                    multiMenuExpanded = false
                                    selectedLocalUris = emptySet()
                                    multiSelectMode = false
                                }
                            )
                        }
                    }
                }
            }

            if (activeJobs.isEmpty() && failedJobs.isEmpty() && existingDownloads.isEmpty()) {
                item("empty_downloads") {
                    EmptyDownloadsState(onNavigateToSearch)
                }
            }

            items(activeJobs, key = { it.id }, contentType = { "active_download" }) { job ->
                DownloadJobCard(job, onRetry = null, onCancel = { onCancelJob(job.id) })
            }

            items(queuedJobs, key = { it.id }, contentType = { "queued_download" }) { job ->
                DownloadJobCard(job, onRetry = null, onCancel = { onCancelJob(job.id) })
            }

            items(failedJobs, key = { it.id }, contentType = { "failed_download" }) { job ->
                DownloadJobCard(job, onRetry = { onRetryJob(job.id) }, onCancel = null)
            }

            items(existingDownloads, key = { it.name }, contentType = { "existing_${it.kind.name}" }) { item ->
                ExistingDownloadCard(
                    item = item,
                    selected = item.sourceUrl in selectedLocalUris,
                    selectionMode = multiSelectMode,
                    onClick = {
                        if (multiSelectMode) {
                            selectedLocalUris = selectedLocalUris.toggle(item.sourceUrl)
                            if (selectedLocalUris.isEmpty()) multiSelectMode = false
                        } else {
                            onItemClick(item)
                        }
                    },
                    onLongClick = {
                        multiSelectMode = true
                        selectedLocalUris = selectedLocalUris + item.sourceUrl
                    },
                    onMore = { onOpenItemMenu(item) }
                )
            }
        }

        if (librarySearchOpen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(start = 7.dp, top = statusTopPadding + 10.dp, end = 7.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                OutlinedTextField(
                    value = librarySearchQuery,
                    onValueChange = { librarySearchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(Color.Black, RoundedCornerShape(28.dp)),
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                librarySearchQuery = ""
                                librarySearchOpen = false
                                scope.launch { libraryListState.scrollToItem(0) }
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar pesquisa", tint = Color.White)
                        }
                    },
                    placeholder = { Text("Buscar titulo") }
                )
                Spacer(modifier = Modifier.height(35.dp))
            }
        }
    }

    if (confirmMultiDelete) {
        ConfirmMultiDeleteDialog(
            count = selectedItems.size,
            onDismiss = { confirmMultiDelete = false },
            onConfirm = {
                onDeleteItems(selectedItems)
                selectedLocalUris = emptySet()
                multiSelectMode = false
                confirmMultiDelete = false
            }
        )
    }
}
@Composable
private fun SearchResultRow(
    item: SearchResultItem,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDownload),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThumbnailBox(
                url = itemThumbnailUrl(item),
                fallbackUrl = fallbackThumbnailUrlFor(item),
                kind = if (item.resultType == "audio") DownloadKind.AUDIO else DownloadKind.VIDEO,
                preferCover = shouldUseCoverThumbnail(item.resultType, item.url)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.author.ifBlank { item.resultType },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.extra.isNotBlank()) {
                    Text(
                        item.extra,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onDownload) {
                Icon(
                    Icons.Default.DownloadForOffline,
                    contentDescription = "Download",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ShimmerResultPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A1A1A))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(14.dp)
                        .background(Color(0xFF1A1A1A))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.48f)
                        .height(12.dp)
                        .background(Color(0xFF1A1A1A))
                )
            }
        }
    }
}

@Composable
private fun EmptyDownloadsState(onNavigateToSearch: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Nenhum arquivo ainda. Busque algo para baixar.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = onNavigateToSearch) {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Buscar")
        }
    }
}

private fun listingResultsSubtitle(count: Int, isLoadingMore: Boolean): String {
    return buildString {
        append(count)
        append(" item(ns)")
        if (isLoadingMore) {
            append("  carregando...")
        }
    }
}

@Composable
private fun PlaylistHeader(
    title: String,
    subtitle: String,
    allSelected: Boolean,
    selectedCount: Int,
    onToggleSelectAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        ScreenSectionHeader(title = title, subtitle = subtitle, minHeight = 39.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onToggleSelectAll)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(horizontal = 5.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = allSelected, onClick = null)
            Text(
                text = if (allSelected) "Desmarcar todos" else "Marcar todos",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (selectedCount > 0) {
                Text(
                    text = "$selectedCount",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SearchResultSelectableRow(
    item: SearchResultItem,
    selected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThumbnailBox(
                url = itemThumbnailUrl(item),
                fallbackUrl = fallbackThumbnailUrlFor(item),
                kind = if (item.resultType == "audio") DownloadKind.AUDIO else DownloadKind.VIDEO,
                preferCover = shouldUseCoverThumbnail(item.resultType, item.url)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.author.ifBlank { item.resultType },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = if (selected) Icons.Default.Check else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = if (selected) "Selecionado" else "Não selecionado",
                // tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SettingsDocumentScreen(
    assetPath: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val documentHtml = remember(context, assetPath) { loadStyledAssetDocument(context, assetPath) }
    BackHandler(onBack = onBack)

    SideEffect {
        val window = activity?.window
        if (window != null) {
            @Suppress("DEPRECATION")
            run {
                window.statusBarColor = android.graphics.Color.WHITE
                window.navigationBarColor = android.graphics.Color.WHITE
            }
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    DisposableEffect(activity) {
        val window = activity?.window
        onDispose {
            if (window != null) {
                @Suppress("DEPRECATION")
                run {
                    window.statusBarColor = android.graphics.Color.BLACK
                    window.navigationBarColor = android.graphics.Color.BLACK
                }
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(Color.White),
        factory = { viewContext ->
            WebView(viewContext).apply {
                setBackgroundColor(android.graphics.Color.WHITE)
                settings.javaScriptEnabled = false
                settings.domStorageEnabled = false
                webViewClient = object : WebViewClient() {}
                loadDataWithBaseURL("file:///android_asset/", documentHtml, "text/html", "UTF-8", null)
            }
        }
    )
}

@Composable
private fun DownloadJobCard(
    job: DownloadJobItem,
    onRetry: (() -> Unit)?,
    onCancel: (() -> Unit)?
) {
    val targetProgress = when (job.status) {
        DownloadJobStatus.RUNNING,
        DownloadJobStatus.EXPORTING -> job.progress.coerceIn(0.02f, 1f)
        DownloadJobStatus.COMPLETED -> 1f
        else -> 0f
    }
    val displayedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 180),
        label = "downloadJobProgress"
    )
    val progressLabel = when (job.status) {
        DownloadJobStatus.RUNNING -> "Baixando ${((displayedProgress * 100).roundToInt()).coerceIn(0, 100)}%"
        DownloadJobStatus.EXPORTING -> "Exportando..."
        DownloadJobStatus.QUEUED -> "Na fila..."
        DownloadJobStatus.FAILED -> "Falha no download"
        DownloadJobStatus.COMPLETED -> "Download concluído"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThumbnailBox(
                    url = thumbnailForKind(job.thumbnailUrl, job.kind),
                    kind = job.kind,
                    preferCover = shouldUseCoverThumbnail(sourceUrl = job.sourceUrl)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (job.status == DownloadJobStatus.RUNNING || job.status == DownloadJobStatus.EXPORTING) {
                        LinearProgressIndicator(
                            progress = { displayedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 3.dp)
                        )
                    }
                    Text(
                        progressLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                when {
                    job.status == DownloadJobStatus.FAILED && onRetry != null -> {
                        IconButton(
                            onClick = onRetry,
                            modifier = Modifier.offset(x = (15).dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Tentar novamente",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } // icones de download
                    (job.status == DownloadJobStatus.RUNNING || job.status == DownloadJobStatus.EXPORTING || job.status == DownloadJobStatus.QUEUED) && onCancel != null -> {
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier.offset(x = (15).dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancelar download",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            if (job.savedFiles.isNotEmpty()) {
                Text(
                    text = job.savedFiles.joinToString("\n"),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ExistingDownloadCard(
    item: ExistingDownloadItem,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onMore: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThumbnailBox(
                url = thumbnailForKind(item.thumbnailUrl, item.kind),
                kind = item.kind,
                preferCover = shouldUseCoverThumbnail(sourceUrl = item.sourceUrl)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileNameWithoutExtension(item.name),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (item.kind == DownloadKind.AUDIO) "Áudio salvo" else "Vídeo salvo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selectionMode) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = if (selected) "Selecionado" else "Nao selecionado",
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                IconButton(
                    onClick = onMore,
                    modifier = Modifier.offset(x = (15).dp)
                ){
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Mais opcoes",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ExistingDownloadActionsBottomSheet(
    item: ExistingDownloadItem,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = fileNameWithoutExtension(item.name),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 14.dp),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis
            )
            ActionSheetButton(icon = Icons.Default.DownloadForOffline, label = "Compartilhar", onClick = onShare)
            ActionSheetButton(icon = Icons.Default.Edit, label = "Renomear", onClick = onRename)
            ActionSheetButton(icon = Icons.Default.Info, label = "Detalhes", onClick = onDetails)
            ActionSheetButton(icon = Icons.Default.Delete, label = "Excluir", onClick = onDelete, destructive = true)
            SpacerBlock()
        }
    }
}

@Composable
private fun ActionSheetButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            softWrap = true,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RenameDownloadBottomSheet(
    item: ExistingDownloadItem,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(item.name) {
        mutableStateOf(
            TextFieldValue(
                text = fileNameWithoutExtension(item.name),
                selection = TextRange(fileNameWithoutExtension(item.name).length)
            )
        )
    }
    val canConfirm = value.text.isNotBlank()
    val fieldShape = RoundedCornerShape(10.dp)
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(item.name) {
        focusRequester.requestFocus()
        keyboardController?.show()
        value = value.copy(selection = TextRange(value.text.length))
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Renomear arquivo", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .height(48.dp),
                    shape = fieldShape,
                    placeholder = {
                        Text(
                            text = fileNameWithoutExtension(item.name),
                            maxLines = 1,
                            softWrap = true,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (canConfirm) {
                                onConfirm(renamePreservingExtension(item.name, value.text))
                            }
                        }
                    )
                )
                OutlinedButton(
                    onClick = { onConfirm(renamePreservingExtension(item.name, value.text)) },
                    enabled = canConfirm,
                    shape = fieldShape,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "OK",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            SpacerBlock()
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DownloadDetailsBottomSheet(
    item: ExistingDownloadItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val details = remember(item.name, item.sourceUrl, item.modifiedAt) {
        DownloadFileActions.details(context, item)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Detalhes", style = MaterialTheme.typography.titleLarge)
            SelectionContainer {
                Text(
                    text = DownloadFileActions.formatDetails(details),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            SpacerBlock()
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    item: ExistingDownloadItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Excluir arquivo",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = "Tem certeza que deseja excluir ${item.name}?",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Excluir", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ConfirmMultiDeleteDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir seleção?") },
        text = {
            Text(
                text = "Tem certeza que deseja excluir $count item(ns)?",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Excluir", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ConfirmDuplicateDownloadDialog(
    itemTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Arquivo repetido") },
        text = {
            Text("Já existe um item com o nome \"$itemTitle\". Deseja baixar mesmo assim? O novo arquivo será salvo com numeração automática.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Baixar mesmo assim")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun UpdateAvailableDialog(
    release: UpdateRelease,
    downloading: Boolean,
    progress: Float,
    onLater: () -> Unit,
    onUpdate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text("Nova versão disponível") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("DLSaver v${release.versionName}")
                if (release.notes.isNotBlank()) {
                    Text(
                        text = release.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (downloading) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                        Text("${(progress.coerceIn(0f, 1f) * 100).toInt()}%")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onUpdate,
                enabled = !downloading
            ) {
                Text(if (downloading) "Baixando" else "Atualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onLater, enabled = !downloading) {
                Text("Talvez mais tarde")
            }
        }
    )
}

@Composable
private fun UpdateStatusItem(
    updateState: UpdateUiState,
    currentVersionName: String,
    installPackagesPermissionGranted: Boolean,
    onRequestInstallPackagesPermission: () -> Unit,
    onInstall: (UpdateRelease) -> Unit
) {
    val latest = updateState.manifest?.latest
    val updateAvailable = latest != null && latest.versionCode > BuildConfig.VERSION_CODE
    val label = if (updateAvailable) "Atualização disponível" else "Versão mais recente instalada"
    val value = if (updateAvailable && latest != null) {
        "v${latest.versionName}"
    } else {
        "v$currentVersionName"
    }

    SettingsListItem(
        label = label,
        value = value,
        trailing = {
            when {
                updateState.checking -> {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
                updateState.downloading -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            progress = { updateState.downloadProgress.coerceIn(0f, 1f) },
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "${(updateState.downloadProgress.coerceIn(0f, 1f) * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                !installPackagesPermissionGranted -> {
                    OutlinedButton(onClick = onRequestInstallPackagesPermission) {
                        Text("Permitir APK")
                    }
                }
                updateAvailable && latest != null -> {
                    Button(onClick = { onInstall(latest) }) {
                        Text("Atualizar")
                    }
                }
            }
        }
    )
}

@Composable
private fun InfoCard(title: String, content: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PermissionListItem(
    label: String,
    granted: Boolean,
    onRequest: () -> Unit,
    canRequest: Boolean
) {
    SettingsListItem(
        label = label,
        value = if (granted) "Concedida" else "Não concedida",
        trailing = {
            if (granted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Concedida",
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF00E676)
                )
            } else if (canRequest) {
                OutlinedButton(
                    onClick = onRequest,
                    modifier = Modifier
                        .height(34.dp)
                        .widthIn(min = 86.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("Conceder", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Spacer(modifier = Modifier.width(28.dp))
            }
        }
    )
}

@Composable
private fun SettingsListItem(
    label: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    showTopDivider: Boolean = false,
    showDivider: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showTopDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 20.dp)
                    .background(Color(0xFF242424))
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .let { base -> if (onClick != null) base.clickable(onClick = onClick) else base }
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFF4F4F4),
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis
                )
                if (!value.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp),
                        fontFamily = FontFamily.SansSerif,
                        color = Color(0xFF8A8A8A),
                        maxLines = 2,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            trailing?.invoke()
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 20.dp)
                    .background(Color(0xFF242424))
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    label: String,
    value: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsListItem(
        label = label,
        value = value,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.graphicsLayer(scaleX = 0.80f, scaleY = 0.80f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFD0D0D0),
                    checkedTrackColor = Color(0xFF6A6A6A),
                    checkedBorderColor = Color.Transparent,
                    uncheckedThumbColor = Color(0xFFB5B5B5),
                    uncheckedTrackColor = Color(0xFF3A3A3A),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    )
}

@Composable
private fun SettingsLinkItem(
    label: String,
    showTopDivider: Boolean = false,
    showDivider: Boolean = true,
    onClick: () -> Unit
) {
    SettingsListItem(
        label = label,
        onClick = onClick,
        showTopDivider = showTopDivider,
        showDivider = showDivider
    )
}

@Composable
private fun SettingsInfoItem(
    label: String,
    value: String
) {
    SettingsListItem(label = label, value = value)
}

@Composable
private fun SettingsHubScreen(
    state: DownloadUiState,
    playerMonochromatic: Boolean,
    installPackagesPermissionGranted: Boolean,
    notificationPermissionGranted: Boolean,
    mediaPermissionsGranted: Boolean,
    legacyStoragePermissionGranted: Boolean,
    allFilesPermissionGranted: Boolean,
    downloadPermissionGranted: Boolean,
    onTogglePlayerMonochromatic: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestMediaPermission: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    onRequestAllFilesPermission: () -> Unit,
    onRequestInstallPackagesPermission: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val updateState by UpdateManager.state.collectAsState()
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp)
        ) {
            SettingsCustomHeader(title = "Configurações")
            SettingsToggleItem(
                label = "Player monocromático",
                value = "Quando ativo, o fundo do player fica preto",
                checked = playerMonochromatic,
                onCheckedChange = onTogglePlayerMonochromatic
            )
            PermissionListItem(
                label = "Permissão de notificações",
                granted = notificationPermissionGranted,
                onRequest = onRequestNotificationPermission,
                canRequest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            )
            PermissionListItem(
                label = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    "Permissão de mídia"
                } else {
                    "Permissão de leitura de arquivos"
                },
                granted = mediaPermissionsGranted,
                onRequest = onRequestMediaPermission,
                canRequest = true
            )
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                PermissionListItem(
                    label = "Permissão de armazenamento para downloads",
                    granted = legacyStoragePermissionGranted && downloadPermissionGranted,
                    onRequest = onRequestStoragePermission,
                    canRequest = true
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                PermissionListItem(
                    label = "Permissão de todos os arquivos",
                    granted = allFilesPermissionGranted,
                    onRequest = onRequestAllFilesPermission,
                    canRequest = true
                )
            }
            PermissionListItem(
                label = "Instalar APKs de atualização",
                granted = installPackagesPermissionGranted,
                onRequest = onRequestInstallPackagesPermission,
                canRequest = true
            )
            UpdateStatusItem(
                updateState = updateState,
                currentVersionName = BuildConfig.VERSION_NAME,
                installPackagesPermissionGranted = installPackagesPermissionGranted,
                onRequestInstallPackagesPermission = onRequestInstallPackagesPermission,
                onInstall = { release ->
                    scope.launch {
                        when (UpdateManager.downloadAndInstall(context.applicationContext, release)) {
                            UpdateInstallResult.RequiresInstallPermission -> onRequestInstallPackagesPermission()
                            is UpdateInstallResult.Failed -> {
                                Toast.makeText(context, updateState.error.ifBlank { "Não foi possível baixar a atualização" }, Toast.LENGTH_LONG).show()
                            }
                            else -> Unit
                        }
                    }
                }
            )
            SettingsInfoItem(
                label = "Destino final",
                value = state.outputDir.ifBlank { "Não definido" }
            )
            SettingsLinkItem(label = "Termos e condições") { onOpenTerms() }
            SettingsLinkItem(label = "Privacidade") { onOpenPrivacy() }
            SettingsLinkItem(label = "Enviar feedback", showDivider = false) { composeFeedbackEmail(context) }
            SettingsLinkItem(label = "Portfólio", showTopDivider = true, showDivider = false) { openExternalLink(context, "https://vxncius.com") }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 24.dp, vertical = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Versão ${BuildConfig.VERSION_NAME} - DLSaver",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 9.sp),
                fontFamily = FontFamily.SansSerif,
                color = Color(0xFF8A8A8A),
                textAlign = TextAlign.Center
            )
            // Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Copyright © 2026 Vxncius - Todos os direitos reservados",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 8.sp),
                fontFamily = FontFamily.SansSerif,
                color = Color(0xFF8A8A8A).copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}
@Composable
private fun ScreenSectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = 54.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .padding(bottom = 5.dp, top = 5.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            color = Color.White,
            softWrap = true,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            maxLines = 1,
            color = Color.White.copy(alpha = 0.72f),
            softWrap = true,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SettingsCustomHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 25.dp, bottom = 15.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            color = Color.White,
            softWrap = true,
            overflow = TextOverflow.Ellipsis
        )
    }
}
@Composable
private fun PermissionCard(
    notificationPermissionGranted: Boolean,
    mediaPermissionsGranted: Boolean,
    legacyStoragePermissionGranted: Boolean,
    downloadPermissionGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    val sdk = Build.VERSION.SDK_INT
    val permissionLines = buildList {
        if (sdk >= Build.VERSION_CODES.TIRAMISU) {
            add(
                "Notificações: " + if (notificationPermissionGranted) {
                    "concedida"
                } else {
                    "não concedida"
                }
            )
            add(
                "Leitura de mídia (áudio e vídeo): " + if (mediaPermissionsGranted) {
                    "concedida"
                } else {
                    "não concedida"
                }
            )
        } else if (sdk >= Build.VERSION_CODES.Q) {
            add(
                "Leitura da mídia local: " + if (mediaPermissionsGranted) {
                    "concedida"
                } else {
                    "não concedida"
                }
            )
        } else {
            add(
                "Leitura do armazenamento: " + if (mediaPermissionsGranted) {
                    "concedida"
                } else {
                    "não concedida"
                }
            )
            add(
                "Gravação no armazenamento (downloads): " + if (legacyStoragePermissionGranted) {
                    "concedida"
                } else {
                    "não concedida"
                }
            )
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Permissões do app", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = permissionLines.joinToString("\n"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis
                )
                if (sdk <= Build.VERSION_CODES.P) {
                    Text(
                        text = if (downloadPermissionGranted) {
                            "Os downloads locais estão liberados neste aparelho."
                        } else {
                            "Sem a permissão de armazenamento, o app não consegue salvar arquivos localmente."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            if (!downloadPermissionGranted && sdk <= Build.VERSION_CODES.P) {
                OutlinedButton(onClick = onRequestPermission) {
                    Text("Conceder")
                }
            }
        }
    }
}

@Composable
private fun ThumbnailBox(
    url: String,
    fallbackUrl: String = "",
    kind: DownloadKind = DownloadKind.VIDEO,
    preferCover: Boolean = false
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val model = url.ifBlank { fallbackUrl }.trim()
    val shape = RoundedCornerShape(10.dp)
    val widthPx = with(density) { 58.dp.roundToPx() }
    val heightPx = with(density) { 58.dp.roundToPx() }
    val fallbackPainter = painterResource(id = R.drawable.music_thumbnail)
    val contentScale = when {
        kind == DownloadKind.AUDIO -> ContentScale.Crop
        preferCover -> ContentScale.Crop
        else -> ContentScale.Crop
    }

    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(shape)
            .border(1.dp, Color(0xFF222222), shape)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (model.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(model)
                    .size(widthPx, heightPx)
                    .precision(Precision.INEXACT)
                    .scale(Scale.FILL)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                placeholder = fallbackPainter,
                error = fallbackPainter
            )
        } else {
            ThumbnailFallback()
        }
    }
}

@Composable
private fun ThumbnailFallback() {
    Image(
        painter = painterResource(id = R.drawable.music_thumbnail),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun BottomNavBar(
    current: AppScreen,
    onNavigate: (AppScreen) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NavButton(
            selected = current == AppScreen.HOME,
            label = "Buscar",
            icon = Icons.Default.Search
        ) { onNavigate(AppScreen.HOME) }
        NavButton(
            selected = current == AppScreen.DOWNLOADED_OR_PROGRESS_LIST_ITEMS,
            label = "Mídias",
            icon = Icons.Default.SmartDisplay
        ) {
            onNavigate(AppScreen.DOWNLOADED_OR_PROGRESS_LIST_ITEMS)
        }
        NavButton(selected = current == AppScreen.SETTINGS, label = "Ajustes", icon = Icons.Default.Settings) {
            onNavigate(AppScreen.SETTINGS)
        }
    }
}

@Composable
private fun RowScope.NavButton(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .alpha(if (selected) 1f else 0.42f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Text(label)
    }
}

@Composable
private fun AppTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 15.dp)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DownloadKindBottomSheet(
    item: SearchResultItem,
    onDismiss: () -> Unit,
    onSelect: (DownloadKind) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThumbnailBox(
                    url = itemThumbnailUrl(item),
                    fallbackUrl = fallbackThumbnailUrlFor(item),
                    kind = if (item.resultType == "audio") DownloadKind.AUDIO else DownloadKind.VIDEO,
                    preferCover = shouldUseCoverThumbnail(item.resultType, item.url)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.author.ifBlank { item.resultType.ifBlank { "Conteúdo" } },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.extra.isNotBlank()) {
                        Text(
                            text = item.extra,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = true,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            TextButton(
                onClick = { onSelect(DownloadKind.VIDEO) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SmartDisplay, contentDescription = null)
                    Text("Baixar como vídeo")
                }
            }
            TextButton(
                onClick = { onSelect(DownloadKind.AUDIO) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null)
                    Text("Baixar como áudio")
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DownloadKindMultiBottomSheet(
    title: String,
    count: Int,
    thumbnailUrl: String,
    onDismiss: () -> Unit,
    onSelect: (DownloadKind) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Baixar seleção",
                style = MaterialTheme.typography.titleLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThumbnailBox(
                    url = thumbnailUrl,
                    kind = DownloadKind.VIDEO,
                    preferCover = false
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$count item(ns) selecionado(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            TextButton(
                onClick = { onSelect(DownloadKind.VIDEO) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SmartDisplay, contentDescription = null)
                    Text("Baixar como vídeo")
                }
            }
            TextButton(
                onClick = { onSelect(DownloadKind.AUDIO) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null)
                    Text("Baixar como áudio")
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
private fun PlayerBottomSheet(
    state: PlaybackUiState,
    monochromatic: Boolean,
    currentItem: ExistingDownloadItem?,
    onDismiss: () -> Unit,
    onOpenItemMenu: (ExistingDownloadItem) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipToPrevious: () -> Unit,
    onSkipToNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val embeddedArtwork = rememberEmbeddedArtwork(state.mediaUri)
    val prevArtwork = rememberEmbeddedArtwork(state.prevMediaUri)
    val nextArtwork = rememberEmbeddedArtwork(state.nextMediaUri)
    val embeddedAmbient = rememberAmbientColors(embeddedArtwork)
    val artworkModel: Any = embeddedArtwork?.let { it } ?: (state.artworkUri ?: R.drawable.music_thumbnail)
    val (displayTitle, displayArtist) = remember(state.title, state.artist) {
        splitTitleArtistForPlayerDisplay(state.title, state.artist)
    }
    var manualLyricsPayload by remember(state.mediaUri?.toString()) { mutableStateOf<LyricsPayload?>(null) }
    val autoLyricsState = rememberLyricsState(
        mediaUri = state.mediaUri,
        rawTitle = state.title,
        rawArtist = state.artist,
        displayTitle = displayTitle,
        displayArtist = displayArtist,
        durationMs = state.durationMs
    )
    val lyricsPayload = manualLyricsPayload ?: autoLyricsState.payload
    val lyricsAvailable = lyricsPayload?.hasLyrics == true
    val lyricsLookupFinished = manualLyricsPayload != null || autoLyricsState.loaded
    val showLyricsBadge = lyricsAvailable || lyricsLookupFinished
    var lyricsVisible by rememberSaveable(state.mediaUri?.toString()) { mutableStateOf(false) }
    var manualLyricsSheetOpen by rememberSaveable(state.mediaUri?.toString()) { mutableStateOf(false) }
    val duration = state.durationMs.coerceAtLeast(0L)
    val position = state.positionMs.coerceIn(0L, if (duration > 0L) duration else Long.MAX_VALUE)
    val sliderValue = if (duration > 0L) position.toFloat() / duration.toFloat() else 0f
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = Color.Black,
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        val bgColors = if (monochromatic) {
            listOf(Color.Black, Color.Black, Color.Black)
        } else {
            embeddedAmbient ?: listOf(Color(0xFF111111), Color(0xFF050505), Color(0xFF111111))
        }

        DisposableEffect(activity, bgColors) {
            val a = activity ?: return@DisposableEffect onDispose { }
            val window = a.window
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
            onDispose { }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            bgColors[0],
                            bgColors.getOrElse(1) { bgColors[0] },
                            Color.Black
                        )
                    )

                )
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val horizontalPadding = 20.dp
            val availableWidth = (maxWidth - horizontalPadding * 2f).coerceAtLeast(0.dp)

            // Garante um "peek" minimo de prev/next. Se a capa ficar grande demais pra tela,
            // a gente reduz automaticamente pra nao sumir o carrossel.
            val pageSpacing = 7.dp
            val minSidePeek = 12.dp
            val desiredCentral = (availableWidth * 0.89f).coerceIn(228.dp, 400.dp)
            val maxCentralForPeek = (availableWidth - (minSidePeek + pageSpacing) * 2f).coerceAtLeast(180.dp)
            var centralSize = if (desiredCentral < maxCentralForPeek) desiredCentral else maxCentralForPeek
            if (availableWidth > 0.dp && centralSize > availableWidth) centralSize = availableWidth
            val pagerSidePadding = ((availableWidth - centralSize) / 2f).coerceAtLeast(0.dp)

            val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })

            // Sempre volta pro centro quando a musica muda (ou quando atualizamos a fila).
            LaunchedEffect(state.mediaUri, state.prevMediaUri, state.nextMediaUri) {
                pagerState.scrollToPage(1)
            }

            // Ao finalizar um swipe pro lado, troca a musica e volta pro centro.
            LaunchedEffect(pagerState, state.prevMediaUri, state.nextMediaUri) {
                snapshotFlow { pagerState.settledPage }
                    .distinctUntilChanged()
                    .collect { page ->
                        when (page) {
                            0 -> {
                                if (state.prevMediaUri != null) onSkipToPrevious()
                                pagerState.scrollToPage(1)
                            }
                            2 -> {
                                if (state.nextMediaUri != null) onSkipToNext()
                                pagerState.scrollToPage(1)
                            }
                        }
                    }
            }

            val contentPaddingTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp
            val contentPaddingBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding)
                    .padding(top = contentPaddingTop)
                    .padding(bottom = contentPaddingBottom)
            ) {
                Box(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Fechar player",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Tocando agora",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                    IconButton(
                        enabled = currentItem != null,
                        onClick = { currentItem?.let(onOpenItemMenu) },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opções da música",
                            tint = Color.White
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .align(Alignment.Center)
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val canPrev = state.prevMediaUri != null
                    val canNext = state.nextMediaUri != null
                    Spacer(modifier = Modifier.weight(1f))
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(centralSize),
                        pageSize = PageSize.Fixed(centralSize),
                        contentPadding = PaddingValues(horizontal = pagerSidePadding),
                        pageSpacing = pageSpacing
                    ) { page ->
                        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        val absOffset = abs(pageOffset).coerceIn(0f, 1f)
                        val scale = 0.54f + (1f - absOffset) * 0.46f
                        val alpha = 0.15f + (1f - absOffset) * 0.85f

                        // Se nao tiver prev/next, ainda mostramos um "ghost" usando a capa atual.
                        // Isso evita o carrossel parecer quebrado quando a fila tem 1 item.
                        val (image, alphaMultiplier) = when (page) {
                            0 -> (if (canPrev) prevArtwork else embeddedArtwork) to (if (canPrev) 1f else 0.22f)
                            2 -> (if (canNext) nextArtwork else embeddedArtwork) to (if (canNext) 1f else 0.22f)
                            else -> embeddedArtwork to 1f
                        }

                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            val isCenterPage = page == 1
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(scaleX = scale, scaleY = scale)
                                    .pointerInput(isCenterPage, lyricsAvailable, lyricsLookupFinished) {
                                        if (!isCenterPage) return@pointerInput
                                        detectTapGestures(
                                            onTap = {
                                                if (lyricsAvailable) {
                                                    lyricsVisible = !lyricsVisible
                                                } else if (lyricsLookupFinished) {
                                                    manualLyricsSheetOpen = true
                                                }
                                            },
                                            onLongPress = {
                                                if (lyricsAvailable || lyricsLookupFinished) {
                                                    manualLyricsSheetOpen = true
                                                }
                                            }
                                        )
                                    }
                            ) {
                                if (isCenterPage && lyricsVisible && lyricsAvailable) {
                                    LyricsCover(
                                        lyrics = lyricsPayload ?: LyricsPayload(),
                                        positionMs = state.positionMs,
                                        backgroundImage = embeddedArtwork,
                                        fallbackModel = artworkModel,
                                        ambientColors = embeddedAmbient,
                                        modifier = Modifier.fillMaxSize(),
                                        alpha = alpha * alphaMultiplier
                                    )
                                } else {
                                    CarouselCover(
                                        image = image,
                                        fallbackModel = artworkModel,
                                        fallback = R.drawable.music_thumbnail,
                                        modifier = Modifier.fillMaxSize(),
                                        alpha = alpha * alphaMultiplier,
                                        isCenter = isCenterPage
                                    )
                                }

                                if (isCenterPage && showLyricsBadge) {
                                    LyricsAvailabilityBadge(
                                        hasLyrics = lyricsAvailable,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = displayTitle.toString().ifBlank { "Reproduzindo" },
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = true,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                    initialDelayMillis = 1200,
                                    repeatDelayMillis = 1200,
                                    spacing = MarqueeSpacing(28.dp),
                                    velocity = 22.dp
                                )
                        )
                        if (displayArtist.isNotBlank()) {
                            Text(
                                text = displayArtist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = true,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PlayerSeekBar(
                            fraction = sliderValue,
                            onFractionChange = { value ->
                                if (duration > 0L) onSeekTo((duration * value).toLong())
                            }
                        )

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(formatTime(position), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                "-" + formatTime((duration - position).coerceAtLeast(0L)),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val shuffleTint = if (state.shuffleEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        val repeatTint = if (state.repeatMode != 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        val repeatIcon = when (state.repeatMode) {
                            1 -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    onCycleRepeatMode()
                                    val nextRepeatLabel = when (state.repeatMode) {
                                        Player.REPEAT_MODE_OFF -> "Repetição da lista ativada"
                                        Player.REPEAT_MODE_ALL -> "Repetição de uma faixa ativada"
                                        else -> "Repetição: desativada"
                                    }
                                    Toast.makeText(context, nextRepeatLabel, Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    repeatIcon,
                                    contentDescription = "Repetir",
                                    tint = repeatTint
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onSkipToPrevious) {
                                    Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = Color.White)
                                }
                                IconButton(
                                    onClick = onTogglePlayPause,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                ) {
                                    Icon(
                                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                IconButton(onClick = onSkipToNext) {
                                    Icon(Icons.Default.SkipNext, contentDescription = "Próximo", tint = Color.White)
                                }
                            }

                            IconButton(
                                onClick = {
                                    onToggleShuffle()
                                    val shuffleLabel = if (state.shuffleEnabled) {
                                        "Shuffle desativado"
                                    } else {
                                        "Shuffle ativado"
                                    }
                                    Toast.makeText(context, shuffleLabel, Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = "Aleatório", tint = shuffleTint)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(126.dp))
                }
            }
        }
    }
    }

    if (manualLyricsSheetOpen) {
        LyricsSearchBottomSheet(
            rawTitle = state.title,
            rawArtist = state.artist,
            displayTitle = displayTitle,
            displayArtist = displayArtist,
            durationMs = state.durationMs,
            onDismiss = { manualLyricsSheetOpen = false },
            onLyricsSelected = {
                manualLyricsPayload = it
                lyricsVisible = true
                manualLyricsSheetOpen = false
            }
        )
    }
}

private data class RememberedLyricsState(
    val payload: LyricsPayload? = null,
    val loaded: Boolean = false
)

@Composable
private fun rememberLyricsState(
    mediaUri: Uri?,
    rawTitle: String,
    rawArtist: String,
    displayTitle: String,
    displayArtist: String,
    durationMs: Long
): RememberedLyricsState {
    val context = LocalContext.current
    var lyricsState by remember(mediaUri, rawTitle, rawArtist, displayTitle, displayArtist, durationMs) {
        mutableStateOf(RememberedLyricsState())
    }

    LaunchedEffect(mediaUri, rawTitle, rawArtist, displayTitle, displayArtist, durationMs) {
        lyricsState = RememberedLyricsState()
        if (mediaUri == null || displayTitle.isBlank() && rawTitle.isBlank()) {
            lyricsState = RememberedLyricsState(loaded = true)
            return@LaunchedEffect
        }
        val payload = runCatching {
            LyricsRepository.load(
                context = context.applicationContext,
                rawTitle = rawTitle,
                rawArtist = rawArtist,
                displayTitle = displayTitle,
                displayArtist = displayArtist,
                durationMs = durationMs
            )
        }.getOrNull()
        lyricsState = RememberedLyricsState(payload = payload, loaded = true)
    }

    return lyricsState
}

@Composable
private fun LyricsCover(
    lyrics: LyricsPayload,
    positionMs: Long,
    backgroundImage: androidx.compose.ui.graphics.ImageBitmap? = null,
    fallbackModel: Any? = null,
    ambientColors: List<Color>? = null,
    modifier: Modifier = Modifier,
    alpha: Float
) {
    val activeIndex = remember(lyrics, positionMs) {
        if (lyrics.syncedLines.isEmpty()) {
            -1
        } else {
            lyrics.syncedLines.indexOfLast { it.timeMs <= positionMs }
        }
    }
    val shape = RoundedCornerShape(26.dp)
    val palette = ambientColors ?: listOf(Color(0xFF353535), Color(0xFF191919), Color(0xFF050505))
    val blurModifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
            scaleX = 1.18f
            scaleY = 1.18f
            this.alpha = 0.96f
        }
        .blur(24.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(palette.getOrElse(1) { Color(0xFF0B0B0B) })
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .alpha(alpha)
    ) {
        when {
            backgroundImage != null -> Image(
                bitmap = backgroundImage,
                contentDescription = null,
                modifier = blurModifier,
                contentScale = ContentScale.Crop
            )

            fallbackModel != null -> AsyncImage(
                model = fallbackModel,
                contentDescription = null,
                modifier = blurModifier,
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            palette.first().copy(alpha = 0.46f),
                            palette.getOrElse(1) { palette.first() }.copy(alpha = 0.70f),
                            palette.getOrElse(2) { Color.Black }.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.07f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.28f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Lyrics",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f)
            )

            if (lyrics.syncedLines.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val from = (activeIndex - 2).coerceAtLeast(0)
                    val to = (from + 5).coerceAtMost(lyrics.syncedLines.size)
                    val visibleLines = lyrics.syncedLines.subList(from, to)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        visibleLines.forEachIndexed { index, line ->
                            val realIndex = from + index
                            val isActive = realIndex == activeIndex.coerceAtLeast(0)
                            Text(
                                text = line.text,
                                color = if (isActive) Color.White else Color.White.copy(alpha = 0.44f),
                                style = if (isActive) {
                                    MaterialTheme.typography.titleMedium
                                } else {
                                    MaterialTheme.typography.bodyMedium
                                },
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    lyrics.plainLyrics
                        .lineSequence()
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .forEach { line ->
                            Text(
                                text = line,
                                color = Color.White.copy(alpha = 0.82f),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun LyricsAvailabilityBadge(
    hasLyrics: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.72f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (hasLyrics) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Letras disponíveis",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Pesquisar letra",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LyricsSearchBottomSheet(
    rawTitle: String,
    rawArtist: String,
    displayTitle: String,
    displayArtist: String,
    durationMs: Long,
    onDismiss: () -> Unit,
    onLyricsSelected: (LyricsPayload) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf(listOf(displayTitle, displayArtist).filter { it.isNotBlank() }.joinToString(" ")) }
    var isSearching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<LyricsSearchChoice>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    val runSearch = {
        keyboardController?.hide()
        focusManager.clearFocus()
        scope.launch {
            isSearching = true
            message = ""
            results = LyricsRepository.searchManual(query, durationMs)
            if (results.isEmpty()) message = "Nenhuma letra encontrada"
            isSearching = false
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Pesquisar letra", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(50),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (query.isNotBlank() && !isSearching) runSearch()
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                ),
                trailingIcon = {
                    IconButton(
                        enabled = query.isNotBlank() && !isSearching,
                        onClick = { runSearch() }
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Pesquisar")
                    }
                },
                placeholder = { Text("Nome da música ou cantor") }
            )
            if (isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (message.isNotBlank()) {
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            results.take(8).forEach { choice ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(choice.trackName.ifBlank { "Letra encontrada" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOf(choice.artistName, choice.albumName).filter { it.isNotBlank() }.joinToString(" â€¢ "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(
                            onClick = {
                                scope.launch {
                                    LyricsRepository.saveManual(
                                        context = context.applicationContext,
                                        rawTitle = rawTitle,
                                        rawArtist = rawArtist,
                                        displayTitle = displayTitle,
                                        displayArtist = displayArtist,
                                        durationMs = durationMs,
                                        payload = choice.payload
                                    )
                                    onLyricsSelected(choice.payload)
                                }
                            }
                        ) {
                            Text("Setar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun VideoPlayerBottomSheet(
    state: PlaybackUiState,
    currentItem: ExistingDownloadItem?,
    onDismiss: () -> Unit,
    onOpenItemMenu: (ExistingDownloadItem) -> Unit,
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val playerView = remember(context) {
        PlayerView(context).apply {
            setUseController(false)
            setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT)
            setBackgroundColor(android.graphics.Color.BLACK)
            setKeepContentOnPlayerReset(true)
            keepScreenOn = true
        }
    }
    val duration = state.durationMs.coerceAtLeast(0L)
    val position = state.positionMs.coerceIn(0L, if (duration > 0L) duration else Long.MAX_VALUE)
    val sliderValue = if (duration > 0L) position.toFloat() / duration.toFloat() else 0f
    val videoAspectRatio = remember(state.videoWidth, state.videoHeight) {
        if (state.videoWidth > 0 && state.videoHeight > 0) {
            state.videoWidth.toFloat() / state.videoHeight.toFloat()
        } else {
            16f / 9f
        }
    }

    var controller by remember { mutableStateOf<MediaController?>(null) }
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var previousOrientation by rememberSaveable { mutableStateOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) }
    val configuration = LocalConfiguration.current
    val standardTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val maxNonFullscreenVideoHeight =
        (configuration.screenHeightDp.dp - standardTopInset - bottomInset - 160.dp).coerceAtLeast(180.dp)
    val controlsAlpha by animateFloatAsState(
        targetValue = if (controlsVisible || !state.isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "videoControlsAlpha"
    )

    LaunchedEffect(Unit) {
        controller = MediaPlayback.connect(context)
    }

    DisposableEffect(controller, playerView) {
        playerView.player = controller
        onDispose {
            if (playerView.player === controller) {
                playerView.player = null
            }
        }
    }

    LaunchedEffect(state.isPlaying, isFullscreen, controlsVisible, state.positionMs) {
        if (state.isPlaying && controlsVisible) {
            kotlinx.coroutines.delay(2_000)
            if (state.isPlaying) controlsVisible = false
        }
    }

    LaunchedEffect(state.mediaUri) {
        controlsVisible = true
    }

    LaunchedEffect(isFullscreen, controlsVisible, activity) {
        if (!isFullscreen || controlsVisible) return@LaunchedEffect
        repeat(6) { pass ->
            if (pass > 0) {
                kotlinx.coroutines.delay(100)
            }
            activity?.window?.let(::applyFullscreenWindow)
        }
    }

    BackHandler {
        if (isFullscreen) {
            controlsVisible = true
            onFullscreenChange(false)
        } else {
            onDismiss()
        }
    }

    DisposableEffect(activity, isFullscreen) {
        val a = activity
        if (a == null) return@DisposableEffect onDispose { }
        val window = a.window
        val decorView = window.decorView
        val focusListener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus && isFullscreen) {
                decorView.post { applyFullscreenWindow(window) }
            }
        }
        @Suppress("DEPRECATION")
        val systemUiListener = View.OnSystemUiVisibilityChangeListener { visibility ->
            val statusBarVisible = visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0
            val navigationBarVisible = visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0
            if (isFullscreen && !controlsVisible && (statusBarVisible || navigationBarVisible)) {
                decorView.post { applyFullscreenWindow(window) }
            }
        }

        if (previousOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            previousOrientation = a.requestedOrientation
        }

        runCatching {
            decorView.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
        }
        @Suppress("DEPRECATION")
        decorView.setOnSystemUiVisibilityChangeListener(systemUiListener)

        if (isFullscreen) {
            a.requestedOrientation = if (videoAspectRatio < 1f) {
                previousOrientation
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        } else {
            a.requestedOrientation = previousOrientation
        }

        onDispose {
            runCatching {
                val observer = decorView.viewTreeObserver
                if (observer.isAlive) {
                    observer.removeOnWindowFocusChangeListener(focusListener)
                }
            }
            @Suppress("DEPRECATION")
            decorView.setOnSystemUiVisibilityChangeListener(null)
            runCatching {
                a.requestedOrientation = previousOrientation
                clearFullscreenWindow(window)
            }
        }
    }

    LaunchedEffect(activity, isFullscreen, controlsVisible) {
        val window = activity?.window ?: return@LaunchedEffect
        if (isFullscreen) {
            if (controlsVisible) {
                showVideoSystemBars(window, isFullscreen = true)
            } else {
                applyFullscreenWindow(window)
            }
        } else {
            clearFullscreenWindow(window)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(state.isPlaying, controlsVisible) {
                detectTapGestures {
                    controlsVisible = !controlsVisible
                }
            }
    ) {
            Box(
                modifier = if (isFullscreen) {
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .heightIn(max = maxNonFullscreenVideoHeight)
                        .aspectRatio(videoAspectRatio, matchHeightConstraintsFirst = videoAspectRatio < 1f)
                        .background(Color.Black)
                },
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { playerView },
                    update = { it.player = controller },
                    modifier = Modifier.fillMaxSize()
                )

                IconButton(
                    enabled = controlsAlpha > 0.05f,
                    onClick = {
                        controlsVisible = true
                        onTogglePlayPause()
                    },
                    modifier = Modifier
                        .size(86.dp)
                        .graphicsLayer(alpha = controlsAlpha)
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(62.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(
                        top = standardTopInset + 12.dp,
                        start = if (isFullscreen) 20.dp else 24.dp,
                        end = if (isFullscreen) 20.dp else 24.dp
                    )
                    .graphicsLayer(alpha = controlsAlpha),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon(
                //     imageVector = Icons.Default.KeyboardArrowDown,
                //     contentDescription = "Fechar",
                //     tint = Color.White,
                //     modifier = Modifier
                //         .clickable(enabled = controlsAlpha > 0.05f) {
                //             controlsVisible = true
                //             if (isFullscreen) onFullscreenChange(false) else onDismiss()
                //         }
                // )

                Text(
                    text = state.title.ifBlank { "Video" },
                    color = Color.White,
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp)
                )

                IconButton(
                    enabled = currentItem != null && controlsAlpha > 0.05f,
                    onClick = {
                        controlsVisible = true
                        currentItem?.let(onOpenItemMenu)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opções do vídeo",
                        tint = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = bottomInset + if (isFullscreen) 46.dp else 46.dp)
                    .graphicsLayer(alpha = controlsAlpha),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        enabled = controlsAlpha > 0.05f,
                        onClick = {
                            controlsVisible = true
                            onFullscreenChange(!isFullscreen)
                        }
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullscreen) "Sair do fullscreen" else "Fullscreen",
                            tint = Color.White
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(formatTime(position), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.size(10.dp))
                    PlayerSeekBar(
                        modifier = Modifier.weight(1f),
                        fraction = sliderValue,
                        onFractionChange = { value ->
                            controlsVisible = true
                            if (duration > 0L) onSeekTo((duration * value).toLong())
                        }
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(formatTime(duration), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
}
@Composable
private fun PlayerSeekBar(
    modifier: Modifier = Modifier,
    fraction: Float,
    onFractionChange: (Float) -> Unit
) {
    val trackActive = Color(0xFF9B9B9B)
    val trackInactive = Color(0xFF2A2A2A)
    val thumbColor = Color.White
    val trackHeight = 4.dp
    val thumbRadius = 4.5.dp

    var localFraction by remember { mutableStateOf(fraction.coerceIn(0f, 1f)) }
    var dragging by remember { mutableStateOf(false) }

    LaunchedEffect(fraction) {
        if (!dragging) localFraction = fraction.coerceIn(0f, 1f)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
    ) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val density = LocalDensity.current
        val trackPx = with(density) { trackHeight.toPx() }
        val radiusPx = with(density) { thumbRadius.toPx() }

        val centerY = heightPx / 2f
        val startX = radiusPx
        val endX = (widthPx - radiusPx).coerceAtLeast(startX + 1f)
        val usable = (endX - startX).coerceAtLeast(1f)

        fun updateFromX(x: Float) {
            val next = ((x - startX) / usable).coerceIn(0f, 1f)
            localFraction = next
            onFractionChange(next)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(usable) {
                    detectTapGestures { offset -> updateFromX(offset.x) }
                }
                .pointerInput(usable) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragging = true
                            updateFromX(offset.x)
                        },
                        onDragCancel = { dragging = false },
                        onDragEnd = { dragging = false },
                        onDrag = { change, _ ->
                            updateFromX(change.position.x)
                        }
                    )
                }
        ) {
            val x = startX + usable * localFraction
            drawLine(
                color = trackInactive,
                start = Offset(startX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = trackPx,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawLine(
                color = trackActive,
                start = Offset(startX, centerY),
                end = Offset(x, centerY),
                strokeWidth = trackPx,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawCircle(
                color = thumbColor,
                radius = radiusPx,
                center = Offset(x, centerY)
            )
        }
    }
}

private fun splitTitleArtistForDisplay(rawTitle: String, rawArtist: String): Pair<String, String> {
    val title = rawTitle.trim()
    val artist = rawArtist.trim()
    if (title.isBlank()) return "" to artist

    fun normalize(s: String): String =
        s.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")

    fun cleanPart(s: String): String {
        var out = s.trim()
        // Remove separadores sobrando no comeco/fim (apenas display).
        out = out.trim { it == '-' || it == '\u2013' || it == '\u2014' || it == '|' || it == '/' || it.isWhitespace() }
        out = out.replace(Regex("\\s+"), " ").trim()
        return out
    }

    // Se o artista veio da tag, e o titulo ja inclui "Artista - Titulo", removemos o prefixo de forma segura.
    if (artist.isNotBlank()) {
        val nArtist = normalize(artist)
        val patterns = listOf(" - ", " – ", " — ", " | ", " / ")
        for (delim in patterns) {
            // Match bem restrito: "artist{delim}..."
            if (normalize(title).startsWith(nArtist + delim.trim(), ignoreCase = true)) {
                // A linha acima usa normalize() (colapsa espacos), entao fazemos o corte por regex no original.
                val rx = Regex("^\\s*${Regex.escape(artist)}\\s*[-–—|/]\\s+", RegexOption.IGNORE_CASE)
                val cut = rx.replace(title, "")
                val cleanedTitle = cleanPart(cut)
                return cleanedTitle to cleanPart(artist)
            }
        }
        return cleanPart(title) to cleanPart(artist)
    }

    // Sem artista: tenta extrair "Artista - Titulo" apenas com delimitadores com espacos ao redor (bem conservador).
    val delimiters = listOf(" - ", " – ", " — ", " | ", " / ")
    val idx = delimiters
        .map { d -> title.indexOf(d).takeIf { it > 0 }?.let { it to d } }
        .filterNotNull()
        .minByOrNull { it.first }

    if (idx != null) {
        val (pos, delim) = idx
        val left = title.substring(0, pos)
        val right = title.substring(pos + delim.length)
        val leftClean = cleanPart(left)
        val rightClean = cleanPart(right)

        // Garante que os dois lados parecem texto "real" (evita cortar coisas esquisitas).
        val hasLettersLeft = leftClean.any { it.isLetter() }
        val hasLettersRight = rightClean.any { it.isLetter() }
        if (leftClean.isNotBlank() && rightClean.isNotBlank() && hasLettersLeft && hasLettersRight) {
            return rightClean to leftClean
        }
    }

    return cleanPart(title) to ""
}

private fun splitTitleArtistForPlayerDisplay(rawTitle: String, rawArtist: String): Pair<String, String> {
    val title = rawTitle.trim()
    val artist = rawArtist.trim()
    if (title.isBlank()) return "" to artist

    val delimiters = listOf(" - ", " – ", " — ", " | ", " / ")

    fun normalize(value: String): String {
        return value.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
    }

    fun cleanPart(value: String): String {
        return value
            .trim()
            .replace(Regex("""^(?:-|–|—|\||/|\s)+|(?:-|–|—|\||/|\s)+$"""), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    if (artist.isNotBlank()) {
        val normalizedArtist = normalize(artist)
        val normalizedTitle = normalize(title)
        for (delimiter in delimiters) {
            if (normalizedTitle.startsWith(normalizedArtist + delimiter.trim(), ignoreCase = true)) {
                val prefixRegex = Regex(
                    "^\\s*${Regex.escape(artist)}\\s*(?:-|–|—|\\||/)\\s+",
                    RegexOption.IGNORE_CASE
                )
                return cleanPart(prefixRegex.replace(title, "")) to cleanPart(artist)
            }
        }
        return cleanPart(title) to cleanPart(artist)
    }

    val idx = delimiters
        .map { delimiter -> title.indexOf(delimiter).takeIf { it > 0 }?.let { it to delimiter } }
        .filterNotNull()
        .minByOrNull { it.first }

    if (idx != null) {
        val (pos, delimiter) = idx
        val left = cleanPart(title.substring(0, pos))
        val right = cleanPart(title.substring(pos + delimiter.length))
        if (left.isNotBlank() && right.isNotBlank() && left.any { it.isLetter() } && right.any { it.isLetter() }) {
            return right to left
        }
    }

    return cleanPart(title) to ""
}

@Composable
private fun rememberAmbientColors(image: androidx.compose.ui.graphics.ImageBitmap?): List<Color>? {
    var colors by remember(image) { mutableStateOf<List<Color>?>(null) }
    LaunchedEffect(image) {
        colors = if (image == null) {
            null
        } else {
            withContext(Dispatchers.Default) {
                computeAmbientColors(image.asAndroidBitmap())
            }
        }
    }
    return colors
}

private fun computeAmbientColors(bitmap: Bitmap): List<Color> {
    val size = 64
    val scaled = runCatching {
        Bitmap.createScaledBitmap(bitmap, size, size, true)
    }.getOrNull() ?: bitmap
    val createdScaled = scaled !== bitmap

    val palette = runCatching {
        Palette.from(scaled)
            .clearFilters()
            .maximumColorCount(16)
            .generate()
    }.getOrNull()

    val swatches = listOfNotNull(
        palette?.vibrantSwatch,
        palette?.lightVibrantSwatch,
        palette?.mutedSwatch,
        palette?.darkVibrantSwatch,
        palette?.darkMutedSwatch,
        palette?.dominantSwatch
    )

    val base = swatches
        .map { Color(it.rgb) }
        .distinctBy { it.value }
        .toMutableList()

    if (base.isEmpty()) {
        if (createdScaled) scaled.recycle()
        return listOf(Color(0xFF2A2A2A), Color(0xFF101010), Color(0xFF3A3A3A))
    }

    while (base.size < 3) {
        val c = base.last()
        base += adjustRgb(c, if (base.size == 1) 0.72f else 1.22f)
    }

    if (createdScaled) scaled.recycle()
    return base.take(3)
}

private fun adjustRgb(color: Color, factor: Float): Color {
    return Color(
        red = (color.red * factor).coerceIn(0f, 1f),
        green = (color.green * factor).coerceIn(0f, 1f),
        blue = (color.blue * factor).coerceIn(0f, 1f),
        alpha = 1f
    )
}

@Composable
private fun CarouselCover(
    image: androidx.compose.ui.graphics.ImageBitmap? = null,
    fallback: Int = R.drawable.music_thumbnail,
    fallbackModel: Any? = null,
    modifier: Modifier = Modifier,
    alpha: Float,
    isCenter: Boolean = false
) {
    // Centro continua 1x1 (size x size), mas com cantos arredondados (nao pontudo).
    val shape = RoundedCornerShape(if (isCenter) 26.dp else 14.dp)
    val bg = Color(0xFF0B0B0B)
    val modifier2 = modifier
        .clip(shape)
        .background(bg)
        .alpha(alpha)

    if (alpha <= 0f) {
        Box(modifier = modifier2)
        return
    }

    Box(modifier = modifier.alpha(alpha)) {
        when {
            image != null -> Image(bitmap = image, contentDescription = null, modifier = modifier2, contentScale = ContentScale.Crop)
            fallbackModel is androidx.compose.ui.graphics.ImageBitmap -> Image(
                bitmap = fallbackModel,
                contentDescription = null,
                modifier = modifier2,
                contentScale = ContentScale.Crop
            )
            fallbackModel != null -> AsyncImage(model = fallbackModel, contentDescription = null, modifier = modifier2, contentScale = ContentScale.Crop)
            else -> AsyncImage(model = fallback, contentDescription = null, modifier = modifier2, contentScale = ContentScale.Crop)
        }
    }
}

@Composable
private fun CarouselDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(if (active) 7.dp else 6.dp)
            .clip(CircleShape)
            .background(if (active) Color.White else Color(0xFF3A3A3A))
    )
}

@Composable
private fun PeekCover(
    image: androidx.compose.ui.graphics.ImageBitmap? = null,
    fallback: Int = R.drawable.music_thumbnail,
    height: androidx.compose.ui.unit.Dp,
    width: androidx.compose.ui.unit.Dp,
    alpha: Float,
    alignEnd: Boolean
) {
    val shape = RoundedCornerShape(22.dp)
    val containerModifier = Modifier
        .width(width)
        .height(height)
        .clip(shape)
        .background(Color(0xFF0B0B0B))
        .border(1.dp, Color(0xFF1A1A1A), shape)
        .alpha(alpha)

    if (alpha <= 0f) {
        Box(modifier = containerModifier)
        return
    }

    Box(modifier = containerModifier) {
        val imageModifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .align(if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart)

        if (image != null) {
            Image(bitmap = image, contentDescription = null, modifier = imageModifier, contentScale = ContentScale.Crop)
        } else {
            AsyncImage(model = fallback, contentDescription = null, modifier = imageModifier, contentScale = ContentScale.Crop)
        }
    }
}

@Composable
private fun MiniPlayerBar(
    state: PlaybackUiState,
    onOpen: () -> Unit,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues = PaddingValues(start = 4.dp, end = 4.dp, bottom = 4.dp)
) {
    val embeddedArtwork = rememberEmbeddedArtwork(state.mediaUri)
    val artworkModel: Any = embeddedArtwork?.let { it } ?: (state.artworkUri ?: R.drawable.music_thumbnail)
    val (displayTitle, displayArtist) = remember(state.title, state.artist) {
        splitTitleArtistForPlayerDisplay(state.title, state.artist)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(outerPadding)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xFF111111))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (artworkModel) {
                is androidx.compose.ui.graphics.ImageBitmap -> {
                    Image(
                        bitmap = artworkModel,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0B0B0B)),
                        contentScale = ContentScale.Crop
                    )
                }

                else -> {
                    AsyncImage(
                        model = artworkModel,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0B0B0B)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTitle.toString().ifBlank { "Reproduzindo" },
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )
                if (displayArtist.isNotBlank()) {
                    Text(
                        text = displayArtist,
                        maxLines = 1,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF2A2A2A))
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun rememberEmbeddedArtwork(mediaUri: Uri?): androidx.compose.ui.graphics.ImageBitmap? {
    val context = LocalContext.current
    var image by remember(mediaUri) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(mediaUri) {
        image = null
        if (mediaUri == null) return@LaunchedEffect

        image = withContext(Dispatchers.IO) {
            UiArtworkCache.loadEmbeddedArtwork(context, mediaUri)
        }
    }

    return image
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private fun hideVideoStatusBar(window: Window) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowInsetsControllerCompat(window, window.decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        isAppearanceLightStatusBars = false
        hide(WindowInsetsCompat.Type.statusBars())
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.systemBarsBehavior =
            android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.insetsController?.hide(android.view.WindowInsets.Type.statusBars())
    }
}

private fun showVideoSystemBars(window: Window, isFullscreen: Boolean) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = if (isFullscreen) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }
        }
    }
    WindowInsetsControllerCompat(window, window.decorView).apply {
        isAppearanceLightStatusBars = false
        isAppearanceLightNavigationBars = false
        show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.show(
            android.view.WindowInsets.Type.statusBars() or
                android.view.WindowInsets.Type.navigationBars()
        )
    }
}

private fun applyFullscreenWindow(window: Window) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
    WindowInsetsControllerCompat(window, window.decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        isAppearanceLightStatusBars = false
        isAppearanceLightNavigationBars = false
        hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.systemBarsBehavior =
            android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.insetsController?.hide(
            android.view.WindowInsets.Type.statusBars() or
                android.view.WindowInsets.Type.navigationBars()
        )
    }

    window.decorView.post {
        WindowInsetsControllerCompat(window, window.decorView).hide(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(
                android.view.WindowInsets.Type.statusBars() or
                    android.view.WindowInsets.Type.navigationBars()
            )
        }

    }
}

private fun clearFullscreenWindow(window: Window) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        }
    }
    WindowInsetsControllerCompat(window, window.decorView).show(
        WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.show(
            android.view.WindowInsets.Type.statusBars() or
                android.view.WindowInsets.Type.navigationBars()
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (true) {
        when (current) {
            is Activity -> return current
            is ContextWrapper -> current = current.baseContext
            else -> return null
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun openExternalLink(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

private fun composeFeedbackEmail(context: Context) {
    runCatching {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:vxncius@hotmail.com?subject=Feedback%20do%20app%20DLSaver")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

private fun loadStyledAssetDocument(context: Context, assetPath: String): String {
    val assetName = assetPath.removePrefix("file:///android_asset/").trim()
    if (assetName.isBlank()) return ""

    val rawHtml = runCatching {
        context.assets.open(assetName).bufferedReader().use { it.readText() }
    }.getOrDefault("")
    if (rawHtml.isBlank()) return ""

    val styleBlock = """
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <style>
            html, body {
                margin: 0;
                padding: 0;
                background: #ffffff !important;
                color: #111111 !important;
                font-size: 14px !important;
                line-height: 1.55 !important;
                -webkit-text-size-adjust: 100% !important;
                text-size-adjust: 100% !important;
            }
            body {
                padding: 20px 18px 28px 18px !important;
                font-family: sans-serif !important;
            }
            h1, h2, h3, h4, h5, h6 {
                color: #111111 !important;
                line-height: 1.3 !important;
                margin-top: 0 !important;
            }
            p, li, span, div, a {
                color: #111111 !important;
                font-size: 14px !important;
            }
            a {
                text-decoration: underline !important;
            }
        </style>
    """.trimIndent()

    return if ("<head" in rawHtml.lowercase()) {
        rawHtml.replaceFirst(Regex("(?i)</head>"), "$styleBlock</head>")
    } else {
        """
        <html>
            <head>$styleBlock</head>
            <body>$rawHtml</body>
        </html>
        """.trimIndent()
    }
}

private fun screenTitle(screen: AppScreen): String {
    return when (screen) {
        AppScreen.HOME -> "DLSaver"
        AppScreen.LIST_ITEMS_FOR_DOWNLOAD -> "Resultados"
        AppScreen.DOWNLOADED_OR_PROGRESS_LIST_ITEMS -> "Downloads"
        AppScreen.SETTINGS -> "Ajustes"
        AppScreen.SETTINGS_DOCUMENT -> "Documento"
    }
}

private fun fallbackThumbnailUrlFor(item: SearchResultItem): String {
    return if (
        (item.resultType == "video" || item.resultType.isBlank()) &&
        item.id.isNotBlank()
    ) {
        "https://i.ytimg.com/vi/${item.id}/hqdefault.jpg"
    } else {
        ""
    }
}

private fun itemThumbnailUrl(item: SearchResultItem): String {
    return thumbnailForKind(
        rawThumbnailUrl = item.thumbnailUrl.ifBlank { fallbackThumbnailUrlFor(item) },
        kind = if (item.resultType == "audio") DownloadKind.AUDIO else DownloadKind.VIDEO
    )
}

private fun thumbnailForKind(rawThumbnailUrl: String, kind: DownloadKind): String {
    if (rawThumbnailUrl.isBlank()) return ""
    if (rawThumbnailUrl.startsWith("http://")) {
        return rawThumbnailUrl.replaceFirst("http://", "https://")
    }
    return rawThumbnailUrl
}

private fun shouldUseCoverThumbnail(resultType: String = "", sourceUrl: String = ""): Boolean {
    val combined = "${resultType.lowercase()} ${sourceUrl.lowercase()}"
    return listOf("reel", "short", "shorts", "tiktok", "tik tok").any { it in combined }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SimultaneousDownloadsBottomSheet(
    currentValue: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    var sliderValue by remember(currentValue) { mutableStateOf(currentValue.toFloat()) }
    val dismissAndSave = {
        onSelect(sliderValue.toInt())
        onDismiss()
    }

    ModalBottomSheet(onDismissRequest = dismissAndSave) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Downloads simultâneos", style = MaterialTheme.typography.titleLarge)
            Text(
                text = sliderValue.toInt().toString(),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
            )
            Slider(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .graphicsLayer(scaleX = 1f, scaleY = 0.42f),
                value = sliderValue,
                onValueChange = { sliderValue = it.roundToInt().toFloat() },
                valueRange = 1f..4f,
                steps = 2
            )
            SpacerBlock()
        }
    }
}

@Composable
private fun SpacerBlock() {
    Box(modifier = Modifier.height(8.dp))
}

private fun shouldConfirmDuplicateDownload(
    state: DownloadUiState,
    item: SearchResultItem,
    kind: DownloadKind
): Boolean {
    val targetBaseName = normalizedDownloadBaseName(sanitizeDownloadTitle(item.title))
    if (targetBaseName.isBlank()) return false

    return state.existingDownloads.any { existing ->
        existing.kind == kind && normalizedDownloadBaseName(existing.name) == targetBaseName
    } || state.downloadJobs.any { job ->
        job.kind == kind && normalizedDownloadBaseName(job.title) == targetBaseName
    }
}

private fun DownloadEnqueueResult.toastMessage(): String {
    if (enqueuedCount > 1 || ignoredCount > 0) {
        return buildString {
            append(enqueuedCount)
            append(" enfileirado(s)")
            if (ignoredCount > 0) {
                append(" | ")
                append(ignoredCount)
                append(" ignorado(s)")
            }
        }
    }
    return when (status) {
        DownloadEnqueueStatus.ENQUEUED -> "Download iniciado: $title"
        DownloadEnqueueStatus.DUPLICATE_ACTIVE -> "Download duplicado ignorado"
        DownloadEnqueueStatus.DUPLICATE_EXISTING -> "Arquivo já existe na biblioteca"
    }
}

private fun Set<String>.toggle(value: String): Set<String> {
    return if (value in this) this - value else this + value
}

