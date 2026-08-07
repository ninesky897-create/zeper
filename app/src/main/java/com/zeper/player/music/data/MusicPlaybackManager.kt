package com.zeper.player.music.data

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.zeper.player.core.data.MediaFile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MusicPlaybackManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val _currentSong = MutableStateFlow<MediaFile?>(null)
    val currentSong: StateFlow<MediaFile?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var playlist: List<MediaFile> = emptyList()

    companion object {
        @Volatile
        private var INSTANCE: MusicPlaybackManager? = null

        fun getInstance(context: Context): MusicPlaybackManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MusicPlaybackManager(context).also { INSTANCE = it }
            }
        }
    }

    init {
        initializeController()
        startTrackingPosition()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(appContext, ComponentName(appContext, MusicService::class.java))
        controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get()
                mediaController = controller
                if (controller != null) {
                    _isPlaying.value = controller.isPlaying
                    _shuffleModeEnabled.value = controller.shuffleModeEnabled
                    _repeatMode.value = controller.repeatMode
                    _playbackSpeed.value = controller.playbackParameters.speed
                    updateCurrentSongFromController(controller)
                    
                    controller.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _isPlaying.value = isPlaying
                        }

                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            updateCurrentSongFromController(controller)
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_READY) {
                                _duration.value = controller.duration.coerceAtLeast(0L)
                            }
                        }

                        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                            _shuffleModeEnabled.value = shuffleModeEnabled
                        }

                        override fun onRepeatModeChanged(repeatMode: Int) {
                            _repeatMode.value = repeatMode
                        }

                        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                            _playbackSpeed.value = playbackParameters.speed
                        }
                    })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun updateCurrentSongFromController(controller: MediaController) {
        val currentMediaItem = controller.currentMediaItem
        if (currentMediaItem != null) {
            val mediaId = currentMediaItem.mediaId.toLongOrNull() ?: -1L
            val song = playlist.find { it.id == mediaId } ?: MediaFile(
                id = mediaId,
                name = currentMediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                path = currentMediaItem.requestMetadata.mediaUri?.toString() ?: "",
                contentUri = currentMediaItem.requestMetadata.mediaUri?.toString() ?: "",
                duration = controller.duration,
                size = 0L,
                type = "audio",
                artist = currentMediaItem.mediaMetadata.artist?.toString(),
                album = currentMediaItem.mediaMetadata.albumTitle?.toString()
            )
            _currentSong.value = song
            _duration.value = controller.duration.coerceAtLeast(0L)
        } else {
            _currentSong.value = null
        }
    }

    private fun startTrackingPosition() {
        scope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        _currentPosition.value = controller.currentPosition
                        _duration.value = controller.duration.coerceAtLeast(0L)
                    }
                }
                delay(1000)
            }
        }
    }

    fun playSong(song: MediaFile, fullPlaylist: List<MediaFile>) {
        playlist = fullPlaylist
        val controller = mediaController ?: return

        val mediaItems = fullPlaylist.map { file ->
            MediaItem.Builder()
                .setMediaId(file.id.toString())
                .setUri(Uri.parse(file.contentUri))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(file.name)
                        .setArtist(file.artist ?: "Unknown Artist")
                        .setAlbumTitle(file.album ?: "Unknown Album")
                        .build()
                )
                .build()
        }

        controller.setMediaItems(mediaItems)
        val index = fullPlaylist.indexOfFirst { it.id == song.id }
        if (index != -1) {
            controller.seekTo(index, 0L)
        }
        controller.prepare()
        controller.play()
        
        _currentSong.value = song
    }

    fun playPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.mediaItemCount > 0) {
                controller.play()
            }
        }
    }

    fun stop() {
        mediaController?.stop()
        mediaController?.clearMediaItems()
        _currentSong.value = null
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun skipToNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }

    fun toggleShuffle() {
        mediaController?.let {
            it.shuffleModeEnabled = !it.shuffleModeEnabled
        }
    }

    fun toggleRepeat() {
        mediaController?.let {
            val nextMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            it.repeatMode = nextMode
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        mediaController?.playbackParameters = PlaybackParameters(speed)
    }

    fun seekForward10s() {
        mediaController?.let {
            it.seekTo(it.currentPosition + 10000)
        }
    }

    fun seekBackward10s() {
        mediaController?.let {
            it.seekTo((it.currentPosition - 10000).coerceAtLeast(0))
        }
    }

    fun release() {
        scope.cancel()
        controllerFuture?.let { future ->
            MediaController.releaseFuture(future)
        }
        mediaController = null
        INSTANCE = null
    }
}
