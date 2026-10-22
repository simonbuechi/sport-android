package ch.simibu.sport.presentation.exercises

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import ch.simibu.sport.R
import ch.simibu.sport.core.designsystem.AccentGoldLight
import ch.simibu.sport.core.designsystem.ExerciseIcon
import ch.simibu.sport.core.designsystem.SportGradientFloatingActionButton
import ch.simibu.sport.core.designsystem.TactileCard
import ch.simibu.sport.core.designsystem.TactileChip
import ch.simibu.sport.core.model.BodyPart
import ch.simibu.sport.core.model.ExerciseCategory
import ch.simibu.sport.core.model.ExerciseType

private enum class FilterGroup {
    BODY_PART, TYPE, CATEGORY
}

@Composable
fun ExercisesScreen(
    onNavigateToDetails: (exerciseId: String) -> Unit,
    onNavigateToNewExercise: () -> Unit,
    viewModel: ExercisesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedGroup by remember { mutableStateOf<FilterGroup?>(FilterGroup.BODY_PART) }

    Scaffold(
        floatingActionButton = {
            SportGradientFloatingActionButton(
                onClick = onNavigateToNewExercise,
                contentDescription = stringResource(R.string.btn_create_exercise)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.btn_create_exercise))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            // Search Bar
            Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.exercises_search_placeholder)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // 1. Filter Category Selector Header Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                // Favorites toggle
                item {
                    TactileChip(
                        text = stringResource(R.string.filter_favorites),
                        selected = uiState.showOnlyFavorites,
                        onClick = viewModel::toggleFavoritesFilter
                    )
                }

                // Body Part Filter Group
                item {
                    val label = if (uiState.selectedBodyPart != null) {
                        "${stringResource(R.string.filter_tab_body_part)}: ${stringResource(uiState.selectedBodyPart!!.labelRes)}"
                    } else {
                        stringResource(R.string.filter_tab_body_part)
                    }
                    TactileChip(
                        text = label,
                        selected = selectedGroup == FilterGroup.BODY_PART || uiState.selectedBodyPart != null,
                        onClick = {
                            selectedGroup = if (selectedGroup == FilterGroup.BODY_PART) null else FilterGroup.BODY_PART
                        }
                    )
                }

                // Type Filter Group
                item {
                    val label = if (uiState.selectedType != null) {
                        "${stringResource(R.string.filter_tab_type)}: ${stringResource(uiState.selectedType!!.labelRes)}"
                    } else {
                        stringResource(R.string.filter_tab_type)
                    }
                    TactileChip(
                        text = label,
                        selected = selectedGroup == FilterGroup.TYPE || uiState.selectedType != null,
                        onClick = {
                            selectedGroup = if (selectedGroup == FilterGroup.TYPE) null else FilterGroup.TYPE
                        }
                    )
                }

                // Category (Equipment) Filter Group
                item {
                    val label = if (uiState.selectedCategory != null) {
                        "${stringResource(R.string.filter_tab_category)}: ${stringResource(uiState.selectedCategory!!.labelRes)}"
                    } else {
                        stringResource(R.string.filter_tab_category)
                    }
                    TactileChip(
                        text = label,
                        selected = selectedGroup == FilterGroup.CATEGORY || uiState.selectedCategory != null,
                        onClick = {
                            selectedGroup = if (selectedGroup == FilterGroup.CATEGORY) null else FilterGroup.CATEGORY
                        }
                    )
                }

                // Clear All Filters
                if (uiState.hasActiveFilters) {
                    item {
                        TactileChip(
                            text = "✕ ${stringResource(R.string.filter_clear_all)}",
                            selected = false,
                            onClick = viewModel::clearAllFilters
                        )
                    }
                }
            }

            // 2. Options row for the active filter group
            AnimatedVisibility(visible = selectedGroup != null) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                ) {
                    when (selectedGroup) {
                        FilterGroup.BODY_PART -> {
                            item {
                                TactileChip(
                                    text = stringResource(R.string.filter_all_body_parts),
                                    selected = uiState.selectedBodyPart == null,
                                    onClick = { viewModel.selectBodyPart(null) }
                                )
                            }
                            items(BodyPart.entries) { bodyPart ->
                                TactileChip(
                                    text = stringResource(bodyPart.labelRes),
                                    selected = uiState.selectedBodyPart == bodyPart,
                                    onClick = { viewModel.selectBodyPart(bodyPart) }
                                )
                            }
                        }
                        FilterGroup.TYPE -> {
                            item {
                                TactileChip(
                                    text = stringResource(R.string.filter_all_types),
                                    selected = uiState.selectedType == null,
                                    onClick = { viewModel.selectType(null) }
                                )
                            }
                            items(ExerciseType.entries) { type ->
                                TactileChip(
                                    text = stringResource(type.labelRes),
                                    selected = uiState.selectedType == type,
                                    onClick = { viewModel.selectType(type) }
                                )
                            }
                        }
                        FilterGroup.CATEGORY -> {
                            item {
                                TactileChip(
                                    text = stringResource(R.string.filter_all_categories),
                                    selected = uiState.selectedCategory == null,
                                    onClick = { viewModel.selectCategory(null) }
                                )
                            }
                            items(ExerciseCategory.entries) { category ->
                                TactileChip(
                                    text = stringResource(category.labelRes),
                                    selected = uiState.selectedCategory == category,
                                    onClick = { viewModel.selectCategory(category) }
                                )
                            }
                        }
                        null -> {}
                    }
                }
            }

            // 3. Results count summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.exercises_count_matching, uiState.exercises.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                if (uiState.hasActiveFilters) {
                    Text(
                        text = "${uiState.activeFilterCount} active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 4. Content list or loading/empty
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.exercises.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.exercises_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.exercises, key = { it.id }) { exercise ->
                        val isFavorite = uiState.markedExercises[exercise.id]?.favorite == true

                        TactileCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onNavigateToDetails(exercise.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ExerciseIcon(
                                    iconUrl = exercise.iconUrl,
                                    exerciseName = exercise.name,
                                    size = 46.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exercise.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = stringResource(exercise.type.labelRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = stringResource(exercise.bodypart.labelRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = stringResource(exercise.category.labelRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.toggleFavorite(exercise.id) }
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isFavorite) AccentGoldLight else MaterialTheme.colorScheme.onSurfaceVariant
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
