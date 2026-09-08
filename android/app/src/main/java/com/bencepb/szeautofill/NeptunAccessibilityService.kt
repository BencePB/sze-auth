package com.bencepb.szeautofill

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Lets the Quick Settings Tile type the current Neptun code directly into
 * whatever field is focused, with one tap. It is entirely user-initiated:
 * it only ever writes text when CopyTileService explicitly asks it to
 * (right after you tap the tile), and it never reads, logs, or transmits
 * any screen content on its own. This is a separate Android subsystem
 * from the Autofill Framework, so it does not conflict with or disable
 * Google's (or any other) Autofill Service.
 */
class NeptunAccessibilityService : AccessibilityService() {

    companion object {
        var instance: NeptunAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No background monitoring -- this service only acts when
        // fillFocusedField() is called directly from the tile tap.
    }

    override fun onInterrupt() {}

    /** Types [code] into whichever field currently has input focus. */
    fun fillFocusedField(code: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = findFocusedEditable(root) ?: return false
        val bundle = Bundle()
        bundle.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            code
        )
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
    }

    private fun findFocusedEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocused && node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findFocusedEditable(child)
            if (result != null) return result
        }
        return null
    }
}
