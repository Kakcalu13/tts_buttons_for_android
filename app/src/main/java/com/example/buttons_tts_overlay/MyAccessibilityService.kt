package com.example.buttons_tts_overlay

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Bundle

class MyAccessibilityService : AccessibilityService() {

    companion object {
        private var instance: MyAccessibilityService? = null
        fun getInstance(): MyAccessibilityService? = instance
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Not used for button clicks
    }

    override fun onInterrupt() {
        // Not used
    }

    /**
     * Perform a global “Select All” in the focused text field.
     */
    fun performSelectAll() {
        rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { node ->
            val length = node.text?.length ?: 0
            val args = Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, length)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
        }
    }

    /**
     * Perform a global “Undo” via back action.
     */
    fun performUndo() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Extract all visible text on screen.
     */
    fun getCurrentScreenText(): String {
        val sb = StringBuilder()
        fun recurse(node: AccessibilityNodeInfo?) {
            if (node == null) return
            node.text?.let { if (it.isNotEmpty()) sb.append(it).append(" ") }
            node.contentDescription?.let { if (it.isNotEmpty()) sb.append(it).append(" ") }
            for (i in 0 until node.childCount) {
                recurse(node.getChild(i))
            }
        }
        recurse(rootInActiveWindow)
        return sb.toString().trim()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }
}
