package com.inskin.app

/**
 * Holds details about a discovered NFC tag.
 */
data class TagInfo(
    val type: String?,
    val techs: List<String>,
    val uid: String?,
    val atqa: String?,
    val sak: String?,
    val size: Int,
    val used: Int,
    val writable: Boolean,
    val readonly: Boolean,
    /** Human readable payloads of NDEF records present on the tag. */
    val records: List<String> = emptyList(),
)
