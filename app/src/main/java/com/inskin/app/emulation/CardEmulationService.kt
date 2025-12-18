package com.inskin.app.emulation

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log

/**
 * Service d'émulation de carte NFC (Host Card Emulation)
 * Permet au téléphone d'émuler une carte NFC/RFID
 */
class CardEmulationService : HostApduService() {

    companion object {
        private const val TAG = "CardEmulation"

        // AID pour l'émulation (Application ID)
        private const val DEFAULT_AID = "F0010203040506"

        // Commandes APDU standard
        private val SELECT_APDU = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte()
        )

        // Réponses APDU
        private val SUCCESS_RESPONSE = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val UNKNOWN_COMMAND = byteArrayOf(0x00.toByte(), 0x00.toByte())
    }

    private var emulatedUid: ByteArray? = null
    private var emulatedData: ByteArray? = null
    private var isEmulating = false

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Émulation désactivée: raison = $reason")
        isEmulating = false
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) {
            return UNKNOWN_COMMAND
        }

        Log.d(TAG, "Commande APDU reçue: ${commandApdu.toHexString()}")

        return when {
            // Commande SELECT
            commandApdu.size >= 4 &&
            commandApdu[0] == SELECT_APDU[0] &&
            commandApdu[1] == SELECT_APDU[1] -> {
                handleSelectCommand(commandApdu)
            }

            // Commande READ
            commandApdu[0] == 0x30.toByte() -> {
                handleReadCommand(commandApdu)
            }

            // Commande WRITE
            commandApdu[0] == 0xA2.toByte() -> {
                handleWriteCommand(commandApdu)
            }

            else -> {
                Log.w(TAG, "Commande inconnue: ${commandApdu.toHexString()}")
                UNKNOWN_COMMAND
            }
        }
    }

    private fun handleSelectCommand(commandApdu: ByteArray): ByteArray {
        Log.d(TAG, "SELECT reçu")
        isEmulating = true

        // Retourner l'UID émulé si disponible
        return if (emulatedUid != null) {
            emulatedUid!! + SUCCESS_RESPONSE
        } else {
            SUCCESS_RESPONSE
        }
    }

    private fun handleReadCommand(commandApdu: ByteArray): ByteArray {
        Log.d(TAG, "READ reçu")

        if (!isEmulating || emulatedData == null) {
            return UNKNOWN_COMMAND
        }

        // Lire la page demandée
        val page = commandApdu[1].toInt() and 0xFF
        val pageData = getPageData(page)

        return pageData + SUCCESS_RESPONSE
    }

    private fun handleWriteCommand(commandApdu: ByteArray): ByteArray {
        Log.d(TAG, "WRITE reçu")

        if (!isEmulating) {
            return UNKNOWN_COMMAND
        }

        // Pour l'instant, on accepte l'écriture mais ne fait rien
        return SUCCESS_RESPONSE
    }

    private fun getPageData(page: Int): ByteArray {
        // Retourner 16 octets de données pour la page demandée
        return emulatedData?.let { data ->
            val offset = page * 4
            if (offset + 4 <= data.size) {
                data.copyOfRange(offset, offset + 4)
            } else {
                ByteArray(4) { 0x00 }
            }
        } ?: ByteArray(4) { 0x00 }
    }

    /**
     * Configure les données à émuler
     */
    fun setEmulationData(uid: ByteArray, data: ByteArray) {
        this.emulatedUid = uid
        this.emulatedData = data
        Log.d(TAG, "Données d'émulation configurées - UID: ${uid.toHexString()}")
    }

    /**
     * Arrête l'émulation
     */
    fun stopEmulation() {
        isEmulating = false
        emulatedUid = null
        emulatedData = null
        Log.d(TAG, "Émulation arrêtée")
    }
}

/**
 * Extension pour convertir ByteArray en String hexadécimal
 */
private fun ByteArray.toHexString(): String {
    return joinToString("") { "%02X".format(it) }
}
