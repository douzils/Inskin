package com.inskin.app

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

data class SimpleTag(
    val uidHex: String = "",
    val name: String = ""
)

class NfcViewModel : ViewModel() {

    // UI attend un tag au démarrage
    private val _isWaiting = mutableStateOf(true)
    val isWaiting: State<Boolean> = _isWaiting

    // Dernier tag lu (nullable)
    private val _lastTag = mutableStateOf<SimpleTag?>(null)
    val lastTag: State<SimpleTag?> = _lastTag

    fun startWaiting() {
        _isWaiting.value = true
    }

    fun updateTag(uidHex: String, name: String = "") {
        _lastTag.value = SimpleTag(uidHex = uidHex, name = name)
        _isWaiting.value = false
    }
}
