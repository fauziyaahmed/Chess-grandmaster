package com.example.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.chess.engine.GameVariant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID

enum class BtConnectionState(val label: String) {
    IDLE("Not Connected"),
    LISTENING("Waiting for Friend..."),
    CONNECTING("Connecting..."),
    CONNECTED("Connected"),
    ERROR("Connection Failed")
}

data class BtDeviceInfo(
    val name: String,
    val address: String,
    val isPaired: Boolean
)

sealed class BtPacket {
    data class StartMatch(
        val variant: GameVariant,
        val chess960Pos: Int,
        val timeControlLabel: String,
        val hostPlaysWhite: Boolean,
        val hostName: String,
        val hostRating: Int,
        val isRated: Boolean
    ) : BtPacket()

    data class MoveSync(val uci: String, val whiteMs: Long, val blackMs: Long) : BtPacket()
    data class ResignSync(val senderName: String) : BtPacket()
    data object DrawOfferSync : BtPacket()
    data object DrawAcceptSync : BtPacket()
    data class ChatSync(val sender: String, val text: String) : BtPacket()

    fun encode(): String = when (this) {
        is StartMatch ->
            "START|${variant.name}|$chess960Pos|$timeControlLabel|$hostPlaysWhite|${hostName.replace("|", " ")}|$hostRating|$isRated"
        is MoveSync -> "MOVE|$uci|$whiteMs|$blackMs"
        is ResignSync -> "RESIGN|${senderName.replace("|", " ")}"
        DrawOfferSync -> "DRAW_OFFER"
        DrawAcceptSync -> "DRAW_ACCEPT"
        is ChatSync -> "CHAT|${sender.replace("|", " ")}|${text.replace("|", " ").replace("\n", " ")}"
    }

    companion object {
        fun decode(line: String): BtPacket? {
            val parts = line.trim().split("|")
            return when (parts.firstOrNull()) {
                "START" -> if (parts.size >= 8) {
                    StartMatch(
                        variant = GameVariant.entries.firstOrNull { it.name == parts[1] } ?: GameVariant.STANDARD,
                        chess960Pos = parts[2].toIntOrNull() ?: 518,
                        timeControlLabel = parts[3],
                        hostPlaysWhite = parts[4].toBooleanStrictOrNull() ?: true,
                        hostName = parts[5].ifBlank { "Bluetooth Friend" },
                        hostRating = parts[6].toIntOrNull() ?: 1200,
                        isRated = parts[7].toBooleanStrictOrNull() ?: true
                    )
                } else null
                "MOVE" -> if (parts.size >= 4) {
                    MoveSync(
                        uci = parts[1],
                        whiteMs = parts[2].toLongOrNull() ?: 600_000L,
                        blackMs = parts[3].toLongOrNull() ?: 600_000L
                    )
                } else null
                "RESIGN" -> ResignSync(parts.getOrNull(1) ?: "Friend")
                "DRAW_OFFER" -> DrawOfferSync
                "DRAW_ACCEPT" -> DrawAcceptSync
                "CHAT" -> if (parts.size >= 3) ChatSync(parts[1], parts.drop(2).joinToString(" ")) else null
                else -> null
            }
        }
    }
}

class BluetoothChessManager(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val adapter: BluetoothAdapter? =
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val _connectionState = MutableStateFlow(BtConnectionState.IDLE)
    val connectionState: StateFlow<BtConnectionState> = _connectionState.asStateFlow()

    private val _statusText = MutableStateFlow("Bluetooth ready to host or join a friend")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _connectedPeerName = MutableStateFlow<String?>(null)
    val connectedPeerName: StateFlow<String?> = _connectedPeerName.asStateFlow()

    private val _devices = MutableStateFlow<List<BtDeviceInfo>>(emptyList())
    val devices: StateFlow<List<BtDeviceInfo>> = _devices.asStateFlow()

    private val _incomingPackets = MutableSharedFlow<BtPacket>(extraBufferCapacity = 32)
    val incomingPackets: SharedFlow<BtPacket> = _incomingPackets.asSharedFlow()

    private var serverSocket: BluetoothServerSocket? = null
    private var activeSocket: BluetoothSocket? = null
    private var writer: BufferedWriter? = null
    private var ioJob: Job? = null
    private var discoveryReceiver: BroadcastReceiver? = null

    fun isSupported(): Boolean = adapter != null

    fun isEnabled(): Boolean = try {
        adapter?.isEnabled == true
    } catch (_: SecurityException) {
        false
    }

    fun requiredPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }

    fun hasPermissions(): Boolean = requiredPermissions().all { perm ->
        ContextCompat.checkSelfPermission(appContext, perm) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        if (!hasPermissions() || adapter == null) return
        try {
            val paired = adapter.bondedDevices.orEmpty().map { dev ->
                BtDeviceInfo(
                    name = dev.name ?: "Bluetooth Device",
                    address = dev.address,
                    isPaired = true
                )
            }.sortedBy { it.name }
            _devices.value = paired
        } catch (_: SecurityException) {
            _statusText.value = "Bluetooth permission required to list paired devices."
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (!hasPermissions() || adapter == null || !isEnabled()) return
        refreshPairedDevices()
        unregisterDiscoveryReceiver()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    if (device != null) {
                        val name = try { device.name } catch (_: SecurityException) { null } ?: "Nearby Device"
                        val addr = device.address ?: return
                        val current = _devices.value
                        if (current.none { it.address == addr }) {
                            _devices.value = current + BtDeviceInfo(name, addr, isPaired = false)
                        }
                    }
                }
            }
        }
        discoveryReceiver = receiver
        appContext.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        try {
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            adapter.startDiscovery()
            _statusText.value = "Scanning nearby Bluetooth friends..."
        } catch (_: SecurityException) {
            _statusText.value = "Bluetooth scan permission denied."
        }
    }

    @SuppressLint("MissingPermission")
    fun hostMatch(onConnected: (peerName: String) -> Unit) {
        if (!hasPermissions() || adapter == null || !isEnabled()) {
            _connectionState.value = BtConnectionState.ERROR
            _statusText.value = "Enable Bluetooth & grant permissions first."
            return
        }
        closeSocketsOnly()
        ioJob?.cancel()
        _connectionState.value = BtConnectionState.LISTENING
        _statusText.value = "Hosting Bluetooth table... Ask your friend to tap Join."

        ioJob = scope.launch {
            try {
                val srv = adapter.listenUsingRfcommWithServiceRecord("GrandmasterChess", CHESS_UUID)
                serverSocket = srv
                val sock = srv.accept()
                try { srv.close() } catch (_: Exception) {}
                serverSocket = null

                val peerName = try { sock.remoteDevice?.name } catch (_: SecurityException) { null } ?: "Bluetooth Friend"
                onSocketEstablished(sock, peerName, onConnected)
            } catch (e: Exception) {
                if (_connectionState.value != BtConnectionState.IDLE) {
                    _connectionState.value = BtConnectionState.ERROR
                    _statusText.value = "Host stopped: ${e.message ?: "Bluetooth closed"}"
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToFriend(address: String, onConnected: (peerName: String) -> Unit) {
        if (!hasPermissions() || adapter == null || !isEnabled()) {
            _connectionState.value = BtConnectionState.ERROR
            _statusText.value = "Enable Bluetooth & grant permissions first."
            return
        }
        closeSocketsOnly()
        ioJob?.cancel()
        _connectionState.value = BtConnectionState.CONNECTING
        _statusText.value = "Connecting to $address..."

        ioJob = scope.launch {
            try {
                try {
                    if (adapter.isDiscovering) adapter.cancelDiscovery()
                } catch (_: SecurityException) {}

                val device = adapter.getRemoteDevice(address)
                val sock = device.createRfcommSocketToServiceRecord(CHESS_UUID)
                sock.connect()
                val peerName = try { device.name } catch (_: SecurityException) { null } ?: address
                onSocketEstablished(sock, peerName, onConnected)
            } catch (e: Exception) {
                _connectionState.value = BtConnectionState.ERROR
                _statusText.value = "Could not connect: ${e.message ?: "Make sure friend tapped Host Match"}"
            }
        }
    }

    private suspend fun onSocketEstablished(
        socket: BluetoothSocket,
        peerName: String,
        onConnected: (String) -> Unit
    ) {
        activeSocket = socket
        writer = BufferedWriter(OutputStreamWriter(socket.outputStream, Charsets.UTF_8))
        _connectedPeerName.value = peerName
        _connectionState.value = BtConnectionState.CONNECTED
        _statusText.value = "Connected to $peerName"
        withContext(Dispatchers.Main) {
            onConnected(peerName)
        }
        readLoop(socket)
    }

    private suspend fun readLoop(socket: BluetoothSocket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))
            while (true) {
                val line = reader.readLine() ?: break
                val packet = BtPacket.decode(line)
                if (packet != null) {
                    _incomingPackets.emit(packet)
                }
            }
        } catch (_: Exception) {
        } finally {
            if (_connectionState.value == BtConnectionState.CONNECTED) {
                _connectionState.value = BtConnectionState.IDLE
                _statusText.value = "Bluetooth friend disconnected"
                _connectedPeerName.value = null
            }
        }
    }

    fun sendPacket(packet: BtPacket) {
        scope.launch {
            try {
                val w = writer ?: return@launch
                w.write(packet.encode())
                w.newLine()
                w.flush()
            } catch (_: Exception) {
                _statusText.value = "Failed to send move over Bluetooth"
            }
        }
    }

    fun disconnect() {
        ioJob?.cancel()
        closeSocketsOnly()
        unregisterDiscoveryReceiver()
        _connectionState.value = BtConnectionState.IDLE
        _connectedPeerName.value = null
        _statusText.value = "Bluetooth disconnected"
    }

    private fun closeSocketsOnly() {
        try { writer?.close() } catch (_: Exception) {}
        writer = null
        try { activeSocket?.close() } catch (_: Exception) {}
        activeSocket = null
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
    }

    private fun unregisterDiscoveryReceiver() {
        discoveryReceiver?.let { r ->
            try { appContext.unregisterReceiver(r) } catch (_: Exception) {}
        }
        discoveryReceiver = null
    }

    fun release() {
        disconnect()
        scope.cancel()
    }

    companion object {
        val CHESS_UUID: UUID = UUID.fromString("8a9e4c20-6c8b-4a31-9e12-7b4d18c39064")
    }
}
