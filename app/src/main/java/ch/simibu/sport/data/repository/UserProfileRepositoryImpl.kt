package ch.simibu.sport.data.repository

import ch.simibu.sport.core.model.MarkedStatus
import ch.simibu.sport.core.model.MeasurementEntry
import ch.simibu.sport.core.model.UserProfile
import ch.simibu.sport.core.model.UserSettings
import ch.simibu.sport.core.model.WeightEntry
import ch.simibu.sport.domain.repository.UserProfileRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import ch.simibu.sport.core.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationScope private val scope: CoroutineScope
) : UserProfileRepository {

    @Suppress("UNCHECKED_CAST")
    private fun mapDocumentToUserProfile(doc: DocumentSnapshot): UserProfile {
        val weightsRaw = doc.get("weights") as? List<*>
        val weights = weightsRaw?.mapNotNull { item ->
            val map = item as? Map<String, Any?> ?: return@mapNotNull null
            val weightKg = when (val w = map["weightKg"]) {
                is Number -> w.toDouble()
                else -> return@mapNotNull null
            }
            val bodyFat = when (val bf = map["bodyFatPercent"]) {
                is Number -> bf.toDouble()
                else -> null
            }
            WeightEntry(
                id = map["id"] as? String ?: "",
                date = map["date"] as? String ?: "",
                weightKg = weightKg,
                bodyFatPercent = bodyFat
            )
        } ?: emptyList()

        val measurementsRaw = doc.get("measurements") as? List<*>
        val measurements = measurementsRaw?.mapNotNull { item ->
            val map = item as? Map<String, Any?> ?: return@mapNotNull null
            MeasurementEntry(
                id = map["id"] as? String ?: "",
                date = map["date"] as? String ?: "",
                waist = (map["waist"] as? Number)?.toDouble(),
                hips = (map["hips"] as? Number)?.toDouble(),
                neck = (map["neck"] as? Number)?.toDouble(),
                chest = (map["chest"] as? Number)?.toDouble(),
                shoulders = (map["shoulders"] as? Number)?.toDouble(),
                rightBicep = (map["rightBicep"] as? Number)?.toDouble(),
                leftBicep = (map["leftBicep"] as? Number)?.toDouble(),
                rightForearm = (map["rightForearm"] as? Number)?.toDouble(),
                leftForearm = (map["leftForearm"] as? Number)?.toDouble(),
                rightThigh = (map["rightThigh"] as? Number)?.toDouble(),
                leftThigh = (map["leftThigh"] as? Number)?.toDouble(),
                rightCalf = (map["rightCalf"] as? Number)?.toDouble(),
                leftCalf = (map["leftCalf"] as? Number)?.toDouble()
            )
        } ?: emptyList()

        val markedExercisesRaw = doc.get("markedExercises") as? Map<String, *>
        val markedExercises = markedExercisesRaw?.mapNotNull { (exerciseId, value) ->
            val map = value as? Map<String, Any?> ?: return@mapNotNull null
            exerciseId to MarkedStatus(
                favorite = map["favorite"] as? Boolean ?: false,
                notes = map["notes"] as? String
            )
        }?.toMap() ?: emptyMap()

        val settingsMap = doc.get("settings") as? Map<String, Any?>
        val settings = UserSettings(
            theme = settingsMap?.get("theme") as? String ?: "system",
            autoFillSets = settingsMap?.get("autoFillSets") as? Boolean ?: true,
            showTimer = settingsMap?.get("showTimer") as? Boolean ?: true,
            takeWorkoutTime = settingsMap?.get("takeWorkoutTime") as? Boolean ?: true,
            enableSupersets = settingsMap?.get("enableSupersets") as? Boolean ?: false,
            enableExertionTracking = settingsMap?.get("enableExertionTracking") as? Boolean ?: false,
            exertionMode = settingsMap?.get("exertionMode") as? String ?: "rpe",
            enableHealthConnect = settingsMap?.get("enableHealthConnect") as? Boolean ?: false
        )

        return UserProfile(
            uid = doc.id,
            name = doc.getString("name") ?: "",
            birthYear = (doc.get("birthYear") as? Number)?.toInt(),
            height = (doc.get("height") as? Number)?.toDouble(),
            notes = doc.getString("notes") ?: "",
            markedExercises = markedExercises,
            weights = weights,
            measurements = measurements,
            settings = settings
        )
    }

    private fun getCurrentIsoTimestamp(): String = Instant.now().toString()

    private val userProfileFlowMap = ConcurrentHashMap<String, SharedFlow<UserProfile?>>()

    private fun createUserProfileFlow(userId: String): SharedFlow<UserProfile?> {
        return callbackFlow {
            val docRef = firestore.collection("users").document(userId)
            val registration = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(mapDocumentToUserProfile(snapshot))
                } else {
                    trySend(null)
                }
            }
            awaitClose { registration.remove() }
        }.shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 60_000, replayExpirationMillis = 0),
            replay = 1
        )
    }

    override fun getUserProfile(userId: String): Flow<UserProfile?> {
        return userProfileFlowMap.computeIfAbsent(userId) { uid ->
            createUserProfileFlow(uid)
        }
    }

    override suspend fun createUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            val map = hashMapOf(
                "uid" to profile.uid,
                "name" to profile.name,
                "notes" to profile.notes,
                "updatedAt" to getCurrentIsoTimestamp()
            )
            firestore.collection("users").document(profile.uid).set(map, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            val map: Map<String, Any?> = mapOf(
                "name" to profile.name,
                "birthYear" to profile.birthYear,
                "height" to profile.height,
                "notes" to profile.notes,
                "settings" to mapOf<String, Any?>(
                    "theme" to profile.settings.theme,
                    "autoFillSets" to profile.settings.autoFillSets,
                    "showTimer" to profile.settings.showTimer,
                    "takeWorkoutTime" to profile.settings.takeWorkoutTime,
                    "enableSupersets" to profile.settings.enableSupersets,
                    "enableExertionTracking" to profile.settings.enableExertionTracking,
                    "exertionMode" to profile.settings.exertionMode,
                    "enableHealthConnect" to profile.settings.enableHealthConnect
                ),
                "updatedAt" to getCurrentIsoTimestamp()
            )
            firestore.collection("users").document(profile.uid).update(map).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addWeightEntry(userId: String, entry: WeightEntry): Result<Unit> {
        return try {
            val map = hashMapOf(
                "id" to entry.id,
                "date" to entry.date,
                "weightKg" to entry.weightKg,
                "bodyFatPercent" to entry.bodyFatPercent
            )
            firestore.collection("users").document(userId).update(
                "weights", FieldValue.arrayUnion(map),
                "updatedAt", getCurrentIsoTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addMeasurementEntry(userId: String, entry: MeasurementEntry): Result<Unit> {
        return try {
            val map = hashMapOf<String, Any?>(
                "id" to entry.id,
                "date" to entry.date,
                "waist" to entry.waist,
                "hips" to entry.hips,
                "neck" to entry.neck,
                "chest" to entry.chest,
                "shoulders" to entry.shoulders,
                "rightBicep" to entry.rightBicep,
                "leftBicep" to entry.leftBicep,
                "rightForearm" to entry.rightForearm,
                "leftForearm" to entry.leftForearm,
                "rightThigh" to entry.rightThigh,
                "leftThigh" to entry.leftThigh,
                "rightCalf" to entry.rightCalf,
                "leftCalf" to entry.leftCalf
            )
            firestore.collection("users").document(userId).update(
                "measurements", FieldValue.arrayUnion(map),
                "updatedAt", getCurrentIsoTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateExerciseStatus(
        userId: String,
        exerciseId: String,
        favorite: Boolean?,
        notes: String?
    ): Result<Unit> {
        return try {
            val docRef = firestore.collection("users").document(userId)
            val updatedAt = getCurrentIsoTimestamp()
            when {
                favorite != null && notes != null -> {
                    docRef.update(
                        FieldPath.of("markedExercises", exerciseId, "favorite"), favorite,
                        FieldPath.of("markedExercises", exerciseId, "notes"), notes,
                        FieldPath.of("updatedAt"), updatedAt
                    ).await()
                }
                favorite != null -> {
                    docRef.update(
                        FieldPath.of("markedExercises", exerciseId, "favorite"), favorite,
                        FieldPath.of("updatedAt"), updatedAt
                    ).await()
                }
                notes != null -> {
                    docRef.update(
                        FieldPath.of("markedExercises", exerciseId, "notes"), notes,
                        FieldPath.of("updatedAt"), updatedAt
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
