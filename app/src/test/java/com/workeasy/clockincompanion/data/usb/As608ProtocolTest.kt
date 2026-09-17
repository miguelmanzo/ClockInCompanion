package com.workeasy.clockincompanion.data.usb

import com.workeasy.clockincompanion.domain.model.ScanEvent
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class As608ProtocolTest {

    @Test
    fun `auto identify command matches reference packet`() {
        val cmd = As608Protocol.buildAutoIdentifyCommand()
        val expected = byteArrayOf(
            0xEF.toByte(), 0x01,
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            0x01,
            0x00, 0x06,
            0x32,
            0x03, 0x00, 0x00,
            0x00, 0x3C,
        )
        assertArrayEquals(expected, cmd)
    }

    @Test
    fun `parse match response returns page id`() {
        val response = byteArrayOf(
            0xEF.toByte(), 0x01,
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            0x07,
            0x00, 0x07,
            0x00, // confirm OK
            0x00, 0x64, // score
            0x00, 0x01, // pageId 1
            0x00, 0x00, // checksum placeholder (parser does not verify)
        )
        assertEquals(ScanEvent.Matched(1), As608Protocol.parseAutoIdentifyResponse(response))
    }

    @Test
    fun `parse search match returns page id`() {
        val response = byteArrayOf(
            0xEF.toByte(), 0x01,
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            0x07,
            0x00, 0x07,
            0x00, // confirm OK
            0x00, 0x01, // pageId 1
            0x00, 0x64, // score
            0x00, 0x00,
        )
        assertEquals(ScanEvent.Matched(1), As608Protocol.parseSearchResponse(response))
    }

    @Test
    fun `parse search no match`() {
        val response = byteArrayOf(
            0xEF.toByte(), 0x01,
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            0x07,
            0x00, 0x03,
            0x09,
            0x00, 0x13,
        )
        assertEquals(ScanEvent.NoMatch, As608Protocol.parseSearchResponse(response))
    }

    @Test
    fun `extractAckPacket reads length delimited frame`() {
        val packet = As608Protocol.buildGetImageCommand()
        // Echo as if the sensor replied with an ACK-shaped buffer by forcing PID.
        // Use a synthetic ACK: header + addr + 07 + len 0003 + confirm 00 + checksum
        val ack = byteArrayOf(
            0xEF.toByte(), 0x01,
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            0x07,
            0x00, 0x03,
            0x00,
            0x00, 0x0A,
        )
        val noise = byteArrayOf(0x00, 0x11) + ack + byteArrayOf(0x22)
        val extracted = As608Protocol.extractAckPacket(noise)
        assertTrue(extracted != null)
        assertEquals(ack.size + 2, extracted!!.second)
        assertEquals(0x00, As608Protocol.confirmationCode(extracted.first))
        // silence unused
        assertTrue(packet.isNotEmpty())
    }
}
