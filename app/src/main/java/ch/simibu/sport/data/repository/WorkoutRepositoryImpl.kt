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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationScope private val scope: CoroutineScope
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
            val setsRaw = map["sets"] as? List<*>
            val sets = setsRaw?.mapNotNull { setItem ->
                val setMap = setItem as? Map<String, Any?> ?: return@mapNotNull null
                val weight = when (val w = setMap["weight"]) {
                    is Number -> w.toDouble()
                    else -> null
                }
                val reps = when (val r = setMap["reps"]) {
                    is Number -> r.toInt()
                    else -> null
                }
                ExerciseSet(
                    id = setMap["id"] as? String ?: UUID.randomUUID().toString(),
                    weight = weight,
                    reps = reps,
                    notes = setMap["notes"] as? String
                )
            } ?: emptyList()

            WorkoutExercise(
                exerciseId = exerciseId,
                sets = sets,
                note = note
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
            hashMapOf(
                "exerciseId" to we.exerciseId,
                "note" to we.note,
                "sets" to we.sets.map { s ->
                    hashMapOf(
                        "id" to s.id,
                        "weight" to s.weight,
                        "reps" to s.reps,
                        "notes" to s.notes
                    )
                }
            )
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

    override suspend fun createWorkout(workout: Workout): Result<String> {
        return try {
            val map = mapWorkoutToMap(workout)
            val docRef = firestore.collection("users")
                .document(workout.userId)
                .collection("activities")
                .add(map)
                .await()
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
