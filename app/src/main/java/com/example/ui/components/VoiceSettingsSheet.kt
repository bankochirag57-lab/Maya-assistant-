package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantType
import com.example.data.model.AvailableVoice
import com.example.data.model.VoiceConfig
import com.example.ui.theme.HudTextPrimary
import com.example.ui.theme.HudTextSecondary
import com.example.ui.theme.KingPrimary
import com.example.ui.theme.MayaPrimary
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsSheet(
    kingConfig: VoiceConfig,
    mayaConfig: VoiceConfig,
    availableVoices: List<AvailableVoice>,
    onSaveKingConfig: (pitch: Float, speed: Float, voiceName: String?, lang: String) -> Unit,
    onSaveMayaConfig: (pitch: Float, speed: Float, voiceName: String?, lang: String) -> Unit,
    onTestVoice: (AssistantType) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableStateOf(AssistantType.KING) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NavySurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = KingPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VOICE ENGINE MATRIX",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = HudTextPrimary,
                            letterSpacing = 1.sp
                        )
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_settings_sheet")) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = HudTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Selector Tabs for KING vs MAYA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(NavyBackground)
                    .padding(4.dp)
            ) {
                TabButton(
                    title = "KING (Male Voice)",
                    isSelected = selectedTab == AssistantType.KING,
                    accentColor = KingPrimary,
                    onClick = { selectedTab = AssistantType.KING },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                TabButton(
                    title = "MAYA (Female Voice)",
                    isSelected = selectedTab == AssistantType.MAYA,
                    accentColor = MayaPrimary,
                    onClick = { selectedTab = AssistantType.MAYA },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == AssistantType.KING) {
                AssistantVoiceConfigSection(
                    assistant = AssistantType.KING,
                    config = kingConfig,
                    availableVoices = availableVoices.filter { it.isMale || !it.isFemale },
                    accentColor = KingPrimary,
                    onSave = { p, s, v, l -> onSaveKingConfig(p, s, v, l) },
                    onTestVoice = { onTestVoice(AssistantType.KING) }
                )
            } else {
                AssistantVoiceConfigSection(
                    assistant = AssistantType.MAYA,
                    config = mayaConfig,
                    availableVoices = availableVoices.filter { it.isFemale || !it.isMale },
                    accentColor = MayaPrimary,
                    onSave = { p, s, v, l -> onSaveMayaConfig(p, s, v, l) },
                    onTestVoice = { onTestVoice(AssistantType.MAYA) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.25f) else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) accentColor else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) HudTextPrimary else HudTextSecondary
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssistantVoiceConfigSection(
    assistant: AssistantType,
    config: VoiceConfig,
    availableVoices: List<AvailableVoice>,
    accentColor: Color,
    onSave: (pitch: Float, speed: Float, voiceName: String?, lang: String) -> Unit,
    onTestVoice: () -> Unit
) {
    var pitch by remember(config) { mutableFloatStateOf(config.pitch) }
    var speed by remember(config) { mutableFloatStateOf(config.speed) }
    var selectedLanguage by remember(config) { mutableStateOf(config.language) }
    var selectedVoiceName by remember(config) { mutableStateOf(config.voiceName) }
    var isVoiceDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NavySurfaceVariant.copy(alpha = 0.6f))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "${assistant.name} SETTINGS (${if (assistant == AssistantType.KING) "Deep & Confident" else "Sweet & Soft"})",
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Pitch Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Voice Pitch", style = MaterialTheme.typography.bodyMedium.copy(color = HudTextPrimary))
            Text(
                text = "${String.format("%.2f", pitch)}x",
                style = MaterialTheme.typography.bodyMedium.copy(color = accentColor, fontWeight = FontWeight.Bold)
            )
        }
        Slider(
            value = pitch,
            onValueChange = {
                pitch = it
                onSave(pitch, speed, selectedVoiceName, selectedLanguage)
            },
            valueRange = 0.5f..1.5f,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color(0xFF1E2E4E)
            ),
            modifier = Modifier.testTag("pitch_slider_${assistant.name.lowercase()}")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Speed Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Speech Rate (Speed)", style = MaterialTheme.typography.bodyMedium.copy(color = HudTextPrimary))
            Text(
                text = "${String.format("%.2f", speed)}x",
                style = MaterialTheme.typography.bodyMedium.copy(color = accentColor, fontWeight = FontWeight.Bold)
            )
        }
        Slider(
            value = speed,
            onValueChange = {
                speed = it
                onSave(pitch, speed, selectedVoiceName, selectedLanguage)
            },
            valueRange = 0.5f..1.5f,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color(0xFF1E2E4E)
            ),
            modifier = Modifier.testTag("speed_slider_${assistant.name.lowercase()}")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Language Preference Chips
        Text(
            text = "Preferred Language Script",
            style = MaterialTheme.typography.bodyMedium.copy(color = HudTextPrimary)
        )
        Spacer(modifier = Modifier.height(6.dp))

        val languages = listOf("auto" to "Auto-Detect", "gu" to "ગુજરાતી (Gujarati)", "hi" to "हिंदी (Hindi)", "en" to "English")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for ((key, label) in languages) {
                val isSel = selectedLanguage.equals(key, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) accentColor.copy(alpha = 0.2f) else NavyBackground)
                        .border(1.dp, if (isSel) accentColor else Color(0xFF1B2A4A), RoundedCornerShape(8.dp))
                        .clickable {
                            selectedLanguage = key
                            onSave(pitch, speed, selectedVoiceName, selectedLanguage)
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            color = if (isSel) HudTextPrimary else HudTextSecondary,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Installed System TTS Voice Selection
        if (availableVoices.isNotEmpty()) {
            Text(
                text = "Hardware TTS Voice",
                style = MaterialTheme.typography.bodyMedium.copy(color = HudTextPrimary)
            )
            Spacer(modifier = Modifier.height(6.dp))

            ExposedDropdownMenuBox(
                expanded = isVoiceDropdownExpanded,
                onExpandedChange = { isVoiceDropdownExpanded = !isVoiceDropdownExpanded }
            ) {
                val currentVoiceLabel = availableVoices.find { it.id == selectedVoiceName }?.let {
                    "${it.name} (${it.language})"
                } ?: "System Default"

                OutlinedTextField(
                    value = currentVoiceLabel,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isVoiceDropdownExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color(0xFF1E2E4E),
                        focusedTextColor = HudTextPrimary,
                        unfocusedTextColor = HudTextPrimary
                    ),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = isVoiceDropdownExpanded,
                    onDismissRequest = { isVoiceDropdownExpanded = false },
                    modifier = Modifier.background(NavySurface)
                ) {
                    DropdownMenuItem(
                        text = { Text("System Default", color = HudTextPrimary) },
                        onClick = {
                            selectedVoiceName = null
                            isVoiceDropdownExpanded = false
                            onSave(pitch, speed, null, selectedLanguage)
                        }
                    )
                    availableVoices.take(12).forEach { voice ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${voice.name} • ${voice.language}",
                                    color = HudTextPrimary,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = {
                                selectedVoiceName = voice.id
                                isVoiceDropdownExpanded = false
                                onSave(pitch, speed, voice.id, selectedLanguage)
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Test Voice Button
        Button(
            onClick = onTestVoice,
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = NavyBackground
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("test_voice_${assistant.name.lowercase()}")
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Preview ${assistant.name}'s Natural Voice",
                fontWeight = FontWeight.Bold
            )
        }
    }
}
