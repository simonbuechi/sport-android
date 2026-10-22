package ch.simibu.sport.presentation.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.simibu.sport.core.model.BodyPart
import ch.simibu.sport.core.model.Exercise
import ch.simibu.sport.core.model.ExerciseCategory
import ch.simibu.sport.core.model.ExerciseType
import ch.simibu.sport.core.model.MarkedStatus
import ch.simibu.sport.domain.repository.AuthRepository
import ch.simibu.sport.domain.repository.ExerciseRepository
import ch.simibu.sport.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExercisesUiState(
    val searchQuery: String = "",
    val selectedType: ExerciseType? = null,
    val selectedBodyPart: BodyPart? = null,
    val selectedCategory: ExerciseCategory? = null,
    val showOnlyFavorites: Boolean = false,
    val exercises: List<Exercise> = emptyList(),
    val totalCount: Int = 0,
    val markedExercises: Map<String, MarkedStatus> = emptyMap(),
    val isLoading: Boolean = true
) {
    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() || selectedType != null || selectedBodyPart != null || selectedCategory != null || showOnlyFavorites

    val activeFilterCount: Int
        get() = (if (selectedType != null) 1 else 0) +
                (if (selectedBodyPart != null) 1 else 0) +
                (if (selectedCategory != null) 1 else 0) +
                (if (showOnlyFavorites) 1 else 0) +
                (if (searchQuery.isNotBlank()) 1 else 0)
}

@HiltViewModel
class ExercisesViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val userProfileRepository: UserProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val currentUserId = authRepository.currentUserId

    private val _searchQuery = MutableStateFlow("")
    private val _selectedType = MutableStateFlow<ExerciseType?>(null)
    private val _selectedBodyPart = MutableStateFlow<BodyPart?>(null)
    private val _selectedCategory = MutableStateFlow<ExerciseCategory?>(null)
    private val _showOnlyFavorites = MutableStateFlow(false)

    private data class FilterCriteria(
        val query: String,
        val type: ExerciseType?,
        val bodyPart: BodyPart?,
        val category: ExerciseCategory?,
        val onlyFavorites: Boolean
    )

    private val filtersFlow = combine(
        _searchQuery,
        _selectedType,
        _selectedBodyPart,
        _selectedCategory,
        _showOnlyFavorites
    ) { query, type, bodyPart, category, onlyFavorites ->
        FilterCriteria(query, type, bodyPart, category, onlyFavorites)
    }

    val uiState: StateFlow<ExercisesUiState> = combine(
        exerciseRepository.getExercises(),
        if (currentUserId != null) userProfileRepository.getUserProfile(currentUserId) else kotlinx.coroutines.flow.flowOf(null),
        filtersFlow
    ) { exercises, userProfile, filters ->
        val marked = userProfile?.markedExercises ?: emptyMap()

        val filtered = exercises
            .filter { ex ->
                val matchesQuery = filters.query.isBlank() ||
                        ex.name.contains(filters.query, ignoreCase = true) ||
                        ex.aliases.any { it.contains(filters.query, ignoreCase = true) }
                val matchesType = filters.type == null || ex.type == filters.type
                val matchesBodyPart = filters.bodyPart == null || ex.bodypart == filters.bodyPart
                val matchesCategory = filters.category == null || ex.category == filters.category
                val isFav = marked[ex.id]?.favorite == true
                val matchesFav = !filters.onlyFavorites || isFav

                matchesQuery && matchesType && matchesBodyPart && matchesCategory && matchesFav
            }
            .sortedWith(
                compareByDescending<Exercise> { it.popular }
                    .thenBy { it.name.lowercase() }
            )

        ExercisesUiState(
            searchQuery = filters.query,
            selectedType = filters.type,
            selectedBodyPart = filters.bodyPart,
            selectedCategory = filters.category,
            showOnlyFavorites = filters.onlyFavorites,
            exercises = filtered,
            totalCount = exercises.size,
            markedExercises = marked,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExercisesUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun selectType(type: ExerciseType?) {
        _selectedType.value = if (_selectedType.value == type) null else type
    }

    fun selectBodyPart(bodyPart: BodyPart?) {
        _selectedBodyPart.value = if (_selectedBodyPart.value == bodyPart) null else bodyPart
    }

    fun selectCategory(category: ExerciseCategory?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun toggleFavoritesFilter() {
        _showOnlyFavorites.update { !it }
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _selectedType.value = null
        _selectedBodyPart.value = null
        _selectedCategory.value = null
        _showOnlyFavorites.value = false
    }

    fun toggleFavorite(exerciseId: String) {
        val uid = currentUserId ?: return
        val currentFav = uiState.value.markedExercises[exerciseId]?.favorite == true
        viewModelScope.launch {
            userProfileRepository.updateExerciseStatus(
                userId = uid,
                exerciseId = exerciseId,
                favorite = !currentFav,
                notes = null
            )
        }
    }
}
