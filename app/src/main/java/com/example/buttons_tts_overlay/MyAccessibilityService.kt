package com.example.buttons_tts_overlay

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

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
        Log.d("MyAccessibilityService", "Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // no-op
    }

    override fun onInterrupt() {
        // no-op
    }

    /**
     * Selects all text on the focused node (or root) and copies it to the clipboard.
     */
    fun performSelectAllAndCopy() {
        val root = rootInActiveWindow ?: return
        // Try to find the focused input; otherwise use root
        val focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: root

        // Get its text length
        val txt = focus.text?.toString().orEmpty()
        val length = txt.length
        if (length == 0) {
            Log.w("MyAccessibilityService", "Nothing to select")
            return
        }

        // Build arguments for selecting [0, length]
        val args = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, length)
        }

        // Perform set-selection
        if (focus.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)) {
            Log.d("MyAccessibilityService", "Selection set [0,$length]")
            // Delay then copy
            Handler(Looper.getMainLooper()).postDelayed({
                if (focus.performAction(AccessibilityNodeInfo.ACTION_COPY)) {
                    Log.d("MyAccessibilityService", "Copy succeeded")
                } else {
                    Log.w("MyAccessibilityService", "Copy failed")
                }
            }, 100)
        } else {
            Log.w("MyAccessibilityService", "ACTION_SET_SELECTION failed")
        }
    }

    fun performClearText() {
        val root = rootInActiveWindow ?: return
        // Find the focused input node; if none, nothing to clear
        val focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return

        // Build arguments to set the text to an empty string
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                ""
            )
        }

        // Perform the clear action
        if (focus.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
            Log.d("MyAccessibilityService", "Cleared input text")
        } else {
            Log.w("MyAccessibilityService", "Failed to clear input text")
        }
    }


    /**
     * Performs global undo (if supported).
     */
    fun performUndo() {
        Log.d("MyAccessibilityService", "Performing Undo")
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Reads all visible text nodes on screen.
     */
    fun getCurrentScreenText(): String {
        val root = rootInActiveWindow ?: return ""
        val sb = StringBuilder()
        fun recurse(node: AccessibilityNodeInfo?) {
            node ?: return
            node.text?.let { sb.append(it).append(" ") }
            for (i in 0 until node.childCount) recurse(node.getChild(i))
        }
        recurse(root)
        return sb.toString().trim()
    }
}
