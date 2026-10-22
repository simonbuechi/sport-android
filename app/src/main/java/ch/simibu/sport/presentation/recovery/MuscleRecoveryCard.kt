package ch.simibu.sport.presentation.recovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.simibu.sport.R
import ch.simibu.sport.core.designsystem.TactileCard
import ch.simibu.sport.core.designsystem.TactileElevation

@Composable
fun MuscleRecoveryCard(
    recoverySummary: BodyRecoverySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    TactileCard(
        modifier = modifier.fillMaxWidth(),
        elevation = TactileElevation.RAISED,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.recovery_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.recovery_readiness),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Overall score badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(recoverySummary.status.defaultColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${recoverySummary.overallRecoveryPercent}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = recoverySummary.status.defaultColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body: Silhouettes on the left, status & summary on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Miniature front and back silhouettes
                Row(
                    modifier = Modifier.weight(0.48f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AnatomicalBodySilhouette(
                        viewType = BodyViewType.FRONT,
                        recoveryMap = recoverySummary.muscles,
                        selectedMuscle = null,
                        onMuscleSelected = { onClick() },
                        height = 120.dp
                    )
                    AnatomicalBodySilhouette(
                        viewType = BodyViewType.BACK,
                        recoveryMap = recoverySummary.muscles,
                        selectedMuscle = null,
                        onMuscleSelected = { onClick() },
                        height = 120.dp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Breakdown list & status on right
                Column(
                    modifier = Modifier.weight(0.52f),
                    verticalArrangement = Arrangement.Center
                ) {
                    LinearProgressIndicator(
                        progress = { recoverySummary.overallRecoveryPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = recoverySummary.status.defaultColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Fatigued muscles summary if any
                    if (recoverySummary.fatiguedMuscles.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val names = recoverySummary.fatiguedMuscles.take(2)
                                .joinToString(", ") { context.getString(it.bodyPart.labelRes) }
                            Text(
                                text = stringResource(R.string.recovery_fatigued) + ": $names",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFEF4444),
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Recovering muscles summary if any
                    if (recoverySummary.recoveringMuscles.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF59E0B))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val names = recoverySummary.recoveringMuscles.take(2)
                                .joinToString(", ") { context.getString(it.bodyPart.labelRes) }
                            Text(
                                text = stringResource(R.string.recovery_recovering) + ": $names",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Ready muscles summary
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${recoverySummary.readyMuscles.size} " + stringResource(R.string.recovery_ready),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.dashboard_view_all),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}
