package ch.simibu.sport.presentation.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.simibu.sport.core.model.Exercise
import ch.simibu.sport.core.model.ExerciseSet
import ch.simibu.sport.core.model.ExerciseType
import ch.simibu.sport.core.model.Workout
import ch.simibu.sport.core.model.WorkoutExercise
import ch.simibu.sport.data.repository.ActiveWorkoutManager
import ch.simibu.sport.data.repository.DraftRepository
import ch.simibu.sport.domain.repository.AuthRepository
import ch.simibu.sport.domain.repository.ExerciseRepository
import ch.simibu.sport.domain.repository.TemplateRepository
import ch.simibu.sport.domain.repository.UserProfileRepository
import ch.simibu.sport.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class WorkoutFormUiState(
    val id: String? = null,
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val time: String = SimpleDateFormat("HH:mm", Locale.US).format(Date()),
    val length: Int = 45,
    val sessionType: ExerciseType = ExerciseType.STRENGTH,
    val intensity: Int = 3,
    val maxPulse: Int? = null,
    val comment: String = "",
    val exercises: List<WorkoutExercise> = emptyList(),
    val availableExercises: List<Exercise> = emptyList(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    // Timer state
    val timerRunning: Boolean = false,
    val timerSecondsRemaining: Int = 0,
    val timerInitialSeconds: Int = 90,
    val takeWorkoutTime: Boolean = true,
    val elapsedWorkoutSeconds: Long = 0L
)

@HiltViewModel
class WorkoutFormViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val templateRepository: TemplateRepository,
    private val draftRepository: DraftRepository,
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    val activeWorkoutManager: ActiveWorkoutManager
) : ViewModel() {

    private val currentUserId = authRepository.currentUserId

    private val _uiState = MutableStateFlow(WorkoutFormUiState())
    val uiState: StateFlow<WorkoutFormUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            exerciseRepository.getExercises().collect { list ->
                _uiState.update { it.copy(availableExercises = list) }
            }
        }
        viewModelScope.launch {
            activeWorkoutManager.elapsedSeconds.collect { seconds ->
                _uiState.update {
                    it.copy(
                        elapsedWorkoutSeconds = seconds,
                        length = if (it.takeWorkoutTime && it.id == null) {
                            maxOf(1, (seconds / 60).toInt())
                        } else {
                            it.length
                        }
                    )
                }
            }
        }
    }

    private fun syncActiveSession() {
        if (_uiState.value.id == null) {
            activeWorkoutManager.updateState(_uiState.value)
        }
    }

    fun initWorkout(workoutId: String?, templateId: String?) {
        val uid = currentUserId ?: return

        viewModelScope.launch {
            if (workoutId != null) {
                // Editing existing workout
                val existing = workoutRepository.getWorkout(uid, workoutId).firstOrNull()
                if (existing != null) {
                    _uiState.update {
                        it.copy(
                            id = existing.id,
                            date = existing.date,
                            time = existing.time ?: it.time,
                            length = existing.length ?: 45,
                            sessionType = existing.sessionType,
                            intensity = existing.intensity ?: 3,
                            maxPulse = existing.maxPulse,
                            comment = existing.comment,
                            exercises = existing.exercises,
                            takeWorkoutTime = false
                        )
                    }
                }
            } else {
                // Live active workout session
                val existingSession = activeWorkoutManager.activeSession.value
                if (existingSession != null) {
                    _uiState.value = existingSession.state.copy(
                        takeWorkoutTime = existingSession.takeWorkoutTime,
                        availableExercises = _uiState.value.availableExercises
                    )
                } else {
                    val profile = userProfileRepository.getUserProfile(uid).firstOrNull()
                    val takeWorkoutTime = profile?.settings?.takeWorkoutTime ?: true

                    var initialExercises = emptyList<WorkoutExercise>()
                    var templateName: String? = null

                    if (templateId != null) {
                        val template = templateRepository.getTemplate(uid, templateId).firstOrNull()
                        if (template != null) {
                            templateName = template.name
                            initialExercises = template.exercises.map { te ->
                                WorkoutExercise(
                                    exerciseId = te.exerciseId,
                                    note = te.note,
                                    sets = if (te.sets.isNotEmpty()) te.sets else listOf(ExerciseSet())
                                )
                            }
                        }
                    }

                    val newState = WorkoutFormUiState(
                        exercises = initialExercises,
                        takeWorkoutTime = takeWorkoutTime,
                        availableExercises = _uiState.value.availableExercises
                    )
                    _uiState.value = newState
                    activeWorkoutManager.startSession(
                        templateId = templateId,
                        templateName = templateName,
                        takeWorkoutTime = takeWorkoutTime,
                        initialState = newState
                    )
                }
            }
        }
    }

    fun onDateChange(date: String) {
        _uiState.update { it.copy(date = date) }
        syncActiveSession()
    }

    fun onLengthChange(length: Int) {
        _uiState.update { it.copy(length = length) }
        syncActiveSession()
    }

    fun onSessionTypeChange(type: ExerciseType) {
        _uiState.update { it.copy(sessionType = type) }
        syncActiveSession()
    }

    fun onIntensityChange(intensity: Int) {
        _uiState.update { it.copy(intensity = intensity) }
        syncActiveSession()
    }

    fun onMaxPulseChange(pulse: Int?) {
        _uiState.update { it.copy(maxPulse = pulse) }
        syncActiveSession()
    }

    fun onCommentChange(comment: String) {
        _uiState.update { it.copy(comment = comment) }
        syncActiveSession()
    }

    fun addExercise(exerciseId: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            // Check previous workout to autofill weight & reps!
            val workouts = workoutRepository.getWorkouts(uid).firstOrNull() ?: emptyList()
            var autofillSets = listOf(ExerciseSet(weight = 20.0, reps = 10))

            for (w in workouts) {
                val match = w.exercises.find { it.exerciseId == exerciseId }
                if (match != null && match.sets.isNotEmpty()) {
                    autofillSets = match.sets.map { s ->
                        ExerciseSet(weight = s.weight, reps = s.reps)
                    }
                    break
                }
            }

            _uiState.update {
                it.copy(
                    exercises = it.exercises + WorkoutExercise(
                        exerciseId = exerciseId,
                        sets = autofillSets
                    )
                )
            }
            syncActiveSession()
        }
    }

    fun removeExercise(index: Int) {
        _uiState.update { state ->
            val updated = state.exercises.toMutableList().apply { removeAt(index) }
            state.copy(exercises = updated)
        }
        syncActiveSession()
    }

    fun addSet(exerciseIndex: Int) {
        _uiState.update { state ->
            val updated = state.exercises.toMutableList()
            val ex = updated[exerciseIndex]
            val lastSet = ex.sets.lastOrNull()
            val newSet = ExerciseSet(
                weight = lastSet?.weight ?: 20.0,
                reps = lastSet?.reps ?: 10
            )
            updated[exerciseIndex] = ex.copy(sets = ex.sets + newSet)
            state.copy(exercises = updated)
        }
        syncActiveSession()
        startTimer(uiState.value.timerInitialSeconds)
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        _uiState.update { state ->
            val updated = state.exercises.toMutableList()
            val ex = updated[exerciseIndex]
            val setList = ex.sets.toMutableList().apply { removeAt(setIndex) }
            updated[exerciseIndex] = ex.copy(sets = setList)
            state.copy(exercises = updated)
        }
        syncActiveSession()
    }

    fun updateSet(exerciseIndex: Int, setIndex: Int, weight: Double?, reps: Int?) {
        _uiState.update { state ->
            val updated = state.exercises.toMutableList()
            val ex = updated[exerciseIndex]
            val setList = ex.sets.toMutableList()
            val current = setList[setIndex]
            setList[setIndex] = current.copy(
                weight = weight ?: current.weight,
                reps = reps ?: current.reps
            )
            updated[exerciseIndex] = ex.copy(sets = setList)
            state.copy(exercises = updated)
        }
        syncActiveSession()
    }

    fun startTimer(seconds: Int) {
        timerJob?.cancel()
        _uiState.update { it.copy(timerRunning = true, timerSecondsRemaining = seconds, timerInitialSeconds = seconds) }
        timerJob = viewModelScope.launch {
            while (_uiState.value.timerSecondsRemaining > 0) {
                delay(1000)
                _uiState.update { it.copy(timerSecondsRemaining = it.timerSecondsRemaining - 1) }
            }
            _uiState.update { it.copy(timerRunning = false) }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(timerRunning = false, timerSecondsRemaining = 0) }
    }

    fun discardWorkout() {
        activeWorkoutManager.clearSession()
    }

    fun saveWorkout() {
        val uid = currentUserId ?: return
        val state = _uiState.value

        if (state.exercises.isEmpty()) {
            _uiState.update { it.copy(error = "Please add at least one exercise to the workout.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val finalLength = if (state.takeWorkoutTime && state.id == null) {
                maxOf(1, (activeWorkoutManager.elapsedSeconds.value / 60).toInt())
            } else {
                state.length
            }

            val workout = Workout(
                id = state.id ?: "",
                userId = uid,
                date = state.date,
                time = state.time,
                length = finalLength,
                sessionType = state.sessionType,
                intensity = state.intensity,
                maxPulse = state.maxPulse,
                comment = state.comment,
                exerciseIds = state.exercises.map { it.exerciseId },
                exercises = state.exercises
            )

            val result = if (state.id == null) {
                workoutRepository.createWorkout(workout)
            } else {
                workoutRepository.updateWorkout(workout)
            }

            result.fold(
                onSuccess = {
                    activeWorkoutManager.clearSession()
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isSaving = false, error = err.localizedMessage ?: "Failed to save workout") }
                }
            )
        }
    }
}
