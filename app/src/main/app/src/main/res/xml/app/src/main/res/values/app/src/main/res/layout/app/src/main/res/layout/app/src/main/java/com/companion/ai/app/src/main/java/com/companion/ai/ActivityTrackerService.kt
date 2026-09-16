package com.companion.ai

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class ActivityTrackerService : AccessibilityService() {

    private var lastTriggerTime: Long = 0
    private var currentApp: String = ""
    private var appStartTime: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            
            if (pkg != currentApp) {
                currentApp = pkg
                appStartTime = System.currentTimeMillis()
            } else {
                val usageDuration = (System.currentTimeMillis() - appStartTime) / (1000 * 60)
                val timeSinceLastPing = (System.currentTimeMillis() - lastTriggerTime) / (1000 * 60)

                // 20 நிமிடங்களுக்கு மேல் ஒரே ஆப் இயங்கினால் உரையாடலைத் தொடங்கும்
                if (usageDuration >= 20 && timeSinceLastPing >= 30) {
                    val intent = Intent(this, FloatingBubbleService::class.java).apply {
                        putExtra("CURRENT_APP", currentApp)
                        putExtra("MINUTES", usageDuration)
                    }
                    startService(intent)
                    lastTriggerTime = System.currentTimeMillis()
                }
            }
        }
    }

    override fun onInterrupt() {}
}
