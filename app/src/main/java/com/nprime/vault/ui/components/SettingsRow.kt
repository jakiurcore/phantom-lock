package com.nprime.vault.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nprime.vault.ui.theme.Accent
import com.nprime.vault.ui.theme.Danger
import com.nprime.vault.ui.theme.Divider
import com.nprime.vault.ui.theme.TextSecondary

/** A single row inside a settings group. */
@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    titleColor: Color = Color.White,
    trailing: @Composable () -> Unit = {},
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = if (subtitle != null) 12.dp else 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            trailing()
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                color = Divider,
                thickness = 0.5.dp
            )
        }
    }
}

/** Chevron trailing icon for navigation rows. */
@Composable
fun ChevronTrailing() {
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = TextSecondary
    )
}

/** Toggle row with on/off switch. */
@Composable
fun ToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    danger: Boolean = false,
    showDivider: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsRow(
        title = title,
        subtitle = subtitle,
        showDivider = showDivider,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = if (danger) Danger else Accent,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = Color(0xFF2C2C2E),
                    disabledCheckedTrackColor = Color(0xFF3A3A3C),
                    disabledUncheckedTrackColor = Color(0xFF2C2C2E)
                )
            )
        }
    )
}

/** Section header label (uppercase, tracked). */
@Composable
fun SectionHeader(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}
