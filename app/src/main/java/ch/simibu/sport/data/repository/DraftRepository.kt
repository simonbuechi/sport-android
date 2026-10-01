package ch.simibu.sport.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DraftRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val workoutDraftKey = stringPreferencesKey("workout_draft_json")

    fun getWorkoutDraft(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[workoutDraftKey]
        }
    }

    suspend fun saveWorkoutDraft(json: String) {
        dataStore.edit { preferences ->
            preferences[workoutDraftKey] = json
        }
    }

    suspend fun clearWorkoutDraft() {
        dataStore.edit { preferences ->
            preferences.remove(workoutDraftKey)
        }
    }
}
