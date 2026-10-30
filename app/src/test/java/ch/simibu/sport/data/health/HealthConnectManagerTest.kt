package ch.simibu.sport.data.health

import android.content.Context
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import ch.simibu.sport.core.model.ExerciseType
import ch.simibu.sport.core.model.UserProfile
import ch.simibu.sport.core.model.UserSettings
import ch.simibu.sport.core.model.Workout
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

class HealthConnectManagerTest {

    private lateinit var context: Context
    private lateinit var manager: HealthConnectManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        manager = HealthConnectManager(context)
    }

    @Test
    fun userSettings_defaultHealthConnectDisabled() {
        val settings = UserSettings()
        assertFalse(settings.enableHealthConnect)

        val profile = UserProfile()
        assertFalse(profile.settings.enableHealthConnect)
    }

    @Test
    fun mapWorkoutToRecord_mapsStrengthWorkoutCorrectly() {
        val workout = Workout(
            id = "w123",
            userId = "user_abc",
            date = "2026-05-10",
            time = "10:30",
            length = 60,
            sessionType = ExerciseType.STRENGTH,
            comment = "Leg day session"
        )

        val record = manager.mapWorkoutToRecord(workout)

        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING, record.exerciseType)
        assertEquals("Strength Training", record.title)
        assertEquals("Leg day session", record.notes)
        assertEquals("w123", record.metadata.clientRecordId)
        assertEquals(Metadata.RECORDING_METHOD_MANUAL_ENTRY, record.metadata.recordingMethod)
        assertNotNull(record.startTime)
        assertNotNull(record.endTime)
        org.junit.Assert.assertTrue(record.endTime.isAfter(record.startTime))
    }

    @Test
    fun mapWorkoutToRecord_mapsCardioWorkoutCorrectly() {
        val workout = Workout(
            id = "cardio_456",
            userId = "user_abc",
            date = "2026-05-11",
            time = "08:00",
            length = 30,
            sessionType = ExerciseType.CARDIO,
            comment = ""
        )

        val record = manager.mapWorkoutToRecord(workout)

        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, record.exerciseType)
        assertEquals("Cardio", record.title)
        assertNull(record.notes)
        assertEquals("cardio_456", record.metadata.clientRecordId)
    }

    @Test
    fun mapWorkoutToRecord_mapsFlexibilityWorkoutCorrectly() {
        val workout = Workout(
            id = "flex_789",
            userId = "user_abc",
            date = "2026-05-12",
            time = null,
            length = null, // fallback length
            sessionType = ExerciseType.FLEXIBILITY
        )

        val record = manager.mapWorkoutToRecord(workout)

        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING, record.exerciseType)
        assertEquals("Flexibility / Mobility", record.title)
        assertEquals("flex_789", record.metadata.clientRecordId)
    }
}
