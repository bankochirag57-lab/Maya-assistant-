package com.example.voice

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.model.AssistantType
import com.example.data.model.AvailableVoice
import com.example.data.model.VoiceConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private var pendingSpeech: PendingSpeechItem? = null

    private data class PendingSpeechItem(
        val text: String,
        val assistant: AssistantType,
        val config: VoiceConfig
    )

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<AvailableVoice>>(emptyList())
    val availableVoices: StateFlow<List<AvailableVoice>> = _availableVoices.asStateFlow()

    private val gujaratiLocale = Locale.Builder().setLanguage("gu").setRegion("IN").build()
    private val hindiLocale = Locale.Builder().setLanguage("hi").setRegion("IN").build()
    private val indianEnglishLocale = Locale.Builder().setLanguage("en").setRegion("IN").build()

    init {
        mainHandler.post {
            tts = TextToSpeech(context, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setupProgressListener()
            loadAvailableVoices()

            // Process any speech that was queued before initialization finished
            pendingSpeech?.let { item ->
                speak(item.text, item.assistant, item.config)
                pendingSpeech = null
            }
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
            }
        })
    }

    private fun loadAvailableVoices() {
        val ttsInstance = tts ?: return
        try {
            val systemVoices = ttsInstance.voices ?: emptySet()
            val list = mutableListOf<AvailableVoice>()
            for (voice in systemVoices) {
                val nameLower = voice.name.lowercase()
                val isFemale = nameLower.contains("female") ||
                        nameLower.contains("#female") ||
                        nameLower.contains("woman") ||
                        nameLower.contains("-fem-") ||
                        nameLower.contains("f0") ||
                        nameLower.contains("f1") ||
                        nameLower.contains("voice-b") ||
                        nameLower.contains("voice-d")

                val isMale = nameLower.contains("male") ||
                        nameLower.contains("#male") ||
                        nameLower.contains("man") ||
                        nameLower.contains("-male-") ||
                        nameLower.contains("m0") ||
                        nameLower.contains("m1") ||
                        nameLower.contains("voice-a") ||
                        nameLower.contains("voice-c")

                list.add(
                    AvailableVoice(
                        id = voice.name,
                        name = voice.name.substringAfterLast(":").take(28),
                        language = "${voice.locale.displayLanguage} (${voice.locale.country})",
                        isMale = isMale,
                        isFemale = isFemale
                    )
                )
            }
            _availableVoices.value = list
        } catch (_: Exception) {
            _availableVoices.value = emptyList()
        }
    }

    /**
     * Determines text language by analyzing characters in Gujarati, Devanagari or Latin script
     */
    private fun detectLanguage(text: String, configLang: String): Locale {
        if (configLang != "auto") {
            return when (configLang.lowercase()) {
                "gu", "gujarati" -> gujaratiLocale
                "hi", "hindi" -> hindiLocale
                else -> indianEnglishLocale
            }
        }
        for (char in text) {
            val code = char.code
            if (code in 0x0A80..0x0AFF) return gujaratiLocale // Gujarati script
            if (code in 0x0900..0x097F) return hindiLocale    // Devanagari script
        }
        return indianEnglishLocale
    }

    fun speak(
        text: String,
        assistant: AssistantType,
        config: VoiceConfig
    ) {
        mainHandler.post {
            val ttsInstance = tts
            if (ttsInstance == null || !isInitialized) {
                pendingSpeech = PendingSpeechItem(text, assistant, config)
                return@post
            }

            ttsInstance.stop()

            val targetLocale = detectLanguage(text, config.language)
            val langResult = ttsInstance.setLanguage(targetLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                // If Gujarati data is missing on the device, try Hindi or Indian English
                val hiResult = ttsInstance.setLanguage(hindiLocale)
                if (hiResult == TextToSpeech.LANG_MISSING_DATA || hiResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    ttsInstance.setLanguage(Locale.US)
                }
            }

            // Apply direct calibrated pitch and speed without double-scaling
            // KING: deep male tone (~0.82f)
            // MAYA: sweet soft female tone (~1.15f)
            val effectivePitch = config.pitch.coerceIn(0.5f, 2.0f)
            val effectiveSpeed = config.speed.coerceIn(0.5f, 2.0f)

            ttsInstance.setPitch(effectivePitch)
            ttsInstance.setSpeechRate(effectiveSpeed)

            // Select distinct hardware voice if present
            selectBestVoice(assistant, config.voiceName, targetLocale)

            val params = Bundle()
            val utteranceId = "msg_${System.currentTimeMillis()}"
            _isSpeaking.value = true
            ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        }
    }

    private fun selectBestVoice(assistant: AssistantType, preferredVoiceName: String?, locale: Locale) {
        val ttsInstance = tts ?: return
        try {
            val voices = ttsInstance.voices ?: return
            if (!preferredVoiceName.isNullOrBlank()) {
                val matching = voices.firstOrNull { it.name == preferredVoiceName }
                if (matching != null) {
                    ttsInstance.voice = matching
                    return
                }
            }

            // Pick distinct voice based on assistant gender
            val candidate = voices.filter { it.locale.language == locale.language }
                .firstOrNull { voice ->
                    val nameLower = voice.name.lowercase()
                    if (assistant == AssistantType.KING) {
                        nameLower.contains("male") || nameLower.contains("man") || nameLower.contains("-male-")
                    } else {
                        nameLower.contains("female") || nameLower.contains("woman") || nameLower.contains("-fem-")
                    }
                }

            if (candidate != null) {
                ttsInstance.voice = candidate
            }
        } catch (_: Exception) {
            // Default system voice
        }
    }

    fun stop() {
        mainHandler.post {
            tts?.stop()
            _isSpeaking.value = false
            pendingSpeech = null
        }
    }

    fun shutdown() {
        mainHandler.post {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
            pendingSpeech = null
        }
    }
}
