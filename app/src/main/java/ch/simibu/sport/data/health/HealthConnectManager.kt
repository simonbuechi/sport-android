package ch.simibu.sport.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import ch.simibu.sport.core.model.ExerciseType
import ch.simibu.sport.core.model.Workout
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val permissions: Set<String> = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class)
    )

    fun getSdkStatus(): Int {
        return HealthConnectClient.getSdkStatus(context)
    }

    val isAvailable: Boolean
        get() = getSdkStatus() == HealthConnectClient.SDK_AVAILABLE

    private val healthConnectClient: HealthConnectClient?
        get() = if (isAvailable) HealthConnectClient.getOrCreate(context) else null

    suspend fun hasPermissions(): Boolean = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext false
        try {
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(permissions)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Converts a Sport Workout into an ExerciseSessionRecord.
     */
    fun mapWorkoutToRecord(workout: Workout): ExerciseSessionRecord {
        val date = try {
            LocalDate.parse(workout.date)
        } catch (_: Exception) {
            LocalDate.now()
        }

        val time = try {
            if (!workout.time.isNullOrBlank()) {
                LocalTime.parse(workout.time)
            } else {
                LocalTime.of(12, 0)
            }
        } catch (_: Exception) {
            LocalTime.of(12, 0)
        }

        val zoneId = ZoneId.systemDefault()
        val zonedDateTime = ZonedDateTime.of(date, time, zoneId)
        val zoneOffset = zoneId.rules.getOffset(zonedDateTime.toInstant())

        val startTime = zonedDateTime.toInstant()
        val lengthMinutes = (workout.length ?: 45).coerceAtLeast(5)
        var endTime = startTime.plusSeconds((lengthMinutes * 60).toLong())

        val now = Instant.now()
        if (endTime.isAfter(now)) {
            endTime = now
        }
        val safeStartTime = if (startTime.isAfter(endTime)) endTime.minusSeconds(60) else startTime

        val exerciseType = when (workout.sessionType) {
            ExerciseType.STRENGTH -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            ExerciseType.CARDIO -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            ExerciseType.FLEXIBILITY -> ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING
            ExerciseType.OTHER -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
        }

        val title = when (workout.sessionType) {
            ExerciseType.STRENGTH -> "Strength Training"
            ExerciseType.CARDIO -> "Cardio"
            ExerciseType.FLEXIBILITY -> "Flexibility / Mobility"
            ExerciseType.OTHER -> "Workout"
        }

        return ExerciseSessionRecord(
            startTime = safeStartTime,
            startZoneOffset = zoneOffset,
            endTime = endTime,
            endZoneOffset = zoneOffset,
            exerciseType = exerciseType,
            title = title,
            notes = workout.comment.ifBlank { null },
            metadata = Metadata(
                clientRecordId = workout.id,
                recordingMethod = Metadata.RECORDING_METHOD_MANUAL_ENTRY
            )
        )
    }

    /**
     * Upserts a single workout into Health Connect using its workout.id as clientRecordId.
     */
    suspend fun exportWorkout(workout: Workout): Result<Unit> = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext Result.failure(
            IllegalStateException("Health Connect is not available on this device")
        )

        if (!hasPermissions()) {
            return@withContext Result.failure(
                SecurityException("Health Connect write permission not granted")
            )
        }

        try {
            val record = mapWorkoutToRecord(workout)
            // insertRecords automatically updates records that share the same clientRecordId
            client.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a workout record from Health Connect by its clientRecordId.
     */
    suspend fun deleteWorkout(workoutId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext Result.success(Unit)
        if (!hasPermissions()) return@withContext Result.success(Unit)

        try {
            client.deleteRecords(
                recordType = ExerciseSessionRecord::class,
                recordIdsList = emptyList(),
                clientRecordIdsList = listOf(workoutId)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports a batch of workouts (e.g. historical workouts).
     * Chunks by 100 to stay well within limits.
     */
    suspend fun exportWorkoutsBatch(workouts: List<Workout>): Result<Int> = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext Result.failure(
            IllegalStateException("Health Connect is not available on this device")
        )

        if (!hasPermissions()) {
            return@withContext Result.failure(
                SecurityException("Health Connect write permission not granted")
            )
        }

        try {
            var syncedCount = 0
            val validWorkouts = workouts.filter { it.id.isNotBlank() && it.date.isNotBlank() }

            validWorkouts.chunked(100).forEach { chunk ->
                val records = chunk.map { mapWorkoutToRecord(it) }
                client.insertRecords(records)
                syncedCount += records.size
            }

            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
