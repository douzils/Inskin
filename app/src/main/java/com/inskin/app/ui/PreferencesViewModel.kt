package com.inskin.app.ui

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// DataStore au niveau Application
val Application.dataStore by preferencesDataStore(name = "settings")

class PreferencesViewModel(app: Application) : AndroidViewModel(app) {
    private val keyDark = booleanPreferencesKey("dark_theme")

    val darkTheme: StateFlow<Boolean> =
        app.dataStore.data.map { prefs ->
            if (prefs.contains(keyDark)) prefs[keyDark] ?: false
            else false // clair par défaut si aucune préférence
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun toggleTheme() {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[keyDark] = !(prefs[keyDark] ?: false)
            }
        }
    }
}
