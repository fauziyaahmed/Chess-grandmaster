package com.example.ui.components

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.chess.engine.GameVariant
import com.example.chess.engine.PieceColor
import com.example.chess.engine.TimeControl
import com.example.data.bluetooth.BluetoothChessManager
import com.example.data.bluetooth.BtConnectionState
import com.example.data.bluetooth.BtDeviceInfo
import com.example.data.preferences.UserSettings
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldPrimary

@Composable
fun BluetoothLobbySheet(
    bluetoothManager: BluetoothChessManager,
    connectionState: BtConnectionState,
    statusText: String,
    connectedPeer: String?,
    devices: List<BtDeviceInfo>,
    settings: UserSettings,
    onRefreshDevices: () -> Unit,
    onStartDiscovery: () -> Unit,
    onHostMatch: (
        variant: GameVariant,
        chess960Pos: Int?,
        timeControl: TimeControl,
        hostColor: PieceColor?,
        isRated: Boolean,
        coachEnabled: Boolean
    ) -> Unit,
    onConnectDevice: (address: String, coachEnabled: Boolean) -> Unit,
    onDisconnect: () -> Unit,
    onClose: () -> Unit
) {
    var hasPerms by remember { mutableStateOf(bluetoothManager.hasPermissions()) }
    var isBtEnabled by remember { mutableStateOf(bluetoothManager.isEnabled()) }
    val isBtSupported = remember { bluetoothManager.isSupported() }

    var variant by remember { mutableStateOf(GameVariant.STANDARD) }
    var posText by remember { mutableStateOf("") }
    var useCustomTime by remember(settings.useCustomTimeDefault) { mutableStateOf(settings.useCustomTimeDefault) }
    var selectedPreset by remember(settings.selectedTimeControlLabel) {
        mutableStateOf(TimeControl.fromLabel(settings.selectedTimeControlLabel))
    }
    var customMin by remember(settings.customMinutes) { mutableIntStateOf(settings.customMinutes) }
    var customSec by remember(settings.customSeconds) { mutableIntStateOf(settings.customSeconds) }
    var customInc by remember(settings.customIncrementSec) { mutableIntStateOf(settings.customIncrementSec) }
    var hostColor by remember { mutableStateOf<PieceColor?>(PieceColor.WHITE) }
    var isRated by remember { mutableStateOf(true) }
    var coachEnabled by remember(settings.coachDavidDefault) { mutableStateOf(settings.coachDavidDefault) }

    val activeTc = remember(useCustomTime, selectedPreset, customMin, customSec, customInc) {
        if (useCustomTime) TimeControl.custom(customMin, customSec, customInc) else selectedPreset
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPerms = bluetoothManager.hasPermissions() || results.values.all { it }
        isBtEnabled = bluetoothManager.isEnabled()
        if (hasPerms && isBtEnabled) {
            onRefreshDevices()
        }
    }

    val enableBtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isBtEnabled = bluetoothManager.isEnabled()
        if (hasPerms && isBtEnabled) {
            onRefreshDevices()
        }
    }

    LaunchedEffect(hasPerms, isBtEnabled) {
        if (hasPerms && isBtEnabled) {
            onRefreshDevices()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag("bluetooth_lobby_sheet"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Bluetooth, contentDescription = null, tint = ElectricBlue)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Bluetooth Friend Arena", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = connectedPeer?.let { "Linked with $it" } ?: connectionState.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close Bluetooth Lobby")
            }
        }

        Surface(
            color = when (connectionState) {
                BtConnectionState.CONNECTED -> EmeraldPrimary.copy(alpha = 0.16f)
                BtConnectionState.ERROR -> CrimsonRed.copy(alpha = 0.16f)
                BtConnectionState.LISTENING, BtConnectionState.CONNECTING -> ElectricBlue.copy(alpha = 0.16f)
                BtConnectionState.IDLE -> MaterialTheme.colorScheme.surfaceContainer
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(statusText, style = MaterialTheme.typography.bodyMedium)
                if (connectionState == BtConnectionState.LISTENING || connectionState == BtConnectionState.CONNECTING) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        if (!isBtSupported) {
            Card(colors = CardDefaults.cardColors(containerColor = CrimsonRed.copy(alpha = 0.12f))) {
                Text(
                    text = "Bluetooth hardware is not available on this device/emulator. You can still pair devices on physical Android phones or test Pass & Play on the same screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else if (!hasPerms) {
            Button(
                onClick = { permLauncher.launch(bluetoothManager.requiredPermissions()) },
                modifier = Modifier.fillMaxWidth().testTag("bt_grant_permission_btn")
            ) {
                Icon(Icons.Filled.BluetoothSearching, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Grant Bluetooth Permissions")
            }
        } else if (!isBtEnabled) {
            Button(
                onClick = {
                    enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                },
                modifier = Modifier.fillMaxWidth().testTag("bt_enable_adapter_btn")
            ) {
                Icon(Icons.Filled.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Turn On Bluetooth")
            }
        }

        if (connectionState == BtConnectionState.CONNECTED) {
            OutlinedButton(
                onClick = onDisconnect,
                modifier = Modifier.fillMaxWidth().testTag("bt_disconnect_btn")
            ) {
                Icon(Icons.Filled.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Disconnect Bluetooth Session")
            }
        }

        // Match Settings for Host
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Host a Bluetooth Table", style = MaterialTheme.typography.titleMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameVariant.entries.forEach { v ->
                        FilterChip(
                            selected = variant == v,
                            onClick = { variant = v },
                            label = { Text(v.displayName) }
                        )
                    }
                }

                if (variant == GameVariant.FISCHER960) {
                    OutlinedTextField(
                        value = posText,
                        onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) posText = it },
                        label = { Text("Chess960 # (blank = random)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                TimeControlPickerSection(
                    selectedPreset = selectedPreset,
                    useCustomTime = useCustomTime,
                    customMinutes = customMin,
                    customSeconds = customSec,
                    customIncrementSec = customInc,
                    onSelectPreset = {
                        useCustomTime = false
                        selectedPreset = it
                    },
                    onSelectCustomMode = { useCustomTime = true },
                    onCustomChanged = { m, s, inc ->
                        customMin = m
                        customSec = s
                        customInc = inc
                        useCustomTime = true
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Host Plays", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = hostColor == PieceColor.WHITE,
                            onClick = { hostColor = PieceColor.WHITE },
                            label = { Text("White") }
                        )
                        FilterChip(
                            selected = hostColor == PieceColor.BLACK,
                            onClick = { hostColor = PieceColor.BLACK },
                            label = { Text("Black") }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rated Match (Elo Stakes)", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isRated, onCheckedChange = { isRated = it })
                }

                CoachDavidOptionToggle(
                    enabled = coachEnabled,
                    onToggle = { coachEnabled = it }
                )

                Button(
                    onClick = {
                        onHostMatch(
                            variant,
                            posText.toIntOrNull()?.coerceIn(0, 959),
                            activeTc,
                            hostColor,
                            isRated,
                            coachEnabled
                        )
                    },
                    enabled = isBtSupported && hasPerms && isBtEnabled,
                    modifier = Modifier.fillMaxWidth().testTag("bt_host_btn")
                ) {
                    Icon(Icons.Filled.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Host Bluetooth Match (${activeTc.label})")
                }
            }
        }

        // Join a Friend Device List
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Join a Friend (${devices.size})", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onRefreshDevices,
                    enabled = hasPerms && isBtEnabled
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh Paired Devices")
                }
                FilledTonalButton(
                    onClick = onStartDiscovery,
                    enabled = isBtSupported && hasPerms && isBtEnabled,
                    modifier = Modifier.testTag("bt_scan_btn")
                ) {
                    Icon(Icons.Filled.BluetoothSearching, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Scan")
                }
            }
        }

        if (devices.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Text(
                    text = "No paired or scanned Bluetooth devices yet. Pair phones in Android Bluetooth Settings or tap Scan while your friend's Bluetooth is visible.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                devices.forEachIndexed { idx, dev ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = hasPerms && isBtEnabled) {
                                onConnectDevice(dev.address, coachEnabled)
                            }
                            .testTag("bt_device_$idx")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (dev.isPaired) Icons.Filled.CheckCircle else Icons.Filled.Bluetooth,
                                    contentDescription = null,
                                    tint = if (dev.isPaired) EmeraldPrimary else ChampionshipGold
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(dev.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${dev.address} · ${if (dev.isPaired) "Paired" else "Nearby"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = { onConnectDevice(dev.address, coachEnabled) },
                                enabled = hasPerms && isBtEnabled
                            ) {
                                Text("Join")
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
