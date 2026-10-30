package ch.simibu.sport.presentation.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.simibu.sport.core.model.Exercise
import ch.simibu.sport.core.model.Program
import ch.simibu.sport.core.model.TrainingTemplate
import ch.simibu.sport.domain.repository.AuthRepository
import ch.simibu.sport.domain.repository.ExerciseRepository
import ch.simibu.sport.domain.repository.ProgramRepository
import ch.simibu.sport.domain.repository.TemplateRepository
import ch.simibu.sport.domain.repository.UserProfileRepository
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RoutinesTab {
    TEMPLATES,
    PROGRAMS
}

data class TemplatesUiState(
    val templates: List<TrainingTemplate> = emptyList(),
    val programs: List<Program> = emptyList(),
    val exercisesMap: Map<String, Exercise> = emptyMap(),
    val selectedTab: RoutinesTab = RoutinesTab.TEMPLATES,
    val enableSupersets: Boolean = false,
    val exertionMode: String = "rpe",
    val isLoading: Boolean = true
) {
    val templatesMap: Map<String, TrainingTemplate> by lazy {
        templates.associateBy { it.id }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
    private val programRepository: ProgramRepository,
    private val exerciseRepository: ExerciseRepository,
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val currentUserId: String?
        get() = authRepository.currentUserId

    private val selectedTabState = MutableStateFlow(RoutinesTab.TEMPLATES)

    val uiState: StateFlow<TemplatesUiState> = authRepository.currentUser
        .flatMapLatest { user ->
            val uid = user?.uid
            if (uid == null) {
                flowOf(TemplatesUiState(isLoading = false))
            } else {
                combine(
                    templateRepository.getTemplates(uid),
                    programRepository.getPrograms(uid),
                    exerciseRepository.getExercises(),
                    userProfileRepository.getUserProfile(uid),
                    selectedTabState
                ) { templates, programs, exercises, profile, tab ->
                    val exMap = exercises.associateBy { it.id }
                    TemplatesUiState(
                        templates = templates.sortedByDescending { it.isFavorite },
                        programs = programs.sortedWith(
                            compareByDescending<Program> { it.isActive }
                                .thenByDescending { it.updatedAt ?: it.createdAt ?: "" }
                        ),
                        exercisesMap = exMap,
                        selectedTab = tab,
                        enableSupersets = profile?.settings?.enableSupersets ?: false,
                        exertionMode = profile?.settings?.exertionMode ?: "rpe",
                        isLoading = false
                    )
                }
            }
        }.flowOn(Dispatchers.Default).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TemplatesUiState()
        )

    fun selectTab(tab: RoutinesTab) {
        selectedTabState.update { tab }
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

    fun toggleProgramActive(program: Program) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            val newActiveTarget = if (program.isActive) null else program.id
            programRepository.setActiveProgram(uid, newActiveTarget)
        }
    }

    fun deleteProgram(programId: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            programRepository.deleteProgram(uid, programId)
        }
    }
}
