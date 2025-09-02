// app/src/main/java/com/inskin/app/tags/nfc/KeysRepository.kt
package com.inskin.app.tags.nfc

import android.content.Context
import android.net.Uri

internal class KeysRepository(private val ctx: Context) {

    /** Charge toutes les .keys embarquées dans assets/key-files/ */
    fun loadAllFromAssets(): List<ByteArray> {
        val assetDir = "key-files"
        val acc = mutableListOf<ByteArray>()
        runCatching {
            val list = ctx.assets.list(assetDir)?.filter { it.endsWith(".keys") }.orEmpty()
            list.forEach { file ->
                acc += parseMctKeys(ctx.assets.open("$assetDir/$file").bufferedReader().readText())
            }
        }
        return acc.distinctBy { it.toHex12() }
    }

    /** Permet d’ajouter un fichier .keys choisi par l’utilisateur (SAF/partage). */
    fun loadFromUri(uri: Uri): List<ByteArray> =
        runCatching {
            ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()?.let(::parseMctKeys)
        }.getOrNull().orEmpty().distinctBy { it.toHex12() }

    /** Parser compatible MCT : lignes hex (12), commentaires # ; //, espaces autorisés. */
    private fun parseMctKeys(text: String): List<ByteArray> =
        text.lineSequence()
            .map { it.substringBefore('#').substringBefore(';').substringBefore("//").trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val hex = line.replace(" ", "").uppercase()
                if (hex.length == 12 && hex.all { it in "0123456789ABCDEF" }) hex6(hex) else null
            }
            .toList()

    private fun ByteArray.toHex12(): String = joinToString("") { "%02X".format(it) }
    private fun hex6(hex: String): ByteArray =
        ByteArray(6) { i -> ((Character.digit(hex[i*2],16) shl 4) or Character.digit(hex[i*2+1],16)).toByte() }
}
