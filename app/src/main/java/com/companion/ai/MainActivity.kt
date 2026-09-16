package com.companion.ai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ஆடியோ ரெக்கார்டிங் அனுமதி கோருதல்
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 101)
        }

        // மிதக்கும் பபுளுக்கான மேலடுக்கு அனுமதி (Overlay Permission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val overlayIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(overlayIntent)
        }

        val prefs = getSharedPreferences("AI_PARTNER_PREFS", Context.MODE_PRIVATE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 80, 50, 50)
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.parseColor("#F1F3F4"))
        }

        val title = TextView(this).apply {
            text = "⚡ AI Partner"
            textSize = 22f
            setTextColor(Color.parseColor("#1A73E8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        val apiKeyInput = EditText(this).apply {
            hint = "Gemini API Key உள்ளிடவும்"
            setText(prefs.getString("GEMINI_API_KEY", ""))
            setBackgroundColor(Color.WHITE)
            setPadding(30, 30, 30, 30)
            textSize = 14f
        }

        val saveBtn = Button(this).apply {
            text = "API Key சேமிக்க"
            setBackgroundColor(Color.parseColor("#1A73E8"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val key = apiKeyInput.text.toString().trim()
                prefs.edit().putString("GEMINI_API_KEY", key).apply()
                Toast.makeText(this@MainActivity, "API Key சேமிக்கப்பட்டது Master!", Toast.LENGTH_SHORT).show()
            }
        }

        val openSettingsBtn = Button(this).apply {
            text = "Accessibility அமைப்பைத் திற"
            setBackgroundColor(Color.parseColor("#34A853"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 20, 0, 20)
        }

        layout.addView(title)
        layout.addView(apiKeyInput, lp)
        layout.addView(saveBtn, lp)
        layout.addView(openSettingsBtn, lp)

        setContentView(layout)
    }
}
