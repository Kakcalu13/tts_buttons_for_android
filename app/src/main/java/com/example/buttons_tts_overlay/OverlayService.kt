package com.example.buttons_tts_overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.util.Locale

class OverlayService : Service(), TextToSpeech.OnInitListener {

    private lateinit var windowManager: WindowManager
    private lateinit var tts: TextToSpeech
    private lateinit var audioManager: AudioManager
    private var overlayView: View? = null
    private lateinit var params: WindowManager.LayoutParams

    // For drag handling
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    companion object {
        private const val CHANNEL_ID = "overlay_service_channel"
        private const val NOTIFICATION_ID = 1234
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        tts = TextToSpeech(this, this)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission missing", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }

        // Inflate overlay layout
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)

        // Single LayoutParams instance
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = (16 * resources.displayMetrics.density).toInt()    // inset from right
            y = -(50 * resources.displayMetrics.density).toInt()   // shift up
        }

        // Drag handle on topbar
        overlayView?.findViewById<View>(R.id.topbar)?.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = ev.rawX
                    initialTouchY = ev.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // Subtract deltaX to move in the expected direction
                    params.x = initialX - (ev.rawX - initialTouchX).toInt()
                    params.y = initialY + (ev.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(overlayView, params)
                    true
                }
                else -> false
            }
        }


        // Button listeners
        overlayView?.apply {
            findViewById<ImageButton>(R.id.btn_select_all).setOnClickListener {
                MyAccessibilityService.getInstance()?.performSelectAllAndCopy()
            }

            findViewById<ImageButton>(R.id.btn_read_aloud).setOnClickListener {
                // Get focused input text
                val root = MyAccessibilityService.getInstance()?.rootInActiveWindow
                if (root == null) {
                    Toast.makeText(this@OverlayService, "Accessibility root unavailable", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                if (focus == null) {
                    Toast.makeText(this@OverlayService, "No input focused", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val text = focus.text?.toString().orEmpty()
                if (text.isBlank()) {
                    Toast.makeText(this@OverlayService, "Nothing selected", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@OverlayService, "Reading: $text", Toast.LENGTH_SHORT).show()

                    // Launch the system Read Aloud UI with built-in preview/highlighting
                    Intent(Intent.ACTION_PROCESS_TEXT).apply {
                        putExtra(Intent.EXTRA_PROCESS_TEXT, text)
                        putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)
                        type = "text/plain"
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }.also {
                        startActivity(it)
//                        stopSelf()
                    }
                }
            }

            findViewById<ImageButton>(R.id.btn_undo).setOnClickListener {
//                MyAccessibilityService.getInstance()?.performUndo()
                tts.stop()
            }

            findViewById<ImageButton>(R.id.btn_stop_tts).setOnClickListener {
                Log.d("OverlayService", "Stop clicked, stopping service")
                stopSelf()
            }
        }

        // Add overlay to window
        windowManager.addView(overlayView, params)
        Log.d("OverlayService", "Overlay added")
    }

    // Force audio to internal speaker
    private fun forcePhoneSpeaker() {
        try {
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = true
        } catch (e: Exception) {
            Log.w("OverlayService", "Speakerforce failed: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
        } else {
            Log.w("OverlayService", "TTS init failed: $status")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
        audioManager.isSpeakerphoneOn = false
        audioManager.mode = AudioManager.MODE_NORMAL
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: IllegalArgumentException) { }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(CHANNEL_ID, "Overlay Service", NotificationManager.IMPORTANCE_LOW)
                .also { getSystemService(NotificationManager::class.java).createNotificationChannel(it) }
        }
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Overlay Active")
            .setContentText("Use buttons to control TTS")
            .setSmallIcon(R.drawable.ic_overlay)
            .build()
}
