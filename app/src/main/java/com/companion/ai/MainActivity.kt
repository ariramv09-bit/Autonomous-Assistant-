package com.companion.ai

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 100, 50, 50)
            gravity = android.view.Gravity.CENTER
        }

        val title = TextView(this).apply {
            text = "AI Partner (Autonomous Assistant)"
            textSize = 22f
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setPadding(0, 0, 0, 40)
        }

        val statusText = TextView(this).apply {
            text = "Accessibility அனுமதியை ஆன் செய்துவிட்டு ஆப்பைப் பயன்படுத்தவும்."
            textSize = 16f
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setPadding(0, 0, 0, 50)
        }

        val openSettingsBtn = Button(this).apply {
            text = "அனுமதியை இயக்கு (Open Settings)"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(openSettingsBtn)

        setContentView(layout)
    }
}
