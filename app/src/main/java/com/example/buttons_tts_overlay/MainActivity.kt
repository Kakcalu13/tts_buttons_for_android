package com.example.buttons_tts_overlay

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val ACCESSIBILITY_PERMISSION_REQ_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_enable_accessibility).setOnClickListener {
            checkAndRequestAccessibility()
        }
    }

    private fun checkAndRequestAccessibility() {
        // Force applicationContext to non-nullable Context
        val ctx: Context = applicationContext!!
        if (isAccessibilityServiceEnabled(ctx, MyAccessibilityService::class.java)) {
            Toast.makeText(this, "Accessibility Service already enabled", Toast.LENGTH_SHORT).show()
            MyAccessibilityService.getInstance()?.let { service ->
                // Example: service.performSelectAll()
            }
        } else {
            startActivityForResult(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                ACCESSIBILITY_PERMISSION_REQ_CODE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACCESSIBILITY_PERMISSION_REQ_CODE) {
            val ctx: Context = applicationContext!!
            if (isAccessibilityServiceEnabled(ctx, MyAccessibilityService::class.java)) {
                Toast.makeText(this, "Accessibility Service enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enable Accessibility Service", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Checks if the given AccessibilityService is enabled.
     */
    private fun isAccessibilityServiceEnabled(
        context: Context,
        serviceClass: Class<*>
    ): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        val componentName = ComponentName(context, serviceClass)
        return enabledServices.any { enabled ->
            ComponentName.unflattenFromString(
                enabled.resolveInfo.serviceInfo.name
            ) == componentName
        }
    }
}
