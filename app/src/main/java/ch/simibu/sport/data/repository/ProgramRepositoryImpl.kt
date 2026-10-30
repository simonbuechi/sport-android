package ch.simibu.sport.data.repository

import ch.simibu.sport.core.di.ApplicationScope
import ch.simibu.sport.core.model.Program
import ch.simibu.sport.core.model.ProgramDay
import ch.simibu.sport.domain.repository.ProgramRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgramRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationScope private val scope: CoroutineScope
) : ProgramRepository {

    @Suppress("UNCHECKED_CAST")
    private fun mapDocumentToProgram(doc: DocumentSnapshot): Program {
        val daysRaw = doc.get("days") as? List<*>
        val days = daysRaw?.mapNotNull { item ->
            val map = item as? Map<String, Any?> ?: return@mapNotNull null
            val id = map["id"] as? String ?: UUID.randomUUID().toString()
            val dayNumber = (map["dayNumber"] as? Number)?.toInt() ?: 1
            val date = map["date"] as? String
            val templateId = map["templateId"] as? String
            val note = map["note"] as? String
            ProgramDay(
                id = id,
                dayNumber = dayNumber,
                date = date,
                templateId = templateId,
                note = note
            )
        } ?: emptyList()

        return Program(
            id = doc.id,
            userId = doc.getString("userId") ?: "",
            name = doc.getString("name") ?: "",
            description = doc.getString("description"),
            type = doc.getString("type") ?: Program.TYPE_REPETITIVE,
            isActive = doc.getBoolean("isActive") ?: false,
            startDate = doc.getString("startDate"),
            currentDayIndex = (doc.get("currentDayIndex") as? Number)?.toInt() ?: 0,
            days = days,
            createdAt = doc.getString("createdAt"),
            updatedAt = doc.getString("updatedAt")
        )
    }

    private fun mapProgramToMap(program: Program): Map<String, Any?> {
        val daysList = program.days.map { day ->
            val dayMap = hashMapOf<String, Any?>(
                "id" to day.id,
                "dayNumber" to day.dayNumber
            )
            if (!day.date.isNullOrBlank()) dayMap["date"] = day.date
            if (!day.templateId.isNullOrBlank()) dayMap["templateId"] = day.templateId
            if (!day.note.isNullOrBlank()) dayMap["note"] = day.note
            dayMap
        }

        val map = hashMapOf<String, Any?>(
            "userId" to program.userId,
            "name" to program.name,
            "description" to program.description,
            "type" to program.type,
            "isActive" to program.isActive,
            "startDate" to program.startDate,
            "currentDayIndex" to program.currentDayIndex,
            "days" to daysList,
            "updatedAt" to getIsoTimestamp()
        )
        if (!program.createdAt.isNullOrBlank()) {
            map["createdAt"] = program.createdAt
        }
        return map
    }

    private val programsFlowMap = ConcurrentHashMap<String, SharedFlow<List<Program>>>()

    private fun createProgramsFlow(userId: String): SharedFlow<List<Program>> {
        return callbackFlow {
            val query = firestore.collection("users")
                .document(userId)
                .collection("programs")

            val registration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val programs = snapshot.documents.map { mapDocumentToProgram(it) }
                    trySend(programs)
                }
            }
            awaitClose { registration.remove() }
        }.shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 60_000, replayExpirationMillis = 0),
            replay = 1
        )
    }

    override fun getPrograms(userId: String): Flow<List<Program>> {
        return programsFlowMap.computeIfAbsent(userId) { uid ->
            createProgramsFlow(uid)
        }
    }

    override fun getProgram(userId: String, programId: String): Flow<Program?> {
        return getPrograms(userId).map { list ->
            list.find { it.id == programId }
        }
    }

    override suspend fun createProgram(program: Program): Result<String> {
        return try {
            val map = mapProgramToMap(program).toMutableMap()
            map["createdAt"] = getIsoTimestamp()
            val docRef = firestore.collection("users")
                .document(program.userId)
                .collection("programs")
                .add(map)
                .await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProgram(program: Program): Result<Unit> {
        return try {
            val map = mapProgramToMap(program)
            firestore.collection("users")
                .document(program.userId)
                .collection("programs")
                .document(program.id)
                .update(map)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProgram(userId: String, programId: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("programs")
                .document(programId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setActiveProgram(userId: String, programId: String?): Result<Unit> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("programs")
                .get()
                .await()

            firestore.runBatch { batch ->
                for (doc in snapshot.documents) {
                    val shouldBeActive = doc.id == programId
                    val isCurrentlyActive = doc.getBoolean("isActive") ?: false
                    if (shouldBeActive != isCurrentlyActive) {
                        batch.update(doc.reference, "isActive", shouldBeActive)
                    }
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getIsoTimestamp(): String = Instant.now().toString()
}
