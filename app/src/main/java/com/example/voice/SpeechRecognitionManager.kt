package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SpeechState {
    object Idle : SpeechState()
    object Listening : SpeechState()
    data class PartialResult(val text: String) : SpeechState()
    data class FinalResult(val text: String) : SpeechState()
    data class Error(val message: String, val isNoMatch: Boolean = false) : SpeechState()
}

class SpeechRecognitionManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(languagePreference: String = "auto") {
        mainHandler.post {
            if (!isRecognitionAvailable()) {
                _speechState.value = SpeechState.Error(
                    "Speech recognition service not found on this system. You can type commands in the text box below.",
                    isNoMatch = false
                )
                return@post
            }

            stopListeningInternal()

            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _speechState.value = SpeechState.Listening
                        }

                        override fun onBeginningOfSpeech() {}

                        override fun onRmsChanged(rmsdB: Float) {
                            // Normalize RMS dB typically in range [-2f, 10f] to [0f, 1f]
                            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                            _rmsLevel.value = normalized
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _rmsLevel.value = 0f
                        }

                        override fun onError(error: Int) {
                            _rmsLevel.value = 0f
                            val isNoMatchError = (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                            val errorMessage = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check your microphone."
                                SpeechRecognizer.ERROR_CLIENT -> "Speech recognizer client error. Tap mic to retry."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required to listen."
                                SpeechRecognizer.ERROR_NETWORK -> "Network issue encountered during speech recognition."
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out. Please try again."
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please speak clearly into your mic, or use the text box below."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice engine is busy. Please wait a moment."
                                SpeechRecognizer.ERROR_SERVER -> "Voice server returned an error. Tap mic to retry."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input received. Speak closer to the microphone or tap any quick prompt."
                                else -> "Speech recognition error ($error). You can also type commands below."
                            }
                            _speechState.value = SpeechState.Error(errorMessage, isNoMatch = isNoMatchError)
                        }

                        override fun onResults(results: Bundle?) {
                            _rmsLevel.value = 0f
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.trim() ?: ""
                            if (text.isNotBlank()) {
                                _speechState.value = SpeechState.FinalResult(text)
                            } else {
                                _speechState.value = SpeechState.Error(
                                    "No speech detected. Please speak clearly or type your message.",
                                    isNoMatch = true
                                )
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partialText = matches?.firstOrNull()?.trim() ?: ""
                            if (partialText.isNotBlank()) {
                                _speechState.value = SpeechState.PartialResult(partialText)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                // IMPORTANT: EXTRA_LANGUAGE must be a SINGLE valid BCP-47 tag!
                val primaryLangTag = when (languagePreference.lowercase()) {
                    "gu", "gujarati" -> "gu-IN"
                    "hi", "hindi" -> "hi-IN"
                    "en", "english" -> "en-US"
                    else -> "en-IN" // Valid default for Indian English with multilingual capability
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, primaryLangTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, primaryLangTag)
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("gu-IN", "hi-IN", "en-IN", "en-US"))
                    // Set generous silence lengths so the mic doesn't close immediately on subtle pauses
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2200L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2200L)
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                _speechState.value = SpeechState.Error("Could not start microphone: ${e.message}", isNoMatch = false)
            }
        }
    }

    private fun stopListeningInternal() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _rmsLevel.value = 0f
    }

    fun stopListening() {
        mainHandler.post {
            stopListeningInternal()
            if (_speechState.value is SpeechState.Listening) {
                _speechState.value = SpeechState.Idle
            }
        }
    }

    fun resetState() {
        _speechState.value = SpeechState.Idle
        _rmsLevel.value = 0f
    }
}
