package ch.simibu.sport.presentation.exercises

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import ch.simibu.sport.R
import androidx.lifecycle.viewModelScope
import ch.simibu.sport.core.designsystem.AccentGoldLight
import ch.simibu.sport.core.designsystem.ChartDataPoint
import ch.simibu.sport.core.designsystem.ChartTimeFrame
import ch.simibu.sport.core.designsystem.ExerciseIcon
import ch.simibu.sport.core.designsystem.SportLineChart
import ch.simibu.sport.core.designsystem.SportPurple
import ch.simibu.sport.core.designsystem.SportRuby
import ch.simibu.sport.core.designsystem.TactileCard
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import ch.simibu.sport.core.model.Exercise
import ch.simibu.sport.core.model.ExerciseSet
import ch.simibu.sport.domain.repository.AuthRepository
import ch.simibu.sport.domain.repository.ExerciseRepository
import ch.simibu.sport.domain.repository.WorkoutRepository
import ch.simibu.sport.domain.usecase.WorkoutCalculationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

data class ExerciseSessionHistory(
    val workoutId: String,
    val date: String,
    val note: String?,
    val sets: List<ExerciseSet>,
    val maxWeight: Double?,
    val best1RM: Double?
)

data class ExerciseHistoryUiState(
    val exercise: Exercise? = null,
    val sessions: List<ExerciseSessionHistory> = emptyList(),
    val maxWeightAllTime: Double? = null,
    val best1RMAllTime: Double? = null,
    val totalSetsAllTime: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class ExerciseHistoryViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val authRepository: AuthRepository,
    private val calculationUseCase: WorkoutCalculationUseCase
) : ViewModel() {

    private val currentUserId: String?
        get() = authRepository.currentUserId
    private val uiStateCache = ConcurrentHashMap<String, StateFlow<ExerciseHistoryUiState>>()

    fun getUiState(exerciseId: String): StateFlow<ExerciseHistoryUiState> {
        val uid = currentUserId ?: "guest"
        return uiStateCache.computeIfAbsent("$uid:$exerciseId") {
            createUiStateFlow(exerciseId, currentUserId)
        }
    }

    private fun createUiStateFlow(exerciseId: String, uid: String?): StateFlow<ExerciseHistoryUiState> {
        val workoutsFlow = if (uid != null) {
            workoutRepository.getWorkouts(uid)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }

        return combine(
            exerciseRepository.getExercise(exerciseId),
            workoutsFlow
        ) { exercise, workouts ->
            val sessions = mutableListOf<ExerciseSessionHistory>()
            var maxWeightAllTime: Double? = null
            var best1RMAllTime: Double? = null
            var totalSets = 0

            for (w in workouts) {
                val matched = w.exercises.filter { it.exerciseId == exerciseId }
                if (matched.isNotEmpty()) {
                    for (m in matched) {
                        var sessionMaxW: Double? = null
                        var sessionBest1RM: Double? = null
                        totalSets += m.sets.size

                        for (s in m.sets) {
                            val wKg = s.weight
                            val reps = s.reps ?: 1
                            if (wKg != null) {
                                if (sessionMaxW == null || wKg > sessionMaxW) {
                                    sessionMaxW = wKg
                                }
                                if (maxWeightAllTime == null || wKg > maxWeightAllTime) {
                                    maxWeightAllTime = wKg
                                }

                                val oneRm = calculationUseCase.calculate1RM(wKg, reps)
                                if (sessionBest1RM == null || oneRm > sessionBest1RM) {
                                    sessionBest1RM = oneRm
                                }
                                if (best1RMAllTime == null || oneRm > best1RMAllTime) {
                                    best1RMAllTime = oneRm
                                }
                            }
                        }

                        sessions.add(
                            ExerciseSessionHistory(
                                workoutId = w.id,
                                date = w.date,
                                note = m.note,
                                sets = m.sets,
                                maxWeight = sessionMaxW,
                                best1RM = sessionBest1RM
                            )
                        )
                    }
                }
            }

            ExerciseHistoryUiState(
                exercise = exercise,
                sessions = sessions.sortedByDescending { it.date },
                maxWeightAllTime = maxWeightAllTime,
                best1RMAllTime = best1RMAllTime,
                totalSetsAllTime = totalSets,
                isLoading = false
            )
        }.flowOn(Dispatchers.Default).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ExerciseHistoryUiState()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseHistoryScreen(
    exerciseId: String,
    onNavigateBack: () -> Unit,
    viewModel: ExerciseHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.getUiState(exerciseId).collectAsStateWithLifecycle()
    var selectedTimeFrame by remember { mutableStateOf(ChartTimeFrame.SIX_MONTHS) }
    var selectedMetricIndex by remember { mutableIntStateOf(0) }

    val chartDataPoints = remember(uiState.sessions, selectedTimeFrame, selectedMetricIndex) {
        val cutoff = selectedTimeFrame.getCutoffDate()
        uiState.sessions
            .mapNotNull { session ->
                val localDate = try {
                    LocalDate.parse(session.date.substringBefore('T'))
                } catch (_: Exception) {
                    null
                } ?: return@mapNotNull null

                if (cutoff != null && localDate.isBefore(cutoff)) {
                    return@mapNotNull null
                }

                val value = if (selectedMetricIndex == 0) {
                    session.best1RM?.toFloat()
                } else {
                    session.maxWeight?.toFloat()
                } ?: return@mapNotNull null

                val timestamp = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val formattedDate = localDate.format(DateTimeFormatter.ofPattern("MMM d"))
                ChartDataPoint(
                    timestamp = timestamp,
                    value = value,
                    formattedDate = formattedDate
                )
            }
            .sortedBy { it.timestamp }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.exercise?.name ?: stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
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
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Exercise Header Card
            item {
                TactileCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExerciseIcon(
                            iconUrl = uiState.exercise?.iconUrl,
                            exerciseName = uiState.exercise?.name ?: "",
                            size = 52.dp,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.exercise?.name ?: "Exercise",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (uiState.exercise != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${stringResource(uiState.exercise!!.bodypart.labelRes)} • ${stringResource(uiState.exercise!!.category.labelRes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // PR Banner Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TactileCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = AccentGoldLight,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = stringResource(R.string.metric_max_weight),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (uiState.maxWeightAllTime != null) "${uiState.maxWeightAllTime} kg" else "--",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    TactileCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = stringResource(R.string.metric_est_1rm),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (uiState.best1RMAllTime != null) "${uiState.best1RMAllTime} kg" else "--",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Progress Chart (similar to web app)
            item {
                SportLineChart(
                    title = stringResource(R.string.chart_weight_progress),
                    dataPoints = chartDataPoints,
                    unit = stringResource(R.string.unit_kg),
                    lineColor = MaterialTheme.colorScheme.primary,
                    selectedTimeFrame = selectedTimeFrame,
                    onTimeFrameSelected = { selectedTimeFrame = it },
                    metricTabs = listOf(
                        stringResource(R.string.chart_metric_1rm),
                        stringResource(R.string.metric_max_weight)
                    ),
                    selectedMetricIndex = selectedMetricIndex,
                    onMetricTabSelected = { selectedMetricIndex = it }
                )
            }

            item {
                Text(
                    text = stringResource(R.string.history_past_sessions, uiState.sessions.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (uiState.sessions.isEmpty()) {
                item {
                    TactileCard(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.history_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.sessions) { session ->
                    TactileCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = session.date,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (session.best1RM != null) {
                                    Text(
                                        text = stringResource(R.string.history_1rm_label, session.best1RM.toString()),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (!session.note.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = session.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // List of sets
                            val repsUnit = stringResource(R.string.unit_reps)
                            session.sets.forEachIndexed { idx, set ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.workout_set_label, idx + 1),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${set.weight ?: 0.0} kg × ${set.reps ?: 0} $repsUnit",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
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
