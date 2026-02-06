package com.inskin.app.emulation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel pour gérer l'émulation de cartes NFC
 */
class EmulationViewModel : ViewModel() {

    // Liste des cartes virtuelles
    private val _virtualCards = mutableStateListOf<VirtualCard>()
    val virtualCards: List<VirtualCard> = _virtualCards

    // État de l'émulation
    private val _emulationState = MutableStateFlow<EmulationState>(EmulationState.Idle)
    val emulationState: StateFlow<EmulationState> = _emulationState.asStateFlow()

    // Carte actuellement active
    private val _activeCard = mutableStateOf<VirtualCard?>(null)
    val activeCard get() = _activeCard.value

    // Statistiques
    private val _totalEmulations = mutableStateOf(0)
    val totalEmulations get() = _totalEmulations.value

    init {
        // Charger les cartes sauvegardées (à implémenter avec Room)
        loadSampleCards()
    }

    /**
     * Charge des cartes d'exemple
     */
    private fun loadSampleCards() {
        _virtualCards.addAll(
            listOf(
                VirtualCard(
                    name = "Badge Bureau",
                    uid = "04:52:B2:EA:3C:80",
                    type = CardType.NTAG216,
                    data = ByteArray(888) { 0x00 },
                    color = Color(0xFF4CAF50)
                ),
                VirtualCard(
                    name = "Carte Transport",
                    uid = "08:A3:D4:12:5F:90",
                    type = CardType.MIFARE_CLASSIC_1K,
                    data = ByteArray(1024) { 0x00 },
                    color = Color(0xFF2196F3)
                ),
                VirtualCard(
                    name = "xNT Implant Clone",
                    uid = "E0:04:01:00:00:01:23:45",
                    type = CardType.NTAG216,
                    data = ByteArray(888) { 0x00 },
                    color = Color(0xFF9C27B0)
                )
            )
        )
    }

    /**
     * Démarre l'émulation d'une carte
     */
    fun startEmulation(card: VirtualCard) {
        viewModelScope.launch {
            try {
                // Désactiver l'ancienne carte active
                _activeCard.value?.let { oldCard ->
                    val index = _virtualCards.indexOf(oldCard)
                    if (index != -1) {
                        _virtualCards[index] = oldCard.copy(isActive = false)
                    }
                }

                // Activer la nouvelle carte
                val index = _virtualCards.indexOf(card)
                if (index != -1) {
                    val updatedCard = card.copy(
                        isActive = true,
                        lastUsed = System.currentTimeMillis(),
                        useCount = card.useCount + 1
                    )
                    _virtualCards[index] = updatedCard
                    _activeCard.value = updatedCard
                    _emulationState.value = EmulationState.Active(updatedCard)
                    _totalEmulations.value++
                }
            } catch (e: Exception) {
                _emulationState.value = EmulationState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    /**
     * Arrête l'émulation
     */
    fun stopEmulation() {
        viewModelScope.launch {
            _activeCard.value?.let { card ->
                val index = _virtualCards.indexOf(card)
                if (index != -1) {
                    _virtualCards[index] = card.copy(isActive = false)
                }
            }
            _activeCard.value = null
            _emulationState.value = EmulationState.Idle
        }
    }

    /**
     * Ajoute une nouvelle carte virtuelle
     */
    fun addVirtualCard(card: VirtualCard) {
        _virtualCards.add(card)
    }

    /**
     * Supprime une carte virtuelle
     */
    fun deleteVirtualCard(card: VirtualCard) {
        if (card.isActive) {
            stopEmulation()
        }
        _virtualCards.remove(card)
    }

    /**
     * Clone une carte NFC scannée
     */
    fun cloneFromScanned(uid: String, name: String, type: CardType, data: ByteArray) {
        val newCard = VirtualCard(
            name = name,
            uid = uid,
            type = type,
            data = data,
            color = type.color
        )
        addVirtualCard(newCard)
    }

    /**
     * Modifie une carte existante
     */
    fun updateVirtualCard(oldCard: VirtualCard, newCard: VirtualCard) {
        val index = _virtualCards.indexOf(oldCard)
        if (index != -1) {
            _virtualCards[index] = newCard
        }
    }
}
