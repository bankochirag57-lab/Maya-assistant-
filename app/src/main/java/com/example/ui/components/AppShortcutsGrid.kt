package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActionType
import com.example.ui.theme.HudTextPrimary
import com.example.ui.theme.HudTextSecondary
import com.example.ui.theme.KingPrimary
import com.example.ui.theme.MayaPrimary

@Composable
fun AppShortcutsGrid(
    onOpenShortcut: (ActionType, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MOBILE TELEMETRY & APP COMMANDS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = HudTextSecondary,
                    letterSpacing = 1.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main app launch buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ShortcutIcon(
                icon = Icons.Default.Chat,
                label = "WhatsApp",
                color = Color(0xFF25D366),
                onClick = { onOpenShortcut(ActionType.OPEN_WHATSAPP, null) },
                testTag = "shortcut_whatsapp"
            )

            ShortcutIcon(
                icon = Icons.Default.PlayCircleFilled,
                label = "YouTube",
                color = Color(0xFFFF0000),
                onClick = { onOpenShortcut(ActionType.OPEN_YOUTUBE, null) },
                testTag = "shortcut_youtube"
            )

            ShortcutIcon(
                icon = Icons.Default.Share,
                label = "Instagram",
                color = MayaPrimary,
                onClick = { onOpenShortcut(ActionType.OPEN_INSTAGRAM, null) },
                testTag = "shortcut_instagram"
            )

            ShortcutIcon(
                icon = Icons.Default.CameraAlt,
                label = "Camera",
                color = KingPrimary,
                onClick = { onOpenShortcut(ActionType.OPEN_CAMERA, null) },
                testTag = "shortcut_camera"
            )

            ShortcutIcon(
                icon = Icons.Default.Phone,
                label = "Phone",
                color = Color(0xFF00E5FF),
                onClick = { onOpenShortcut(ActionType.OPEN_PHONE, null) },
                testTag = "shortcut_phone"
            )

            ShortcutIcon(
                icon = Icons.Default.Settings,
                label = "Settings",
                color = Color(0xFFB0BEC5),
                onClick = { onOpenShortcut(ActionType.OPEN_SETTINGS, null) },
                testTag = "shortcut_settings"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick YouTube Music Chips
        val musicChips = listOf(
            "Arijit Singh Hits" to "Arijit Singh Best Songs",
            "Gujarati Garba" to "Kinjal Dave Geeta Rabari Gujarati Songs",
            "Jarvis Ambient Synth" to "Iron Man Jarvis Cyberpunk Synthwave",
            "Bollywood Melodies" to "Top Bollywood Songs"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for ((label, query) in musicChips) {
                MusicChip(
                    label = label,
                    onClick = { onOpenShortcut(ActionType.SEARCH_YOUTUBE, query) }
                )
            }
        }
    }
}

@Composable
private fun ShortcutIcon(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF091426))
                .border(1.dp, color.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = HudTextPrimary,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun MusicChip(
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0C1930))
            .border(1.dp, KingPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = KingPrimary,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = HudTextPrimary
            )
        )
    }
}
