package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AssistantType
import com.example.ui.components.AppShortcutsGrid
import com.example.ui.components.ChatBubble
import com.example.ui.components.JarvisAnimatedSphere
import com.example.ui.components.JarvisHudHeader
import com.example.ui.components.PendingActionConfirmationCard
import com.example.ui.components.VoiceSettingsSheet
import com.example.ui.theme.HudGold
import com.example.ui.theme.HudRed
import com.example.ui.theme.HudTextPrimary
import com.example.ui.theme.HudTextSecondary
import com.example.ui.theme.KingPrimary
import com.example.ui.theme.MayaPrimary
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.viewmodel.AssistantViewModel

@Composable
fun AssistantDashboardScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeAssistant by viewModel.activeAssistant.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val rmsLevel by viewModel.rmsLevel.collectAsState()
    val transcriptPreview by viewModel.transcriptPreview.collectAsState()
    val pendingAction by viewModel.pendingAction.collectAsState()
    val kingVoiceConfig by viewModel.kingVoiceConfig.collectAsState()
    val mayaVoiceConfig by viewModel.mayaVoiceConfig.collectAsState()
    val availableVoices by viewModel.availableVoices.collectAsState()
    val infoBanner by viewModel.infoBanner.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    var showVoiceSettingsSheet by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var persistentNotice by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Auto-scroll on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(infoBanner) {
        infoBanner?.let { msg ->
            persistentNotice = msg
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    // Permission launcher for Microphone
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onMicClicked()
        } else {
            showPermissionDialog = true
        }
    }

    fun handleMicClick() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.onMicClicked()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val activeAccentColor by animateColorAsState(
        targetValue = if (activeAssistant == AssistantType.KING) KingPrimary else MayaPrimary,
        label = "active_accent"
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground),
        containerColor = NavyBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            JarvisHudHeader(
                activeAssistant = activeAssistant,
                isSpeaking = isSpeaking,
                onSwitchAssistant = { viewModel.switchAssistant(it) },
                onOpenSettings = { showVoiceSettingsSheet = true },
                onClearChat = { viewModel.clearChat() },
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Section: Animated Futuristic AI Sphere
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    JarvisAnimatedSphere(
                        assistant = activeAssistant,
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        isThinking = isThinking,
                        rmsLevel = rmsLevel,
                        size = 165.dp,
                        onClick = { handleMicClick() }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Status Text Indicator
                    val statusText = when {
                        isListening -> "LISTENING TO SPEECH..."
                        isSpeaking -> "${activeAssistant.name} IS SPEAKING..."
                        isThinking -> "PROCESSING INTENT MATRIX..."
                        else -> "TAP SPHERE OR MIC TO COMMAND"
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(NavySurfaceVariant.copy(alpha = 0.7f))
                            .border(0.8.dp, activeAccentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeAccentColor,
                                letterSpacing = 1.sp
                            )
                        )

                        if (isSpeaking) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { viewModel.stopSpeaking() },
                                modifier = Modifier
                                    .size(20.dp)
                                    .testTag("stop_speaking_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Voice",
                                    tint = HudRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Live Speech Transcript preview
                    AnimatedVisibility(
                        visible = transcriptPreview.isNotBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = "\"$transcriptPreview\"",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = HudTextPrimary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 2.dp)
                                .fillMaxWidth()
                        )
                    }

                    // Recognition Language Bar (Gujarati, Hindi, English)
                    Row(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = HudTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        LanguageChip(
                            label = "Auto",
                            isSelected = currentLanguage == "auto",
                            accentColor = activeAccentColor,
                            onClick = { viewModel.setLanguage("auto") }
                        )
                        LanguageChip(
                            label = "ગુજરાતી (Gujarati)",
                            isSelected = currentLanguage == "gu",
                            accentColor = activeAccentColor,
                            onClick = { viewModel.setLanguage("gu") }
                        )
                        LanguageChip(
                            label = "हिंदी (Hindi)",
                            isSelected = currentLanguage == "hi",
                            accentColor = activeAccentColor,
                            onClick = { viewModel.setLanguage("hi") }
                        )
                        LanguageChip(
                            label = "English",
                            isSelected = currentLanguage == "en",
                            accentColor = activeAccentColor,
                            onClick = { viewModel.setLanguage("en") }
                        )
                    }
                }
            }

            // Notice / Error Banner (if any)
            persistentNotice?.let { notice ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF261313))
                        .border(1.dp, HudGold.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (notice.contains("No speech", ignoreCase = true)) Icons.Default.Warning else Icons.Default.Info,
                                contentDescription = null,
                                tint = HudGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = notice,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = HudTextPrimary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        IconButton(
                            onClick = { persistentNotice = null },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = HudTextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Quick Speech Prompt Suggestions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val suggestions = listOf(
                    "KING, WhatsApp open karo",
                    "MAYA, Rahul ko message likho meeting 5pm",
                    "Arijit Singh songs on YouTube",
                    "કેમ છો માયા?",
                    "KING, call MAYA",
                    "MAYA, call KING"
                )
                for (prompt in suggestions) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0B172E))
                            .border(0.8.dp, activeAccentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.processUserInput(prompt) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = HudTextSecondary
                            )
                        )
                    }
                }
            }

            // Security Confirmation Card for Actions (e.g. WhatsApp sending confirmation)
            pendingAction?.let { pending ->
                PendingActionConfirmationCard(
                    pendingAction = pending,
                    onConfirm = { viewModel.confirmPendingAction() },
                    onDismiss = { viewModel.dismissPendingAction() },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            // Quick App Shortcuts HUD
            AppShortcutsGrid(
                onOpenShortcut = { actionType, payload ->
                    viewModel.openShortcut(actionType, payload)
                }
            )

            // Conversation Chat History LazyColumn
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items = messages, key = { it.id }) { message ->
                        ChatBubble(
                            message = message,
                            onSpeakAgain = { text ->
                                viewModel.processUserInput(text)
                            }
                        )
                    }
                }
            }

            // Bottom Input Controls: Glowing Microphone FAB + Full Text Input Bar
            BottomInputBar(
                isListening = isListening,
                activeAccentColor = activeAccentColor,
                textInput = textInput,
                onTextChange = { textInput = it },
                onSendText = {
                    if (textInput.isNotBlank()) {
                        viewModel.processUserInput(textInput)
                        textInput = ""
                    }
                },
                onMicClick = { handleMicClick() },
                modifier = Modifier.navigationBarsPadding()
            )
        }

        // Voice Settings Bottom Sheet
        if (showVoiceSettingsSheet) {
            VoiceSettingsSheet(
                kingConfig = kingVoiceConfig,
                mayaConfig = mayaVoiceConfig,
                availableVoices = availableVoices,
                onSaveKingConfig = { p, s, v, l -> viewModel.updateKingVoiceConfig(p, s, v, l) },
                onSaveMayaConfig = { p, s, v, l -> viewModel.updateMayaVoiceConfig(p, s, v, l) },
                onTestVoice = { assistant -> viewModel.testSampleVoice(assistant) },
                onDismiss = { showVoiceSettingsSheet = false }
            )
        }

        // Permission Rationale Dialog
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                containerColor = NavySurface,
                titleContentColor = activeAccentColor,
                title = {
                    Text("Microphone Permission Required", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "KING & MAYA AI Assistant needs microphone access to listen to your voice commands in Gujarati, Hindi, and English. Audio is processed solely on your device for speech recognition.",
                        color = HudTextPrimary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionDialog = false
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor)
                    ) {
                        Text("Grant Permission", color = NavyBackground, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionDialog = false }) {
                        Text("Type Instead", color = HudTextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun LanguageChip(
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.25f) else Color(0xFF0B1424))
            .border(
                width = 1.dp,
                color = if (isSelected) accentColor else Color(0xFF1B2A4A),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = if (isSelected) HudTextPrimary else HudTextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

@Composable
private fun BottomInputBar(
    isListening: Boolean,
    activeAccentColor: Color,
    textInput: String,
    onTextChange: (String) -> Unit,
    onSendText: () -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Text input field with keyboard actions
        OutlinedTextField(
            value = textInput,
            onValueChange = onTextChange,
            placeholder = {
                Text(
                    text = "Type command or chat in Guj/Hin/Eng...",
                    fontSize = 12.sp,
                    color = HudTextSecondary
                )
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = { onSendText() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = activeAccentColor,
                unfocusedBorderColor = Color(0xFF1E2E4E),
                focusedTextColor = HudTextPrimary,
                unfocusedTextColor = HudTextPrimary,
                focusedContainerColor = Color(0xFF091426),
                unfocusedContainerColor = Color(0xFF091426)
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("command_text_input"),
            trailingIcon = {
                if (textInput.isNotBlank()) {
                    IconButton(
                        onClick = onSendText,
                        modifier = Modifier.testTag("send_command_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Command",
                            tint = activeAccentColor
                        )
                    }
                }
            },
            maxLines = 3
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Glowing Pulsing Microphone Button
        Box(
            modifier = Modifier
                .size(50.dp)
                .scale(if (isListening) pulseScale else 1f)
                .clip(CircleShape)
                .background(
                    if (isListening) {
                        Brush.radialGradient(
                            listOf(
                                HudRed,
                                activeAccentColor,
                                Color(0xFF1B2A4A)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                activeAccentColor,
                                activeAccentColor.copy(alpha = 0.75f)
                            )
                        )
                    }
                )
                .border(
                    width = 2.dp,
                    color = if (isListening) HudRed else Color.White.copy(alpha = 0.8f),
                    shape = CircleShape
                )
                .clickable(onClick = onMicClick)
                .testTag("microphone_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (isListening) "Stop Listening" else "Start Listening",
                tint = if (isListening) Color.White else NavyBackground,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
