package ch.simibu.sport.presentation.profile

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.simibu.sport.R
import ch.simibu.sport.core.designsystem.SportPurple
import ch.simibu.sport.core.designsystem.SportRuby
import ch.simibu.sport.core.designsystem.TactileButton
import ch.simibu.sport.core.designsystem.TactileCard
import ch.simibu.sport.core.designsystem.TactileChip
import ch.simibu.sport.data.repository.LanguageMode
import ch.simibu.sport.data.repository.ThemeMode
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    onNavigateToBodyHistory: () -> Unit,
    onSignOutSuccess: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showLogWeightDialog by remember { mutableStateOf(false) }
    var showLogMeasurementDialog by remember { mutableStateOf(false) }

    Scaffold { paddingValues ->
        if (uiState.isLoading || uiState.profile == null) {
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

        val profile = uiState.profile!!
        val latestWeight = profile.weights.maxByOrNull { it.date }
        val latestMeasurement = profile.measurements.maxByOrNull { it.date }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            item {
                TactileCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = profile.name.ifBlank { "User" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val details = mutableListOf<String>()
                        if (profile.height != null) details.add("${profile.height.roundToInt()} cm")
                        if (profile.birthYear != null) details.add("Born ${profile.birthYear}")
                        if (details.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = details.joinToString(" • "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (profile.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = profile.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(onClick = { showEditProfileDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_edit_profile))
                        }
                    }
                }
            }

            // Quick Body Tracking Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TactileButton(
                        onClick = { showLogWeightDialog = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_log_weight), color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    TactileButton(
                        onClick = { showLogMeasurementDialog = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Straighten, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_measure), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Body Metrics Card with History Link
            item {
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToBodyHistory
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.profile_body_metrics), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View History", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.metric_weight), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (latestWeight != null) "${latestWeight.weightKg} kg" else "--",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.metric_body_fat), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (latestWeight?.bodyFatPercent != null) "${latestWeight.bodyFatPercent}%" else "--",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (latestMeasurement?.waist != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(stringResource(R.string.metric_waist), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${latestMeasurement.waist} cm",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // App Settings Card
            item {
                val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()
                val currentLanguageMode by viewModel.languageMode.collectAsStateWithLifecycle()

                TactileCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.settings_appearance_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                                ThemeMode.LIGHT to stringResource(R.string.theme_light),
                                ThemeMode.DARK to stringResource(R.string.theme_dark)
                            ).forEach { (mode, label) ->
                                val selected = currentThemeMode == mode
                                TactileChip(
                                    text = label,
                                    selected = selected,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.settings_language_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                LanguageMode.SYSTEM to stringResource(R.string.theme_system),
                                LanguageMode.ENGLISH to LanguageMode.ENGLISH.nativeName,
                                LanguageMode.GERMAN to LanguageMode.GERMAN.nativeName
                            ).forEach { (mode, label) ->
                                val selected = currentLanguageMode == mode
                                TactileChip(
                                    text = label,
                                    selected = selected,
                                    onClick = { viewModel.setLanguageMode(mode) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                LanguageMode.FRENCH to LanguageMode.FRENCH.nativeName,
                                LanguageMode.SPANISH to LanguageMode.SPANISH.nativeName,
                                LanguageMode.ITALIAN to LanguageMode.ITALIAN.nativeName
                            ).forEach { (mode, label) ->
                                val selected = currentLanguageMode == mode
                                TactileChip(
                                    text = label,
                                    selected = selected,
                                    onClick = { viewModel.setLanguageMode(mode) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.pref_autofill), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text(stringResource(R.string.pref_autofill_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = profile.settings.autoFillSets,
                                onCheckedChange = { checked ->
                                    viewModel.updateSettings(profile.settings.theme, checked, profile.settings.showTimer)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.pref_timer_haptics), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text(stringResource(R.string.pref_timer_haptics_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = profile.settings.showTimer,
                                onCheckedChange = { checked ->
                                    viewModel.updateSettings(profile.settings.theme, profile.settings.autoFillSets, checked)
                                }
                            )
                        }
                    }
                }
            }

            // Sign Out Button
            item {
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        viewModel.signOut()
                        onSignOutSuccess()
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_sign_out), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        val currentProfile = uiState.profile ?: return
        var editName by remember { mutableStateOf(currentProfile.name) }
        var editHeight by remember { mutableStateOf(currentProfile.height?.toString() ?: "") }
        var editBirthYear by remember { mutableStateOf(currentProfile.birthYear?.toString() ?: "") }
        var editNotes by remember { mutableStateOf(currentProfile.notes) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editHeight,
                        onValueChange = { editHeight = it },
                        label = { Text("Height (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editBirthYear,
                        onValueChange = { editBirthYear = it },
                        label = { Text("Birth Year (YYYY)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Fitness Goals / Notes") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateProfileInfo(
                        name = editName.trim(),
                        birthYear = editBirthYear.toIntOrNull(),
                        height = editHeight.toDoubleOrNull(),
                        notes = editNotes.trim()
                    )
                    showEditProfileDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Log Weight Dialog
    if (showLogWeightDialog) {
        var weightStr by remember { mutableStateOf("") }
        var bodyFatStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showLogWeightDialog = false },
            title = { Text("Log Body Weight") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = weightStr,
                        onValueChange = { weightStr = it },
                        label = { Text("Weight (kg) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = bodyFatStr,
                        onValueChange = { bodyFatStr = it },
                        label = { Text("Body Fat % (optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val w = weightStr.toDoubleOrNull()
                    if (w != null && w > 0) {
                        viewModel.addWeightEntry(w, bodyFatStr.toDoubleOrNull())
                        showLogWeightDialog = false
                    }
                }) {
                    Text("Save Entry")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogWeightDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Log Measurements Dialog
    if (showLogMeasurementDialog) {
        var waist by remember { mutableStateOf("") }
        var hips by remember { mutableStateOf("") }
        var chest by remember { mutableStateOf("") }
        var shoulders by remember { mutableStateOf("") }
        var bicep by remember { mutableStateOf("") }
        var thigh by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showLogMeasurementDialog = false },
            title = { Text("Log Circumferences (cm)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = waist, onValueChange = { waist = it }, label = { Text("Waist (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = chest, onValueChange = { chest = it }, label = { Text("Chest (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = shoulders, onValueChange = { shoulders = it }, label = { Text("Shoulders (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = bicep, onValueChange = { bicep = it }, label = { Text("Bicep (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = thigh, onValueChange = { thigh = it }, label = { Text("Thigh (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addMeasurementEntry(
                        waist = waist.toDoubleOrNull(),
                        hips = hips.toDoubleOrNull(),
                        chest = chest.toDoubleOrNull(),
                        shoulders = shoulders.toDoubleOrNull(),
                        bicep = bicep.toDoubleOrNull(),
                        thigh = thigh.toDoubleOrNull()
                    )
                    showLogMeasurementDialog = false
                }) {
                    Text("Save Measurements")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogMeasurementDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
