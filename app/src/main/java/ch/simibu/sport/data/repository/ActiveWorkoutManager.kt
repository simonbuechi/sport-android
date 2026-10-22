package ch.simibu.sport.data.repository

import ch.simibu.sport.core.model.ExerciseType
import ch.simibu.sport.core.model.WorkoutExercise
import ch.simibu.sport.presentation.journal.WorkoutFormUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ActiveWorkoutDraft(
    val startTimeMs: Long,
    val templateId: String? = null,
    val templateName: String? = null,
    val takeWorkoutTime: Boolean = true,
    val workoutId: String? = null,
    val date: String = "",
    val time: String = "",
    val length: Int = 45,
    val sessionType: ExerciseType = ExerciseType.STRENGTH,
    val intensity: Int = 3,
    val maxPulse: Int? = null,
    val comment: String = "",
    val exercises: List<WorkoutExercise> = emptyList()
)

data class ActiveWorkoutSession(
    val startTimeMs: Long,
    val templateId: String? = null,
    val templateName: String? = null,
    val takeWorkoutTime: Boolean = true,
    val state: WorkoutFormUiState
)

fun formatElapsedTime(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

@Singleton
class ActiveWorkoutManager @Inject constructor(
    private val draftRepository: DraftRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val json = Json { ignoreUnknownKeys = true }

    private val _activeSession = MutableStateFlow<ActiveWorkoutSession?>(null)
    val activeSession: StateFlow<ActiveWorkoutSession?> = _activeSession.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow<Long>(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private var tickerJob: Job? = null

    init {
        scope.launch {
            try {
                val draftJson = draftRepository.getWorkoutDraft().firstOrNull()
                if (!draftJson.isNullOrBlank()) {
                    val draft = json.decodeFromString<ActiveWorkoutDraft>(draftJson)
                    val restoredState = WorkoutFormUiState(
                        id = draft.workoutId,
                        date = draft.date,
                        time = draft.time,
                        length = draft.length,
                        sessionType = draft.sessionType,
                        intensity = draft.intensity,
                        maxPulse = draft.maxPulse,
                        comment = draft.comment,
                        exercises = draft.exercises,
                        takeWorkoutTime = draft.takeWorkoutTime
                    )
                    _activeSession.value = ActiveWorkoutSession(
                        startTimeMs = draft.startTimeMs,
                        templateId = draft.templateId,
                        templateName = draft.templateName,
                        takeWorkoutTime = draft.takeWorkoutTime,
                        state = restoredState
                    )
                    startTicker(draft.startTimeMs)
                }
            } catch (e: Exception) {
                // Ignore corrupted draft
            }
        }
    }

    fun startSession(
        templateId: String?,
        templateName: String?,
        takeWorkoutTime: Boolean,
        initialState: WorkoutFormUiState
    ) {
        val startTime = System.currentTimeMillis()
        val session = ActiveWorkoutSession(
            startTimeMs = startTime,
            templateId = templateId,
            templateName = templateName,
            takeWorkoutTime = takeWorkoutTime,
            state = initialState.copy(takeWorkoutTime = takeWorkoutTime)
        )
        _activeSession.value = session
        persistDraft(session)
        startTicker(startTime)
    }

    fun updateState(state: WorkoutFormUiState) {
        val current = _activeSession.value ?: return
        val updated = current.copy(state = state)
        _activeSession.value = updated
        persistDraft(updated)
    }

    private fun persistDraft(session: ActiveWorkoutSession) {
        scope.launch {
            try {
                val draft = ActiveWorkoutDraft(
                    startTimeMs = session.startTimeMs,
                    templateId = session.templateId,
                    templateName = session.templateName,
                    takeWorkoutTime = session.takeWorkoutTime,
                    workoutId = session.state.id,
                    date = session.state.date,
                    time = session.state.time,
                    length = session.state.length,
                    sessionType = session.state.sessionType,
                    intensity = session.state.intensity,
                    maxPulse = session.state.maxPulse,
                    comment = session.state.comment,
                    exercises = session.state.exercises
                )
                draftRepository.saveWorkoutDraft(json.encodeToString(draft))
            } catch (e: Exception) {
                // Ignore error saving draft
            }
        }
    }

    private fun startTicker(startTimeMs: Long) {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && _activeSession.value != null) {
                val elapsed = maxOf(0L, (System.currentTimeMillis() - startTimeMs) / 1000)
                _elapsedSeconds.value = elapsed
                delay(1000)
            }
        }
    }

    fun clearSession() {
        tickerJob?.cancel()
        tickerJob = null
        _activeSession.value = null
        _elapsedSeconds.value = 0L
        scope.launch {
            draftRepository.clearWorkoutDraft()
        }
    }
}
