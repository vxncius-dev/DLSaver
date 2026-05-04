package com.vxncius.dlsaver

import android.net.Uri

data class PlaybackUiState(
    val hasMedia: Boolean = false,
    val mediaUri: Uri? = null,
    val prevMediaUri: Uri? = null,
    val nextMediaUri: Uri? = null,
    val title: String = "",
    val artist: String = "",
    val artworkUri: Uri? = null,
    val isVideo: Boolean = false,
    val isPlaying: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = 0,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)
