package ch.simibu.sport.presentation.navigation

import androidx.lifecycle.ViewModel
import ch.simibu.sport.data.repository.ActiveWorkoutManager
import ch.simibu.sport.data.repository.ActiveWorkoutSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    val activeWorkoutManager: ActiveWorkoutManager
) : ViewModel() {
    val activeSession: StateFlow<ActiveWorkoutSession?> = activeWorkoutManager.activeSession
    val elapsedSeconds: StateFlow<Long> = activeWorkoutManager.elapsedSeconds

    fun discardWorkout() {
        activeWorkoutManager.clearSession()
    }
}
