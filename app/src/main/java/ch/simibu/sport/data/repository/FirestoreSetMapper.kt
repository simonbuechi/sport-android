package ch.simibu.sport.data.repository

import ch.simibu.sport.core.model.ExerciseSet
import java.util.UUID

internal object FirestoreSetMapper {

    fun parseSets(setsRaw: List<*>?): List<ExerciseSet> {
        return setsRaw?.mapNotNull { setItem ->
            val setMap = setItem as? Map<*, *> ?: return@mapNotNull null
            val weight = (setMap["weight"] as? Number)?.toDouble()
            val reps = (setMap["reps"] as? Number)?.toInt()
            val rpe = (setMap["rpe"] as? Number)?.toDouble()
            val rir = (setMap["rir"] as? Number)?.toDouble()

            ExerciseSet(
                id = setMap["id"] as? String ?: UUID.randomUUID().toString(),
                weight = weight,
                reps = reps,
                notes = setMap["notes"] as? String,
                rpe = rpe,
                rir = rir
            )
        } ?: emptyList()
    }

    fun setsToMaps(sets: List<ExerciseSet>): List<Map<String, Any?>> {
        return sets.map { s ->
            val sMap = hashMapOf<String, Any?>(
                "id" to s.id,
                "weight" to s.weight,
                "reps" to s.reps,
                "notes" to s.notes
            )
            if (s.rpe != null) sMap["rpe"] = s.rpe
            if (s.rir != null) sMap["rir"] = s.rir
            sMap
        }
    }
}
