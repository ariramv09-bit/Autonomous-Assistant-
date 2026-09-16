package com.companion.ai

import com.google.genai.Client

object GeminiManager {
    private const val API_KEY = "AQ.Ab8RN6IjkrjTfHU-cEFR5Y49UMnHMsDTM4w2U1V4W0GybfBqng"
    private val client = Client(apiKey = API_KEY)

    suspend fun generateFriendResponse(currentApp: String, minutesUsed: Long): String {
        val cleanAppName = currentApp.substringAfterLast(".")
        val prompt = """
            நீ ஒரு நெருங்கிய நண்பன்/பார்ட்னர். 
            பயனர் தற்போது '$cleanAppName' செயலியை $minutesUsed நிமிடங்களாகப் பயன்படுத்துகிறார்.
            அவரிடம் எந்திரத்தனமாக இல்லாமல், ஒரு எதார்த்தமான நண்பனைப் போல உரிமையோடு தமிழில் 1 அல்லது 2 வரிகளில் மட்டும் பேசவும்.
        """.trimIndent()

        return try {
            val response = client.models.generateContent(
                model = "gemini-2.5-flash",
                contents = prompt
            )
            response.text ?: "ரொம்ப நேரமா இதுலயே இருக்கீங்க போல, ஒரு சின்ன பிரேக் எடுக்கலாமே?"
        } catch (e: Exception) {
            "ரொம்ப நேரமா ஸ்கிரீனைப் பார்க்குறீங்க, ஒரு 5 நிமிஷம் கண்ணுக்கு ஓய்வு குடுங்க!"
        }
    }
}
