package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AssistantType
import com.example.data.model.VoiceConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "assistant_voice_settings")

class VoiceSettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val ACTIVE_ASSISTANT = stringPreferencesKey("active_assistant")
        val KING_PITCH = floatPreferencesKey("king_pitch")
        val KING_SPEED = floatPreferencesKey("king_speed")
        val KING_VOICE_NAME = stringPreferencesKey("king_voice_name")
        val KING_LANG = stringPreferencesKey("king_lang")

        val MAYA_PITCH = floatPreferencesKey("maya_pitch")
        val MAYA_SPEED = floatPreferencesKey("maya_speed")
        val MAYA_VOICE_NAME = stringPreferencesKey("maya_voice_name")
        val MAYA_LANG = stringPreferencesKey("maya_lang")
    }

    val activeAssistantFlow: Flow<AssistantType> = context.dataStore.data.map { preferences ->
        val name = preferences[PreferencesKeys.ACTIVE_ASSISTANT] ?: AssistantType.KING.name
        try {
            AssistantType.valueOf(name)
        } catch (_: Exception) {
            AssistantType.KING
        }
    }

    val kingVoiceConfigFlow: Flow<VoiceConfig> = context.dataStore.data.map { preferences ->
        VoiceConfig(
            pitch = preferences[PreferencesKeys.KING_PITCH] ?: 0.80f, // Deep confident male pitch
            speed = preferences[PreferencesKeys.KING_SPEED] ?: 0.95f,
            voiceName = preferences[PreferencesKeys.KING_VOICE_NAME],
            language = preferences[PreferencesKeys.KING_LANG] ?: "auto"
        )
    }

    val mayaVoiceConfigFlow: Flow<VoiceConfig> = context.dataStore.data.map { preferences ->
        VoiceConfig(
            pitch = preferences[PreferencesKeys.MAYA_PITCH] ?: 1.18f, // Sweet soft female pitch
            speed = preferences[PreferencesKeys.MAYA_SPEED] ?: 1.02f,
            voiceName = preferences[PreferencesKeys.MAYA_VOICE_NAME],
            language = preferences[PreferencesKeys.MAYA_LANG] ?: "auto"
        )
    }

    suspend fun setActiveAssistant(assistant: AssistantType) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_ASSISTANT] = assistant.name
        }
    }

    suspend fun saveKingVoiceConfig(config: VoiceConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KING_PITCH] = config.pitch
            preferences[PreferencesKeys.KING_SPEED] = config.speed
            config.voiceName?.let { preferences[PreferencesKeys.KING_VOICE_NAME] = it }
            preferences[PreferencesKeys.KING_LANG] = config.language
        }
    }

    suspend fun saveMayaVoiceConfig(config: VoiceConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MAYA_PITCH] = config.pitch
            preferences[PreferencesKeys.MAYA_SPEED] = config.speed
            config.voiceName?.let { preferences[PreferencesKeys.MAYA_VOICE_NAME] = it }
            preferences[PreferencesKeys.MAYA_LANG] = config.language
        }
    }
}
