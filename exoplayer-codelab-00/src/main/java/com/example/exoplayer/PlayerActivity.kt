/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
* limitations under the License.
 */
package com.example.exoplayer

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C.WAKE_MODE_NETWORK
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.example.exoplayer.databinding.ActivityPlayerBinding


private const val TAG = "PlayerActivity"

/**
 * A fullscreen activity to play audio or video streams.
 */
class PlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private var playWhenReady = true
    private var mediaItemIndex = 0
    private var playbackPosition = 0L
    private val playbackStateListener: Player.Listener = playbackStateListener()
    private var mediaSession: MediaSession? = null
    private val m_activity: Activity? = null


    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityPlayerBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        initializePlayer()
        val myWebView: WebView = findViewById(R.id.web_view)
        myWebView.settings.javaScriptEnabled = true
        myWebView.settings.domStorageEnabled = true
    }

    private fun initializePlayer() {
        val player = ExoPlayer.Builder(this)
            .build()
            .also { exoPlayer ->
                viewBinding.videoView.player = exoPlayer
                val mediaItem = MediaItem.fromUri(getString(R.string.media_url_mp3))
                exoPlayer.setMediaItems(listOf(mediaItem), mediaItemIndex, playbackPosition)
                exoPlayer.playWhenReady = playWhenReady
                exoPlayer.setWakeMode(WAKE_MODE_NETWORK)
                exoPlayer.addListener(playbackStateListener)
                exoPlayer.prepare()
            }
        if (mediaSession != null) {
            mediaSession = null;
        }
        mediaSession = MediaSession.Builder(this, player).build()
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, viewBinding.videoView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            mediaItemIndex = exoPlayer.currentMediaItemIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.removeListener(playbackStateListener)
            exoPlayer.release()
        }
        player = null
        mediaSession = null;
    }

    private fun playbackStateListener() = object : Player.Listener {
        @OptIn(UnstableApi::class) override fun onPlaybackStateChanged(playbackState: Int) {
            val stateString: String = when (playbackState) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
                ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
                else -> "UNKNOWN_STATE             -"
            }
            Log.d(TAG, "changed state to $stateString")
        }

        @OptIn(UnstableApi::class) override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            if (mediaMetadata.title != null && m_activity?.hasWindowFocus() != false) {
                val regex = """\D+(\D)(\d+) - (.*)""".toRegex()
                var matchResult: MatchResult? = null
                if (regex.find(mediaMetadata.title.toString()) != null) {
                    matchResult = regex.find(mediaMetadata.title.toString())
                }  //else throw NullPointerException("Expression 'regex.find(mediaMetadata.title.toString())' must not be null")
                if (matchResult != null) {
                    val (hymnType, hymnNum, hymnTitle) = matchResult.destructured
                    val typeCode = if (hymnType == "N") "ns" else "h"
                    val textView = findViewById<View>(R.id.title_text) as TextView
                    textView.text = hymnTitle //set text for text view
                    val myWebView: WebView = findViewById(R.id.web_view)
                    if (typeCode == "h") {
                        myWebView.loadUrl("https://songbase.life/english_hymnal/" + hymnNum)
                    } else {
                        myWebView.loadUrl("https://www.hymnal.net/en/hymn/" + typeCode + "/" + hymnNum + "#fb-root")
                    }
                }
            }
        }
    }
}