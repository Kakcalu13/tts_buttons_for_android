package com.example.buttons_tts_overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
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

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    companion object {
        private const val CHANNEL_ID = "overlay_service_channel"
        private const val NOTIFICATION_ID = 1234
    }

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        Toast.makeText(this, "OverlayService started", Toast.LENGTH_SHORT).show()
        Log.d("OverlayService", "onCreate() called")

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission missing, stopping", Toast.LENGTH_LONG).show()
            Log.d("OverlayService", "Missing overlay permission, stopping")
            stopSelf()
            return
        }

        // Inflate the layout with four buttons
        overlayView = LayoutInflater.from(this)
            .inflate(R.layout.overlay_layout, null)

        overlayView?.apply {
            findViewById<ImageButton>(R.id.btn_select_all).setOnClickListener {
                Log.d("OverlayService", "Select All clicked")
                MyAccessibilityService.getInstance()?.performSelectAll()
            }
            findViewById<ImageButton>(R.id.btn_read_aloud).setOnClickListener {
                Log.d("OverlayService", "Read Aloud clicked")
                val text = MyAccessibilityService.getInstance()?.getCurrentScreenText() ?: ""
                Toast.makeText(this@OverlayService, "Reading: $text", Toast.LENGTH_SHORT).show()
                Intent(Intent.ACTION_PROCESS_TEXT).apply {
                    putExtra(Intent.EXTRA_PROCESS_TEXT, text)
                    type = "text/plain"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }.also(::startActivity)
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

        // Center the overlay on screen
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
            gravity = Gravity.CENTER
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

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                CHANNEL_ID,
                "Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).let { channel ->
                getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            }
        }
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Overlay Active")
            .setContentText("Use the buttons or tap Stop")
            .setSmallIcon(R.drawable.ic_overlay)
            .build()
}
