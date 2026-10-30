package ch.simibu.sport.presentation.journal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import ch.simibu.sport.data.repository.formatElapsedTime
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.simibu.sport.R
import ch.simibu.sport.core.designsystem.ExerciseIcon
import ch.simibu.sport.core.designsystem.SportPurple
import ch.simibu.sport.core.designsystem.SportRuby
import ch.simibu.sport.core.designsystem.TactileButton
import ch.simibu.sport.core.designsystem.TactileCard
import ch.simibu.sport.core.designsystem.TactileChip
import ch.simibu.sport.core.designsystem.tactileClickable
import ch.simibu.sport.core.model.Exercise
import ch.simibu.sport.core.model.ExerciseType
import ch.simibu.sport.core.model.WorkoutExercise
import ch.simibu.sport.core.util.ExertionUtils
import ch.simibu.sport.core.util.SupersetGroup
import ch.simibu.sport.core.util.SupersetItemWrapper
import ch.simibu.sport.core.util.SupersetUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutFormScreen(
    workoutId: String? = null,
    templateId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: WorkoutFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showExercisePicker by remember { mutableStateOf(false) }
    var exerciseSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(workoutId, templateId) {
        viewModel.initWorkout(workoutId, templateId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    var showDiscardDialog by remember { mutableStateOf(false) }

    var editingSupersetId by remember { mutableStateOf<String?>(null) }
    var tempSupersetNote by remember { mutableStateOf("") }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.dialog_discard_workout_title)) },
            text = { Text(stringResource(R.string.dialog_discard_workout_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.discardWorkout()
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.btn_discard_workout), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (uiState.id != null) R.string.workout_form_title_edit else R.string.workout_form_title_new)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_cancel))
                    }
                },
                actions = {
                    if (uiState.id == null) {
                        IconButton(onClick = { showDiscardDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.btn_discard_workout),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    TactileButton(
                        onClick = viewModel::saveWorkout,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Text(stringResource(R.string.btn_save), color = Color.White, fontWeight = FontWeight.Bold)
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
            // Running Workout Timer Bar
            AnimatedVisibility(visible = uiState.takeWorkoutTime && uiState.id == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.workout_timer_elapsed),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = formatElapsedTime(uiState.elapsedWorkoutSeconds),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            // Rest Timer Active Bar
            AnimatedVisibility(visible = uiState.timerRunning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.timer_rest, uiState.timerSecondsRemaining),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = viewModel::stopTimer, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.timer_stop), modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val progress = if (uiState.timerInitialSeconds > 0) {
                            uiState.timerSecondsRemaining.toFloat() / uiState.timerInitialSeconds.toFloat()
                        } else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Workout Overview Card
                item {
                    TactileCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = uiState.date,
                                    onValueChange = viewModel::onDateChange,
                                    label = { Text("Date (YYYY-MM-DD)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = "${uiState.length}",
                                    onValueChange = { str -> str.toIntOrNull()?.let { viewModel.onLengthChange(it) } },
                                    label = { Text("${stringResource(R.string.metric_duration)} (${stringResource(R.string.dashboard_min)})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = uiState.comment,
                                onValueChange = viewModel::onCommentChange,
                                label = { Text(stringResource(R.string.workout_notes_placeholder)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Error Message if any
                if (uiState.error != null) {
                    item {
                        Text(
                            text = uiState.error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                // List of Exercises in active workout (grouped into supersets if enabled)
                val exerciseBlocks = if (uiState.enableSupersets) {
                    SupersetUtils.groupWorkoutExercises(uiState.exercises)
                } else {
                    uiState.exercises.mapIndexed { idx, ex ->
                        SupersetGroup(
                            isSuperset = false,
                            items = listOf(SupersetItemWrapper(ex, idx))
                        )
                    }
                }

                exerciseBlocks.forEachIndexed { blockIdx, block ->
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
                                // Superset header
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
                                        WorkoutExerciseCard(
                                            exIndex = itemWrapper.originalIndex,
                                            workoutEx = itemWrapper.item,
                                            supersetTag = itemWrapper.tag,
                                            isInsideSuperset = true,
                                            enableSupersets = uiState.enableSupersets,
                                            enableExertionTracking = uiState.enableExertionTracking,
                                            exertionMode = uiState.exertionMode,
                                            availableExercises = uiState.availableExercises,
                                            onUpdateSet = { sIdx, w, r, rpe, rir, updateExertion ->
                                                viewModel.updateSet(itemWrapper.originalIndex, sIdx, w, r, rpe, rir, updateExertion)
                                            },
                                            onAddSet = { viewModel.addSet(itemWrapper.originalIndex) },
                                            onRemoveSet = { sIdx -> viewModel.removeSet(itemWrapper.originalIndex, sIdx) },
                                            onRemoveExercise = { viewModel.removeExercise(itemWrapper.originalIndex) },
                                            onUnlinkSuperset = { viewModel.unlinkSuperset(itemWrapper.originalIndex) },
                                            onLinkWithNext = null
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        item(key = "exercise_${block.items[0].originalIndex}") {
                            val itemWrapper = block.items[0]
                            WorkoutExerciseCard(
                                exIndex = itemWrapper.originalIndex,
                                workoutEx = itemWrapper.item,
                                supersetTag = null,
                                isInsideSuperset = false,
                                enableSupersets = uiState.enableSupersets,
                                enableExertionTracking = uiState.enableExertionTracking,
                                exertionMode = uiState.exertionMode,
                                availableExercises = uiState.availableExercises,
                                onUpdateSet = { sIdx, w, r, rpe, rir, updateExertion ->
                                    viewModel.updateSet(itemWrapper.originalIndex, sIdx, w, r, rpe, rir, updateExertion)
                                },
                                onAddSet = { viewModel.addSet(itemWrapper.originalIndex) },
                                onRemoveSet = { sIdx -> viewModel.removeSet(itemWrapper.originalIndex, sIdx) },
                                onRemoveExercise = { viewModel.removeExercise(itemWrapper.originalIndex) },
                                onUnlinkSuperset = null,
                                onLinkWithNext = if (uiState.enableSupersets && itemWrapper.originalIndex < uiState.exercises.size - 1) {
                                    { viewModel.linkWithNext(itemWrapper.originalIndex) }
                                } else null
                            )
                        }
                    }
                }

                // Add Exercise CTA
                item {
                    TactileCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showExercisePicker = true }
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
                            Text(
                                stringResource(R.string.btn_add_exercise),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Bottom spacer
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }

        // Exercise Picker Bottom Sheet
        if (showExercisePicker) {
            ch.simibu.sport.core.designsystem.ExercisePickerSheet(
                availableExercises = uiState.availableExercises,
                searchQuery = exerciseSearchQuery,
                onSearchQueryChange = { exerciseSearchQuery = it },
                onExerciseSelected = { ex ->
                    viewModel.addExercise(ex.id)
                    showExercisePicker = false
                    exerciseSearchQuery = ""
                },
                onDismissRequest = { showExercisePicker = false }
            )
        }
    }
}

@Composable
private fun WorkoutExerciseCard(
    exIndex: Int,
    workoutEx: WorkoutExercise,
    supersetTag: String?,
    isInsideSuperset: Boolean,
    enableSupersets: Boolean,
    enableExertionTracking: Boolean,
    exertionMode: String,
    availableExercises: List<Exercise>,
    onUpdateSet: (setIndex: Int, weight: Double?, reps: Int?, rpe: Double?, rir: Double?, updateExertion: Boolean) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (setIndex: Int) -> Unit,
    onRemoveExercise: () -> Unit,
    onUnlinkSuperset: (() -> Unit)?,
    onLinkWithNext: (() -> Unit)?
) {
    val exInfo = availableExercises.find { it.id == workoutEx.exerciseId }
    val exerciseName = exInfo?.name ?: "Exercise"

    TactileCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (supersetTag != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = supersetTag,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                ExerciseIcon(
                    iconUrl = exInfo?.iconUrl,
                    exerciseName = exerciseName,
                    size = 38.dp,
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (exInfo != null) {
                        Text(
                            text = "${stringResource(exInfo.bodypart.labelRes)} • ${stringResource(exInfo.category.labelRes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isInsideSuperset && onUnlinkSuperset != null) {
                    IconButton(onClick = onUnlinkSuperset, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.LinkOff,
                            contentDescription = stringResource(R.string.supersets_unlink),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(onClick = onRemoveExercise, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove Exercise",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (!isInsideSuperset && onLinkWithNext != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .tactileClickable { onLinkWithNext() }
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

            Spacer(modifier = Modifier.height(10.dp))

            // Sets header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.unit_sets).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${stringResource(R.string.metric_weight)} (${stringResource(R.string.unit_kg)})".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.unit_reps).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (enableExertionTracking) {
                    Text(
                        (if (exertionMode == ExertionUtils.MODE_RPE) stringResource(R.string.exertion_mode_rpe) else stringResource(R.string.exertion_mode_rir)).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(28.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Set rows
            workoutEx.sets.forEachIndexed { setIndex, set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Set Number badge
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${setIndex + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Weight quick editor
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val w = (set.weight ?: 20.0) - 2.5
                                onUpdateSet(setIndex, if (w >= 0) w else 0.0, null, null, null, false)
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "-2.5kg", modifier = Modifier.size(15.dp))
                        }

                        Text(
                            text = "${set.weight ?: 0.0}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        IconButton(
                            onClick = {
                                val w = (set.weight ?: 20.0) + 2.5
                                onUpdateSet(setIndex, w, null, null, null, false)
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "+2.5kg", modifier = Modifier.size(15.dp))
                        }
                    }

                    // Reps quick editor
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val r = (set.reps ?: 10) - 1
                                onUpdateSet(setIndex, null, if (r >= 0) r else 0, null, null, false)
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "-1 rep", modifier = Modifier.size(15.dp))
                        }

                        Text(
                            text = "${set.reps ?: 0}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = {
                                val r = (set.reps ?: 10) + 1
                                onUpdateSet(setIndex, null, r, null, null, false)
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "+1 rep", modifier = Modifier.size(15.dp))
                        }
                    }

                    // Exertion editor (RPE / RIR)
                    if (enableExertionTracking) {
                        val displayedVal = ExertionUtils.getDisplayedExertion(set.rpe, set.rir, exertionMode)
                        val textVal = when {
                            displayedVal == null -> "—"
                            displayedVal % 1.0 == 0.0 -> displayedVal.toInt().toString()
                            else -> displayedVal.toString()
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val current = displayedVal ?: if (exertionMode == ExertionUtils.MODE_RPE) 8.0 else 2.0
                                    val nextVal = maxOf(0.0, current - 0.5)
                                    val calc = ExertionUtils.calculateExertionValues(nextVal, exertionMode)
                                    onUpdateSet(setIndex, null, null, calc.first, calc.second, true)
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "-0.5", modifier = Modifier.size(15.dp))
                            }

                            Text(
                                text = textVal,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        val nextVal = if (displayedVal == null) {
                                            if (exertionMode == ExertionUtils.MODE_RPE) 8.0 else 2.0
                                        } else if (displayedVal >= (if (exertionMode == ExertionUtils.MODE_RPE) 10.0 else 5.0)) {
                                            null
                                        } else {
                                            displayedVal + 0.5
                                        }
                                        val calc = ExertionUtils.calculateExertionValues(nextVal, exertionMode)
                                        onUpdateSet(setIndex, null, null, calc.first, calc.second, true)
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )

                            IconButton(
                                onClick = {
                                    val current = displayedVal ?: if (exertionMode == ExertionUtils.MODE_RPE) 7.5 else 1.5
                                    val maxLimit = if (exertionMode == ExertionUtils.MODE_RPE) 10.0 else 10.0
                                    val nextVal = minOf(maxLimit, current + 0.5)
                                    val calc = ExertionUtils.calculateExertionValues(nextVal, exertionMode)
                                    onUpdateSet(setIndex, null, null, calc.first, calc.second, true)
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "+0.5", modifier = Modifier.size(15.dp))
                            }
                        }
                    }

                    // Remove set
                    IconButton(
                        onClick = { onRemoveSet(setIndex) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Delete Set", modifier = Modifier.size(15.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Add Set Button with Tactile haptics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .tactileClickable { onAddSet() }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        stringResource(R.string.btn_add_set),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
