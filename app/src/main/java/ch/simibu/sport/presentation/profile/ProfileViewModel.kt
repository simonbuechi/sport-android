package ch.simibu.sport.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.simibu.sport.core.model.MeasurementEntry
import ch.simibu.sport.core.model.UserProfile
import ch.simibu.sport.core.model.UserSettings
import ch.simibu.sport.core.model.WeightEntry
import ch.simibu.sport.domain.repository.AuthRepository
import ch.simibu.sport.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import ch.simibu.sport.data.repository.LanguageMode
import ch.simibu.sport.data.repository.LanguageRepository
import ch.simibu.sport.data.repository.ThemeMode
import ch.simibu.sport.data.repository.ThemeRepository
import ch.simibu.sport.data.health.HealthConnectManager
import ch.simibu.sport.domain.repository.WorkoutRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = true,
    val isSyncingHealthConnect: Boolean = false,
    val healthConnectMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val authRepository: AuthRepository,
    private val themeRepository: ThemeRepository,
    private val languageRepository: LanguageRepository,
    private val healthConnectManager: HealthConnectManager,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themeRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val languageMode: StateFlow<LanguageMode> = languageRepository.languageMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LanguageMode.SYSTEM)

    private val currentUserId: String?
        get() = authRepository.currentUserId

    private val _isSyncingHealthConnect = MutableStateFlow(false)
    private val _healthConnectMessage = MutableStateFlow<String?>(null)

    val isHealthConnectAvailable: Boolean get() = healthConnectManager.isAvailable
    val healthConnectSdkStatus: Int get() = healthConnectManager.getSdkStatus()
    val healthConnectPermissions: Set<String> get() = healthConnectManager.permissions

    suspend fun checkHealthConnectPermissions(): Boolean {
        return healthConnectManager.hasPermissions()
    }

    val uiState: StateFlow<ProfileUiState> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(ProfileUiState(isLoading = false))
            } else {
                combine(
                    userProfileRepository.getUserProfile(user.uid),
                    _isSyncingHealthConnect,
                    _healthConnectMessage
                ) { profile, syncing, message ->
                    if (profile != null) {
                        val remoteMode = ThemeMode.fromString(profile.settings.theme)
                        if (remoteMode != themeMode.value) {
                            viewModelScope.launch {
                                themeRepository.setThemeMode(remoteMode)
                            }
                        }
                    }
                    ProfileUiState(
                        profile = profile,
                        isLoading = false,
                        isSyncingHealthConnect = syncing,
                        healthConnectMessage = message
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfileUiState(isLoading = true)
        )

    fun clearHealthConnectMessage() {
        _healthConnectMessage.value = null
    }

    fun syncAllWorkoutsToHealthConnect() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            _isSyncingHealthConnect.value = true
            try {
                val workouts = workoutRepository.getWorkouts(uid).firstOrNull() ?: emptyList()
                val result = healthConnectManager.exportWorkoutsBatch(workouts)
                result.onSuccess { count ->
                    _healthConnectMessage.value = "synced:$count"
                }.onFailure { err ->
                    _healthConnectMessage.value = "error:${err.localizedMessage ?: "Sync failed"}"
                }
            } catch (e: Exception) {
                _healthConnectMessage.value = "error:${e.localizedMessage ?: "Sync failed"}"
            } finally {
                _isSyncingHealthConnect.value = false
            }
        }
    }

    fun updateProfileInfo(name: String, birthYear: Int?, height: Double?, notes: String) {
        val current = uiState.value.profile ?: return
        viewModelScope.launch {
            userProfileRepository.updateUserProfile(
                current.copy(
                    name = name,
                    birthYear = birthYear,
                    height = height,
                    notes = notes
                )
            )
        }
    }

    fun updateUserSettings(settings: UserSettings) {
        val current = uiState.value.profile ?: return
        viewModelScope.launch {
            val updated = current.copy(settings = settings)
            userProfileRepository.updateUserProfile(updated)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themeRepository.setThemeMode(mode)
            val current = uiState.value.profile
            if (current != null && current.settings.theme != mode.value) {
                userProfileRepository.updateUserProfile(
                    current.copy(
                        settings = current.settings.copy(theme = mode.value)
                    )
                )
            }
        }
    }

    fun setLanguageMode(mode: LanguageMode) {
        viewModelScope.launch {
            languageRepository.setLanguageMode(mode)
        }
    }

    fun addWeightEntry(weightKg: Double, bodyFat: Double?) {
        val uid = currentUserId ?: return
        val date = LocalDate.now().toString()
        val entry = WeightEntry(
            id = UUID.randomUUID().toString(),
            date = date,
            weightKg = weightKg,
            bodyFatPercent = bodyFat
        )
        viewModelScope.launch {
            userProfileRepository.addWeightEntry(uid, entry)
        }
    }

    fun addMeasurementEntry(
        waist: Double?,
        hips: Double?,
        chest: Double?,
        shoulders: Double?,
        bicep: Double?,
        thigh: Double?
    ) {
        val uid = currentUserId ?: return
        val date = LocalDate.now().toString()
        val entry = MeasurementEntry(
            id = UUID.randomUUID().toString(),
            date = date,
            waist = waist,
            hips = hips,
            chest = chest,
            shoulders = shoulders,
            rightBicep = bicep,
            rightThigh = thigh
        )
        viewModelScope.launch {
            userProfileRepository.addMeasurementEntry(uid, entry)
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
