package com.companion.ai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
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

        // Runtime Audio Permission Request
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 101)
        }

        val prefs = getSharedPreferences("AI_PARTNER_PREFS", Context.MODE_PRIVATE)

        // Programmatic Clean UI (No Missing XML Layout Crashes)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 80, 50, 50)
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.parseColor("#F8F9FA"))
        }

        val title = TextView(this).apply {
            text = "⚡ AI Partner"
            textSize = 24f
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
            text = "API Key சேமி"
            setBackgroundColor(Color.parseColor("#1A73E8"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val key = apiKeyInput.text.toString().trim()
                prefs.edit().putString("GEMINI_API_KEY", key).apply()
                Toast.makeText(this@MainActivity, "API Key சேமிக்கப்பட்டது!", Toast.LENGTH_SHORT).show()
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

        layout.addView(title)
        layout.addView(apiKeyInput)
        layout.addView(saveBtn)
        layout.addView(openSettingsBtn)

        setContentView(layout)
    }
}
