package net.craftventure.composer.controller

import io.reactivex.BackpressureStrategy
import io.reactivex.subjects.BehaviorSubject
import javafx.animation.AnimationTimer
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import net.craftventure.composer.extension.clamp
import java.io.File

class PlaybackController {
    private val time = BehaviorSubject.createDefault(0.0)
    private val playing = BehaviorSubject.createDefault(false)
    private var timer: AnimationTimer? = null

    var player: MediaPlayer? = null

    private var lastUpdate = System.currentTimeMillis()

    init {
        playing.subscribe({ playing ->
            player?.let { player ->
                if (playing)
                    player.play()
                else
                    player.pause()
            }
        })
        time.subscribe({ time ->
            player?.let { player ->
                val currentTime = player.currentTime.toSeconds()
                if (currentTime < time - 0.2 || currentTime > time + 0.2) {
                    if (time > player.totalDuration.toSeconds()) {
                        player.pause()
                    } else {
//                        println("$currentTime $time")
                        player.seek(Duration.seconds(time))
                    }
                }
            }
        })
    }

    fun setTrack(file: File?) {
        if (file == null) {
            player = null
            return
        }
        val pick = Media(file.toURI().toString())
        player = MediaPlayer(pick)
    }

    fun sync(seconds: Double) {
        time.onNext(seconds.clamp(0.0, 60 * 10.0))
        pause()
    }

    fun toggle() {
        if (playing.value!!)
            pause()
        else
            play()
    }

    fun play() {
        playing.onNext(true)
        lastUpdate = System.currentTimeMillis()
        timer = object : AnimationTimer() {
            override fun handle(now: Long) {
                update()
            }
        }.apply { start() }
    }

    fun pause() {
        playing.onNext(false)
        timer?.stop()
        timer = null
    }

    fun stop() {
        playing.onNext(false)
        pause()
        time.onNext(0.0)
    }

    fun time() = time.value!!

    fun timeAsFlowable() = time.toFlowable(BackpressureStrategy.LATEST)

    fun playing() = playing.toFlowable(BackpressureStrategy.LATEST)

    fun isPlaying() = playing.value ?: false

    private fun update() {
        val last = lastUpdate
        lastUpdate = System.currentTimeMillis()

        val delta = lastUpdate - last
        time.onNext(time.value!! + (delta / 1000.0))
    }
}