package com.example.lidar

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tela principal do visualizador LiDAR.
 */
@Composable
fun LidarViewerScreen(viewModel: LidarViewModel) {
    val state by viewModel.uiState.collectAsState()
    val backColor = if (state.darkMode) Color.Black else Color.White
    val gridMajor = if (state.darkMode) Color(0xFF5050FF) else Color(0xFF000080)
    val gridMinor = if (state.darkMode) Color(0xFF000060) else Color(0xFFC0C0FF)
    val gridAxis  = if (state.darkMode) Color.White else Color.Black

    var showConfig by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backColor)
            .systemBarsPadding()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            drawRadarGrid(gridMajor, gridMinor, gridAxis)
            state.lastFrame?.let { drawLidarPoints(it, state.scale, state.darkMode) }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            FloatingActionButton(onClick = { viewModel.changeScale(-0.125f) }) {
                Icon(Icons.Filled.Add, contentDescription = "Zoom In")
            }
            FloatingActionButton(onClick = { viewModel.changeScale(+0.125f) }) {
                Icon(Icons.Filled.Remove, contentDescription = "Zoom Out")
            }
        }

        IconButton(
            onClick = { showConfig = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(12.dp)
        ) {
            Icon(
                Icons.Filled.Settings,
                tint = if (state.darkMode) Color.White else Color.Black,
                contentDescription = "Configurações"
            )
        }

        if (state.showOverlay) {
            val scaleMeters = 8f * state.scale
            TextOverlay(
                fps = state.fps,
                delayMs = state.delayMs,
                connection = state.connection,
                scaleMeters = scaleMeters,
                dark = state.darkMode,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(12.dp)
            )
        }

        if (showConfig) {
            HostPortDialog(
                initialHost = state.host,
                initialPort = state.port,
                initialFps = state.maxFps,
                initialDelay = state.reconnectDelayMs,
                dark = state.darkMode,
                onDismiss = { showConfig = false }
            ) { h, p, fps, delayMs ->
                viewModel.updateHostPort(h, p)
                viewModel.changeMaxFps(fps)
                viewModel.updateReconnectDelay(delayMs)
                showConfig = false
            }
        }
    }
}

@Composable
private fun HostPortDialog(
    initialHost: String,
    initialPort: Int,
    initialFps: Int,
    initialDelay: Int,
    dark: Boolean,
    onDismiss: () -> Unit,
    onApply: (String, Int, Int, Int) -> Unit,
) {
    var host by remember { mutableStateOf(TextFieldValue(initialHost)) }
    var port by remember { mutableStateOf(TextFieldValue(initialPort.toString())) }
    var fps by remember { mutableStateOf(initialFps.toFloat()) }
    var delay by remember { mutableStateOf(initialDelay.toFloat()) }
    val textColor = if (dark) Color.White else Color.Black
    val containerColor = if (dark) Color.DarkGray else Color.White

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        title = { Text("Configurações", color = textColor) },
        text = {
            Column {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host", color = textColor) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Porta", color = textColor) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("Max FPS: ${fps.toInt()}", color = textColor)
                Slider(
                    value = fps,
                    onValueChange = { fps = it },
                    valueRange = 1f..60f,
                    steps = 59,
                    colors = SliderDefaults.colors(
                        thumbColor = textColor,
                        activeTrackColor = textColor
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("Reconnect Delay: ${delay.toInt()} ms", color = textColor)
                Slider(
                    value = delay,
                    onValueChange = { delay = it },
                    valueRange = 200f..1000f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = textColor,
                        activeTrackColor = textColor
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val portInt = port.text.toIntOrNull() ?: 0
                if (host.text.isNotBlank() && portInt in 1..65535) {
                    onApply(host.text.trim(), portInt, fps.toInt(), delay.toInt())
                }
            }) { Text("Aplicar", color = textColor) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = textColor) }
        }
    )
}
/* ============ Desenho do Grid & Pontos (inalterados) ============ */
private fun DrawScope.drawRadarGrid(majorColor: Color, minorColor: Color, axisColor: Color) {
    val radius = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val ringStep = radius / 8f
    val minorStep = ringStep / 5f

    for (i in 1..8) {
        drawCircle(majorColor, i * ringStep, center, style = Stroke(1f))
        for (j in 1 until 5) drawCircle(minorColor, i * ringStep - j * minorStep, center, style = Stroke(1f))
    }

    for (i in 0 until 24) {
        val angle = i * PI / 12
        val end = Offset(center.x + radius * cos(angle).toFloat(), center.y - radius * sin(angle).toFloat())
        drawLine(if (i % 3 == 0) majorColor else minorColor, center, end, strokeWidth = 1f)
    }

    drawLine(axisColor, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y))
    drawLine(axisColor, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius))
}

private fun DrawScope.drawLidarPoints(frame: LidarFrame, scale: Float, dark: Boolean) {
    val radius = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxDistMm = 8000f
    val pxPerMm = (radius / maxDistMm) / scale
    val color = if (dark) Color.Red else Color(0xFFFF0000)

    frame.points.forEach { p ->
        val distPx = p.distanceMm * pxPerMm
        val angRad = (90f - p.angleDeg) * (PI / 180f)
        val x = center.x + distPx * cos(angRad).toFloat()
        val y = center.y - distPx * sin(angRad).toFloat()
        drawCircle(color, 3f, Offset(x, y))
    }
}

@Composable
private fun TextOverlay(
    fps: Float,
    delayMs: Float?,
    connection: ConnectionState,
    scaleMeters: Float,
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    val textColor = if (dark) Color.Yellow else Color.Blue
    val connColor = when (connection) {
        ConnectionState.Connected -> Color.Green
        ConnectionState.Connecting -> Color.Yellow
        else -> Color.Red
    }

    Column(modifier) {
        Text("FPS: ${"%.1f".format(fps)}", color = textColor, style = MaterialTheme.typography.bodyMedium)
        delayMs?.let { Text("Delay: ${"%.0f".format(it)} ms", color = textColor, style = MaterialTheme.typography.bodyMedium) }
        Text("Escala: ${"%.1f".format(scaleMeters)} m", color = textColor, style = MaterialTheme.typography.bodyMedium)
        Text("Conn: $connection", color = connColor, style = MaterialTheme.typography.bodySmall)
    }
}
