package ch.simibu.sport.presentation.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.simibu.sport.core.model.Exercise
import ch.simibu.sport.core.model.TrainingTemplate
import ch.simibu.sport.domain.repository.AuthRepository
import ch.simibu.sport.domain.repository.ExerciseRepository
import ch.simibu.sport.domain.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TemplatesUiState(
    val templates: List<TrainingTemplate> = emptyList(),
    val exercisesMap: Map<String, Exercise> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
    private val exerciseRepository: ExerciseRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val currentUserId = authRepository.currentUserId

    val uiState: StateFlow<TemplatesUiState> = if (currentUserId == null) {
        MutableStateFlow(TemplatesUiState(isLoading = false))
    } else {
        combine(
            templateRepository.getTemplates(currentUserId),
            exerciseRepository.getExercises()
        ) { templates, exercises ->
            val exMap = exercises.associateBy { it.id }
            TemplatesUiState(
                templates = templates.sortedByDescending { it.isFavorite },
                exercisesMap = exMap,
                isLoading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TemplatesUiState()
        )
    }

    fun toggleFavorite(template: TrainingTemplate) {
        viewModelScope.launch {
            templateRepository.updateTemplate(template.copy(isFavorite = !template.isFavorite))
        }
    }

    fun deleteTemplate(templateId: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            templateRepository.deleteTemplate(uid, templateId)
        }
    }
}
