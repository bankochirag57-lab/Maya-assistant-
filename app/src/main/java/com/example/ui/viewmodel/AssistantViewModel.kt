package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.action.ActionResult
import com.example.action.MobileActionDispatcher
import com.example.ai.GeminiAiService
import com.example.data.datastore.VoiceSettingsRepository
import com.example.data.model.ActionType
import com.example.data.model.AssistantType
import com.example.data.model.AvailableVoice
import com.example.data.model.ChatMessage
import com.example.data.model.PendingAction
import com.example.data.model.Sender
import com.example.data.model.VoiceConfig
import com.example.voice.SpeechRecognitionManager
import com.example.voice.SpeechState
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val voiceRepo = VoiceSettingsRepository(application)
    private val actionDispatcher = MobileActionDispatcher(application)
    private val ttsManager = TextToSpeechManager(application)
    private val speechManager = SpeechRecognitionManager(application)
    private val geminiService = GeminiAiService()

    private val _activeAssistant = MutableStateFlow(AssistantType.KING)
    val activeAssistant: StateFlow<AssistantType> = _activeAssistant.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    val rmsLevel: StateFlow<Float> = speechManager.rmsLevel

    private val _transcriptPreview = MutableStateFlow("")
    val transcriptPreview: StateFlow<String> = _transcriptPreview.asStateFlow()

    private val _pendingAction = MutableStateFlow<PendingAction?>(null)
    val pendingAction: StateFlow<PendingAction?> = _pendingAction.asStateFlow()

    private val _kingVoiceConfig = MutableStateFlow(VoiceConfig(pitch = 0.80f, speed = 0.95f))
    val kingVoiceConfig: StateFlow<VoiceConfig> = _kingVoiceConfig.asStateFlow()

    private val _mayaVoiceConfig = MutableStateFlow(VoiceConfig(pitch = 1.18f, speed = 1.02f))
    val mayaVoiceConfig: StateFlow<VoiceConfig> = _mayaVoiceConfig.asStateFlow()

    val availableVoices: StateFlow<List<AvailableVoice>> = ttsManager.availableVoices

    private val _currentLanguage = MutableStateFlow("auto")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _infoBanner = MutableStateFlow<String?>(null)
    val infoBanner: StateFlow<String?> = _infoBanner.asStateFlow()

    init {
        // Collect persisted settings
        viewModelScope.launch {
            voiceRepo.activeAssistantFlow.collectLatest { saved ->
                _activeAssistant.value = saved
            }
        }

        viewModelScope.launch {
            voiceRepo.kingVoiceConfigFlow.collectLatest { config ->
                _kingVoiceConfig.value = config
            }
        }

        viewModelScope.launch {
            voiceRepo.mayaVoiceConfigFlow.collectLatest { config ->
                _mayaVoiceConfig.value = config
            }
        }

        // Collect speech recognition events
        viewModelScope.launch {
            speechManager.speechState.collectLatest { state ->
                when (state) {
                    is SpeechState.Idle -> {
                        _isListening.value = false
                        _transcriptPreview.value = ""
                    }
                    is SpeechState.Listening -> {
                        _isListening.value = true
                        _transcriptPreview.value = "Listening to voice input..."
                    }
                    is SpeechState.PartialResult -> {
                        _isListening.value = true
                        _transcriptPreview.value = state.text
                    }
                    is SpeechState.FinalResult -> {
                        _isListening.value = false
                        _transcriptPreview.value = ""
                        processUserInput(state.text)
                    }
                    is SpeechState.Error -> {
                        _isListening.value = false
                        _transcriptPreview.value = ""
                        _infoBanner.value = state.message
                    }
                }
            }
        }

        // Add initial welcoming greeting from KING
        val welcomeGreeting = "JARVIS Subsystems Activated. KING is online and at your command, Sir. Say 'KING, call MAYA' to switch, or give any command in Gujarati, Hindi, or English."
        _messages.value = listOf(
            ChatMessage(sender = Sender.KING, text = welcomeGreeting)
        )
    }

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
    }

    fun onMicClicked() {
        if (_isListening.value) {
            speechManager.stopListening()
            _isListening.value = false
        } else {
            ttsManager.stop()
            val currentLang = if (_currentLanguage.value != "auto") {
                _currentLanguage.value
            } else if (_activeAssistant.value == AssistantType.KING) {
                _kingVoiceConfig.value.language
            } else {
                _mayaVoiceConfig.value.language
            }
            speechManager.startListening(currentLang)
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun switchAssistant(target: AssistantType, announce: Boolean = true) {
        if (_activeAssistant.value == target && !announce) return

        _activeAssistant.value = target
        viewModelScope.launch {
            voiceRepo.setActiveAssistant(target)
        }

        val greetingText = if (target == AssistantType.MAYA) {
            "Hello! Maya is here. How can I brighten your day, Boss?"
        } else {
            "King online. At your service, Sir. Systems fully operational."
        }

        val sender = if (target == AssistantType.MAYA) Sender.MAYA else Sender.KING
        val switchMsg = ChatMessage(sender = sender, text = greetingText)
        _messages.value = _messages.value + switchMsg

        if (announce) {
            speakText(greetingText, target)
        }
    }

    fun processUserInput(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return

        // Stop any active TTS
        ttsManager.stop()

        // Append user chat message
        val userMsg = ChatMessage(sender = Sender.USER, text = trimmed)
        _messages.value = _messages.value + userMsg

        // Check if user is answering a pending confirmation
        val currentPending = _pendingAction.value
        if (currentPending != null) {
            val lower = trimmed.lowercase()
            if (lower.contains("yes") || lower.contains("send") || lower.contains("ha") ||
                lower.contains("bhejo") || lower.contains("moklo") || lower.contains("confirm")
            ) {
                confirmPendingAction()
                return
            } else if (lower.contains("no") || lower.contains("cancel") || lower.contains("nahi") || lower.contains("rehvado")) {
                dismissPendingAction()
                val cancelReply = "Action cancelled as requested."
                val cancelMsg = ChatMessage(
                    sender = if (_activeAssistant.value == AssistantType.KING) Sender.KING else Sender.MAYA,
                    text = cancelReply
                )
                _messages.value = _messages.value + cancelMsg
                speakText(cancelReply, _activeAssistant.value)
                return
            }
        }

        _isThinking.value = true

        viewModelScope.launch {
            val aiResponse = geminiService.processQuery(
                userInput = trimmed,
                assistant = _activeAssistant.value,
                history = _messages.value
            )
            _isThinking.value = false

            // Check if switching assistant
            if (aiResponse.switchAssistant != null) {
                switchAssistant(aiResponse.switchAssistant, announce = true)
                return@launch
            }

            // Check if action requires security confirmation
            if (aiResponse.requiresConfirmation) {
                _pendingAction.value = PendingAction(
                    actionType = aiResponse.actionType,
                    targetName = aiResponse.actionTarget.orEmpty(),
                    messagePayload = aiResponse.actionPayload.orEmpty(),
                    promptText = aiResponse.replyText
                )
            } else if (aiResponse.actionType != ActionType.NONE) {
                // Execute action immediately
                executeActionDirect(aiResponse.actionType, aiResponse.actionPayload)
            }

            aiResponse.errorMessage?.let {
                _infoBanner.value = it
            }

            val currentAssistant = _activeAssistant.value
            val sender = if (currentAssistant == AssistantType.KING) Sender.KING else Sender.MAYA
            val replyMsg = ChatMessage(
                sender = sender,
                text = aiResponse.replyText,
                actionType = aiResponse.actionType,
                actionPayload = aiResponse.actionPayload
            )
            _messages.value = _messages.value + replyMsg

            speakText(aiResponse.replyText, currentAssistant)
        }
    }

    private fun executeActionDirect(actionType: ActionType, payload: String?) {
        val result = when (actionType) {
            ActionType.OPEN_WHATSAPP -> actionDispatcher.openWhatsApp()
            ActionType.OPEN_YOUTUBE -> actionDispatcher.openYouTube()
            ActionType.SEARCH_YOUTUBE -> actionDispatcher.searchYouTube(payload ?: "")
            ActionType.OPEN_INSTAGRAM -> actionDispatcher.openInstagram()
            ActionType.OPEN_CAMERA -> actionDispatcher.openCamera()
            ActionType.OPEN_PHONE -> actionDispatcher.openPhoneDialer(payload)
            ActionType.OPEN_SETTINGS -> actionDispatcher.openSettings()
            else -> null
        }
        if (result is ActionResult.Failure) {
            _infoBanner.value = result.reason
        }
    }

    fun confirmPendingAction() {
        val pending = _pendingAction.value ?: return
        _pendingAction.value = null

        when (pending.actionType) {
            ActionType.SEND_WHATSAPP -> {
                val res = actionDispatcher.sendConfirmedWhatsAppMessage(pending.messagePayload)
                val confirmText = "WhatsApp message dispatched to ${pending.targetName}."
                val msg = ChatMessage(
                    sender = if (_activeAssistant.value == AssistantType.KING) Sender.KING else Sender.MAYA,
                    text = confirmText
                )
                _messages.value = _messages.value + msg
                speakText(confirmText, _activeAssistant.value)
                if (res is ActionResult.Failure) {
                    _infoBanner.value = res.reason
                }
            }
            else -> {}
        }
    }

    fun dismissPendingAction() {
        _pendingAction.value = null
    }

    fun openShortcut(actionType: ActionType, payload: String? = null) {
        ttsManager.stop()
        executeActionDirect(actionType, payload)
    }

    fun updateKingVoiceConfig(pitch: Float, speed: Float, voiceName: String?, language: String) {
        val updated = VoiceConfig(pitch, speed, voiceName, language)
        _kingVoiceConfig.value = updated
        viewModelScope.launch {
            voiceRepo.saveKingVoiceConfig(updated)
        }
    }

    fun updateMayaVoiceConfig(pitch: Float, speed: Float, voiceName: String?, language: String) {
        val updated = VoiceConfig(pitch, speed, voiceName, language)
        _mayaVoiceConfig.value = updated
        viewModelScope.launch {
            voiceRepo.saveMayaVoiceConfig(updated)
        }
    }

    fun testSampleVoice(assistant: AssistantType) {
        ttsManager.stop()
        val testPhrase = if (assistant == AssistantType.KING) {
            "નમસ્તે! I am King, your deep-voiced AI Commander. Systems online."
        } else {
            "Hello! હું માયા છું. Maya is happy to speak with you today!"
        }
        speakText(testPhrase, assistant)
    }

    fun clearBanner() {
        _infoBanner.value = null
    }

    fun clearChat() {
        _messages.value = emptyList()
    }

    private fun speakText(text: String, assistant: AssistantType) {
        val config = if (assistant == AssistantType.KING) _kingVoiceConfig.value else _mayaVoiceConfig.value
        ttsManager.speak(text, assistant, config)
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.stopListening()
        ttsManager.shutdown()
    }
}
