package ch.simibu.sport.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.simibu.sport.core.model.TrainingTemplate
import ch.simibu.sport.core.model.UserProfile
import ch.simibu.sport.core.model.WeightEntry
import ch.simibu.sport.core.model.Workout
import ch.simibu.sport.domain.repository.AuthRepository
import ch.simibu.sport.domain.repository.TemplateRepository
import ch.simibu.sport.domain.repository.UserProfileRepository
import ch.simibu.sport.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DashboardUiState(
    val userName: String = "Athlete",
    val workoutsLast7Days: Int = 0,
    val totalWorkouts: Int = 0,
    val recentWorkouts: List<Workout> = emptyList(),
    val favoriteTemplates: List<TrainingTemplate> = emptyList(),
    val latestWeight: WeightEntry? = null,
    val weightTrendKg: Double? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val workoutRepository: WorkoutRepository,
    private val templateRepository: TemplateRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val currentUserId = authRepository.currentUserId

    val uiState: StateFlow<DashboardUiState> = if (currentUserId == null) {
        MutableStateFlow(DashboardUiState(isLoading = false))
    } else {
        combine(
            workoutRepository.getWorkouts(currentUserId),
            templateRepository.getTemplates(currentUserId),
            userProfileRepository.getUserProfile(currentUserId)
        ) { workouts, templates, profile ->
            val now = Calendar.getInstance()
            val sevenDaysAgoCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -7)
            }
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

            val workoutsLast7 = workouts.count { w ->
                try {
                    val d = dateFormat.parse(w.date)
                    d != null && d.after(sevenDaysAgoCal.time)
                } catch (e: Exception) {
                    false
                }
            }

            val favTemplates = templates.filter { it.isFavorite }
            val sortedWeights = profile?.weights?.sortedByDescending { it.date } ?: emptyList()
            val latestW = sortedWeights.firstOrNull()
            val prevW = sortedWeights.getOrNull(1)
            val weightTrend = if (latestW != null && prevW != null) {
                latestW.weightKg - prevW.weightKg
            } else null

            DashboardUiState(
                userName = profile?.name?.ifBlank { "Athlete" } ?: "Athlete",
                workoutsLast7Days = workoutsLast7,
                totalWorkouts = workouts.size,
                recentWorkouts = workouts.take(3),
                favoriteTemplates = favTemplates,
                latestWeight = latestW,
                weightTrendKg = weightTrend,
                isLoading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState()
        )
    }
}
