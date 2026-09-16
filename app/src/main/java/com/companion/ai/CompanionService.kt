package com.companion.ai

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class CompanionService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("CompanionService", "AI Partner Service Connected")
        Toast.makeText(this, "AI Partner Ready!", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            if (it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val pkgName = it.packageName?.toString() ?: ""
                Log.d("CompanionService", "Current App: $pkgName")
            }
        }
    }

    override fun onInterrupt() {
        Log.d("CompanionService", "Service Interrupted")
    }
}
