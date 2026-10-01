package ch.simibu.sport.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.simibu.sport.core.model.MeasurementEntry
import ch.simibu.sport.core.model.UserProfile
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import ch.simibu.sport.data.repository.LanguageMode
import ch.simibu.sport.data.repository.LanguageRepository
import ch.simibu.sport.data.repository.ThemeMode
import ch.simibu.sport.data.repository.ThemeRepository
import java.util.UUID
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val authRepository: AuthRepository,
    private val themeRepository: ThemeRepository,
    private val languageRepository: LanguageRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themeRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val languageMode: StateFlow<LanguageMode> = languageRepository.languageMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LanguageMode.SYSTEM)

    private val currentUserId = authRepository.currentUserId

    val uiState: StateFlow<ProfileUiState> = if (currentUserId == null) {
        MutableStateFlow(ProfileUiState(isLoading = false))
    } else {
        userProfileRepository.getUserProfile(currentUserId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            ).let { profileFlow ->
                MutableStateFlow(ProfileUiState()).apply {
                    viewModelScope.launch {
                        profileFlow.collect { profile ->
                            value = ProfileUiState(profile = profile, isLoading = false)
                        }
                    }
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

    fun updateSettings(theme: String, autoFillSets: Boolean, showTimer: Boolean) {
        val current = uiState.value.profile ?: return
        viewModelScope.launch {
            val updated = current.copy(
                settings = current.settings.copy(
                    theme = theme,
                    autoFillSets = autoFillSets,
                    showTimer = showTimer
                )
            )
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
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
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
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
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
