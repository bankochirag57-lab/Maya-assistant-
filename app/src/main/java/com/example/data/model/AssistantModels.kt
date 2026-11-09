package com.example.data.model

import java.util.UUID

enum class AssistantType(val displayName: String, val title: String, val description: String) {
    KING(
        displayName = "KING",
        title = "AI Executive Commander",
        description = "Deep, confident male voice. Multilingual intelligence in Gujarati, Hindi & English."
    ),
    MAYA(
        displayName = "MAYA",
        title = "AI Companion Core",
        description = "Sweet, soft female voice. Friendly, witty multilingual intelligence in Gujarati, Hindi & English."
    );

    fun switchTarget(): AssistantType = if (this == KING) MAYA else KING
}

enum class Sender {
    USER,
    KING,
    MAYA,
    SYSTEM
}

enum class ActionType {
    NONE,
    OPEN_WHATSAPP,
    SEND_WHATSAPP,
    OPEN_YOUTUBE,
    SEARCH_YOUTUBE,
    OPEN_INSTAGRAM,
    OPEN_CAMERA,
    OPEN_PHONE,
    OPEN_SETTINGS,
    SWITCH_ASSISTANT
}

data class VoiceConfig(
    val pitch: Float,
    val speed: Float,
    val voiceName: String? = null,
    val language: String = "auto"
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: Sender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: ActionType = ActionType.NONE,
    val actionPayload: String? = null
)

data class PendingAction(
    val actionType: ActionType,
    val targetName: String = "",
    val messagePayload: String = "",
    val promptText: String = ""
)

data class AvailableVoice(
    val id: String,
    val name: String,
    val language: String,
    val isMale: Boolean,
    val isFemale: Boolean
)
