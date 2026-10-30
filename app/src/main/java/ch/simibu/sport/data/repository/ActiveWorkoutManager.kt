package ch.simibu.sport.data.repository

import ch.simibu.sport.core.model.ExerciseType
import ch.simibu.sport.core.model.WorkoutExercise
import ch.simibu.sport.data.notification.WorkoutNotificationManager
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
    private val draftRepository: DraftRepository,
    private val workoutNotificationManager: WorkoutNotificationManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    private val _activeSession = MutableStateFlow<ActiveWorkoutSession?>(null)
    val activeSession: StateFlow<ActiveWorkoutSession?> = _activeSession.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow<Long>(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private var tickerJob: Job? = null
    private var persistJob: Job? = null

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
                        maxPulse = draft.maxPulse,
                        comment = draft.comment,
                        exercises = draft.exercises,
                        takeWorkoutTime = draft.takeWorkoutTime
                    )
                    val session = ActiveWorkoutSession(
                        startTimeMs = draft.startTimeMs,
                        templateId = draft.templateId,
                        templateName = draft.templateName,
                        takeWorkoutTime = draft.takeWorkoutTime,
                        state = restoredState
                    )
                    _activeSession.value = session
                    val initialElapsed = maxOf(0L, (System.currentTimeMillis() - draft.startTimeMs) / 1000)
                    _elapsedSeconds.value = initialElapsed
                    workoutNotificationManager.updateNotification(session, initialElapsed)
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
        workoutNotificationManager.updateNotification(session, 0L)
        persistDraft(session)
        startTicker(startTime)
    }

    fun updateState(state: WorkoutFormUiState) {
        val current = _activeSession.value ?: return
        val updated = current.copy(state = state)
        _activeSession.value = updated
        workoutNotificationManager.updateNotification(updated, _elapsedSeconds.value)
        persistDraft(updated)
    }

    private fun persistDraft(session: ActiveWorkoutSession, immediate: Boolean = false) {
        persistJob?.cancel()
        persistJob = ioScope.launch {
            if (!immediate) {
                delay(400L)
            }
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
                val session = _activeSession.value
                if (session != null) {
                    workoutNotificationManager.updateNotification(session, elapsed)
                }
                delay(1000)
            }
        }
    }

    fun clearSession() {
        workoutNotificationManager.cancelNotification()
        persistJob?.cancel()
        persistJob = null
        tickerJob?.cancel()
        tickerJob = null
        _activeSession.value = null
        _elapsedSeconds.value = 0L
        ioScope.launch {
            draftRepository.clearWorkoutDraft()
        }
    }
}
