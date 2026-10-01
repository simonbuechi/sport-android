package ch.simibu.sport.data.repository

import ch.simibu.sport.core.model.BodyPart
import ch.simibu.sport.core.model.Exercise
import ch.simibu.sport.core.model.ExerciseCategory
import ch.simibu.sport.core.model.ExerciseType
import ch.simibu.sport.domain.repository.ExerciseRepository
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationScope private val scope: CoroutineScope
) : ExerciseRepository {

    private fun mapDocumentToExercise(doc: DocumentSnapshot): Exercise {
        val typeStr = doc.getString("type")
        val bodyPartStr = doc.getString("bodypart")
        val categoryStr = doc.getString("category")
        val aliasesRaw = doc.get("aliases") as? List<*>
        val aliases = aliasesRaw?.filterIsInstance<String>() ?: emptyList()

        return Exercise(
            id = doc.id,
            name = doc.getString("name") ?: "",
            type = ExerciseType.fromString(typeStr),
            bodypart = BodyPart.fromString(bodyPartStr),
            category = ExerciseCategory.fromString(categoryStr),
            aliases = aliases,
            description = doc.getString("description"),
            iconUrl = doc.getString("icon_url"),
            show = doc.getBoolean("show") ?: true,
            popular = doc.getBoolean("popular") ?: false
        )
    }

    private val exercisesSharedFlow: SharedFlow<List<Exercise>> = callbackFlow {
        val query = firestore.collection("exercises")
            .orderBy("popular", Query.Direction.DESCENDING)
            .orderBy("name", Query.Direction.ASCENDING)
            .limit(2000)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val exercises = snapshot.documents.map { mapDocumentToExercise(it) }
                trySend(exercises)
            }
        }
        awaitClose { registration.remove() }
    }.shareIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 60_000, replayExpirationMillis = 0),
        replay = 1
    )

    override fun getExercises(): Flow<List<Exercise>> = exercisesSharedFlow

    override fun getExercise(id: String): Flow<Exercise?> = exercisesSharedFlow.map { list ->
        list.find { it.id == id }
    }

    override suspend fun createExercise(exercise: Exercise): Result<String> {
        return try {
            val map = hashMapOf<String, Any?>(
                "name" to exercise.name,
                "type" to exercise.type.value,
                "bodypart" to exercise.bodypart.displayName,
                "category" to exercise.category.displayName,
                "aliases" to exercise.aliases,
                "description" to exercise.description,
                "icon_url" to exercise.iconUrl,
                "show" to exercise.show,
                "popular" to exercise.popular
            )
            val docRef = firestore.collection("exercises").add(map).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateExercise(exercise: Exercise): Result<Unit> {
        return try {
            val map = hashMapOf<String, Any?>(
                "name" to exercise.name,
                "type" to exercise.type.value,
                "bodypart" to exercise.bodypart.displayName,
                "category" to exercise.category.displayName,
                "aliases" to exercise.aliases,
                "description" to exercise.description,
                "icon_url" to exercise.iconUrl,
                "show" to exercise.show,
                "popular" to exercise.popular
            )
            firestore.collection("exercises").document(exercise.id).update(map).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteExercise(id: String): Result<Unit> {
        return try {
            firestore.collection("exercises").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
