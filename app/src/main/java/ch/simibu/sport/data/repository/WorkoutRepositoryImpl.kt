package ch.simibu.sport.data.repository

import ch.simibu.sport.core.model.ExerciseSet
import ch.simibu.sport.core.model.ExerciseType
import ch.simibu.sport.core.model.Workout
import ch.simibu.sport.core.model.WorkoutExercise
import ch.simibu.sport.domain.repository.WorkoutRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import ch.simibu.sport.core.di.ApplicationScope
import ch.simibu.sport.data.health.HealthConnectManager
import ch.simibu.sport.domain.repository.UserProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationScope private val scope: CoroutineScope,
    private val healthConnectManager: HealthConnectManager,
    private val userProfileRepositoryProvider: Provider<UserProfileRepository>
) : WorkoutRepository {

    @Suppress("UNCHECKED_CAST")
    private fun mapDocumentToWorkout(doc: DocumentSnapshot): Workout {
        val exerciseIdsRaw = doc.get("exerciseIds") as? List<*>
        val exerciseIds = exerciseIdsRaw?.filterIsInstance<String>() ?: emptyList()

        val exercisesRaw = doc.get("exercises") as? List<*>
        val exercises = exercisesRaw?.mapNotNull { item ->
            val map = item as? Map<String, Any?> ?: return@mapNotNull null
            val exerciseId = map["exerciseId"] as? String ?: return@mapNotNull null
            val note = map["note"] as? String
            val supersetId = map["supersetId"] as? String
            val supersetNote = map["supersetNote"] as? String
            val setsRaw = map["sets"] as? List<*>
            val sets = FirestoreSetMapper.parseSets(setsRaw)

            WorkoutExercise(
                exerciseId = exerciseId,
                sets = sets,
                note = note,
                supersetId = supersetId,
                supersetNote = supersetNote
            )
        } ?: emptyList()

        return Workout(
            id = doc.id,
            userId = doc.getString("userId") ?: "",
            date = doc.getString("date") ?: "",
            time = doc.getString("time"),
            length = (doc.get("length") as? Number)?.toInt(),
            sessionType = ExerciseType.fromString(doc.getString("sessionType")),
            intensity = (doc.get("intensity") as? Number)?.toInt(),
            maxPulse = (doc.get("maxPulse") as? Number)?.toInt(),
            comment = doc.getString("comment") ?: "",
            exerciseIds = exerciseIds,
            exercises = exercises
        )
    }

    private fun mapWorkoutToMap(workout: Workout): Map<String, Any?> {
        val exercisesList = workout.exercises.map { we ->
            val exMap = hashMapOf<String, Any?>(
                "exerciseId" to we.exerciseId,
                "note" to we.note,
                "sets" to FirestoreSetMapper.setsToMaps(we.sets)
            )
            if (we.supersetId != null) exMap["supersetId"] = we.supersetId
            if (we.supersetNote != null) exMap["supersetNote"] = we.supersetNote
            exMap
        }

        return hashMapOf(
            "userId" to workout.userId,
            "date" to workout.date,
            "time" to workout.time,
            "length" to workout.length,
            "sessionType" to workout.sessionType.value,
            "intensity" to workout.intensity,
            "maxPulse" to workout.maxPulse,
            "comment" to workout.comment,
            "exerciseIds" to workout.exerciseIds,
            "exercises" to exercisesList
        )
    }

    private val workoutsFlowMap = ConcurrentHashMap<String, SharedFlow<List<Workout>>>()

    private fun createWorkoutsFlow(userId: String): SharedFlow<List<Workout>> {
        return callbackFlow {
            val query = firestore.collection("users")
                .document(userId)
                .collection("activities")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(1000)

            val registration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val workouts = snapshot.documents.map { mapDocumentToWorkout(it) }
                    trySend(workouts)
                }
            }
            awaitClose { registration.remove() }
        }.shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 60_000, replayExpirationMillis = 0),
            replay = 1
        )
    }

    override fun getWorkouts(userId: String): Flow<List<Workout>> {
        return workoutsFlowMap.computeIfAbsent(userId) { uid ->
            createWorkoutsFlow(uid)
        }
    }

    override fun getWorkout(userId: String, workoutId: String): Flow<Workout?> {
        return getWorkouts(userId).map { list ->
            list.find { it.id == workoutId }
        }
    }

    private suspend fun syncToHealthConnectIfEnabled(workout: Workout) {
        try {
            val profile = userProfileRepositoryProvider.get().getUserProfile(workout.userId).firstOrNull()
            if (profile?.settings?.enableHealthConnect == true && healthConnectManager.isAvailable) {
                healthConnectManager.exportWorkout(workout)
            }
        } catch (_: Exception) {
            // Silently ignore to avoid blocking Firestore save
        }
    }

    private suspend fun deleteFromHealthConnectIfEnabled(userId: String, workoutId: String) {
        try {
            val profile = userProfileRepositoryProvider.get().getUserProfile(userId).firstOrNull()
            if (profile?.settings?.enableHealthConnect == true && healthConnectManager.isAvailable) {
                healthConnectManager.deleteWorkout(workoutId)
            }
        } catch (_: Exception) {
            // Silently ignore
        }
    }

    override suspend fun createWorkout(workout: Workout): Result<String> {
        return try {
            val map = mapWorkoutToMap(workout)
            val docRef = firestore.collection("users")
                .document(workout.userId)
                .collection("activities")
                .add(map)
                .await()
            val newWorkout = workout.copy(id = docRef.id)
            syncToHealthConnectIfEnabled(newWorkout)
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateWorkout(workout: Workout): Result<Unit> {
        return try {
            val map = mapWorkoutToMap(workout)
            firestore.collection("users")
                .document(workout.userId)
                .collection("activities")
                .document(workout.id)
                .update(map)
                .await()
            syncToHealthConnectIfEnabled(workout)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteWorkout(userId: String, workoutId: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("activities")
                .document(workoutId)
                .delete()
                .await()
            deleteFromHealthConnectIfEnabled(userId, workoutId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
