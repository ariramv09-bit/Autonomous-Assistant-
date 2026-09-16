package com.companion.ai

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.Toast
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

class CompanionService : AccessibilityService(), TextToSpeech.OnInitListener {

    private var windowManager: WindowManager? = null
    private var floatingBubble: Button? = null
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isBusy = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        tts = TextToSpeech(this, this)
        showFloatingBubble()
    }

    private fun showFloatingBubble() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 350
        }

        floatingBubble = Button(this).apply {
            text = "🎤 AI"
            textSize = 14f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1A73E8"))
            setPadding(30, 30, 30, 30)

            setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var isClick = false

                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    val currentParams = layoutParams as WindowManager.LayoutParams
                    when (event?.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = currentParams.x
                            initialY = currentParams.y
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
                            currentParams.x = initialX + diffX
                            currentParams.y = initialY + diffY
                            windowManager?.updateViewLayout(floatingBubble, currentParams)
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (isClick) {
                                startVoiceCommandListening()
                            }
                            return true
                        }
                    }
                    return false
                }
            })
        }

        try {
            windowManager?.addView(floatingBubble, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVoiceCommandListening() {
        if (isBusy) {
            Toast.makeText(this, "AI ஏற்கனவே பணியில் உள்ளது...", Toast.LENGTH_SHORT).show()
            return
        }

        mainHandler.post {
            floatingBubble?.text = "👂..."
            floatingBubble?.setBackgroundColor(Color.parseColor("#D93025"))

            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    floatingBubble?.text = "⚡ AI"
                    floatingBubble?.setBackgroundColor(Color.parseColor("#1A73E8"))
                }
                override fun onError(error: Int) {
                    floatingBubble?.text = "🎤 AI"
                    floatingBubble?.setBackgroundColor(Color.parseColor("#1A73E8"))
                    speakTamil("குரல் கேட்கவில்லை Master.")
                }
                override fun onResults(results: Bundle?) {
                    floatingBubble?.text = "⚡ AI"
                    floatingBubble?.setBackgroundColor(Color.parseColor("#1A73E8"))
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val command = matches[0]
                        Toast.makeText(this@CompanionService, "கட்டளை: $command", Toast.LENGTH_SHORT).show()
                        processAutonomousLoop(command)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ta-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ta-IN")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ta-IN")
            }
            speechRecognizer?.startListening(intent)
        }
    }

    private fun processAutonomousLoop(userCommand: String) {
        val prefs = getSharedPreferences("AI_PARTNER_PREFS", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("GEMINI_API_KEY", "") ?: ""

        if (apiKey.isEmpty()) {
            speakTamil("முதலில் ஆப்பில் சென்று Gemini API Key சேமிக்கவும்.")
            return
        }

        isBusy = true
        val screenContext = parseCurrentScreen()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=$apiKey")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    doOutput = true
                    doInput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                val systemPrompt = """
                    You are an Autonomous Android Assistant.
                    User Command: "$userCommand"
                    Current Screen Nodes are provided below. Decide the exact next action to fulfill the command.
                    Respond ONLY with a JSON object in this schema without any markdown formatting:
                    {
                      "action": "CLICK" | "TYPE" | "SCROLL_DOWN" | "SCROLL_UP" | "GLOBAL_HOME" | "GLOBAL_BACK" | "SPEAK_ONLY",
                      "x": integer_x,
                      "y": integer_y,
                      "text_to_type": "text if action is TYPE",
                      "tamil_reply": "Brief Tamil speech response"
                    }
                """.trimIndent()

                val payload = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", "$systemPrompt\n\nScreen Data:\n$screenContext")
                                })
                            })
                        })
                    })
                }

                OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()); it.flush() }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val responseJson = JSONObject(responseStr)
                    val candidates = responseJson.optJSONArray("candidates")
                    val rawAiText = candidates?.getJSONObject(0)?.getJSONObject("content")
                        ?.getJSONArray("parts")?.getJSONObject(0)?.optString("text") ?: ""

                    val cleanJson = rawAiText.replace("```json", "").replace("```", "").trim()
                    val actionObj = JSONObject(cleanJson)

                    val action = actionObj.optString("action", "SPEAK_ONLY")
                    val x = actionObj.optInt("x", 0)
                    val y = actionObj.optInt("y", 0)
                    val textToType = actionObj.optString("text_to_type", "")
                    val tamilReply = actionObj.optString("tamil_reply", "பணி தொடங்குகிறது.")

                    withContext(Dispatchers.Main) {
                        speakTamil(tamilReply)
                        executeAction(action, x, y, textToType)
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    speakTamil("செயல்முறை பிழை ஏற்பட்டது Master.")
                }
            } finally {
                isBusy = false
            }
        }
    }

    private fun executeAction(action: String, x: Int, y: Int, textToType: String) {
        when (action) {
            "CLICK" -> {
                if (x > 0 && y > 0) performVirtualClick(x.toFloat(), y.toFloat())
            }
            "TYPE" -> {
                if (textToType.isNotEmpty()) {
                    val root = rootInActiveWindow
                    val focused = root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    if (focused != null && focused.isEditable) {
                        val args = Bundle().apply {
                            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
                        }
                        focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    } else if (x > 0 && y > 0) {
                        performVirtualClick(x.toFloat(), y.toFloat())
                    }
                }
            }
            "SCROLL_DOWN" -> {
                val path = Path().apply {
                    moveTo(500f, 1200f)
                    lineTo(500f, 400f)
                }
                dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 300)).build(), null, null)
            }
            "SCROLL_UP" -> {
                val path = Path().apply {
                    moveTo(500f, 400f)
                    lineTo(500f, 1200f)
                }
                dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 300)).build(), null, null)
            }
            "GLOBAL_HOME" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "GLOBAL_BACK" -> performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun parseCurrentScreen(): String {
        val rootNode = rootInActiveWindow ?: return "Empty Screen"
        val builder = StringBuilder()
        traverseNodes(rootNode, builder)
        return builder.toString()
    }

    private fun traverseNodes(node: AccessibilityNodeInfo?, builder: StringBuilder) {
        if (node == null) return

        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val isClickable = node.isClickable
        val isEditable = node.isEditable

        if (text.isNotEmpty() || desc.isNotEmpty() || isClickable || isEditable) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            builder.append("{text:'$text', desc:'$desc', clickable:$isClickable, editable:$isEditable, center:(${rect.centerX()},${rect.centerY()})}\n")
        }

        for (i in 0 until node.childCount) {
            traverseNodes(node.getChild(i), builder)
        }
    }

    private fun performVirtualClick(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun speakTamil(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("ta", "IN")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
        if (floatingBubble != null && windowManager != null) {
            windowManager?.removeView(floatingBubble)
        }
    }
}
