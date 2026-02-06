package com.inskin.app.ui

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inskin.app.ui.theme.AccentColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Application.advancedDataStore: DataStore<Preferences> by preferencesDataStore(name = "advanced_settings")

class AdvancedPreferencesViewModel(app: Application) : AndroidViewModel(app) {
    private val dataStore = app.advancedDataStore

    companion object {
        private val ACCENT_COLOR_KEY = intPreferencesKey("accent_color")
        private val PASSWORD_ENABLED_KEY = booleanPreferencesKey("password_enabled")
        private val PASSWORD_HASH_KEY = stringPreferencesKey("password_hash")
        private val LOCK_TIMEOUT_KEY = intPreferencesKey("lock_timeout") // en secondes
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
    }

    // Couleur d'accent
    val accentColor: Flow<AccentColor> = dataStore.data.map { prefs ->
        AccentColor.fromOrdinal(prefs[ACCENT_COLOR_KEY] ?: 0)
    }

    fun setAccentColor(color: AccentColor) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[ACCENT_COLOR_KEY] = color.ordinal
            }
        }
    }

    // Mot de passe
    val passwordEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PASSWORD_ENABLED_KEY] ?: false
    }

    val passwordHash: Flow<String?> = dataStore.data.map { prefs ->
        prefs[PASSWORD_HASH_KEY]
    }

    fun setPasswordEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[PASSWORD_ENABLED_KEY] = enabled
            }
        }
    }

    fun setPassword(hash: String) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[PASSWORD_HASH_KEY] = hash
                prefs[PASSWORD_ENABLED_KEY] = true
            }
        }
    }

    fun removePassword() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs.remove(PASSWORD_HASH_KEY)
                prefs[PASSWORD_ENABLED_KEY] = false
            }
        }
    }

    // Timeout de verrouillage
    val lockTimeout: Flow<Int> = dataStore.data.map { prefs ->
        prefs[LOCK_TIMEOUT_KEY] ?: 300 // 5 minutes par défaut
    }

    fun setLockTimeout(seconds: Int) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[LOCK_TIMEOUT_KEY] = seconds
            }
        }
    }

    // Biométrie
    val biometricEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[BIOMETRIC_ENABLED_KEY] ?: false
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[BIOMETRIC_ENABLED_KEY] = enabled
            }
        }
    }
}

/**
 * Utilitaire pour hasher les mots de passe
 */
object PasswordUtils {
    fun hashPassword(password: String): String {
        return password.hashCode().toString()
    }

    fun verifyPassword(password: String, hash: String): Boolean {
        return hashPassword(password) == hash
    }
}
