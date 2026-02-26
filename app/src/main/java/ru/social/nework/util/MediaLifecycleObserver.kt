package ru.social.nework.util

import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import ru.social.nework.R
import ru.social.nework.dto.Attachment

@Suppress("DEPRECATION")
class MediaLifecycleObserver: LifecycleEventObserver {

    var mediaPlayer: MediaPlayer? = MediaPlayer()
    var runnable: Runnable? = null
    private var handler = Handler()
    private var playButton: ImageButton? = null
    private var seekBar: SeekBar? = null
    private lateinit var seekBarListener: SeekBar.OnSeekBarChangeListener

    private fun pause() {
        mediaPlayer?.pause()
        playButton?.setBackgroundResource(R.drawable.play_48)
    }

    fun stop() {
        mediaPlayer?.stop()
        //seekBar?.progress = 0
        playButton?.setBackgroundResource(R.drawable.play_48)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_PAUSE -> pause()//mediaPlayer?.pause()
            Lifecycle.Event.ON_STOP -> {
                //seekBar.removeCallbacks(runnable)
                mediaPlayer?.release()
                mediaPlayer = null
                if (runnable != null) {
                    if (handler.hasCallbacks(runnable!!)) {
                        handler.removeCallbacks(runnable!!)
                    }
                }


            }
            //отписываемся от источника жизненного цикла
            Lifecycle.Event.ON_DESTROY -> source.lifecycle.removeObserver(this)
            else -> Unit
        }
    }

    fun playAudio(attachment: Attachment?, seekBar: SeekBar?, playButton: ImageButton?) {
        if (attachment == null) {
            return
        }
        if (seekBar == null) {
            return
        }
        if (playButton == null) {
            return
        }
        this.playButton = playButton
        this.seekBar = seekBar
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        }
        try {
            if (!mediaPlayer!!.isPlaying) {
                mediaPlayer!!.reset()
                //загрузка трека
                try {
                    mediaPlayer!!.setDataSource(attachment.url)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                mediaPlayer!!.prepareAsync()
                mediaPlayer!!.setOnPreparedListener {
                    it.start()
                    seekBar.progress = 0
                    seekBar.max = it.duration
                    attachment.isPlaying = true
                }
                playButton.setBackgroundResource(R.drawable.pause_48)
            } else {
                pause()
                attachment.isPlaying = false
            }
            seekbar(attachment)

        } catch (e: NullPointerException) {
            e.printStackTrace()
        }

    }

    fun playAudioFromDescriptor(
        fileDescriptor: AssetFileDescriptor,
        seekBar: SeekBar,
        playButton: ImageButton
    ) {
        this.playButton = playButton
        this.seekBar = seekBar
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        }
        try {
            if (!mediaPlayer!!.isPlaying) {
                mediaPlayer!!.reset()
                //загрузка трека
                try {
                    mediaPlayer!!.setDataSource(fileDescriptor)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                mediaPlayer!!.prepareAsync()
                mediaPlayer!!.setOnPreparedListener {
                    it.start()
                    seekBar.progress = 0
                    seekBar.max = it.duration
                }
                playButton.setBackgroundResource(R.drawable.pause_48)
            } else {
                pause()
            }
            seekbar(null)

        } catch (e: NullPointerException) {
            e.printStackTrace()
        }

    }

    fun seekbar(attachment: Attachment?) {
        if (mediaPlayer != null) {
            seekBarListener = SeekBarListener(mediaPlayer as MediaPlayer)
            seekBar?.setOnSeekBarChangeListener(seekBarListener)
            runnable = Runnable {
                seekBar?.progress = mediaPlayer?.currentPosition ?: 0
                attachment?.progress = seekBar?.progress ?: 0
                handler.let { seekBar?.postDelayed(runnable, 1000) }
            }
            seekBar?.postDelayed(runnable!!, 1000)
            mediaPlayer?.setOnCompletionListener {
                mediaPlayer?.pause()
                playButton?.setBackgroundResource(R.drawable.play_48)
            }
        }
    }

    fun seekSet(seekBar: SeekBar) {
        this.seekBar = seekBar
    }
}

class SeekBarListener(private val mediaPlayer: MediaPlayer): SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
        if (p2) {
            mediaPlayer.seekTo(p1)
        }
    }
    override fun onStartTrackingTouch(p0: SeekBar?) {
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
    }

}


