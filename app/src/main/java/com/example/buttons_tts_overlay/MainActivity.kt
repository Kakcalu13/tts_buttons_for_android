package com.example.buttons_tts_overlay

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.buttons_tts_overlay.MyAccessibilityService
import com.example.buttons_tts_overlay.OverlayService

class MainActivity : AppCompatActivity() {
    companion object {
        private const val ACCESSIBILITY_PERMISSION_REQ_CODE = 1001
        private const val OVERLAY_PERMISSION_REQ_CODE       = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_enable_accessibility).setOnClickListener {
            checkAndRequestAccessibility()
        }
        findViewById<Button>(R.id.btn_test_overlay).setOnClickListener {
            startOverlay()
        }
    }

    private fun checkAndRequestAccessibility() {
        if (isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Accessibility enabled", Toast.LENGTH_SHORT).show()
            requestOverlayPermission()
        } else {
            startActivityForResult(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                ACCESSIBILITY_PERMISSION_REQ_CODE
            )
        }
    }


    private fun requestOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            startOverlay()
        } else {
            startActivityForResult(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ),
                OVERLAY_PERMISSION_REQ_CODE
            )
        }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ACCESSIBILITY_PERMISSION_REQ_CODE -> {
                if (isAccessibilityServiceEnabled()) {
                    Toast.makeText(this, "Accessibility enabled", Toast.LENGTH_SHORT).show()
                    requestOverlayPermission()
                } else {
                    Toast.makeText(this, "Please enable Accessibility Service", Toast.LENGTH_LONG).show()
                }
            }
            OVERLAY_PERMISSION_REQ_CODE -> {
                if (Settings.canDrawOverlays(this)) {
                    startOverlay()
                } else {
                    Toast.makeText(this, "Overlay permission required", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    private fun startOverlay() {
        Intent(this, OverlayService::class.java).also { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        val componentName = ComponentName(this, MyAccessibilityService::class.java)
        return enabledServices.any { enabled ->
            ComponentName.unflattenFromString(
                enabled.resolveInfo.serviceInfo.name
            ) == componentName
        }
    }
}
