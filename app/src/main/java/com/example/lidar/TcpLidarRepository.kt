package com.example.lidar

import com.example.lidar.LidarFrame
import com.example.lidar.LidarFrameParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.DataInputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Cliente TCP para receber frames do LiDAR.
 * Agora respeita reconnectDelayMs entre cada tentativa,
 * seja em caso de falha ou após leitura bem-sucedida.
 */
class TcpLidarRepository(
    private val host: String = "127.0.0.1",
    private val port: Int = 9999,
    reconnectDelayMs: Long = 1000L,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : Closeable {

    private val reconnectDelay: Duration = reconnectDelayMs.milliseconds

    private val _frames = MutableSharedFlow<LidarFrame>(replay = 1, extraBufferCapacity = 32)
    /** Fluxo público de frames; colete na UI. */
    val frames: SharedFlow<LidarFrame> = _frames.asSharedFlow()

    private var readerJob: kotlinx.coroutines.Job? = null

    /** Inicia ou reinicia o loop de leitura. */
    fun start(scope: CoroutineScope) {
        stop()
        readerJob = scope.launch(ioDispatcher) { readerLoop(this) }
    }

    /** Cancela o loop de leitura. */
    fun stop() {
        readerJob?.cancel()
        readerJob = null
    }

    private suspend fun readerLoop(jobScope: CoroutineScope) {
        while (jobScope.isActive) {
            try {
                Socket().use { socket ->
                    socket.soTimeout = 2500
                    socket.connect(InetSocketAddress(host, port), 1500)
                    DataInputStream(socket.getInputStream()).use { input ->
                        // lê header
                        val header = ByteArray(12)
                        input.readFully(header)
                        val numPoints = (header[0].toInt() and 0xFF) or
                                ((header[1].toInt() and 0xFF) shl 8) or
                                ((header[2].toInt() and 0xFF) shl 16) or
                                ((header[3].toInt() and 0xFF) shl 24)
                        // lê payload
                        val expected = 12 + numPoints * 6
                        val payload = ByteArray(expected - 12)
                        input.readFully(payload)
                        // parseia e emite frame
                        val frame = LidarFrameParser.parseFrame(header + payload)
                        _frames.emit(frame)
                    }
                }
            } catch (ex: Exception) {
                // falha de conexão ou leitura; ignora e aguarda reconnectDelay
            } finally {
                if (jobScope.isActive) {
                    delay(reconnectDelay)
                }
            }
        }
    }

    override fun close() {
        stop()
    }
}

