package com.companion.ai

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.Toast

class CompanionService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var floatingBubble: Button? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        showFloatingBubble()
    }

    private fun showFloatingBubble() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        floatingBubble = Button(this).apply {
            text = "🎤 AI"
            textSize = 14f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1A73E8"))
            setPadding(25, 25, 25, 25)

            // நகர்த்தும் திறன் (Draggable Floating Bubble)
            setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var isClick = false

                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    when (event?.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = layoutParams!!.x
                            initialY = layoutParams!!.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            isClick = true
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val diffX = (event.rawX - initialTouchX).toInt()
                            val diffY = (event.rawY - initialTouchY).toInt()
                            if (Math.abs(diffX) > 10 || Math.abs(diffY) > 10) {
                                isClick = false
                            }
                            layoutParams!!.x = initialX + diffX
                            layoutParams!!.y = initialY + diffY
                            windowManager?.updateViewLayout(floatingBubble, layoutParams)
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (isClick) {
                                onFloatingBubbleClicked()
                            }
                            return true
                        }
                    }
                    return false
                }
            })
        }

        try {
            windowManager?.addView(floatingBubble, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onFloatingBubbleClicked() {
        Toast.makeText(this, "AI: திரை வாசிக்கப்படுகிறது...", Toast.LENGTH_SHORT).show()
        // அடுத்த கட்டமாக இங்கே திரையின் நோடுகளை எடுக்கும் ஸ்கேனர் இயங்கும்
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // திரையில் ஏற்படும் மாற்றங்களை உடனுக்குடன் கவனிக்கும் இடம்
    }

    override fun onInterrupt() {
        // சேவை தடைபடும்போது அழைக்கப்படும்
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingBubble != null && windowManager != null) {
            windowManager?.removeView(floatingBubble)
        }
    }
}
