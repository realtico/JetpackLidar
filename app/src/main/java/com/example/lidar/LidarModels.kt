package com.example.lidar

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Representa um ponto polar recebido do LiDAR.
 * @param angleDeg Ângulo em graus (0‑359.99…)
 * @param distanceMm Distância em milímetros
 */
data class PolarPoint(
    val angleDeg: Float,
    val distanceMm: Int,
)

/**
 * Um frame completo do LiDAR, contendo timestamp e lista de pontos.
 * O timestamp vem em milissegundos, conforme protocolo existente.
 */
data class LidarFrame(
    val timestampMs: Long,
    val points: List<PolarPoint>,
)

/**
 * Utilitário para transformar o payload binário (header + pontos) em LidarFrame.
 * Protocolo: <u32 numPoints> <u64 timestampMs> seguido de numPoints repetições de
 * <f32 angleDeg> <u16 distanceMm> ‑ todos little‑endian.
 */
object LidarFrameParser {
    private const val HEADER_SIZE = 12          // 4 + 8 bytes
    private const val POINT_SIZE = 6            // 4 + 2 bytes

    fun parseFrame(packet: ByteArray): LidarFrame {
        require(packet.size >= HEADER_SIZE) { "Pacote menor que o cabeçalho." }

        val buf = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN)
        val numPoints = buf.int
        val timestampMs = buf.long

        val expectedSize = HEADER_SIZE + numPoints * POINT_SIZE
        require(packet.size == expectedSize) {
            "Tamanho inesperado: esperado=$expectedSize, recebido=${packet.size}"
        }

        val points = buildList(numPoints) {
            repeat(numPoints) {
                val angle = buf.float      // f32
                val dist = buf.short.toInt() and 0xFFFF  // u16
                add(PolarPoint(angle, dist))
            }
        }
        return LidarFrame(timestampMs, points)
    }
}