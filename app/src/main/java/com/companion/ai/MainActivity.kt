package com.companion.ai

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var statusText: TextView
    private lateinit var apiKeyInput: EditText
    private val RECORD_AUDIO_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val prefs = getSharedPreferences("AI_PARTNER_PREFS", Context.MODE_PRIVATE)
        val savedKey = prefs.getString("GEMINI_API_KEY", "") ?: ""

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }

        val title = TextView(this).apply {
            text = "AI Partner (தமிழ் உதவியாளர்)"
            textSize = 22f
            setPadding(0, 0, 0, 30)
        }

        apiKeyInput = EditText(this).apply {
            hint = "Gemini API Key உள்ளிடவும்"
            setText(savedKey)
            setPadding(20, 20, 20, 20)
        }

        val saveKeyBtn = Button(this).apply {
            text = "API Key சேமி"
            setOnClickListener {
                val key = apiKeyInput.text.toString().trim()
                if (key.isNotEmpty()) {
                    prefs.edit().putString("GEMINI_API_KEY", key).apply()
                    Toast.makeText(this@MainActivity, "API Key சேமிக்கப்பட்டது!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "சரியான API Key உள்ளிடவும்", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val settingsBtn = Button(this).apply {
            text = "Accessibility அமைப்புகள்"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        val speakBtn = Button(this).apply {
            text = "🎤 தமிழில் பேசவும் (Speak Tamil)"
            setPadding(30, 30, 30, 30)
            setOnClickListener {
                checkPermissionAndListen()
            }
        }

        statusText = TextView(this).apply {
            text = "மைக் பட்டனைத் தொட்டு தமிழில் பேசவும்..."
            textSize = 16f
            setPadding(0, 40, 0, 0)
        }

        layout.addView(title)
        layout.addView(apiKeyInput)
        layout.addView(saveKeyBtn)
        layout.addView(settingsBtn)
        layout.addView(speakBtn)
        layout.addView(statusText)

        setContentView(layout)
        setupSpeechRecognizer()
    }

    private fun checkPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
        } else {
            startListening()
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ta-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ta-IN")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ta-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "தமிழில் பேசவும்...")
        }
        statusText.text = "கேட்டுக்கொண்டிருக்கிறேன்..."
        speechRecognizer.startListening(intent)
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                statusText.text = "பகுப்பாய்வு செய்கிறது..."
            }
            override fun onError(error: Int) {
                statusText.text = "குரல் கேட்கவில்லை (பிழை: $error). மீண்டும் முயற்சிக்கவும்."
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val userQuery = matches[0]
                    statusText.text = "நீங்கள் கேட்டது: $userQuery\n\nAI சிந்திக்கிறது..."
                    askGemini(userQuery)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun askGemini(prompt: String) {
        val prefs = getSharedPreferences("AI_PARTNER_PREFS", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("GEMINI_API_KEY", "") ?: ""

        if (apiKey.isEmpty()) {
            val msg = "முதலில் Gemini API Key உள்ளிட்டு சேமிக்கவும்."
            statusText.text = msg
            speakTamil(msg)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // கூகுள் பரிந்துரைத்த gemini-3.5-flash-lite நேரடி எண்ட்பாயிண்ட்
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=$apiKey")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    doOutput = true
                    doInput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                val jsonPayload = JSONObject().apply {
                    val contentsArray = JSONArray()
                    val contentObj = JSONObject()
                    val partsArray = JSONArray()
                    val partObj = JSONObject().apply {
                        put("text", "You are an autonomous Android AI assistant. Always respond concisely and clearly in Tamil language only. User question: $prompt")
                    }
                    partsArray.put(partObj)
                    contentObj.put("parts", partsArray)
                    contentsArray.put(contentObj)
                    put("contents", contentsArray)
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(jsonPayload.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val responseJson = JSONObject(responseStr)
                    val candidates = responseJson.optJSONArray("candidates")
                    val reply = if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        parts?.getJSONObject(0)?.optString("text") ?: "பதில் கிடைக்கவில்லை."
                    } else {
                        "பதில் கிடைக்கவில்லை."
                    }

                    withContext(Dispatchers.Main) {
                        statusText.text = "பதில்:\n$reply"
                        speakTamil(reply)
                    }
                } else {
                    val errorStr = BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { it.readText() }
                    withContext(Dispatchers.Main) {
                        statusText.text = "API பிழை ($responseCode):\n$errorStr"
                        speakTamil("தகவல் பெறுவதில் பிழை ஏற்பட்டுள்ளது.")
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMsg = "பிழை: ${e.localizedMessage}"
                    statusText.text = errorMsg
                    speakTamil("இணைப்பில் பிழை ஏற்பட்டுள்ளது.")
                }
            }
        }
    }

    private fun speakTamil(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("ta", "IN")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
        speechRecognizer.destroy()
    }
}
