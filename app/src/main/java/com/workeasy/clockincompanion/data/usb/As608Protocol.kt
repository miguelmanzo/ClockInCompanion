package com.workeasy.clockincompanion.data.usb

import com.workeasy.clockincompanion.domain.model.ScanEvent

/**
 * AS608 / R307 / FPM11A UART packet helpers.
 *
 * Packet: header(2) + address(4) + pid(1) + length(2) + payload + checksum(2)
 * length = byte count from first payload byte through checksum inclusive.
 */
object As608Protocol {

    private const val HEADER_0 = 0xEF.toByte()
    private const val HEADER_1 = 0x01.toByte()
    private const val PID_COMMAND: Byte = 0x01
    private const val PID_ACK: Byte = 0x07

    const val CMD_GET_IMAGE: Byte = 0x01
    const val CMD_IMAGE_2_TZ: Byte = 0x02
    const val CMD_SEARCH: Byte = 0x04
    const val CMD_REG_MODEL: Byte = 0x05
    const val CMD_STORE: Byte = 0x06
    const val CMD_EMPTY: Byte = 0x0D
    const val CMD_AUTO_IDENTIFY: Byte = 0x32

    const val CONFIRM_OK = 0x00
    const val CONFIRM_NO_FINGER = 0x02
    const val CONFIRM_NO_MATCH = 0x09

    fun buildCommand(instruction: Byte, vararg params: Byte): ByteArray {
        val payload = byteArrayOf(instruction) + params
        val length = payload.size + 2 // payload + checksum
        val lengthHi = ((length shr 8) and 0xFF).toByte()
        val lengthLo = (length and 0xFF).toByte()
        val sumBase = byteArrayOf(PID_COMMAND, lengthHi, lengthLo) + payload
        val checksum = sumBase.fold(0) { acc, b -> acc + (b.toInt() and 0xFF) } and 0xFFFF
        val checksumHi = ((checksum shr 8) and 0xFF).toByte()
        val checksumLo = (checksum and 0xFF).toByte()
        return byteArrayOf(
            HEADER_0, HEADER_1,
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            PID_COMMAND, lengthHi, lengthLo,
        ) + payload + byteArrayOf(checksumHi, checksumLo)
    }

    fun buildAutoIdentifyCommand(securityLevel: Byte = 0x03): ByteArray {
        // Reference: EF 01 FF FF FF FF 01 00 06 32 03 00 00 00 3C
        return buildCommand(CMD_AUTO_IDENTIFY, securityLevel, 0x00, 0x00)
    }

    fun buildGetImageCommand(): ByteArray = buildCommand(CMD_GET_IMAGE)

    fun buildImage2TzCommand(bufferId: Int): ByteArray =
        buildCommand(CMD_IMAGE_2_TZ, (bufferId and 0xFF).toByte())

    /** Search templates using CharBuffer [bufferId] over [startPage, startPage + pageNum). */
    fun buildSearchCommand(
        bufferId: Int = 1,
        startPage: Int = 0,
        pageNum: Int = 200,
    ): ByteArray = buildCommand(
        CMD_SEARCH,
        (bufferId and 0xFF).toByte(),
        ((startPage shr 8) and 0xFF).toByte(),
        (startPage and 0xFF).toByte(),
        ((pageNum shr 8) and 0xFF).toByte(),
        (pageNum and 0xFF).toByte(),
    )

    fun buildRegModelCommand(): ByteArray = buildCommand(CMD_REG_MODEL)

    fun buildStoreCommand(pageId: Int, bufferId: Int = 1): ByteArray =
        buildCommand(
            CMD_STORE,
            (bufferId and 0xFF).toByte(),
            ((pageId shr 8) and 0xFF).toByte(),
            (pageId and 0xFF).toByte(),
        )

    fun buildEmptyCommand(): ByteArray = buildCommand(CMD_EMPTY)

    fun confirmationCode(packet: ByteArray): Int? {
        if (packet.size < 10) return null
        if (packet[0] != HEADER_0 || packet[1] != HEADER_1) return null
        if (packet[6] != PID_ACK) return null
        return packet[9].toInt() and 0xFF
    }

    fun parseAutoIdentifyResponse(bytes: ByteArray): ScanEvent {
        if (bytes.size < 12) {
            return ScanEvent.Error("Response too short: ${bytes.size} bytes")
        }
        if (bytes[0] != HEADER_0 || bytes[1] != HEADER_1) {
            return ScanEvent.Error("Invalid header")
        }
        return when (val confirm = confirmationCode(bytes) ?: return ScanEvent.Error("Bad packet")) {
            CONFIRM_OK -> {
                if (bytes.size < 14) {
                    ScanEvent.Error("Match response truncated")
                } else {
                    val pageId = ((bytes[12].toInt() and 0xFF) shl 8) or (bytes[13].toInt() and 0xFF)
                    ScanEvent.Matched(pageId)
                }
            }
            CONFIRM_NO_MATCH -> ScanEvent.NoMatch
            CONFIRM_NO_FINGER -> ScanEvent.Error("No finger on sensor")
            else -> ScanEvent.Error("Sensor error code: 0x${confirm.toString(16)}")
        }
    }

    /** Search ACK: confirm + pageId(2) + matchScore(2). */
    fun parseSearchResponse(bytes: ByteArray): ScanEvent {
        return when (val confirm = confirmationCode(bytes) ?: return ScanEvent.Error("Bad packet")) {
            CONFIRM_OK -> {
                if (bytes.size < 12) {
                    ScanEvent.Error("Search response truncated")
                } else {
                    val pageId = ((bytes[10].toInt() and 0xFF) shl 8) or (bytes[11].toInt() and 0xFF)
                    ScanEvent.Matched(pageId)
                }
            }
            CONFIRM_NO_MATCH -> ScanEvent.NoMatch
            CONFIRM_NO_FINGER -> ScanEvent.Error("No finger on sensor")
            else -> ScanEvent.Error("Sensor error code: 0x${confirm.toString(16)}")
        }
    }

    /**
     * Extracts the first complete ACK packet from [buffer], or null if incomplete.
     * Returns pair of (packet, bytesConsumed).
     */
    fun extractAckPacket(buffer: ByteArray): Pair<ByteArray, Int>? {
        var start = 0
        while (start + 9 < buffer.size) {
            if (buffer[start] != HEADER_0 || buffer[start + 1] != HEADER_1) {
                start++
                continue
            }
            if (start + 8 >= buffer.size) return null
            val length = ((buffer[start + 7].toInt() and 0xFF) shl 8) or
                (buffer[start + 8].toInt() and 0xFF)
            val total = 9 + length // header(2)+addr(4)+pid(1)+len(2)+payload(length)
            if (start + total > buffer.size) return null
            val packet = buffer.copyOfRange(start, start + total)
            return packet to (start + total)
        }
        return null
    }
}
