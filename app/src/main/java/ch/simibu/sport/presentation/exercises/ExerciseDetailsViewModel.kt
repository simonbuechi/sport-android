package ch.simibu.sport.presentation.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.simibu.sport.core.designsystem.ChartDataPoint
import ch.simibu.sport.core.model.Exercise
import ch.simibu.sport.core.model.Workout
import ch.simibu.sport.domain.repository.AuthRepository
import ch.simibu.sport.domain.repository.ExerciseRepository
import ch.simibu.sport.domain.repository.UserProfileRepository
import ch.simibu.sport.domain.repository.WorkoutRepository
import ch.simibu.sport.domain.usecase.WorkoutCalculationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

data class ExerciseDetailsUiState(
    val exercise: Exercise? = null,
    val isFavorite: Boolean = false,
    val personalNotes: String = "",
    val lastTrainedDate: String? = null,
    val lastTrainedRelative: String? = null,
    val lastTrainedSummary: String? = null,
    val workoutsCount: Int = 0,
    val allChartDataPoints: List<ChartDataPoint> = emptyList(),
    val maxWeightAllTime: Double? = null,
    val best1RMAllTime: Double? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class ExerciseDetailsViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val userProfileRepository: UserProfileRepository,
    private val authRepository: AuthRepository,
    private val calculationUseCase: WorkoutCalculationUseCase
) : ViewModel() {

    private val currentUserId: String?
        get() = authRepository.currentUserId
    private val uiStateCache = ConcurrentHashMap<String, StateFlow<ExerciseDetailsUiState>>()

    fun getUiState(exerciseId: String): StateFlow<ExerciseDetailsUiState> {
        val uid = currentUserId ?: "guest"
        return uiStateCache.computeIfAbsent("$uid:$exerciseId") {
            createUiStateFlow(exerciseId, currentUserId)
        }
    }

    private fun createUiStateFlow(exerciseId: String, uid: String?): StateFlow<ExerciseDetailsUiState> {
        val workoutsFlow = if (uid != null) {
            workoutRepository.getWorkouts(uid)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }

        return combine(
            exerciseRepository.getExercise(exerciseId),
            workoutsFlow,
            if (uid != null) userProfileRepository.getUserProfile(uid) else kotlinx.coroutines.flow.flowOf(null)
        ) { exercise, workouts, profile ->
            val marked = profile?.markedExercises?.get(exerciseId)
            val isFav = marked?.favorite == true
            val notes = marked?.notes ?: ""

            // Extract all workouts with this exercise
            val relevantWorkouts = workouts.filter { w ->
                w.exercises.any { it.exerciseId == exerciseId }
            }.sortedBy { it.date }

            var maxWeightAllTime: Double? = null
            var best1RMAllTime: Double? = null

            val chartPoints = relevantWorkouts.mapNotNull { w ->
                val ex = w.exercises.find { it.exerciseId == exerciseId } ?: return@mapNotNull null
                val localDate = try {
                    LocalDate.parse(w.date.substringBefore('T'))
                } catch (_: Exception) {
                    null
                } ?: return@mapNotNull null

                var sessionMax1RM = 0.0
                for (s in ex.sets) {
                    val wKg = s.weight ?: 0.0
                    val reps = s.reps ?: 1
                    if (s.weight != null) {
                        if (maxWeightAllTime == null || wKg > maxWeightAllTime!!) {
                            maxWeightAllTime = wKg
                        }
                        val oneRm = calculationUseCase.calculate1RM(wKg, reps)
                        if (oneRm > sessionMax1RM) {
                            sessionMax1RM = oneRm
                        }
                        if (best1RMAllTime == null || oneRm > best1RMAllTime!!) {
                            best1RMAllTime = oneRm
                        }
                    }
                }

                if (sessionMax1RM <= 0.0) return@mapNotNull null

                val timestamp = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val formattedDate = localDate.format(DateTimeFormatter.ofPattern("MMM d"))
                ChartDataPoint(
                    timestamp = timestamp,
                    value = sessionMax1RM.toFloat(),
                    formattedDate = formattedDate
                )
            }

            // Last trained details
            val latestWorkout = relevantWorkouts.lastOrNull()
            var lastDateStr: String? = null
            var lastRelativeStr: String? = null
            var lastSummaryStr: String? = null

            if (latestWorkout != null) {
                try {
                    val localDate = LocalDate.parse(latestWorkout.date.substringBefore('T'))
                    lastDateStr = localDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                    val daysAgo = ChronoUnit.DAYS.between(localDate, LocalDate.now())
                    lastRelativeStr = when {
                        daysAgo == 0L -> "Today"
                        daysAgo == 1L -> "Yesterday"
                        daysAgo > 1L -> "$daysAgo days ago"
                        else -> "Upcoming"
                    }
                } catch (_: Exception) {
                    lastDateStr = latestWorkout.date
                }

                val lastEx = latestWorkout.exercises.find { it.exerciseId == exerciseId }
                if (lastEx != null) {
                    val setsCount = lastEx.sets.size
                    val sessionMax1RM = lastEx.sets.maxOfOrNull {
                        calculationUseCase.calculate1RM(it.weight ?: 0.0, it.reps ?: 1)
                    } ?: 0.0
                    lastSummaryStr = if (sessionMax1RM > 0.0) {
                        "$setsCount sets • 1RM: ${Math.round(sessionMax1RM)} kg"
                    } else {
                        "$setsCount sets"
                    }
                }
            }

            ExerciseDetailsUiState(
                exercise = exercise,
                isFavorite = isFav,
                personalNotes = notes,
                lastTrainedDate = lastDateStr,
                lastTrainedRelative = lastRelativeStr,
                lastTrainedSummary = lastSummaryStr,
                workoutsCount = relevantWorkouts.size,
                allChartDataPoints = chartPoints,
                maxWeightAllTime = maxWeightAllTime,
                best1RMAllTime = best1RMAllTime,
                isLoading = false
            )
        }.flowOn(Dispatchers.Default).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ExerciseDetailsUiState()
        )
    }

    fun toggleFavorite(exerciseId: String, currentFavorite: Boolean) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            userProfileRepository.updateExerciseStatus(
                userId = uid,
                exerciseId = exerciseId,
                favorite = !currentFavorite,
                notes = null
            )
        }
    }

    fun savePersonalNotes(exerciseId: String, notes: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            userProfileRepository.updateExerciseStatus(
                userId = uid,
                exerciseId = exerciseId,
                favorite = null,
                notes = notes
            )
        }
    }
}
