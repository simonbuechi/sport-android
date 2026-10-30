package ch.simibu.sport.presentation.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.simibu.sport.core.model.Exercise
import ch.simibu.sport.core.model.ExerciseType
import ch.simibu.sport.core.model.Workout
import ch.simibu.sport.domain.repository.AuthRepository
import ch.simibu.sport.domain.repository.ExerciseRepository
import ch.simibu.sport.domain.repository.WorkoutRepository
import ch.simibu.sport.domain.usecase.WorkoutCalculationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JournalUiState(
    val workouts: List<Workout> = emptyList(),
    val exercisesMap: Map<String, Exercise> = emptyMap(),
    val selectedTypeFilter: ExerciseType? = null,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class JournalViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val authRepository: AuthRepository,
    val calculationUseCase: WorkoutCalculationUseCase
) : ViewModel() {

    private val currentUserId: String?
        get() = authRepository.currentUserId

    private val _selectedTypeFilter = MutableStateFlow<ExerciseType?>(null)

    val uiState: StateFlow<JournalUiState> = authRepository.currentUser
        .flatMapLatest { user ->
            val uid = user?.uid
            if (uid == null) {
                flowOf(JournalUiState(isLoading = false))
            } else {
                combine(
                    workoutRepository.getWorkouts(uid),
                    exerciseRepository.getExercises(),
                    _selectedTypeFilter
                ) { workouts, exercises, filter ->
                    val exMap = exercises.associateBy { it.id }
                    val filteredWorkouts = if (filter == null) {
                        workouts
                    } else {
                        workouts.filter { it.sessionType == filter }
                    }

                    JournalUiState(
                        workouts = filteredWorkouts,
                        exercisesMap = exMap,
                        selectedTypeFilter = filter,
                        isLoading = false
                    )
                }
            }
        }.flowOn(Dispatchers.Default).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = JournalUiState()
        )

    fun selectFilter(type: ExerciseType?) {
        _selectedTypeFilter.value = if (_selectedTypeFilter.value == type) null else type
    }

    fun deleteWorkout(workoutId: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            workoutRepository.deleteWorkout(uid, workoutId)
        }
    }
}
