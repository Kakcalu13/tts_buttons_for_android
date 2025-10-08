package com.example.buttons_tts_overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.view.accessibility.AccessibilityNodeInfo
import android.content.ClipboardManager
import java.util.Locale
import android.speech.tts.TextToSpeech
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.NotificationCompat

class OverlayService : Service(), TextToSpeech.OnInitListener {

    private lateinit var windowManager: WindowManager
    private lateinit var tts: TextToSpeech
    private var overlayView: View? = null

    companion object {
        private const val CHANNEL_ID = "overlay_service_channel"
        private const val NOTIFICATION_ID = 1234
        private lateinit var tts: TextToSpeech
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        tts = TextToSpeech(this, this)

        Toast.makeText(this, "OverlayService started", Toast.LENGTH_SHORT).show()
        Log.d("OverlayService", "onCreate() called")

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission missing, stopping", Toast.LENGTH_LONG).show()
            Log.d("OverlayService", "Missing overlay permission")
            stopSelf()
            return
        }

        // Inflate the layout containing four buttons
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)

        overlayView?.apply {
            findViewById<ImageButton>(R.id.btn_select_all).setOnClickListener {
                Log.d("OverlayService", "Select All clicked")
                MyAccessibilityService.getInstance()?.performSelectAllAndCopy()
            }
            findViewById<ImageButton>(R.id.btn_read_aloud).setOnClickListener {
                Log.d("OverlayService", "Read Aloud clicked")

                // 1. Get the accessibility root and focused node
                val rootNode = MyAccessibilityService.getInstance()?.rootInActiveWindow
                if (rootNode == null) {
                    Toast.makeText(this@OverlayService, "Accessibility root unavailable", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val focusNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                if (focusNode == null) {
                    Toast.makeText(this@OverlayService, "No input focused", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // 2. Read the focused node’s text directly
                val selectedText = focusNode.text?.toString().orEmpty()
                if (selectedText.isBlank()) {
                    Toast.makeText(this@OverlayService, "Nothing selected", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@OverlayService, "Reading: $selectedText", Toast.LENGTH_SHORT).show()
                    Intent(Intent.ACTION_PROCESS_TEXT).apply {
                        putExtra(Intent.EXTRA_PROCESS_TEXT, selectedText)
                        type = "text/plain"
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }.also(this@OverlayService::startActivity)
                }

//                MyAccessibilityService.getInstance()?.clearStoredText() // clear the text
            }
            findViewById<ImageButton>(R.id.btn_undo).setOnClickListener {
                Log.d("OverlayService", "Undo clicked")
                MyAccessibilityService.getInstance()?.performUndo()
            }
            findViewById<ImageButton>(R.id.btn_stop_tts).setOnClickListener {
                Log.d("OverlayService", "Stop clicked, stopping service")
                stopSelf()
            }
        }

        val params = WindowManager.LayoutParams(
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
            // Dock to the right side, vertically centered
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            // Optional: inset 16dp from right edge (convert dp to px)
            val insetPx = (16 * resources.displayMetrics.density).toInt()
            x = insetPx
            // Move up by 50dp
            val shiftUpPx = (100 * resources.displayMetrics.density).toInt()
            y = -shiftUpPx
        }

        windowManager.addView(overlayView, params)
        Log.d("OverlayService", "Overlay view added")
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let {
            try {
                windowManager.removeView(it)
                Log.d("OverlayService", "Overlay view removed")
            } catch (e: IllegalArgumentException) {
                Log.w("OverlayService", "Overlay view not attached, skipping removal")
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
        } else {
            Log.w("OverlayService","TTS init failed, status=$status")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(CHANNEL_ID, "Overlay Service", NotificationManager.IMPORTANCE_LOW).also { channel ->
                getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            }
        }
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Overlay Active")
            .setContentText("Use buttons or tap Stop")
            .setSmallIcon(R.drawable.ic_overlay)
            .build()
}
