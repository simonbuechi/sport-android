package ch.simibu.sport.presentation.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.simibu.sport.core.model.Program
import ch.simibu.sport.core.model.ProgramDay
import ch.simibu.sport.core.model.TrainingTemplate
import ch.simibu.sport.domain.repository.AuthRepository
import ch.simibu.sport.domain.repository.ProgramRepository
import ch.simibu.sport.domain.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class ProgramFormUiState(
    val programId: String? = null,
    val name: String = "",
    val description: String = "",
    val type: String = Program.TYPE_REPETITIVE,
    val useDates: Boolean = false,
    val isActive: Boolean = false,
    val startDate: String? = null,
    val days: List<ProgramDay> = listOf(
        ProgramDay(id = UUID.randomUUID().toString(), dayNumber = 1, templateId = null, note = null)
    ),
    val availableTemplates: List<TrainingTemplate> = emptyList(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProgramFormViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val templateRepository: TemplateRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val currentUserId: String?
        get() = authRepository.currentUserId
    private val _uiState = MutableStateFlow(ProgramFormUiState())
    val uiState: StateFlow<ProgramFormUiState> = _uiState.asStateFlow()

    init {
        loadTemplates()
    }

    private fun loadTemplates() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            templateRepository.getTemplates(uid).collect { templates ->
                _uiState.update { it.copy(availableTemplates = templates.sortedBy { t -> t.name }) }
            }
        }
    }

    fun loadProgram(programId: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            val program = programRepository.getProgram(uid, programId).firstOrNull() ?: return@launch
            val usesDates = program.isOneTime && program.days.any { !it.date.isNullOrBlank() }
            _uiState.update {
                it.copy(
                    programId = program.id,
                    name = program.name,
                    description = program.description ?: "",
                    type = program.type,
                    useDates = usesDates,
                    isActive = program.isActive,
                    startDate = program.startDate,
                    days = if (program.days.isNotEmpty()) program.days else listOf(
                        ProgramDay(id = UUID.randomUUID().toString(), dayNumber = 1, templateId = null, note = null)
                    )
                )
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, error = null) }
    }

    fun onDescriptionChange(desc: String) {
        _uiState.update { it.copy(description = desc) }
    }

    fun onTypeChange(type: String) {
        _uiState.update { state ->
            val updatedDays = if (type == Program.TYPE_REPETITIVE) {
                // If switching back to repetitive, remove dates
                state.days.map { it.copy(date = null) }
            } else {
                state.days
            }
            state.copy(
                type = type,
                useDates = if (type == Program.TYPE_REPETITIVE) false else state.useDates,
                days = updatedDays
            )
        }
    }

    fun onUseDatesChange(useDates: Boolean) {
        _uiState.update { state ->
            val updatedDays = if (useDates) {
                // Pre-fill sequential dates starting from today
                var currentDate = LocalDate.now()
                state.days.map { day ->
                    val dStr = if (!day.date.isNullOrBlank()) {
                        day.date
                    } else {
                        val date = currentDate.toString()
                        currentDate = currentDate.plusDays(1)
                        date
                    }
                    day.copy(date = dStr)
                }
            } else {
                state.days.map { it.copy(date = null) }
            }
            state.copy(useDates = useDates, days = updatedDays)
        }
    }

    fun addDay() {
        _uiState.update { state ->
            val nextNumber = state.days.size + 1
            val nextDate = if (state.useDates) {
                computeNextDate(state.days.lastOrNull()?.date)
            } else null

            val newDay = ProgramDay(
                id = UUID.randomUUID().toString(),
                dayNumber = nextNumber,
                date = nextDate,
                templateId = null,
                note = null
            )
            state.copy(days = state.days + newDay)
        }
    }

    fun removeDay(index: Int) {
        _uiState.update { state ->
            if (state.days.size <= 1) return@update state
            val mutable = state.days.toMutableList()
            if (index in mutable.indices) {
                mutable.removeAt(index)
                // Re-index remaining days
                val renumbered = mutable.mapIndexed { idx, day ->
                    day.copy(dayNumber = idx + 1)
                }
                state.copy(days = renumbered)
            } else state
        }
    }

    fun updateDayTemplate(index: Int, templateId: String?) {
        _uiState.update { state ->
            val updated = state.days.mapIndexed { idx, day ->
                if (idx == index) day.copy(templateId = templateId?.takeIf { it.isNotBlank() }) else day
            }
            state.copy(days = updated)
        }
    }

    fun updateDayDate(index: Int, date: String) {
        _uiState.update { state ->
            val updated = state.days.mapIndexed { idx, day ->
                if (idx == index) day.copy(date = date) else day
            }
            state.copy(days = updated)
        }
    }

    fun updateDayNote(index: Int, note: String) {
        _uiState.update { state ->
            val updated = state.days.mapIndexed { idx, day ->
                if (idx == index) day.copy(note = note.takeIf { it.isNotBlank() }) else day
            }
            state.copy(days = updated)
        }
    }

    fun saveProgram() {
        val uid = currentUserId ?: return
        val state = _uiState.value

        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Program name cannot be empty") }
            return
        }

        if (state.days.isEmpty()) {
            _uiState.update { it.copy(error = "Program must have at least one day") }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            val program = Program(
                id = state.programId ?: "",
                userId = uid,
                name = state.name.trim(),
                description = state.description.trim().takeIf { it.isNotBlank() },
                type = state.type,
                isActive = state.isActive,
                startDate = state.startDate,
                days = state.days
            )

            val result = if (state.programId.isNullOrBlank()) {
                programRepository.createProgram(program).map { program.id }
            } else {
                programRepository.updateProgram(program).map { state.programId }
            }

            if (result.isSuccess) {
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to save program"
                    )
                }
            }
        }
    }

    private fun computeNextDate(lastDateStr: String?): String {
        if (!lastDateStr.isNullOrBlank()) {
            try {
                return LocalDate.parse(lastDateStr.trim()).plusDays(1).toString()
            } catch (_: Exception) {
                // Fallback to today
            }
        }
        return LocalDate.now().toString()
    }
}
