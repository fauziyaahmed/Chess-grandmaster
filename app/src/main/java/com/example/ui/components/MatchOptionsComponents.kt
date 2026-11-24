package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chess.engine.TimeControl
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.roundToInt

@Composable
fun TimeControlPickerSection(
    selectedPreset: TimeControl,
    useCustomTime: Boolean,
    customMinutes: Int,
    customSeconds: Int,
    customIncrementSec: Int,
    onSelectPreset: (TimeControl) -> Unit,
    onSelectCustomMode: () -> Unit,
    onCustomChanged: (minutes: Int, seconds: Int, incrementSec: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val customTc = remember(customMinutes, customSeconds, customIncrementSec) {
        TimeControl.custom(customMinutes, customSeconds, customIncrementSec)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Time Control", style = MaterialTheme.typography.titleMedium)
            }
            val activeTc = if (useCustomTime) customTc else selectedPreset
            Surface(
                color = EmeraldPrimary.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${activeTc.label} · ${activeTc.category.displayName}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = EmeraldPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            item {
                FilterChip(
                    selected = useCustomTime,
                    onClick = onSelectCustomMode,
                    label = { Text("Custom") },
                    modifier = Modifier.testTag("custom_time_chip")
                )
            }
            items(TimeControl.PRESETS) { tc ->
                FilterChip(
                    selected = !useCustomTime && selectedPreset.label == tc.label,
                    onClick = { onSelectPreset(tc) },
                    label = { Text("${tc.label} ${tc.category.displayName}") }
                )
            }
        }

        AnimatedVisibility(visible = useCustomTime) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("custom_time_panel")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Custom Clock: ${customTc.label}",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChampionshipGold
                        )
                        Text(
                            text = customTc.category.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldPrimary
                        )
                    }

                    // Minutes slider + stepper
                    TimeUnitRow(
                        label = "Minutes",
                        value = customMinutes,
                        unitSuffix = "m",
                        range = 0..180,
                        onValueChange = { m ->
                            val safeSec = if (m == 0 && customSeconds < 15) 15 else customSeconds
                            onCustomChanged(m, safeSec, customIncrementSec)
                        },
                        testTagPrefix = "custom_min"
                    )

                    // Seconds slider + stepper (useful for ultra-bullet or exact timings)
                    TimeUnitRow(
                        label = "Extra Seconds",
                        value = customSeconds,
                        unitSuffix = "s",
                        range = 0..59,
                        step = 5,
                        onValueChange = { s ->
                            val safeSec = if (customMinutes == 0 && s < 15) 15 else s
                            onCustomChanged(customMinutes, safeSec, customIncrementSec)
                        },
                        testTagPrefix = "custom_sec"
                    )

                    // Increment slider + stepper
                    TimeUnitRow(
                        label = "Increment / Move",
                        value = customIncrementSec,
                        unitSuffix = "s",
                        range = 0..60,
                        onValueChange = { inc ->
                            onCustomChanged(customMinutes, customSeconds, inc)
                        },
                        testTagPrefix = "custom_inc"
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeUnitRow(
    label: String,
    value: Int,
    unitSuffix: String,
    range: IntRange,
    step: Int = 1,
    onValueChange: (Int) -> Unit,
    testTagPrefix: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: $value$unitSuffix",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onValueChange((value - step).coerceIn(range)) },
                    modifier = Modifier.size(36.dp).testTag("${testTagPrefix}_minus")
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease $label", modifier = Modifier.size(18.dp))
                }
                Text(
                    text = "$value$unitSuffix",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                IconButton(
                    onClick = { onValueChange((value + step).coerceIn(range)) },
                    modifier = Modifier.size(36.dp).testTag("${testTagPrefix}_plus")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase $label", modifier = Modifier.size(18.dp))
                }
            }
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { raw ->
                val snapped = if (step > 1) ((raw / step).roundToInt() * step) else raw.roundToInt()
                onValueChange(snapped.coerceIn(range))
            },
            valueRange = range.first.toFloat()..range.last.toFloat()
        )
    }
}

@Composable
fun CoachDavidOptionToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = "Live move grading, threat alerts, plans & best-move arrows"
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) ElectricBlue.copy(alpha = 0.16f)
            else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (enabled) ElectricBlue.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Psychology,
                        contentDescription = null,
                        tint = if (enabled) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Train with Coach David",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Surface(
                            color = ChampionshipGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "COACH",
                                style = MaterialTheme.typography.labelSmall,
                                color = ChampionshipGold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("coach_david_toggle")
            )
        }
    }
}

@Composable
fun AccountChoiceDialog(
    currentUsername: String,
    googleNotice: String?,
    onGoogleSignIn: () -> Unit,
    onContinueAsGuest: (String) -> Unit,
    onDismissNotice: () -> Unit
) {
    var guestName by remember(currentUsername) { mutableStateOf(currentUsername) }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = EmeraldPrimary)
                }
                Column {
                    Text("Welcome to Grandmaster", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Choose how you want to sign in",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "All games, ratings, puzzles, and Coach David sessions are saved automatically in your Match History.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = onGoogleSignIn,
                    modifier = Modifier.fillMaxWidth().testTag("google_login_btn")
                ) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign in with Google")
                }

                if (googleNotice != null) {
                    Surface(
                        color = ChampionshipGold.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("google_login_notice")
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = googleNotice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            FilledTonalButton(
                                onClick = onDismissNotice,
                                modifier = Modifier.align(Alignment.End)
                            ) { Text("OK") }
                        }
                    }
                }

                OutlinedTextField(
                    value = guestName,
                    onValueChange = { if (it.length <= 28) guestName = it },
                    label = { Text("Guest Player Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("guest_name_input")
                )

                Button(
                    onClick = { onContinueAsGuest(guestName) },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("continue_as_guest_btn")
                ) {
                    Icon(Icons.Filled.PersonOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Continue as Guest")
                }
            }
        },
        confirmButton = {}
    )
}
