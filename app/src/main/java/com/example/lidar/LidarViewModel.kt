package com.example.lidar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lidar.DataStoreUtil
import com.example.lidar.LidarFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * Estado observado pela UI, incluindo host, porta e delay de reconexão.
 */
data class LidarUiState(
    val host: String = "127.0.0.1",
    val port: Int = 9999,
    val reconnectDelayMs: Int = 1000,
    val connection: ConnectionState = ConnectionState.Disconnected,
    val lastFrame: LidarFrame? = null,
    val delayMs: Float? = null,
    val fps: Float = 0f,
    val scale: Float = 1f,
    val darkMode: Boolean = true,
    val showOverlay: Boolean = true,
    val maxFps: Int = 30,
)

enum class ConnectionState { Connecting, Connected, Disconnected, Error }

/**
 * ViewModel que gerencia a conexão TCP, configurações persistidas e estado da UI.
 */
class LidarViewModel : ViewModel() {

    private var repository: TcpLidarRepository? = null
    private var lastFpsTimestamp = System.nanoTime()
    private var framesRendered = 0
    private var lastUiUpdate = 0L

    private val _uiState = MutableStateFlow(LidarUiState())
    val uiState: StateFlow<LidarUiState> = _uiState.asStateFlow()

    init {
        // Coleta host, porta e reconnectDelay persistidos
        viewModelScope.launch(Dispatchers.IO) {
            DataStoreUtil.settingsFlow.collect { (h, p, delayMs) ->
                _uiState.update { it.copy(host = h, port = p, reconnectDelayMs = delayMs) }
                connect()
            }
        }
    }

    /**
     * Reinicia a conexão TCP usando os valores atuais do estado.
     */
    fun connect() {
        repository?.close()
        _uiState.update { it.copy(connection = ConnectionState.Connecting) }

        val state = _uiState.value
        repository = TcpLidarRepository(
            host = state.host,
            port = state.port,
            reconnectDelayMs = state.reconnectDelayMs.toLong()
        ).also { repo ->
            repo.start(viewModelScope)
            viewModelScope.launch {
                repo.frames.collect { frame ->
                    val nowMs = System.currentTimeMillis()
                    // Throttle de UI conforme maxFps
                    val interval = 1000L / state.maxFps
                    if (nowMs - lastUiUpdate < interval) return@collect
                    lastUiUpdate = nowMs

                    // Calcula FPS
                    framesRendered++
                    val nowNano = System.nanoTime()
                    val dt = (nowNano - lastFpsTimestamp) / 1_000_000_000.0
                    val fpsCalculated = if (dt >= 1.0) {
                        val f = framesRendered / dt.toFloat()
                        framesRendered = 0
                        lastFpsTimestamp = nowNano
                        f
                    } else _uiState.value.fps

                    val delay = (System.currentTimeMillis() - frame.timestampMs).toFloat()

                    _uiState.update {
                        it.copy(
                            connection = ConnectionState.Connected,
                            lastFrame = frame,
                            delayMs = delay,
                            fps = fpsCalculated
                        )
                    }
                }
            }
        }
    }

    /** Fecha a conexão TCP e atualiza status. */
    fun disconnect() {
        repository?.close()
        repository = null
        _uiState.update { it.copy(connection = ConnectionState.Disconnected) }
    }

    /**
     * Persiste host e porta; a coleta de settingsFlow dispara nova conexão.
     */
    fun updateHostPort(newHost: String, newPort: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            DataStoreUtil.saveHostPort(newHost, newPort)
        }
    }

    /**
     * Persiste reconnect delay; a coleta de settingsFlow dispara nova conexão.
     */
    fun updateReconnectDelay(newDelayMs: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            DataStoreUtil.saveReconnectDelay(newDelayMs)
        }
    }

    /** Alterna entre modo escuro e claro. */
    fun toggleDarkMode() {
        _uiState.update { it.copy(darkMode = !it.darkMode) }
    }

    /** Alterna visibilidade do overlay. */
    fun toggleOverlay() {
        _uiState.update { it.copy(showOverlay = !it.showOverlay) }
    }

    /** Ajusta escala de zoom (0.125–1). */
    fun changeScale(delta: Float) {
        _uiState.update { it.copy(scale = (it.scale + delta).coerceIn(0.125f, 1f)) }
    }

    /** Ajusta o limite máximo de FPS da UI (1–60). */
    fun changeMaxFps(newFps: Int) {
        _uiState.update { it.copy(maxFps = newFps.coerceIn(1, 60)) }
    }

    override fun onCleared() {
        repository?.close()
        super.onCleared()
    }
}
