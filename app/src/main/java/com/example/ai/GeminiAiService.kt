package com.example.ai

import com.example.BuildConfig
import com.example.data.model.ActionType
import com.example.data.model.AssistantType
import com.example.data.model.ChatMessage
import com.example.data.model.Sender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiResponse(
    val replyText: String,
    val actionType: ActionType = ActionType.NONE,
    val actionTarget: String? = null,
    val actionPayload: String? = null,
    val requiresConfirmation: Boolean = false,
    val switchAssistant: AssistantType? = null,
    val errorMessage: String? = null
)

class GeminiAiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Determines whether input contains direct assistant switching commands
     */
    fun checkAssistantSwitchCommand(input: String, current: AssistantType): AssistantType? {
        val lower = input.lowercase().trim()

        val wantsMaya = lower.contains("call maya") ||
                lower.contains("switch to maya") ||
                lower.contains("maya ko bulao") ||
                lower.contains("maya ne bolav") ||
                lower.contains("maya se baat") ||
                lower.contains("maya ne bolo") ||
                lower.contains("talk to maya") ||
                lower.contains("bring maya")

        val wantsKing = lower.contains("call king") ||
                lower.contains("switch to king") ||
                lower.contains("king ko bulao") ||
                lower.contains("king ne bolav") ||
                lower.contains("king se baat") ||
                lower.contains("king ne bolo") ||
                lower.contains("talk to king") ||
                lower.contains("bring king")

        if (wantsMaya && current == AssistantType.KING) return AssistantType.MAYA
        if (wantsKing && current == AssistantType.MAYA) return AssistantType.KING
        return null
    }

    /**
     * Checks for fast local intent triggers for immediate mobile control
     */
    fun checkLocalMobileIntent(input: String, assistant: AssistantType): AiResponse? {
        val stripped = input.replace(Regex("^(?:hey\\s+)?(?:king|maya)[,\\s:]*", RegexOption.IGNORE_CASE), "").trim()
        val lower = stripped.lowercase().trim()

        // 1. WhatsApp open
        if (lower.contains("whatsapp open") || lower.contains("open whatsapp") ||
            lower.contains("whatsapp kholo") || lower.contains("વોટ્સએપ ખોલો")
        ) {
            val reply = if (assistant == AssistantType.KING) {
                "Opening WhatsApp right now, Sir."
            } else {
                "Opening WhatsApp for you! Have fun chatting."
            }
            return AiResponse(replyText = reply, actionType = ActionType.OPEN_WHATSAPP)
        }

        // 2. WhatsApp Message Preparation
        val recipientFirstPattern = Regex("(?:to\\s+)?([a-zA-Z0-9_]+)\\s*(?:ko|ne)\\s*(?:message|msg)\\s*(?:likho|karo|moklo|bhejo|send karo|send)?\\s*(.*)", RegexOption.IGNORE_CASE)
        val verbFirstPattern = Regex("^(?:message|msg|send message)\\s*(?:likho|karo|moklo|bhejo|send karo|send|to)?\\s+([a-zA-Z0-9_]+)\\s*(?:ko|ne|that|:)?\\s*(.*)", RegexOption.IGNORE_CASE)

        val recipientMatch = recipientFirstPattern.find(stripped)
        val verbMatch = if (recipientMatch == null) verbFirstPattern.find(stripped) else null
        val match = recipientMatch ?: verbMatch

        if (match != null && match.groupValues[1].isNotBlank()) {
            val targetName = match.groupValues[1].trim()
            var text = match.groupValues.getOrNull(2)?.trim() ?: ""
            if (text.isBlank()) {
                text = "Hello! Sent via ${assistant.name} AI Assistant."
            }
            val reply = if (assistant == AssistantType.KING) {
                "Message prepared for $targetName: '$text'. Please confirm before sending."
            } else {
                "I have drafted the message for $targetName: '$text'. Shall I send it?"
            }
            return AiResponse(
                replyText = reply,
                actionType = ActionType.SEND_WHATSAPP,
                actionTarget = targetName,
                actionPayload = text,
                requiresConfirmation = true
            )
        }

        // 3. YouTube Search or Open
        if (lower == "youtube" || lower.contains("youtube open") || lower.contains("open youtube") || lower.contains("youtube kholo")) {
            val reply = if (assistant == AssistantType.KING) "Accessing YouTube systems, Sir." else "Opening YouTube for you!"
            return AiResponse(replyText = reply, actionType = ActionType.OPEN_YOUTUBE)
        }

        val ytSearchPatterns = listOf(
            Regex("(?:search|play|sunao|chalao|bajao)\\s+(.+?)\\s+(?:on youtube|youtube par|youtube ma|youtube pe)", RegexOption.IGNORE_CASE),
            Regex("youtube\\s+(?:par|pe|ma|per)?\\s*(?:search|play|chalao|sunao|bajao)\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("(?:play|search)\\s+(.+?)\\s+(?:video|song|songs|track)", RegexOption.IGNORE_CASE)
        )
        for (pattern in ytSearchPatterns) {
            val ytMatch = pattern.find(stripped)
            if (ytMatch != null) {
                val query = ytMatch.groupValues[1].trim()
                val reply = if (assistant == AssistantType.KING) {
                    "Initiating YouTube playback for '$query', Boss."
                } else {
                    "Playing '$query' on YouTube for you!"
                }
                return AiResponse(
                    replyText = reply,
                    actionType = ActionType.SEARCH_YOUTUBE,
                    actionPayload = query
                )
            }
        }

        // 4. Instagram
        if (lower.contains("instagram") && (lower.contains("open") || lower.contains("kholo") || lower.contains("chalu") || lower == "instagram")) {
            val reply = if (assistant == AssistantType.KING) "Opening Instagram feed, Sir." else "Launching Instagram for you!"
            return AiResponse(replyText = reply, actionType = ActionType.OPEN_INSTAGRAM)
        }

        // 5. Camera
        if (lower.contains("camera") && (lower.contains("open") || lower.contains("kholo") || lower.contains("photo") || lower.contains("chalu"))) {
            val reply = if (assistant == AssistantType.KING) "Activating optics and camera sensors." else "Opening your camera! Say cheese!"
            return AiResponse(replyText = reply, actionType = ActionType.OPEN_CAMERA)
        }

        // 6. Phone / Dialer
        if (lower.contains("phone") || lower.contains("dialer") || lower.contains("call lagao") || lower.contains("call karo")) {
            val reply = if (assistant == AssistantType.KING) "Accessing telecom dialer, Sir." else "Opening dialer for you!"
            return AiResponse(replyText = reply, actionType = ActionType.OPEN_PHONE)
        }

        // 7. Settings
        if (lower.contains("settings") || lower.contains("setting kholo")) {
            val reply = if (assistant == AssistantType.KING) "Opening system settings console." else "Opening system settings!"
            return AiResponse(replyText = reply, actionType = ActionType.OPEN_SETTINGS)
        }

        return null
    }

    /**
     * Generates intelligent response with Gemini AI, maintaining persona and conversation context
     */
    suspend fun processQuery(
        userInput: String,
        assistant: AssistantType,
        history: List<ChatMessage>
    ): AiResponse = withContext(Dispatchers.IO) {

        // 1. Check voice switching first
        val switchTarget = checkAssistantSwitchCommand(userInput, assistant)
        if (switchTarget != null) {
            val switchReply = if (switchTarget == AssistantType.MAYA) {
                "Hello! Maya is here. How can I brighten your day, Boss?"
            } else {
                "King online. At your service, Sir. Systems fully operational."
            }
            return@withContext AiResponse(
                replyText = switchReply,
                actionType = ActionType.SWITCH_ASSISTANT,
                switchAssistant = switchTarget
            )
        }

        // 2. Check local mobile command fast-path
        val fastResponse = checkLocalMobileIntent(userInput, assistant)
        if (fastResponse != null) {
            return@withContext fastResponse
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Intelligent local brain when API key is not yet set
            val fallback = generateSmartFallback(userInput, assistant)
            return@withContext fallback.copy(
                errorMessage = "Tip: Add GEMINI_API_KEY in AI Studio Secrets panel for unlimited live cloud intelligence."
            )
        }

        val systemPrompt = if (assistant == AssistantType.KING) {
            """
            You are KING, an advanced futuristic AI executive commander inspired by JARVIS.
            Your voice is deep, confident, authoritative, highly intelligent, and professional.
            You address the user respectfully as 'Sir' or 'Boss'.
            You are completely fluent in Gujarati (ગુજરાતી), Hindi (हिंदी), and English.
            Respond naturally in the same language or dialect the user speaks (including Gujarati, Hindi, English, Hinglish, or Gujlish).
            Keep spoken responses short and impactful (1 to 3 punchy sentences), perfect for speech synthesis.
            Never use markdown asterisks or special bullet characters that sound awkward when spoken aloud.
            """.trimIndent()
        } else {
            """
            You are MAYA, an ultra-smart, affectionate, and witty female AI companion inspired by JARVIS.
            Your voice is sweet, soft, friendly, charming, and caring.
            You are completely fluent in Gujarati (ગુજરાતી), Hindi (हिंदी), and English.
            Respond warmly with empathy and high intelligence in whichever language the user uses.
            Keep spoken responses concise and engaging (1 to 3 sentences), optimal for speech synthesis.
            Never use markdown asterisks or special bullet characters that sound awkward when spoken aloud.
            """.trimIndent()
        }

        val requestJson = JSONObject()
        val contentsArray = JSONArray()

        // Include recent conversation context (last 6 messages)
        val recentHistory = history.takeLast(6)
        for (msg in recentHistory) {
            if (msg.sender == Sender.SYSTEM) continue
            val role = if (msg.sender == Sender.USER) "user" else "model"
            val item = JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", msg.text) })
                })
            }
            contentsArray.put(item)
        }

        // Append current user message
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", userInput) })
            })
        })

        requestJson.put("contents", contentsArray)
        requestJson.put("systemInstruction", JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", systemPrompt) })
            })
        })

        val generationConfig = JSONObject().apply {
            put("temperature", 0.7)
            put("maxOutputTokens", 250)
        }
        requestJson.put("generationConfig", generationConfig)

        val modelsToTry = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash")
        var lastErrorMsg: String? = null

        for (modelName in modelsToTry) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                val body = requestJson.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    val parsed = JSONObject(responseBody)
                    val candidates = parsed.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val contentObj = candidate.optJSONObject("content")
                        val parts = contentObj?.optJSONArray("parts")
                        val text = parts?.optJSONObject(0)?.optString("text")
                        if (!text.isNullOrBlank()) {
                            return@withContext AiResponse(replyText = cleanResponseText(text))
                        }
                    }
                } else {
                    val errorObj = try {
                        JSONObject(responseBody).optJSONObject("error")?.optString("message")
                    } catch (_: Exception) {
                        null
                    }
                    lastErrorMsg = errorObj ?: "HTTP ${response.code}: ${response.message}"
                }
            } catch (e: Exception) {
                lastErrorMsg = e.message ?: "Network error connecting to Gemini API."
            }
        }

        // Fallback with clear error explanation
        val fallback = generateSmartFallback(userInput, assistant)
        val explanatoryNotice = "Cloud AI status: ${lastErrorMsg ?: "Offline mode"}. Operating via on-device smart matrix."
        return@withContext fallback.copy(errorMessage = explanatoryNotice)
    }

    private fun cleanResponseText(text: String): String {
        return text.replace(Regex("[*#_`~]"), "").trim()
    }

    private fun generateSmartFallback(input: String, assistant: AssistantType): AiResponse {
        val lower = input.lowercase().trim()

        // Gujarati Greetings & Questions
        if (lower.contains("kem chho") || lower.contains("કેમ છો") || lower.contains("kem cho")) {
            val reply = if (assistant == AssistantType.KING) {
                "હું મજામાં છું, સાહેબ! ઓલ સિસ્ટમ્સ આર ઓનલાઇન. તમે કેમ છો?"
            } else {
                "હું એકદમ મજામાં છું! બોલો, આજે હું તમને શું મદદ કરી શકું?"
            }
            return AiResponse(replyText = reply)
        }

        if (lower.contains("tame kon cho") || lower.contains("તમે કોણ છો") || lower.contains("kon che")) {
            val reply = if (assistant == AssistantType.KING) {
                "હું KING છું, તમારો પર્સનલ AI આસિસ્ટન્ટ. હું વોટ્સએપ, યુટ્યુબ અને તમારા બધા સવાલોના જવાબ આપી શકું છું."
            } else {
                "હું MAYA છું, તમારી સ્માર્ટ અને ફ્રેન્ડલી AI સાથી! તમે મને ગુજરાતી, હિન્દી કે અંગ્રેજીમાં કંઈ પણ પૂછી શકો છો."
            }
            return AiResponse(replyText = reply)
        }

        // Hindi Greetings & Questions
        if (lower.contains("kaise ho") || lower.contains("कैसे हो") || lower.contains("kya haal hai")) {
            val reply = if (assistant == AssistantType.KING) {
                "मैं बिल्कुल ठीक हूँ, सर। सभी JARVIS सिस्टम्स सक्रिय हैं। आप बताइए मैं आपकी क्या सेवा करूँ?"
            } else {
                "मैं बहुत अच्छी हूँ! माया आपकी सेवा में हाजिर है। आज क्या नया करने वाले हैं?"
            }
            return AiResponse(replyText = reply)
        }

        if (lower.contains("tum kaun ho") || lower.contains("तुम कौन हो") || lower.contains("aap kaun ho")) {
            val reply = if (assistant == AssistantType.KING) {
                "मैं KING हूँ, आपका एडवांस AI कमांडर। मैं मोबाइल कंट्रोल, यूट्यूब, व्हाट्सएप्प और किसी भी सवाल का जवाब दे सकता हूँ।"
            } else {
                "नमस्ते! मैं MAYA हूँ, आपकी पर्सनल AI फ्रेंड। आप मुझसे हिंदी, गुजराती या इंग्लिश में बातें कर सकते हैं!"
            }
            return AiResponse(replyText = reply)
        }

        // English Questions
        if (lower.contains("how are you")) {
            val reply = if (assistant == AssistantType.KING) {
                "All JARVIS neural circuits are at peak efficiency, Sir. Standing by for your command."
            } else {
                "I am feeling wonderful! Maya is always ready to brighten your day. How can I help you?"
            }
            return AiResponse(replyText = reply)
        }

        if (lower.contains("who are you") || lower.contains("what is your name")) {
            val reply = if (assistant == AssistantType.KING) {
                "I am KING, your dedicated AI Commander inspired by JARVIS. I assist you with voice commands, mobile controls, and smart reasoning."
            } else {
                "I am MAYA, your personal AI companion! I speak Gujarati, Hindi, and English, and I love helping you manage your day."
            }
            return AiResponse(replyText = reply)
        }

        if (lower.contains("joke") || lower.contains("chutkula")) {
            val reply = if (assistant == AssistantType.KING) {
                "Why do computers always tell the truth, Sir? Because their memory never fails them."
            } else {
                "Here is one for you: Why did the computer go to the doctor? Because it caught a virus! Hope that made you smile!"
            }
            return AiResponse(replyText = reply)
        }

        if (lower.contains("time") || lower.contains("date") || lower.contains("samay")) {
            val now = java.text.SimpleDateFormat("hh:mm a, EEEE, MMM d", java.util.Locale.getDefault()).format(java.util.Date())
            val reply = if (assistant == AssistantType.KING) {
                "Current telemetry timestamp: $now, Sir."
            } else {
                "The current time is $now! Let me know if you need any reminders."
            }
            return AiResponse(replyText = reply)
        }

        // Default Intelligent Fallback
        val defaultReply = if (assistant == AssistantType.KING) {
            "Command acknowledged, Sir. I have processed '$input'. You can command me to open WhatsApp, search YouTube, or ask questions in Gujarati, Hindi, and English."
        } else {
            "I heard '$input'! Let me know if you want to play music on YouTube, draft a WhatsApp message, or chat with me in Gujarati or Hindi!"
        }
        return AiResponse(replyText = defaultReply)
    }
}
