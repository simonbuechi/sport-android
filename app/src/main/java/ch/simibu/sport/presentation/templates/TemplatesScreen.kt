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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import ch.simibu.sport.core.designsystem.SportGradientFloatingActionButton
import ch.simibu.sport.core.designsystem.AccentGoldLight
import ch.simibu.sport.core.designsystem.ExerciseIcon
import ch.simibu.sport.core.designsystem.TactileButton
import ch.simibu.sport.core.designsystem.TactileCard
import ch.simibu.sport.core.designsystem.tactileClickable
import ch.simibu.sport.core.model.TrainingTemplate

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TemplatesScreen(
    onNavigateToStartWorkout: (templateId: String) -> Unit,
    onNavigateToNewTemplate: () -> Unit,
    onNavigateToEditTemplate: (templateId: String) -> Unit,
    viewModel: TemplatesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTemplateId by remember { mutableStateOf<String?>(null) }
    var templateToDelete by remember { mutableStateOf<TrainingTemplate?>(null) }

    val selectedTemplate = uiState.templates.find { it.id == selectedTemplateId }

    BackHandler(enabled = selectedTemplate != null) {
        selectedTemplateId = null
    }

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

    SharedTransitionLayout {
        AnimatedContent(
            targetState = selectedTemplate,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "routine_container_transform"
        ) { targetTemplate ->
            if (targetTemplate == null) {
                // Dense Routines Screen
                Scaffold(
                    floatingActionButton = {
                        SportGradientFloatingActionButton(
                            onClick = onNavigateToNewTemplate,
                            contentDescription = stringResource(R.string.btn_create_routine)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.btn_create_routine))
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

                    if (uiState.templates.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(paddingValues)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.routines_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                        item {
                            Text(
                                text = stringResource(R.string.routines_title),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

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
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
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
                                                    contentDescription = stringResource(R.string.btn_edit_routine),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            // Start Routine Button (Subtle soft accent)
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
                                                        contentDescription = stringResource(R.string.btn_start_routine),
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
            } else {
                // Maximized Routine View with Container Transform
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
                                        contentDescription = stringResource(R.string.btn_edit_routine),
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

                        // Exercise & Set Details
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (!targetTemplate.notes.isNullOrBlank()) {
                                item {
                                    TactileCard(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = stringResource(R.string.workout_comment_title),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = targetTemplate.notes,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = stringResource(R.string.exercises_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            items(targetTemplate.exercises) { te ->
                                val ex = uiState.exercisesMap[te.exerciseId]
                                val exName = ex?.name ?: "Exercise"
                                val bodyPartLabel = if (ex != null) stringResource(ex.bodypart.labelRes) else ""
                                val categoryLabel = if (ex != null) stringResource(ex.category.labelRes) else ""
                                val subtitle = listOf(bodyPartLabel, categoryLabel).filter { it.isNotBlank() }.joinToString(" • ")

                                TactileCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        // Header Row: Icon + Name + Category
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            ExerciseIcon(
                                                iconUrl = ex?.iconUrl,
                                                exerciseName = exName,
                                                size = 36.dp,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = exName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (subtitle.isNotBlank()) {
                                                    Text(
                                                        text = subtitle,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        if (!te.note.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = te.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontStyle = FontStyle.Italic,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Sets Table
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
                                            val repsStr = if (s.reps != null && s.reps > 0) "${s.reps}" else "—"

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
                                                    modifier = Modifier.weight(1f),
                                                    textAlign = TextAlign.End
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Sticky Action Bar
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TactileButton(
                                    onClick = { onNavigateToEditTemplate(targetTemplate.id) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.btn_edit_routine),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                TactileButton(
                                    onClick = { onNavigateToStartWorkout(targetTemplate.id) },
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.btn_start_routine),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
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
