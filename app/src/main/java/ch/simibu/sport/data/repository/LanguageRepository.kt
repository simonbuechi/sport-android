package ch.simibu.sport.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
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

private val Context.languageDataStore: DataStore<Preferences> by preferencesDataStore(name = "language_preferences")

@Singleton
class LanguageRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val languageKey = stringPreferencesKey("app_language_code")

    val languageMode: Flow<LanguageMode> = context.languageDataStore.data.map { preferences ->
        LanguageMode.fromCode(preferences[languageKey])
    }

    suspend fun setLanguageMode(mode: LanguageMode) {
        context.languageDataStore.edit { preferences ->
            preferences[languageKey] = mode.code
        }
    }
}
