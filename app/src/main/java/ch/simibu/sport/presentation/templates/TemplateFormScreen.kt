package ch.simibu.sport.presentation.templates

import androidx.compose.foundation.background
import ch.simibu.sport.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.simibu.sport.core.designsystem.TactileButton
import ch.simibu.sport.core.designsystem.TactileCard
import ch.simibu.sport.core.designsystem.tactileClickable
import ch.simibu.sport.core.model.Exercise
import ch.simibu.sport.core.model.ExerciseSet
import ch.simibu.sport.core.model.TemplateExercise
import ch.simibu.sport.core.model.TrainingTemplate
import ch.simibu.sport.domain.repository.AuthRepository
import ch.simibu.sport.domain.repository.ExerciseRepository
import ch.simibu.sport.domain.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TemplateFormUiState(
    val templateId: String? = null,
    val isFavorite: Boolean = false,
    val name: String = "",
    val notes: String = "",
    val exercises: List<TemplateExercise> = emptyList(),
    val availableExercises: List<Exercise> = emptyList(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TemplateFormViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
    private val exerciseRepository: ExerciseRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val currentUserId = authRepository.currentUserId

    private val _uiState = MutableStateFlow(TemplateFormUiState())
    val uiState: StateFlow<TemplateFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            exerciseRepository.getExercises().collect { list ->
                _uiState.update { it.copy(availableExercises = list) }
            }
        }
    }

    fun loadTemplate(templateId: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            templateRepository.getTemplate(uid, templateId).collect { template ->
                if (template != null) {
                    _uiState.update {
                        it.copy(
                            templateId = template.id,
                            isFavorite = template.isFavorite,
                            name = template.name,
                            notes = template.notes ?: "",
                            exercises = template.exercises
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name) }
    fun onNotesChange(notes: String) = _uiState.update { it.copy(notes = notes) }

    fun addExercise(exerciseId: String) {
        val newEx = TemplateExercise(
            exerciseId = exerciseId,
            sets = listOf(
                ExerciseSet(weight = 20.0, reps = 10),
                ExerciseSet(weight = 20.0, reps = 10),
                ExerciseSet(weight = 20.0, reps = 10)
            )
        )
        _uiState.update { it.copy(exercises = it.exercises + newEx) }
    }

    fun removeExercise(index: Int) {
        _uiState.update { state ->
            val updated = state.exercises.toMutableList().apply { removeAt(index) }
            state.copy(exercises = updated)
        }
    }

    fun save() {
        val uid = currentUserId ?: return
        val state = _uiState.value

        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a routine name.") }
            return
        }

        if (state.exercises.isEmpty()) {
            _uiState.update { it.copy(error = "Please add at least one exercise.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val template = TrainingTemplate(
                id = state.templateId ?: "",
                userId = uid,
                name = state.name.trim(),
                notes = state.notes.ifBlank { null },
                isFavorite = state.isFavorite,
                isArchived = false,
                exercises = state.exercises
            )

            val result = if (!state.templateId.isNullOrBlank()) {
                templateRepository.updateTemplate(template).map { template.id }
            } else {
                templateRepository.createTemplate(template)
            }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isSaving = false, error = err.localizedMessage ?: "Failed to save") }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateFormScreen(
    templateId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: TemplateFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showPicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(templateId) {
        if (!templateId.isNullOrBlank()) {
            viewModel.loadTemplate(templateId)
        }
    }

    if (uiState.isSaved) {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (!uiState.templateId.isNullOrBlank()) {
                            androidx.compose.ui.res.stringResource(R.string.edit_routine)
                        } else {
                            androidx.compose.ui.res.stringResource(R.string.new_routine)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TactileButton(
                        onClick = viewModel::save,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Text(androidx.compose.ui.res.stringResource(R.string.btn_save), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    TactileCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = uiState.name,
                                onValueChange = viewModel::onNameChange,
                                label = { Text("Routine Name *") },
                                placeholder = { Text("e.g. Push Day A") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = uiState.notes,
                                onValueChange = viewModel::onNotesChange,
                                label = { Text("Notes / Target muscle focus") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                if (uiState.error != null) {
                    item {
                        Text(
                            text = uiState.error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                item {
                    Text(
                        text = "Exercises in Routine (${uiState.exercises.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                itemsIndexed(uiState.exercises) { idx, te ->
                    val exInfo = uiState.availableExercises.find { it.id == te.exerciseId }
                    val name = exInfo?.name ?: "Exercise"

                    TactileCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${te.sets.size} target sets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.removeExercise(idx) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                item {
                    TactileCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showPicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Exercise to Routine", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        if (showPicker) {
            ModalBottomSheet(
                onDismissRequest = { showPicker = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text("Select Exercise", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search exercises...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val filtered = uiState.availableExercises.filter {
                        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
                    }

                    LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filtered) { ex ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .tactileClickable {
                                        viewModel.addExercise(ex.id)
                                        showPicker = false
                                        searchQuery = ""
                                    }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(ex.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
