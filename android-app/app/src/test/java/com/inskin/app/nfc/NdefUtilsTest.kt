package com.inskin.app.nfc

import android.nfc.NdefMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class NdefUtilsTest {
    @Test
    fun `build and parse text`() {
        val text = "Hello"
        val record = NdefUtils.textRecord(text)
        val parsed = NdefUtils.parse(record)
        assertEquals("TEXT", parsed.type)
        assertEquals(text, parsed.payload)
    }

    @Test
    fun `build and parse uri types`() {
        val uriRecord = NdefUtils.uriRecord("https://example.com")
        val telRecord = NdefUtils.telRecord("12345")
        val smsRecord = NdefUtils.smsRecord("12345", "hi")
        val mailRecord = NdefUtils.mailRecord("a@b.com", subject = "s", body = "b")
        val geoRecord = NdefUtils.geoRecord(1.0, 2.0)
        val vcard = "BEGIN:VCARD\nVERSION:3.0\nEND:VCARD"
        val vcardRecord = NdefUtils.vcardRecord(vcard)

        assertEquals("URI", NdefUtils.parse(uriRecord).type)
        assertEquals("TEL", NdefUtils.parse(telRecord).type)
        assertEquals("SMS", NdefUtils.parse(smsRecord).type)
        assertEquals("MAILTO", NdefUtils.parse(mailRecord).type)
        assertEquals("GEO", NdefUtils.parse(geoRecord).type)
        assertEquals("VCARD", NdefUtils.parse(vcardRecord).type)
    }

    @Test
    fun `estimate size matches actual`() {
        val records = listOf(
            NdefUtils.textRecord("A"),
            NdefUtils.uriRecord("https://example.com")
        )
        val estimate = NdefUtils.estimateSize(records)
        val actual = NdefMessage(records.toTypedArray()).toByteArray().size
        assertEquals(actual, estimate)
    }
}
