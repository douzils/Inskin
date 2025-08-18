package com.inskin.app
import android.nfc.Tag
import android.nfc.tech.Ndef
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TagInfo(
    val title: String,
    val uid: String,
    val total: Int,
    val used: Int,
    val name: String = "NextV2",
    val locked: Boolean = false
)

sealed interface UiState {
    data object Scanning : UiState
    data class Loading(val uid: String) : UiState
    data class Loaded(val info: TagInfo) : UiState
}

class NfcViewModel : ViewModel() {
    private val _state = MutableStateFlow<UiState>(UiState.Scanning)
    val state = _state.asStateFlow()

    fun handleTag(tag: Tag) {
        val uid = tag.id?.joinToString(":") { String.format("%02X", it) } ?: "UNKNOWN"
        _state.value = UiState.Loading(uid)
        viewModelScope.launch {
            val ndef = Ndef.get(tag)
            val total = ndef?.maxSize ?: 888
            val used = 100
            val title = "NTAG216 (NXP)"
            delay(600) // donne le temps d’afficher l’animation "lecture"
            _state.value = UiState.Loaded(TagInfo(title = title, uid = uid, total = total, used = used))
        }
    }

    fun backToScan() { _state.value = UiState.Scanning }
}
