package com.example.buttons_tts_overlay

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MyAccessibilityService : AccessibilityService() {

    // Two buffers: one for the most recent read, one for undo
    private var lastScreenText: String = ""
    private var previousText: String = ""

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
     * Clears only our stored text buffer, leaving the UI untouched.
     */
    fun clearStoredText() {
        previousText = lastScreenText
        lastScreenText = ""
        Log.d("MyAccessibilityService","Stored text cleared; saved for undo: $previousText")
    }

    /**
     * Selects all text in the focused node and copies it.
     */
    fun performSelectAllAndCopy() {
        val root = rootInActiveWindow ?: return
        val focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: root
        val txt = focus.text?.toString().orEmpty()
        val length = txt.length
        if (length == 0) return

        val args = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, length)
        }

        if (focus.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)) {
            Handler(Looper.getMainLooper()).postDelayed({
                focus.performAction(AccessibilityNodeInfo.ACTION_COPY)
            }, 100)
        }
    }

    /**
     * Clears the actual text in the focused input node (if you ever need it).
     */
    fun performClearTextInField() {
        val root = rootInActiveWindow ?: return
        val focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                ""
            )
        }
        focus.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    /**
     * Performs global "back" as an undo placeholder.
     */
    // Restores the saved previousText into the focused input
    fun performUndo() {
        val root = rootInActiveWindow ?: return
        val focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return

        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                previousText
            )
        }
        if (focus.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
            Log.d("MyAccessibilityService","Undo: Restored text: $previousText")
            // After undo, shift buffers so another clear can still be undone
            lastScreenText = previousText
            previousText = ""
        } else {
            Log.w("MyAccessibilityService","Undo failed")
        }
    }

    /**
     * Traverses the screen, builds a string of all visible text,
     * stores it in lastScreenText, and returns it.
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
        lastScreenText = sb.toString().trim()
        return lastScreenText
    }
}
