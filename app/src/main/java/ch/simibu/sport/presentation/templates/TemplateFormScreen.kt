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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
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
import ch.simibu.sport.core.util.SupersetGroup
import ch.simibu.sport.core.util.SupersetItemWrapper
import ch.simibu.sport.core.util.SupersetUtils
import ch.simibu.sport.domain.repository.AuthRepository
import ch.simibu.sport.domain.repository.ExerciseRepository
import ch.simibu.sport.domain.repository.TemplateRepository
import ch.simibu.sport.domain.repository.UserProfileRepository
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
    val enableSupersets: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TemplateFormViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
    private val exerciseRepository: ExerciseRepository,
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val currentUserId: String?
        get() = authRepository.currentUserId

    private val _uiState = MutableStateFlow(TemplateFormUiState())
    val uiState: StateFlow<TemplateFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            exerciseRepository.getExercises().collect { list ->
                _uiState.update { it.copy(availableExercises = list) }
            }
        }
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                val uid = user?.uid ?: return@collect
                userProfileRepository.getUserProfile(uid).collect { profile ->
                    if (profile != null) {
                        _uiState.update {
                            it.copy(enableSupersets = profile.settings.enableSupersets)
                        }
                    }
                }
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
            val cleaned = SupersetUtils.unlinkTemplateFromSuperset(updated, index.coerceAtMost(updated.size - 1))
            state.copy(exercises = cleaned)
        }
    }

    fun linkWithNext(index: Int) {
        _uiState.update { state ->
            state.copy(exercises = SupersetUtils.linkTemplateWithNext(state.exercises, index))
        }
    }

    fun unlinkSuperset(index: Int) {
        _uiState.update { state ->
            state.copy(exercises = SupersetUtils.unlinkTemplateFromSuperset(state.exercises, index))
        }
    }

    fun updateSupersetNote(supersetId: String, note: String) {
        _uiState.update { state ->
            state.copy(exercises = SupersetUtils.updateTemplateSupersetNote(state.exercises, supersetId, note))
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
    var editingSupersetId by remember { mutableStateOf<String?>(null) }
    var tempSupersetNote by remember { mutableStateOf("") }

    if (editingSupersetId != null) {
        val currentSid = editingSupersetId!!
        AlertDialog(
            onDismissRequest = { editingSupersetId = null },
            title = { Text(stringResource(R.string.supersets_note_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = tempSupersetNote,
                    onValueChange = { tempSupersetNote = it },
                    placeholder = { Text(stringResource(R.string.supersets_note_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateSupersetNote(currentSid, tempSupersetNote)
                        editingSupersetId = null
                    }
                ) {
                    Text(stringResource(R.string.btn_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSupersetId = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    androidx.compose.runtime.LaunchedEffect(templateId) {
        if (!templateId.isNullOrBlank()) {
            viewModel.loadTemplate(templateId)
        }
    }

    androidx.compose.runtime.LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
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

                val templateBlocks = if (uiState.enableSupersets) {
                    SupersetUtils.groupTemplateExercises(uiState.exercises)
                } else {
                    uiState.exercises.mapIndexed { idx, ex ->
                        SupersetGroup(
                            isSuperset = false,
                            items = listOf(SupersetItemWrapper(ex, idx))
                        )
                    }
                }

                templateBlocks.forEachIndexed { blockIdx, block ->
                    if (block.isSuperset) {
                        item(key = "superset_${block.supersetId ?: blockIdx}") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f))
                                    .border(
                                        width = 1.5.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.supersets_title, block.letter ?: "A"),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (!block.note.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "\"${block.note}\"",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                modifier = Modifier.clickable {
                                                    editingSupersetId = block.supersetId
                                                    tempSupersetNote = block.note ?: ""
                                                }
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                editingSupersetId = block.supersetId
                                                tempSupersetNote = block.note ?: ""
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.EditNote,
                                                contentDescription = stringResource(if (block.note.isNullOrBlank()) R.string.supersets_add_note else R.string.supersets_edit_note),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                block.items.forEach { item ->
                                                    viewModel.unlinkSuperset(item.originalIndex)
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LinkOff,
                                                contentDescription = stringResource(R.string.supersets_unlink),
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    block.items.forEach { itemWrapper ->
                                        val exInfo = uiState.availableExercises.find { it.id == itemWrapper.item.exerciseId }
                                        val name = exInfo?.name ?: "Exercise"
                                        TactileCard(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    if (itemWrapper.tag != null) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(MaterialTheme.colorScheme.primary)
                                                                .padding(horizontal = 7.dp, vertical = 3.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = itemWrapper.tag,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onPrimary
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                    }
                                                    Column {
                                                        Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                        Text("${itemWrapper.item.sets.size} target sets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(onClick = { viewModel.unlinkSuperset(itemWrapper.originalIndex) }) {
                                                        Icon(Icons.Default.LinkOff, contentDescription = stringResource(R.string.supersets_unlink), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    IconButton(onClick = { viewModel.removeExercise(itemWrapper.originalIndex) }) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item(key = "exercise_${block.items[0].originalIndex}") {
                            val itemWrapper = block.items[0]
                            val exInfo = uiState.availableExercises.find { it.id == itemWrapper.item.exerciseId }
                            val name = exInfo?.name ?: "Exercise"
                            TactileCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("${itemWrapper.item.sets.size} target sets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(onClick = { viewModel.removeExercise(itemWrapper.originalIndex) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }

                                    if (uiState.enableSupersets && itemWrapper.originalIndex < uiState.exercises.size - 1) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                    .tactileClickable { viewModel.linkWithNext(itemWrapper.originalIndex) }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Link,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = stringResource(R.string.supersets_link_with_next),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
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
            ch.simibu.sport.core.designsystem.ExercisePickerSheet(
                availableExercises = uiState.availableExercises,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onExerciseSelected = { ex ->
                    viewModel.addExercise(ex.id)
                    showPicker = false
                    searchQuery = ""
                },
                onDismissRequest = { showPicker = false }
            )
        }
    }
}
