package ch.simibu.sport.presentation.templates

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.simibu.sport.R
import ch.simibu.sport.core.designsystem.AccentGoldLight
import ch.simibu.sport.core.designsystem.ExerciseIcon
import ch.simibu.sport.core.designsystem.SportGradientFloatingActionButton
import ch.simibu.sport.core.designsystem.TactileButton
import ch.simibu.sport.core.designsystem.TactileCard
import ch.simibu.sport.core.model.Exercise
import ch.simibu.sport.core.model.Program
import ch.simibu.sport.core.model.TemplateExercise
import ch.simibu.sport.core.model.TrainingTemplate
import ch.simibu.sport.core.util.ExertionUtils
import ch.simibu.sport.core.util.SupersetGroup
import ch.simibu.sport.core.util.SupersetUtils

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TemplatesScreen(
    onNavigateToStartWorkout: (templateId: String) -> Unit,
    onNavigateToNewTemplate: () -> Unit,
    onNavigateToEditTemplate: (templateId: String) -> Unit,
    onNavigateToNewProgram: () -> Unit,
    onNavigateToEditProgram: (programId: String) -> Unit,
    viewModel: TemplatesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTemplateId by remember { mutableStateOf<String?>(null) }
    var templateToDelete by remember { mutableStateOf<TrainingTemplate?>(null) }
    var programToDelete by remember { mutableStateOf<Program?>(null) }
    var expandedProgramId by remember { mutableStateOf<String?>(null) }

    val selectedTemplate = uiState.templates.find { it.id == selectedTemplateId }

    BackHandler(enabled = selectedTemplate != null) {
        selectedTemplateId = null
    }

    // Delete Template Dialog
    if (templateToDelete != null) {
        val toDelete = templateToDelete!!
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = { Text(toDelete.name) },
            text = { Text(stringResource(R.string.menu_delete) + "?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTemplate(toDelete.id)
                        if (selectedTemplateId == toDelete.id) {
                            selectedTemplateId = null
                        }
                        templateToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.menu_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // Delete Program Dialog
    if (programToDelete != null) {
        val toDelete = programToDelete!!
        AlertDialog(
            onDismissRequest = { programToDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_program_title)) },
            text = { Text(stringResource(R.string.dialog_delete_program_msg, toDelete.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProgram(toDelete.id)
                        if (expandedProgramId == toDelete.id) {
                            expandedProgramId = null
                        }
                        programToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.menu_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { programToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    SharedTransitionLayout {
        AnimatedContent(
            targetState = selectedTemplate,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "routine_container_transform"
        ) { targetTemplate ->
            if (targetTemplate == null) {
                // Dense Routines Screen (Overview of Templates and Programs)
                val isProgramsTab = uiState.selectedTab == RoutinesTab.PROGRAMS

                Scaffold(
                    floatingActionButton = {
                        SportGradientFloatingActionButton(
                            onClick = if (isProgramsTab) onNavigateToNewProgram else onNavigateToNewTemplate,
                            contentDescription = stringResource(
                                if (isProgramsTab) R.string.btn_create_program else R.string.btn_create_template
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(
                                    if (isProgramsTab) R.string.btn_create_program else R.string.btn_create_template
                                )
                            )
                        }
                    }
                ) { paddingValues ->
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                        return@Scaffold
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(paddingValues),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Page Title Header: "Routines" (the overall term)
                        item {
                            Text(
                                text = stringResource(R.string.routines_title),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        // Tab Switcher: Templates vs Programs
                        item {
                            TabRow(
                                selectedTabIndex = uiState.selectedTab.ordinal,
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary,
                                indicator = { tabPositions ->
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab.ordinal]),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                divider = {}
                            ) {
                                Tab(
                                    selected = uiState.selectedTab == RoutinesTab.TEMPLATES,
                                    onClick = { viewModel.selectTab(RoutinesTab.TEMPLATES) },
                                    text = {
                                        Text(
                                            text = "${stringResource(R.string.routines_tab_templates)} (${uiState.templates.size})",
                                            fontWeight = if (uiState.selectedTab == RoutinesTab.TEMPLATES) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                                Tab(
                                    selected = uiState.selectedTab == RoutinesTab.PROGRAMS,
                                    onClick = { viewModel.selectTab(RoutinesTab.PROGRAMS) },
                                    text = {
                                        Text(
                                            text = "${stringResource(R.string.routines_tab_programs)} (${uiState.programs.size})",
                                            fontWeight = if (uiState.selectedTab == RoutinesTab.PROGRAMS) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // Tab Content: Templates
                        if (uiState.selectedTab == RoutinesTab.TEMPLATES) {
                            if (uiState.templates.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 40.dp, horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.templates_empty),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                items(uiState.templates, key = { it.id }) { template ->
                                    val setsCount = template.exercises.sumOf { it.sets.size.coerceAtLeast(1) }
                                    val exercisesCount = template.exercises.size
                                    val unitExercises = stringResource(R.string.unit_exercises)
                                    val unitSets = stringResource(R.string.unit_sets)

                                    val subtitleText = buildString {
                                        append("$exercisesCount $unitExercises • $setsCount $unitSets")
                                        if (!template.notes.isNullOrBlank()) {
                                            append(" • ${template.notes}")
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .sharedBounds(
                                                sharedContentState = rememberSharedContentState(key = "container_${template.id}"),
                                                animatedVisibilityScope = this@AnimatedContent,
                                                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(16.dp))
                                            )
                                    ) {
                                        TactileCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedTemplateId = template.id }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = template.name,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = subtitleText,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    // Star Button
                                                    IconButton(
                                                        onClick = { viewModel.toggleFavorite(template) },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (template.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                                            contentDescription = "Favorite",
                                                            tint = if (template.isFavorite) AccentGoldLight else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }

                                                    // Edit Button
                                                    IconButton(
                                                        onClick = { onNavigateToEditTemplate(template.id) },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = stringResource(R.string.btn_edit_template),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }

                                                    // Start Template Button
                                                    IconButton(
                                                        onClick = { onNavigateToStartWorkout(template.id) },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.PlayArrow,
                                                                contentDescription = stringResource(R.string.btn_start_template),
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(18.dp)
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

                        // Tab Content: Programs
                        if (uiState.selectedTab == RoutinesTab.PROGRAMS) {
                            if (uiState.programs.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 40.dp, horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.programs_empty),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                items(uiState.programs, key = { it.id }) { program ->
                                    ProgramCard(
                                        program = program,
                                        templatesMap = uiState.templatesMap,
                                        isExpanded = expandedProgramId == program.id,
                                        onToggleExpand = {
                                            expandedProgramId = if (expandedProgramId == program.id) null else program.id
                                        },
                                        onToggleActive = { viewModel.toggleProgramActive(program) },
                                        onEdit = { onNavigateToEditProgram(program.id) },
                                        onDelete = { programToDelete = program },
                                        onStartWorkout = onNavigateToStartWorkout
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Maximized Template View with Container Transform
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "container_${targetTemplate.id}"),
                            animatedVisibilityScope = this@AnimatedContent,
                            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(0.dp))
                        )
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Bar
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { selectedTemplateId = null }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                    Text(
                                        text = targetTemplate.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val exercisesCount = targetTemplate.exercises.size
                                    val setsCount = targetTemplate.exercises.sumOf { it.sets.size.coerceAtLeast(1) }
                                    Text(
                                        text = "$exercisesCount ${stringResource(R.string.unit_exercises)} • $setsCount ${stringResource(R.string.unit_sets)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(onClick = { viewModel.toggleFavorite(targetTemplate) }) {
                                    Icon(
                                        imageVector = if (targetTemplate.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Favorite",
                                        tint = if (targetTemplate.isFavorite) AccentGoldLight else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(onClick = { onNavigateToEditTemplate(targetTemplate.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.btn_edit_template),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(onClick = { templateToDelete = targetTemplate }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        // Content List
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!targetTemplate.notes.isNullOrBlank()) {
                                item {
                                    TactileCard(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = targetTemplate.notes,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(14.dp)
                                        )
                                    }
                                }
                            }

                            val templateBlocks = SupersetUtils.groupTemplateExercises(targetTemplate.exercises)

                            templateBlocks.forEach { block ->
                                if (block.isSuperset) {
                                    item(key = "superset_${block.supersetId}") {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .border(
                                                    width = 1.5.dp,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(16.dp)
                                                )
                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(MaterialTheme.colorScheme.primary)
                                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        text = block.letter ?: "A",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = stringResource(R.string.supersets_title, block.letter ?: "A"),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            if (!block.note.isNullOrBlank()) {
                                                Text(
                                                    text = block.note,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontStyle = FontStyle.Italic,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            block.items.forEach { itemWrapper ->
                                                val te = itemWrapper.item
                                                val ex = uiState.exercisesMap[te.exerciseId]
                                                TemplateExerciseDetailCard(
                                                    te = te,
                                                    ex = ex,
                                                    exertionMode = uiState.exertionMode
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    val itemWrapper = block.items[0]
                                    item(key = "exercise_${itemWrapper.originalIndex}") {
                                        val te = itemWrapper.item
                                        val ex = uiState.exercisesMap[te.exerciseId]
                                        TemplateExerciseDetailCard(
                                            te = te,
                                            ex = ex,
                                            exertionMode = uiState.exertionMode
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Actions (Edit & Start Routine)
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TactileButton(
                                    onClick = { onNavigateToEditTemplate(targetTemplate.id) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.btn_edit_template), color = Color.White)
                                }
                                TactileButton(
                                    onClick = { onNavigateToStartWorkout(targetTemplate.id) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.btn_start_template), color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramCard(
    program: Program,
    templatesMap: Map<String, TrainingTemplate>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStartWorkout: (templateId: String) -> Unit
) {
    TactileCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Active Badge / Toggle + Type Badge + Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badges Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Active Badge / Toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (program.isActive) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable { onToggleActive() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (program.isActive) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.program_active_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.program_set_active),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Type Badge (Repetitive vs One-Time)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (program.isRepetitive) Icons.Default.Repeat else Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (program.isRepetitive) {
                                    stringResource(R.string.program_type_repetitive)
                                } else if (program.usesDates) {
                                    "${stringResource(R.string.program_type_onetime)} (Dates)"
                                } else {
                                    stringResource(R.string.program_type_onetime)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Edit & Delete Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.btn_edit_program),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Program",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title & Description
            Text(
                text = program.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!program.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = program.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Summary text and expand hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.program_days_summary,
                        program.days.size,
                        program.workoutDaysCount,
                        program.restDaysCount
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expanded Schedule Details
            if (isExpanded && program.days.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    program.days.forEach { day ->
                        val template = day.templateId?.let { templatesMap[it] }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Day tag & Info
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (day.isRestDay) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (!day.date.isNullOrBlank()) day.date else "Day ${day.dayNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (day.isRestDay) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (day.isRestDay) Icons.Default.Bedtime else Icons.Default.FitnessCenter,
                                            contentDescription = null,
                                            tint = if (day.isRestDay) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (day.isRestDay) stringResource(R.string.program_rest_day)
                                            else template?.name ?: stringResource(R.string.btn_start_template),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (!day.note.isNullOrBlank()) {
                                        Text(
                                            text = day.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Right: Start button for workout days
                            if (!day.isRestDay && !day.templateId.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                        .clickable { onStartWorkout(day.templateId) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Start",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = stringResource(R.string.btn_start_workout),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
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
}

@Composable
private fun TemplateExerciseDetailCard(
    te: TemplateExercise,
    ex: Exercise?,
    exertionMode: String
) {
    TactileCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Exercise Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExerciseIcon(
                    iconUrl = ex?.iconUrl,
                    exerciseName = ex?.name ?: "Exercise",
                    size = 32.dp,
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ex?.name ?: "Exercise",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (ex != null) {
                        Text(
                            text = "${stringResource(ex.bodypart.labelRes)} • ${stringResource(ex.category.labelRes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!te.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = te.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val sets = if (te.sets.isNotEmpty()) te.sets else listOf(
                ch.simibu.sport.core.model.ExerciseSet(weight = null, reps = 10),
                ch.simibu.sport.core.model.ExerciseSet(weight = null, reps = 10),
                ch.simibu.sport.core.model.ExerciseSet(weight = null, reps = 10)
            )

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.table_set).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )
                Text(
                    text = stringResource(R.string.table_weight).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1.2f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.table_reps).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }

            // Sets rows
            sets.forEachIndexed { sIdx, s ->
                val weightStr = if (s.weight != null && s.weight > 0) "${s.weight} kg" else "—"
                val exertionLabel = ExertionUtils.formatExertionLabel(s.rpe, s.rir, exertionMode)
                val repsStr = buildString {
                    append(if (s.reps != null && s.reps > 0) "${s.reps}" else "—")
                    if (exertionLabel.isNotBlank()) {
                        append(" ($exertionLabel)")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${sIdx + 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = weightStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1.2f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = repsStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1.4f),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}
