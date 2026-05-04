package com.vxncius.dlsaver

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.LruCache
import androidx.media3.common.C
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object MediaPlayback {
    @Volatile
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null
    private var metadataEnrichmentJob: Job? = null
    private var metadataEnrichmentKey: String? = null
    private var listenerAttached = false
    private var appContext: Context? = null

    private const val EXTRA_KIND = "dlsaver_kind"
    private const val EXTRA_KIND_AUDIO = "audio"
    private const val EXTRA_KIND_VIDEO = "video"
    private const val EXTRA_METADATA_ENRICHED = "dlsaver_metadata_enriched"

    private val playbackMetadataCache = object : LruCache<String, CachedPlaybackMetadata>(12 * 1024) {
        override fun sizeOf(key: String, value: CachedPlaybackMetadata): Int {
            return ((value.artworkData?.size ?: 0) / 1024).coerceAtLeast(1)
        }
    }

    suspend fun playFromLibrary(
        context: Context,
        items: List<ExistingDownloadItem>,
        startItem: ExistingDownloadItem
    ) {
        if (startItem.kind != DownloadKind.AUDIO) return

        // Mantem a mesma ordem exibida na biblioteca local (pasta/MediaStore).
        val playlist = items.filter { it.kind == DownloadKind.AUDIO }

        val startIndex = playlist.indexOfFirst { it.sourceUrl == startItem.sourceUrl }
            .coerceAtLeast(0)

        val mediaItems = playlist.map { item ->
            val uri = Uri.parse(item.sourceUrl)
            val fallbackTitle = fileNameWithoutExtension(item.name)
            val cached = playbackMetadataCache.get(uri.toString())
            buildMediaItem(
                uri = uri,
                mimeType = item.mimeType,
                title = cached?.title?.ifBlank { fallbackTitle } ?: fallbackTitle,
                artist = cached?.artist.orEmpty(),
                artworkData = cached?.artworkData,
                artworkUri = item.thumbnailUrl.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: defaultArtworkUri(context),
                kind = EXTRA_KIND_AUDIO,
                metadataEnriched = cached != null
            )
        }

        val mediaController = getController(context)
        withContext(Dispatchers.Main) {
            val canReuseQueue = sameQueue(mediaController, mediaItems)
            if (!canReuseQueue) {
                mediaController.setMediaItems(mediaItems, startIndex, 0L)
                mediaController.prepare()
            } else {
                mediaController.seekToDefaultPosition(startIndex)
            }
            mediaController.playWhenReady = true
            mediaController.play()
            refreshUiState(mediaController)
            maybeEnrichCurrentMediaItem(context, mediaController)
        }
    }

    suspend fun playExternalAudio(
        context: Context,
        uri: Uri,
        title: String = ""
    ) {
        val resolverMime = runCatching { context.contentResolver.getType(uri).orEmpty() }.getOrDefault("")
        val fallbackTitle = fileNameWithoutExtension(title).trim().ifBlank { "Audio" }
        val cached = playbackMetadataCache.get(uri.toString())
        val mediaItem = buildMediaItem(
            uri = uri,
            mimeType = resolverMime,
            title = cached?.title?.ifBlank { fallbackTitle } ?: fallbackTitle,
            artist = cached?.artist.orEmpty(),
            artworkData = cached?.artworkData,
            artworkUri = defaultArtworkUri(context),
            kind = EXTRA_KIND_AUDIO,
            metadataEnriched = cached != null
        )

        val mediaController = getController(context)
        withContext(Dispatchers.Main) {
            mediaController.shuffleModeEnabled = false
            mediaController.repeatMode = Player.REPEAT_MODE_OFF
            mediaController.setMediaItems(listOf(mediaItem), 0, 0L)
            mediaController.prepare()
            mediaController.playWhenReady = true
            mediaController.play()
            refreshUiState(mediaController)
            maybeEnrichCurrentMediaItem(context, mediaController)
        }
    }

    suspend fun playExternalVideo(
        context: Context,
        uri: Uri,
        title: String = ""
    ) {
        val resolverMime = runCatching { context.contentResolver.getType(uri).orEmpty() }.getOrDefault("")
        val fallbackTitle = fileNameWithoutExtension(title).trim().ifBlank { "Video" }
        val cached = playbackMetadataCache.get(uri.toString())
        val mediaItem = buildMediaItem(
            uri = uri,
            mimeType = resolverMime,
            title = cached?.title?.ifBlank { fallbackTitle } ?: fallbackTitle,
            artist = cached?.artist.orEmpty(),
            artworkData = cached?.artworkData,
            artworkUri = defaultArtworkUri(context),
            kind = EXTRA_KIND_VIDEO,
            metadataEnriched = cached != null
        )

        val mediaController = getController(context)
        withContext(Dispatchers.Main) {
            mediaController.shuffleModeEnabled = false
            mediaController.repeatMode = Player.REPEAT_MODE_OFF
            // Fila de 1 item: a notificacao fica so com play/pause + seekbar (sem prev/next).
            mediaController.setMediaItems(listOf(mediaItem), 0, 0L)
            mediaController.prepare()
            mediaController.playWhenReady = true
            mediaController.play()
            refreshUiState(mediaController)
            maybeEnrichCurrentMediaItem(context, mediaController)
        }
    }

    // Usado pela UI de video (StyledPlayerView) pra plugar no mesmo player da Session.
    suspend fun connect(context: Context): MediaController = getController(context)

    suspend fun prewarm(context: Context) {
        runCatching {
            val mediaController = getController(context.applicationContext)
            restoreSavedSessionIfEmpty(context.applicationContext, mediaController)
        }
    }

    suspend fun togglePlayPause(context: Context) {
        val mediaController = getController(context)
        withContext(Dispatchers.Main) {
            if (mediaController.isPlaying) {
                mediaController.pause()
            } else {
                if (mediaController.mediaItemCount <= 0) return@withContext
                when (mediaController.playbackState) {
                    Player.STATE_IDLE -> {
                        mediaController.prepare()
                    }
                    Player.STATE_ENDED -> {
                        mediaController.seekToDefaultPosition()
                        mediaController.prepare()
                    }
                }
                mediaController.playWhenReady = true
                mediaController.play()
            }
            refreshUiState(mediaController)
        }
    }

    suspend fun seekTo(context: Context, positionMs: Long) {
        val mediaController = getController(context)
        withContext(Dispatchers.Main) {
            mediaController.seekTo(positionMs.coerceAtLeast(0L))
            refreshUiState(mediaController)
        }
    }

    suspend fun skipToNext(context: Context) {
        val mediaController = getController(context)
        withContext(Dispatchers.Main) {
            mediaController.seekToNext()
            refreshUiState(mediaController)
        }
    }

    suspend fun skipToPrevious(context: Context) {
        val mediaController = getController(context)
        withContext(Dispatchers.Main) {
            mediaController.seekToPrevious()
            refreshUiState(mediaController)
        }
    }

    suspend fun toggleShuffle(context: Context) {
        val mediaController = getController(context)
        withContext(Dispatchers.Main) {
            mediaController.shuffleModeEnabled = !mediaController.shuffleModeEnabled
            refreshUiState(mediaController)
        }
    }

    suspend fun cycleRepeatMode(context: Context) {
        val mediaController = getController(context)
        withContext(Dispatchers.Main) {
            val next = when (mediaController.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            mediaController.repeatMode = next
            refreshUiState(mediaController)
        }
    }

    suspend fun stop(context: Context) {
        withContext(Dispatchers.Main) {
            stopProgressLoop()
            metadataEnrichmentJob?.cancel()
            metadataEnrichmentJob = null
            metadataEnrichmentKey = null

            controller?.let { mediaController ->
                runCatching {
                    mediaController.pause()
                    mediaController.stop()
                    mediaController.clearMediaItems()
                }
                runCatching { mediaController.release() }
            }

            runCatching { controllerFuture?.cancel(true) }
            controller = null
            controllerFuture = null
            listenerAttached = false
            _uiState.value = PlaybackUiState()
            PlaybackSessionStore.clear(context)

            runCatching {
                context.applicationContext.stopService(
                    Intent(context.applicationContext, DlsaverPlaybackService::class.java)
                )
            }
        }
    }

    suspend fun removeDeletedItems(
        context: Context,
        deletedItems: List<ExistingDownloadItem>
    ) {
        val deletedUris = deletedItems
            .map { it.sourceUrl }
            .filter { it.isNotBlank() }
            .toSet()
        if (deletedUris.isEmpty()) return

        val mediaController = controller ?: return
        withContext(Dispatchers.Main) {
            if (mediaController.mediaItemCount <= 0) return@withContext

            val currentIndex = mediaController.currentMediaItemIndex.coerceAtLeast(0)
            val currentUri = mediaController.currentMediaItem?.localConfiguration?.uri?.toString().orEmpty()
            val removingCurrent = currentUri in deletedUris
            val wasPlaying = mediaController.isPlaying || mediaController.playWhenReady
            val currentPosition = mediaController.currentPosition.coerceAtLeast(0L)

            val remaining = buildList {
                for (index in 0 until mediaController.mediaItemCount) {
                    val item = mediaController.getMediaItemAt(index)
                    val uri = item.localConfiguration?.uri?.toString().orEmpty()
                    if (uri !in deletedUris) add(item)
                }
            }

            if (remaining.isEmpty()) {
                mediaController.pause()
                mediaController.stop()
                mediaController.clearMediaItems()
                refreshUiState(mediaController)
                PlaybackSessionStore.clear(context)
                return@withContext
            }

            val nextIndex = if (removingCurrent) {
                currentIndex.coerceAtMost(remaining.lastIndex)
            } else {
                remaining.indexOfFirst {
                    it.localConfiguration?.uri?.toString().orEmpty() == currentUri
                }.takeIf { it >= 0 } ?: currentIndex.coerceAtMost(remaining.lastIndex)
            }
            val nextPosition = if (removingCurrent) 0L else currentPosition

            mediaController.setMediaItems(remaining, nextIndex, nextPosition)
            mediaController.prepare()
            mediaController.playWhenReady = wasPlaying
            if (wasPlaying) {
                mediaController.play()
            }
            refreshUiState(mediaController)
            maybeEnrichCurrentMediaItem(context, mediaController)
        }
    }

    private suspend fun getController(context: Context): MediaController {
        appContext = context.applicationContext
        controller?.let { return it }
        val existingFuture = controllerFuture
        if (existingFuture != null) {
            return runCatching {
                existingFuture.await(ContextCompat.getMainExecutor(context)).also { created ->
                    controller = created
                    attachListenerIfNeeded(context, created)
                }
            }.getOrElse {
                controller = null
                controllerFuture = null
                listenerAttached = false
                buildController(context)
            }
        }

        return buildController(context)
    }

    private suspend fun buildController(context: Context): MediaController {
        appContext = context.applicationContext
        val token = SessionToken(context, DlsaverPlaybackService.componentName(context))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        return runCatching {
            future.await(ContextCompat.getMainExecutor(context)).also { created ->
                controller = created
                attachListenerIfNeeded(context, created)
            }
        }.getOrElse { error ->
            controller = null
            controllerFuture = null
            listenerAttached = false
            throw error
        }
    }

    private fun defaultArtworkUri(context: Context): Uri {
        return Uri.parse("android.resource://${context.packageName}/${R.drawable.music_thumbnail}")
    }

    private fun attachListenerIfNeeded(context: Context, mediaController: MediaController) {
        if (listenerAttached) return
        listenerAttached = true

        mediaController.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    refreshUiState(mediaController)
                    if (isPlaying) startProgressLoop(mediaController) else stopProgressLoop()
                }

                override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                    // Timeline chega apos prepare/setMediaItems; aqui que prev/next indices ficam validos.
                    refreshUiState(mediaController)
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    refreshUiState(mediaController)
                    maybeEnrichCurrentMediaItem(context, mediaController)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    refreshUiState(mediaController)
                    if (playbackState != Player.STATE_IDLE) {
                        maybeEnrichCurrentMediaItem(context, mediaController)
                    }
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    refreshUiState(mediaController)
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    refreshUiState(mediaController)
                }
            }
        )

        // If controller already has state, start the progress loop.
        refreshUiState(mediaController)
        if (mediaController.isPlaying) startProgressLoop(mediaController)
        maybeEnrichCurrentMediaItem(context, mediaController)
    }

    private fun startProgressLoop(mediaController: MediaController) {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                refreshUiState(mediaController)
                delay(500)
            }
        }
    }

    private fun stopProgressLoop() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun refreshUiState(mediaController: MediaController) {
        val item = mediaController.currentMediaItem
        val metadata = item?.mediaMetadata
        val duration = mediaController.duration.let { if (it == C.TIME_UNSET) 0L else it }
        val itemUri = item?.localConfiguration?.uri
        val prevIndex = mediaController.previousMediaItemIndex
        val nextIndex = mediaController.nextMediaItemIndex
        val prevUri = if (prevIndex != C.INDEX_UNSET && prevIndex >= 0 && prevIndex < mediaController.mediaItemCount) {
            mediaController.getMediaItemAt(prevIndex).localConfiguration?.uri
        } else null
        val nextUri = if (nextIndex != C.INDEX_UNSET && nextIndex >= 0 && nextIndex < mediaController.mediaItemCount) {
            mediaController.getMediaItemAt(nextIndex).localConfiguration?.uri
        } else null
        val explicitKind = metadata?.extras?.getString(EXTRA_KIND).orEmpty()
        val isVideo = when (explicitKind) {
            EXTRA_KIND_VIDEO -> true
            EXTRA_KIND_AUDIO -> false
            else -> item?.localConfiguration?.mimeType?.lowercase()?.startsWith("video/") == true
        }
        val videoSize = mediaController.videoSize

        _uiState.value = PlaybackUiState(
            hasMedia = item != null,
            mediaUri = itemUri,
            prevMediaUri = prevUri,
            nextMediaUri = nextUri,
            title = metadata?.title?.toString().orEmpty(),
            artist = metadata?.artist?.toString().orEmpty(),
            artworkUri = metadata?.artworkUri,
            isVideo = isVideo,
            isPlaying = mediaController.isPlaying,
            shuffleEnabled = mediaController.shuffleModeEnabled,
            repeatMode = mediaController.repeatMode,
            videoWidth = videoSize.width,
            videoHeight = videoSize.height,
            positionMs = mediaController.currentPosition.coerceAtLeast(0L),
            durationMs = duration.coerceAtLeast(0L)
        ).also { state ->
            if (state.hasMedia) {
                appContext?.let {
                    PlaybackSessionStore.save(
                        context = it,
                        state = state,
                        queue = savedPlaybackQueue(mediaController),
                        currentIndex = mediaController.currentMediaItemIndex.coerceAtLeast(0)
                    )
                }
            }
        }
    }

    private fun restoreSavedSessionIfEmpty(context: Context, mediaController: MediaController) {
        if (mediaController.mediaItemCount > 0) {
            refreshUiState(mediaController)
            return
        }
        val session = PlaybackSessionStore.load(context) ?: return
        val mediaItems = session.queue.ifEmpty {
            listOf(
                SavedPlaybackQueueItem(
                    uri = session.uri,
                    title = session.title,
                    artist = session.artist,
                    mimeType = session.mimeType,
                    kind = session.kind
                )
            )
        }.map { item ->
            buildMediaItem(
                uri = item.uri,
                mimeType = item.mimeType,
                title = item.title,
                artist = item.artist,
                artworkData = null,
                artworkUri = defaultArtworkUri(context),
                kind = if (item.kind == DownloadKind.VIDEO) EXTRA_KIND_VIDEO else EXTRA_KIND_AUDIO,
                metadataEnriched = false
            )
        }
        mediaController.setMediaItems(
            mediaItems,
            session.currentIndex.coerceIn(0, (mediaItems.size - 1).coerceAtLeast(0)),
            session.positionMs
        )
        mediaController.prepare()
        mediaController.playWhenReady = false
        refreshUiState(mediaController)
        maybeEnrichCurrentMediaItem(context, mediaController)
    }

    private fun savedPlaybackQueue(mediaController: MediaController): List<SavedPlaybackQueueItem> {
        return buildList {
            for (index in 0 until mediaController.mediaItemCount) {
                val item = mediaController.getMediaItemAt(index)
                val uri = item.localConfiguration?.uri ?: continue
                val metadata = item.mediaMetadata
                val explicitKind = metadata.extras?.getString(EXTRA_KIND).orEmpty()
                val kind = if (explicitKind == EXTRA_KIND_VIDEO) DownloadKind.VIDEO else DownloadKind.AUDIO
                add(
                    SavedPlaybackQueueItem(
                        uri = uri,
                        title = metadata.title?.toString().orEmpty(),
                        artist = metadata.artist?.toString().orEmpty(),
                        mimeType = item.localConfiguration?.mimeType.orEmpty(),
                        kind = kind
                    )
                )
            }
        }
    }

    private fun readTitleAndArtist(context: Context, uri: Uri): Pair<String, String> {
        return runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty().trim()
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty().trim()
                title to artist
            } finally {
                retriever.release()
            }
        }.getOrDefault("" to "")
    }

    private fun readEmbeddedArtworkBytes(context: Context, uri: Uri): ByteArray? {
        return runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val bytes = retriever.embeddedPicture ?: return@runCatching null
                // Evita blobs enormes na notificacao.
                if (bytes.size in 1..1_500_000) bytes else null
            } finally {
                retriever.release()
            }
        }.getOrNull()
    }

    private fun readVideoArtworkBytes(context: Context, uri: Uri): ByteArray? {
        return runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)

                // Alguns videos tem thumbnail embutida.
                retriever.embeddedPicture?.let { embedded ->
                    if (embedded.size in 1..1_500_000) return@runCatching embedded
                }

                // Fallback: captura um frame inicial e comprime em JPEG.
                val frame = runCatching {
                    retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }.getOrNull() ?: runCatching {
                    // Overload mais antiga (alguns devices só expõem esse caminho).
                    retriever.getFrameAtTime()
                }.getOrNull() ?: return@runCatching null

                compressBitmapJpeg(frame, maxSidePx = 720, quality = 84)?.also { bytes ->
                    if (bytes.size !in 1..1_500_000) return@runCatching null
                }
            } finally {
                retriever.release()
            }
        }.getOrNull()
    }

    private fun compressBitmapJpeg(bitmap: Bitmap, maxSidePx: Int, quality: Int): ByteArray? {
        val w = bitmap.width.coerceAtLeast(1)
        val h = bitmap.height.coerceAtLeast(1)
        val maxSide = maxOf(w, h)
        val scaled = if (maxSide > maxSidePx) {
            val scale = maxSidePx.toFloat() / maxSide.toFloat()
            Bitmap.createScaledBitmap(
                bitmap,
                (w * scale).toInt().coerceAtLeast(1),
                (h * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }
        val createdScaled = scaled !== bitmap
        return runCatching {
            ByteArrayOutputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(10, 95), out)
                out.toByteArray()
            }
        }.also {
            if (createdScaled) {
                scaled.recycle()
            }
        }.getOrNull()
    }

    private fun sameQueue(controller: MediaController, desired: List<MediaItem>): Boolean {
        val count = controller.mediaItemCount
        if (count <= 0 || count != desired.size) return false
        for (i in 0 until count) {
            val a = controller.getMediaItemAt(i).localConfiguration?.uri
            val b = desired[i].localConfiguration?.uri
            if (a != b) return false
        }
        return true
    }

    fun trimMemory(level: Int) {
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            playbackMetadataCache.evictAll()
        } else if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            playbackMetadataCache.trimToSize(4 * 1024)
        }
    }

    private fun maybeEnrichCurrentMediaItem(context: Context, mediaController: MediaController) {
        val currentItem = mediaController.currentMediaItem ?: return
        val currentUri = currentItem.localConfiguration?.uri ?: return
        val extras = currentItem.mediaMetadata.extras
        if (extras?.getBoolean(EXTRA_METADATA_ENRICHED, false) == true) return

        val key = currentUri.toString()
        val cached = playbackMetadataCache.get(key)
        if (cached != null) {
            applyCachedMetadata(mediaController, cached)
            return
        }

        if (metadataEnrichmentKey == key) return
        metadataEnrichmentJob?.cancel()
        metadataEnrichmentKey = key
        metadataEnrichmentJob = scope.launch {
            val resolved = withContext(Dispatchers.IO) {
                resolveCurrentItemMetadata(context.applicationContext, currentItem)
            } ?: return@launch

            playbackMetadataCache.put(key, resolved)
            if (mediaController.currentMediaItem?.localConfiguration?.uri != currentUri) return@launch
            applyCachedMetadata(mediaController, resolved)
        }
    }

    private fun applyCachedMetadata(
        mediaController: MediaController,
        metadata: CachedPlaybackMetadata
    ) {
        val currentIndex = mediaController.currentMediaItemIndex
        if (currentIndex == C.INDEX_UNSET || currentIndex < 0 || currentIndex >= mediaController.mediaItemCount) return

        val currentItem = mediaController.getMediaItemAt(currentIndex)
        if (currentItem.mediaMetadata.extras?.getBoolean(EXTRA_METADATA_ENRICHED, false) == true) return

        mediaController.replaceMediaItem(currentIndex, buildMediaItem(
            uri = currentItem.localConfiguration?.uri ?: return,
            mimeType = currentItem.localConfiguration?.mimeType.orEmpty(),
            title = metadata.title.ifBlank { currentItem.mediaMetadata.title?.toString().orEmpty() },
            artist = metadata.artist,
            artworkData = metadata.artworkData,
            artworkUri = currentItem.mediaMetadata.artworkUri,
            kind = currentItem.mediaMetadata.extras?.getString(EXTRA_KIND).orEmpty(),
            metadataEnriched = true
        ))
        refreshUiState(mediaController)
    }

    private fun resolveCurrentItemMetadata(
        context: Context,
        item: MediaItem
    ): CachedPlaybackMetadata? {
        val uri = item.localConfiguration?.uri ?: return null
        val kind = item.mediaMetadata.extras?.getString(EXTRA_KIND).orEmpty()
        val fallbackTitle = item.mediaMetadata.title?.toString().orEmpty()
        return when (kind) {
            EXTRA_KIND_AUDIO -> {
                val (tagTitle, tagArtist) = readTitleAndArtist(context, uri)
                CachedPlaybackMetadata(
                    title = tagTitle.ifBlank { fallbackTitle },
                    artist = tagArtist,
                    artworkData = readEmbeddedArtworkBytes(context, uri)
                )
            }
            EXTRA_KIND_VIDEO -> {
                CachedPlaybackMetadata(
                    title = fallbackTitle,
                    artist = "",
                    artworkData = readVideoArtworkBytes(context, uri)
                )
            }
            else -> null
        }
    }

    private fun buildMediaItem(
        uri: Uri,
        mimeType: String,
        title: String,
        artist: String,
        artworkData: ByteArray?,
        artworkUri: Uri?,
        kind: String,
        metadataEnriched: Boolean
    ): MediaItem {
        return MediaItem.Builder()
            .setUri(uri)
            .apply {
                if (mimeType.isNotBlank()) {
                    setMimeType(mimeType)
                }
            }
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setExtras(Bundle().apply {
                        putString(EXTRA_KIND, kind)
                        putBoolean(EXTRA_METADATA_ENRICHED, metadataEnriched)
                    })
                    .apply {
                        if (artist.isNotBlank()) {
                            setArtist(artist)
                        }
                        if (artworkData != null) {
                            setArtworkData(artworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                        } else if (artworkUri != null) {
                            setArtworkUri(artworkUri)
                        }
                    }
                    .build()
            )
            .build()
    }
}

private data class CachedPlaybackMetadata(
    val title: String,
    val artist: String,
    val artworkData: ByteArray?
)

private suspend fun <T> ListenableFuture<T>.await(executor: Executor): T =
    suspendCancellableCoroutine { continuation ->
        addListener(
            {
                try {
                    continuation.resume(get())
                } catch (t: Throwable) {
                    continuation.resumeWithException(t)
                }
            },
            executor
        )
        continuation.invokeOnCancellation {
            cancel(true)
        }
    }
