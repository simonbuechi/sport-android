package ch.simibu.sport.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.simibu.sport.core.model.Exercise
import ch.simibu.sport.core.model.PersonalRecord
import ch.simibu.sport.core.model.TrainingTemplate
import ch.simibu.sport.core.model.UserProfile
import ch.simibu.sport.core.model.WeightEntry
import ch.simibu.sport.core.model.Workout
import ch.simibu.sport.domain.repository.AuthRepository
import ch.simibu.sport.domain.repository.ExerciseRepository
import ch.simibu.sport.domain.repository.TemplateRepository
import ch.simibu.sport.domain.repository.UserProfileRepository
import ch.simibu.sport.domain.repository.WorkoutRepository
import ch.simibu.sport.domain.usecase.WorkoutCalculationUseCase
import ch.simibu.sport.presentation.recovery.BodyRecoverySummary
import ch.simibu.sport.presentation.recovery.MuscleRecoveryCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class DashboardUiState(
    val userName: String = "Athlete",
    val motivationalMessage: String = "",
    val workoutsLast7Days: Int = 0,
    val totalWorkouts: Int = 0,
    val recentWorkouts: List<Workout> = emptyList(),
    val recentPersonalRecords: List<PersonalRecord> = emptyList(),
    val recoverySummary: BodyRecoverySummary = BodyRecoverySummary.defaultFullyRecovered(),
    val exercisesMap: Map<String, Exercise> = emptyMap(),
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
    private val userProfileRepository: UserProfileRepository,
    private val exerciseRepository: ExerciseRepository,
    private val calculationUseCase: WorkoutCalculationUseCase
) : ViewModel() {

    private val currentUserId = authRepository.currentUserId

    val uiState: StateFlow<DashboardUiState> = if (currentUserId == null) {
        MutableStateFlow(DashboardUiState(isLoading = false))
    } else {
        combine(
            workoutRepository.getWorkouts(currentUserId),
            templateRepository.getTemplates(currentUserId),
            userProfileRepository.getUserProfile(currentUserId),
            exerciseRepository.getExercises()
        ) { workouts, templates, profile, exercises ->
            val today = LocalDate.now()
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

            val exMap = exercises.associateBy { it.id }

            // Parse and sort workouts chronologically to detect PR progression
            val chronologicalWorkouts = workouts.mapNotNull { w ->
                val parsed = try {
                    LocalDate.parse(w.date.substringBefore('T').trim())
                } catch (_: Exception) {
                    null
                }
                if (parsed != null) Pair(parsed, w) else null
            }.sortedWith(compareBy({ it.first }, { it.second.time ?: "" }))

            val exerciseBestWeight = mutableMapOf<String, Double>()
            val exerciseBest1RM = mutableMapOf<String, Double>()
            val allPrRecords = mutableListOf<PersonalRecord>()

            for ((date, workout) in chronologicalWorkouts) {
                for (we in workout.exercises) {
                    val exId = we.exerciseId
                    val prevBestWeight = exerciseBestWeight[exId] ?: 0.0
                    val prevBest1RM = exerciseBest1RM[exId] ?: 0.0

                    var sessionMaxWeight = 0.0
                    var sessionBest1RM = 0.0
                    var bestSetWeight = 0.0
                    var bestSetReps = 0

                    for (s in we.sets) {
                        val w = s.weight ?: 0.0
                        val r = s.reps ?: 0
                        if (w > 0 && r > 0) {
                            if (w > sessionMaxWeight) {
                                sessionMaxWeight = w
                            }
                            val oneRm = calculationUseCase.calculate1RM(w, r)
                            if (oneRm > sessionBest1RM) {
                                sessionBest1RM = oneRm
                                bestSetWeight = w
                                bestSetReps = r
                            }
                        }
                    }

                    val isNewWeightPr = sessionMaxWeight > prevBestWeight && sessionMaxWeight > 0.0
                    val isNew1RmPr = sessionBest1RM > prevBest1RM && sessionBest1RM > 0.0

                    if (isNewWeightPr || isNew1RmPr) {
                        val exName = exMap[exId]?.name ?: "Exercise"
                        val exIcon = exMap[exId]?.iconUrl
                        val daysAgo = ChronoUnit.DAYS.between(date, today).coerceAtLeast(0)

                        allPrRecords.add(
                            PersonalRecord(
                                exerciseId = exId,
                                exerciseName = exName,
                                exerciseIconUrl = exIcon,
                                weight = if (bestSetWeight > 0.0) bestSetWeight else sessionMaxWeight,
                                reps = if (bestSetReps > 0) bestSetReps else 1,
                                estimated1RM = sessionBest1RM,
                                date = date.toString(),
                                daysAgo = daysAgo,
                                workoutId = workout.id
                            )
                        )

                        exerciseBestWeight[exId] = maxOf(prevBestWeight, sessionMaxWeight)
                        exerciseBest1RM[exId] = maxOf(prevBest1RM, sessionBest1RM)
                    }
                }
            }

            // Pick up to 3 most recent PRs (prioritizing distinct exercises)
            val sortedPRs = allPrRecords
                .sortedWith(compareByDescending<PersonalRecord> { it.date }.thenByDescending { it.estimated1RM })
            val distinctExercises = sortedPRs.distinctBy { it.exerciseId }
            val recentPRs = if (distinctExercises.size >= 3) {
                distinctExercises.take(3)
            } else {
                sortedPRs.distinctBy { "${it.exerciseId}_${it.date}" }.take(3)
            }

            val lastWorkoutDate = chronologicalWorkouts.lastOrNull()?.first
            val workedOutToday = lastWorkoutDate == today
            val daysSinceLastWorkout = if (lastWorkoutDate != null) {
                ChronoUnit.DAYS.between(lastWorkoutDate, today).toInt().coerceAtLeast(0)
            } else null

            val motivationalMessage = MotivationalMessageGenerator.generateMessage(
                workedOutToday = workedOutToday,
                daysSinceLastWorkout = daysSinceLastWorkout,
                lastPR = recentPRs.firstOrNull(),
                seedDate = today
            )

            val recoverySummary = MuscleRecoveryCalculator.calculateRecovery(
                workouts = workouts,
                exerciseMap = exMap
            )

            DashboardUiState(
                userName = profile?.name?.ifBlank { "Athlete" } ?: "Athlete",
                motivationalMessage = motivationalMessage,
                workoutsLast7Days = workoutsLast7,
                totalWorkouts = workouts.size,
                recentWorkouts = workouts.take(5),
                recentPersonalRecords = recentPRs,
                recoverySummary = recoverySummary,
                exercisesMap = exMap,
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
