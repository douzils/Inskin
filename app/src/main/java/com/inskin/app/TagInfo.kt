package com.inskin.app

/**
 * Modèle unique utilisé par l’UI et la couche NFC.
 * - `uid` et `serial` désignent le même identifiant (hex).
 * - `techs` est la liste brute des technologies, et `tech` une version lisible.
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
