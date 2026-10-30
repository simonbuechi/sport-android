package ch.simibu.sport.data.repository

import ch.simibu.sport.core.model.ExerciseSet
import ch.simibu.sport.core.model.TemplateExercise
import ch.simibu.sport.core.model.TrainingTemplate
import ch.simibu.sport.domain.repository.TemplateRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
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
class TemplateRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationScope private val scope: CoroutineScope
) : TemplateRepository {

    @Suppress("UNCHECKED_CAST")
    private fun mapDocumentToTemplate(doc: DocumentSnapshot): TrainingTemplate {
        val exercisesRaw = doc.get("exercises") as? List<*>
        val exercises = exercisesRaw?.mapNotNull { item ->
            val map = item as? Map<String, Any?> ?: return@mapNotNull null
            val exerciseId = map["exerciseId"] as? String ?: return@mapNotNull null
            val note = map["note"] as? String
            val supersetId = map["supersetId"] as? String
            val supersetNote = map["supersetNote"] as? String
            val setsRaw = map["sets"] as? List<*>
            val sets = FirestoreSetMapper.parseSets(setsRaw)

            TemplateExercise(
                exerciseId = exerciseId,
                note = note,
                supersetId = supersetId,
                supersetNote = supersetNote,
                sets = sets
            )
        } ?: emptyList()

        return TrainingTemplate(
            id = doc.id,
            userId = doc.getString("userId") ?: "",
            name = doc.getString("name") ?: "",
            notes = doc.getString("notes"),
            isFavorite = doc.getBoolean("isFavorite") ?: false,
            isArchived = doc.getBoolean("isArchived") ?: false,
            exercises = exercises
        )
    }

    private fun mapTemplateToMap(template: TrainingTemplate): Map<String, Any?> {
        val exercisesList = template.exercises.map { te ->
            val exMap = hashMapOf<String, Any?>(
                "exerciseId" to te.exerciseId,
                "note" to te.note,
                "sets" to FirestoreSetMapper.setsToMaps(te.sets)
            )
            if (te.supersetId != null) exMap["supersetId"] = te.supersetId
            if (te.supersetNote != null) exMap["supersetNote"] = te.supersetNote
            exMap
        }

        return hashMapOf(
            "userId" to template.userId,
            "name" to template.name,
            "notes" to template.notes,
            "isFavorite" to template.isFavorite,
            "isArchived" to template.isArchived,
            "exercises" to exercisesList
        )
    }

    private val templatesFlowMap = ConcurrentHashMap<String, SharedFlow<List<TrainingTemplate>>>()

    private fun createTemplatesFlow(userId: String): SharedFlow<List<TrainingTemplate>> {
        return callbackFlow {
            val query = firestore.collection("users")
                .document(userId)
                .collection("templates")

            val registration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val templates = snapshot.documents.map { mapDocumentToTemplate(it) }
                    trySend(templates)
                }
            }
            awaitClose { registration.remove() }
        }.shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 60_000, replayExpirationMillis = 0),
            replay = 1
        )
    }

    override fun getTemplates(userId: String): Flow<List<TrainingTemplate>> {
        return templatesFlowMap.computeIfAbsent(userId) { uid ->
            createTemplatesFlow(uid)
        }
    }

    override fun getTemplate(userId: String, templateId: String): Flow<TrainingTemplate?> {
        return getTemplates(userId).map { list ->
            list.find { it.id == templateId }
        }
    }

    override suspend fun createTemplate(template: TrainingTemplate): Result<String> {
        return try {
            val map = mapTemplateToMap(template)
            val docRef = firestore.collection("users")
                .document(template.userId)
                .collection("templates")
                .add(map)
                .await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTemplate(template: TrainingTemplate): Result<Unit> {
        return try {
            val map = mapTemplateToMap(template)
            firestore.collection("users")
                .document(template.userId)
                .collection("templates")
                .document(template.id)
                .update(map)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTemplate(userId: String, templateId: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("templates")
                .document(templateId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
