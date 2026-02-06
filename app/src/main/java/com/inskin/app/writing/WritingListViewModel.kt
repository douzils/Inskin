package com.inskin.app.writing

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WritingListViewModel : ViewModel() {

    // Liste des listes d'écriture personnalisées
    private val _writingLists = mutableStateListOf<WritingList>()
    val writingLists: List<WritingList> = _writingLists

    // Presets disponibles
    private val _availablePresets = mutableStateListOf(*WritingPresets.getAllPresets().toTypedArray())
    val availablePresets: List<WritingList> = _availablePresets

    // Liste actuellement en édition
    private val _currentEditingList = MutableStateFlow<WritingList?>(null)
    val currentEditingList: StateFlow<WritingList?> = _currentEditingList.asStateFlow()

    // Statistiques
    private val _totalWrites = mutableStateOf(0)
    val totalWrites get() = _totalWrites.value

    init {
        loadWritingLists()
    }

    /**
     * Charge les listes depuis le stockage
     */
    private fun loadWritingLists() {
        // TODO: Charger depuis Room/DataStore
        // Pour l'instant, liste vide
    }

    /**
     * Ajoute une nouvelle liste
     */
    fun addWritingList(list: WritingList) {
        _writingLists.add(list)
        saveToStorage()
    }

    /**
     * Met à jour une liste existante
     */
    fun updateWritingList(oldList: WritingList, newList: WritingList) {
        val index = _writingLists.indexOf(oldList)
        if (index != -1) {
            _writingLists[index] = newList
            saveToStorage()
        }
    }

    /**
     * Supprime une liste
     */
    fun deleteWritingList(list: WritingList) {
        _writingLists.remove(list)
        saveToStorage()
    }

    /**
     * Clone un preset vers les listes personnalisées
     */
    fun clonePreset(preset: WritingList) {
        val cloned = preset.copy(
            id = java.util.UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis()
        )
        addWritingList(cloned)
    }

    /**
     * Marque une liste comme favorite
     */
    fun toggleFavorite(list: WritingList) {
        val index = _writingLists.indexOf(list)
        if (index != -1) {
            _writingLists[index] = list.copy(isFavorite = !list.isFavorite)
            saveToStorage()
        }
    }

    /**
     * Enregistre l'utilisation d'une liste
     */
    fun recordUse(list: WritingList) {
        viewModelScope.launch {
            val index = _writingLists.indexOf(list)
            if (index != -1) {
                _writingLists[index] = list.copy(
                    lastUsed = System.currentTimeMillis(),
                    useCount = list.useCount + 1
                )
                _totalWrites.value++
                saveToStorage()
            }
        }
    }

    /**
     * Commence l'édition d'une liste
     */
    fun startEditing(list: WritingList) {
        _currentEditingList.value = list
    }

    /**
     * Annule l'édition
     */
    fun cancelEditing() {
        _currentEditingList.value = null
    }

    /**
     * Sauvegarde l'édition
     */
    fun saveEditing(list: WritingList) {
        val current = _currentEditingList.value
        if (current != null) {
            updateWritingList(current, list)
        } else {
            addWritingList(list)
        }
        _currentEditingList.value = null
    }

    /**
     * Réordonne les listes
     */
    fun reorderLists(from: Int, to: Int) {
        if (from < _writingLists.size && to < _writingLists.size) {
            val item = _writingLists.removeAt(from)
            _writingLists.add(to, item)
            saveToStorage()
        }
    }

    /**
     * Sauvegarde dans le stockage
     */
    private fun saveToStorage() {
        viewModelScope.launch {
            // TODO: Sauvegarder dans Room/DataStore
        }
    }

    /**
     * Recherche dans les listes
     */
    fun searchLists(query: String): List<WritingList> {
        if (query.isBlank()) return _writingLists
        return _writingLists.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true)
        }
    }

    /**
     * Récupère les listes favorites
     */
    fun getFavoriteLists(): List<WritingList> {
        return _writingLists.filter { it.isFavorite }
    }

    /**
     * Récupère les listes les plus utilisées
     */
    fun getMostUsedLists(limit: Int = 5): List<WritingList> {
        return _writingLists.sortedByDescending { it.useCount }.take(limit)
    }

    /**
     * Exporte une liste au format JSON
     */
    fun exportList(list: WritingList): String {
        // TODO: Implémenter l'export JSON
        return ""
    }

    /**
     * Importe une liste depuis JSON
     */
    fun importList(json: String): WritingList? {
        // TODO: Implémenter l'import JSON
        return null
    }
}
