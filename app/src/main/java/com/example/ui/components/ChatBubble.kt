package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActionType
import com.example.data.model.ChatMessage
import com.example.data.model.Sender
import com.example.ui.theme.GlassCard
import com.example.ui.theme.HudTextPrimary
import com.example.ui.theme.HudTextSecondary
import com.example.ui.theme.KingPrimary
import com.example.ui.theme.KingSecondary
import com.example.ui.theme.MayaPrimary
import com.example.ui.theme.NavySurfaceVariant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatBubble(
    message: ChatMessage,
    onSpeakAgain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == Sender.USER
    val isKing = message.sender == Sender.KING
    val isMaya = message.sender == Sender.MAYA

    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    val accentColor = when {
        isKing -> KingPrimary
        isMaya -> MayaPrimary
        else -> KingSecondary
    }

    val bubbleBackground = when {
        isUser -> Brush.linearGradient(
            listOf(
                Color(0xFF132F5C).copy(alpha = 0.85f),
                Color(0xFF0F2042).copy(alpha = 0.85f)
            )
        )
        isKing -> Brush.linearGradient(
            listOf(
                GlassCard.copy(alpha = 0.9f),
                NavySurfaceVariant.copy(alpha = 0.95f)
            )
        )
        else -> Brush.linearGradient(
            listOf(
                Color(0xFF281036).copy(alpha = 0.9f),
                Color(0xFF140A22).copy(alpha = 0.95f)
            )
        )
    }

    val timeString = rememberFormattedTime(message.timestamp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 12.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            // Assistant Avatar Badge
            AssistantAvatarBadge(sender = message.sender, accentColor = accentColor)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            // Sender tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 3.dp)
            ) {
                Text(
                    text = when (message.sender) {
                        Sender.USER -> "YOU"
                        Sender.KING -> "KING [CORE]"
                        Sender.MAYA -> "MAYA [CORE]"
                        Sender.SYSTEM -> "SYSTEM"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 9.sp,
                        color = HudTextSecondary.copy(alpha = 0.6f)
                    )
                )
            }

            // Message Body Box
            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(bubbleBackground)
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = if (isUser) 0.3f else 0.45f),
                        shape = bubbleShape
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("chat_bubble_${message.id}")
            ) {
                Column {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = HudTextPrimary,
                            lineHeight = 20.sp,
                            fontSize = 14.sp
                        )
                    )

                    // Optional Action Badge
                    if (message.actionType != ActionType.NONE) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentColor.copy(alpha = 0.15f))
                                .border(0.5.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ACTION: ${message.actionType.name}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = accentColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    // Replay voice button for assistant messages
                    if (!isUser) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = { onSpeakAgain(message.text) },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("speak_message_${message.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Replay Speech",
                                    tint = accentColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF132F5C))
                    .border(1.dp, KingSecondary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "U",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun AssistantAvatarBadge(sender: Sender, accentColor: Color) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0xFF070E1E))
            .border(1.dp, accentColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (sender == Sender.KING) "👑" else "🌸",
            fontSize = 16.sp
        )
    }
}

@Composable
private fun rememberFormattedTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
