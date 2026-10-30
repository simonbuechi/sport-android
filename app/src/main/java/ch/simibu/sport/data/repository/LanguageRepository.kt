package ch.simibu.sport.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class LanguageMode(val code: String, val displayName: String, val nativeName: String) {
    SYSTEM("system", "System", "System"),
    ENGLISH("en", "English", "English"),
    GERMAN("de", "German", "Deutsch"),
    FRENCH("fr", "French", "Français"),
    SPANISH("es", "Spanish", "Español"),
    ITALIAN("it", "Italian", "Italiano");

    companion object {
        fun fromCode(code: String?): LanguageMode {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: SYSTEM
        }
    }
}

@Singleton
class LanguageRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val languageKey = stringPreferencesKey("app_language_code")

    val languageMode: Flow<LanguageMode> = dataStore.data.map { preferences ->
        LanguageMode.fromCode(preferences[languageKey])
    }

    suspend fun setLanguageMode(mode: LanguageMode) {
        dataStore.edit { preferences ->
            preferences[languageKey] = mode.code
        }
    }
}
