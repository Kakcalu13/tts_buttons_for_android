package com.example.buttons_tts_overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import android.widget.ImageButton
import android.widget.Toast

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    companion object {
        private const val CHANNEL_ID = "overlay_service_channel"
        private const val NOTIFICATION_ID = 1234
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        // Only proceed if we have overlay permission
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Inflate your overlay layout
        overlayView = LayoutInflater.from(this)
            .inflate(R.layout.overlay_layout, null)

        // Find each button in the overlay and set click behavior
        overlayView?.apply {
            findViewById<ImageButton>(R.id.btn_select_all).setOnClickListener {
                MyAccessibilityService.getInstance()?.performSelectAll()
            }
            findViewById<ImageButton>(R.id.btn_read_aloud).setOnClickListener {
                val text = MyAccessibilityService.getInstance()?.getCurrentScreenText() ?: ""
                Toast.makeText(this@OverlayService, text, Toast.LENGTH_SHORT).show()
                // Or forward to TTS engine
            }
            findViewById<ImageButton>(R.id.btn_undo).setOnClickListener {
                MyAccessibilityService.getInstance()?.performUndo()
            }
            findViewById<ImageButton>(R.id.btn_stop_tts).setOnClickListener {
                stopSelf()
            }
        }

        // Layout parameters for the overlay window
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
            // Position in the top-left; adjust x/y or gravity as desired
            x = 0
            y = 100
        }

        // Add the view to the window
        windowManager.addView(overlayView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                CHANNEL_ID,
                "Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(chan)
        }
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Overlay Active")
            .setContentText("Tap stop button to remove overlay")
            .setSmallIcon(R.drawable.ic_overlay)
            .build()
}
