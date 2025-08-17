package com.inskin.app

/**
 * Modèle unique utilisé par l’UI et la couche NFC.
 * - `uid` et `serial` désignent le même identifiant (hex).
 * - `techs` est la liste brute des technologies, et `tech` une version lisible.
 */
data class TagInfo(
    val uid: String,                     // ex: "04A224B12C34"
    val type: String,                    // ex: "MIFARE Ultralight", "NDEF", …
    val techs: List<String> = emptyList(),   // ex: ["android.nfc.tech.Ndef","android.nfc.tech.NfcA"]
    val atqa: String? = null,            // ex: "0x4400"
    val sak: String? = null,             // ex: "0x00"
    val format: String = "Inconnu",      // ex: "NDEF" / "Inconnu"
    val size: Int = 0,                   // taille totale (octets)
    val used: Int = 0,                   // octets utilisés
    val isWritable: Boolean = false,
    val isReadOnly: Boolean = false
) {
    /** Alias attendu par certaines vues existantes */
    val serial: String get() = uid

    /** Version condensée de la liste des techs pour affichage */
    val tech: String get() = if (techs.isEmpty()) "—" else techs.joinToString(", ") { it.substringAfterLast('.') }
}
