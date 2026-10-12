package com.sonic.music

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var player: MediaController? = null
    private lateinit var nowPlaying: TextView
    private lateinit var status: TextView

    private val pickAudio = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (!uris.isNullOrEmpty()) {
            uris.forEach { uri ->
                try { contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            }
            player?.setMediaItems(uris.map { uri -> mediaItem(uri) })
            player?.prepare()
            player?.play()
            nowPlaying.text = displayName(uris.first())
            status.text = "${uris.size} track(s) added to your queue"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8, 9, 11)
        window.navigationBarColor = android.graphics.Color.rgb(8, 9, 11)
        buildUi()
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture?.addListener({
            player = controllerFuture?.get()
            player?.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    nowPlaying.text = mediaItem?.mediaMetadata?.title ?: "Nothing playing"
                }
            })
        }, ContextCompat.getMainExecutor(this))
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 24)
            setBackgroundColor(android.graphics.Color.rgb(8, 9, 11))
        }
        fun label(text: String, size: Float, color: Int, bold: Boolean = false): TextView = TextView(this).apply {
            this.text = text; textSize = size; setTextColor(color)
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val lime = android.graphics.Color.rgb(182, 255, 107)
        val white = android.graphics.Color.rgb(245, 246, 242)
        val muted = android.graphics.Color.rgb(155, 160, 169)
        root.addView(label("S O N I C", 14f, lime, true))
        root.addView(label("Your music.\nYour frequency.", 34f, white, true).apply { setPadding(0, 30, 0, 8) })
        root.addView(label("A personal listening space that learns your taste.", 15f, muted).apply { setPadding(0, 0, 0, 26) })
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(20, 34, 20, 30)
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 28f
                setColor(android.graphics.Color.rgb(24, 29, 25))
                setStroke(1, android.graphics.Color.rgb(54, 68, 54))
            }
        }
        hero.addView(label("NOW PLAYING", 11f, lime, true))
        nowPlaying = label("Nothing playing yet", 23f, white, true).apply { gravity = Gravity.CENTER; setPadding(0, 14, 0, 8) }
        hero.addView(nowPlaying)
        status = label("Add tracks from your device to begin.", 13f, muted).apply { gravity = Gravity.CENTER }
        hero.addView(status)
        root.addView(hero)
        val add = Button(this).apply { text = "＋   Add music from device"; setOnClickListener { pickAudio.launch(arrayOf("audio/*")) } }
        root.addView(add, LinearLayout.LayoutParams(-1, 58).apply { topMargin = 22 })
        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        fun control(text: String, action: () -> Unit) = Button(this).apply { this.text = text; setOnClickListener { action() } }
        controls.addView(control("⏮", { player?.seekToPreviousMediaItem() }))
        controls.addView(control("▶ / Ⅱ", { player?.let { if (it.isPlaying) it.pause() else it.play() } }))
        controls.addView(control("⏭", { player?.seekToNextMediaItem() }))
        root.addView(controls)
        root.addView(label("FIRST BUILD  /  LOCAL LIBRARY", 11f, lime, true).apply { setPadding(0, 28, 0, 8) })
        root.addView(label("This starter proves the player and queue. Next: connected streaming sources, artwork, likes, and recommendations that learn from your listening.", 14f, muted))
        setContentView(root)
    }

    private fun mediaItem(uri: Uri): MediaItem {
        val title = displayName(uri)
        return MediaItem.Builder().setUri(uri).setMediaMetadata(
            MediaMetadata.Builder().setTitle(title).build()
        ).build()
    }

    private fun displayName(uri: Uri): String = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Local audio"

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        player = null
        super.onDestroy()
    }
}
