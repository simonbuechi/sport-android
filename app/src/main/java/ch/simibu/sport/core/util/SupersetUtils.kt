package ch.simibu.sport.core.util

import ch.simibu.sport.core.model.TemplateExercise
import ch.simibu.sport.core.model.WorkoutExercise
import java.util.UUID

data class SupersetItemWrapper<T>(
    val item: T,
    val originalIndex: Int,
    val tag: String? = null // e.g. "A1", "A2"
)

data class SupersetGroup<T>(
    val isSuperset: Boolean,
    val supersetId: String? = null,
    val letter: String? = null, // e.g. "A"
    val label: String? = null, // e.g. "Superset A"
    val note: String? = null,
    val items: List<SupersetItemWrapper<T>>
)

object SupersetUtils {

    /**
     * Converts a 0-based index to an alphabetical label (0 -> A, 1 -> B, ..., 26 -> AA).
     */
    fun getAlphabeticalLabel(index: Int): String {
        var result = ""
        var current = index
        while (current >= 0) {
            result = ('A'.code + (current % 26)).toChar().toString() + result
            current = (current / 26) - 1
        }
        return result
    }

    /**
     * Generates a unique superset ID.
     */
    fun generateSupersetId(): String {
        return "ss_${System.currentTimeMillis()}_${UUID.randomUUID().toString().replace("-", "").take(6)}"
    }

    /**
     * Groups an array of workout exercises into sequential blocks (standalone exercises or supersets).
     */
    fun groupWorkoutExercises(
        exercises: List<WorkoutExercise>
    ): List<SupersetGroup<WorkoutExercise>> {
        return groupIntoBlocks(
            exercises = exercises,
            getSupersetId = { it.supersetId },
            getSupersetNote = { it.supersetNote }
        )
    }

    /**
     * Groups an array of template exercises into sequential blocks (standalone exercises or supersets).
     */
    fun groupTemplateExercises(
        exercises: List<TemplateExercise>
    ): List<SupersetGroup<TemplateExercise>> {
        return groupIntoBlocks(
            exercises = exercises,
            getSupersetId = { it.supersetId },
            getSupersetNote = { it.supersetNote }
        )
    }

    private fun <T> groupIntoBlocks(
        exercises: List<T>,
        getSupersetId: (T) -> String?,
        getSupersetNote: (T) -> String?
    ): List<SupersetGroup<T>> {
        if (exercises.isEmpty()) return emptyList()

        // Count occurrences of each supersetId
        val supersetCounts = mutableMapOf<String, Int>()
        for (ex in exercises) {
            val sid = getSupersetId(ex)
            if (!sid.isNullOrBlank()) {
                supersetCounts[sid] = (supersetCounts[sid] ?: 0) + 1
            }
        }

        // Map valid supersetIds (at least 2 exercises) to alphabetical letters
        val supersetLetterMap = mutableMapOf<String, String>()
        var letterIndex = 0
        for (ex in exercises) {
            val sid = getSupersetId(ex)
            if (!sid.isNullOrBlank() && (supersetCounts[sid] ?: 0) >= 2) {
                if (!supersetLetterMap.containsKey(sid)) {
                    supersetLetterMap[sid] = getAlphabeticalLabel(letterIndex++)
                }
            }
        }

        val groups = mutableListOf<SupersetGroup<T>>()
        var currentGroup: SupersetGroup<T>? = null
        val supersetItemCounters = mutableMapOf<String, Int>()

        for (originalIndex in exercises.indices) {
            val ex = exercises[originalIndex]
            val sid = getSupersetId(ex)
            val isValidSuperset = !sid.isNullOrBlank() && (supersetCounts[sid] ?: 0) >= 2

            if (isValidSuperset && !sid.isNullOrBlank()) {
                val letter = supersetLetterMap[sid] ?: "A"
                val count = (supersetItemCounters[sid] ?: 0) + 1
                supersetItemCounters[sid] = count
                val tag = "$letter$count"
                val note = getSupersetNote(ex)

                if (currentGroup != null && currentGroup.isSuperset && currentGroup.supersetId == sid) {
                    val updatedItems = currentGroup.items + SupersetItemWrapper(ex, originalIndex, tag)
                    val mergedNote = if (currentGroup.note.isNullOrBlank() && !note.isNullOrBlank()) note else currentGroup.note
                    currentGroup = currentGroup.copy(items = updatedItems, note = mergedNote)
                } else {
                    if (currentGroup != null) {
                        groups.add(currentGroup)
                    }
                    currentGroup = SupersetGroup(
                        isSuperset = true,
                        supersetId = sid,
                        letter = letter,
                        label = "Superset $letter",
                        note = note,
                        items = listOf(SupersetItemWrapper(ex, originalIndex, tag))
                    )
                }
            } else {
                if (currentGroup != null) {
                    groups.add(currentGroup)
                    currentGroup = null
                }
                groups.add(
                    SupersetGroup(
                        isSuperset = false,
                        items = listOf(SupersetItemWrapper(ex, originalIndex))
                    )
                )
            }
        }

        if (currentGroup != null) {
            groups.add(currentGroup)
        }

        return groups
    }

    private fun <T> linkItemsWithNext(
        exercises: List<T>,
        index: Int,
        getSupersetId: (T) -> String?,
        getSupersetNote: (T) -> String?,
        updateSuperset: (T, String?, String?) -> T
    ): List<T> {
        if (index < 0 || index >= exercises.size - 1) return exercises
        val current = exercises[index]
        val next = exercises[index + 1]

        val targetSupersetId = getSupersetId(current)?.ifBlank { null }
            ?: getSupersetId(next)?.ifBlank { null }
            ?: generateSupersetId()
        val targetNote = getSupersetNote(current)?.ifBlank { null }
            ?: getSupersetNote(next)?.ifBlank { null }

        return exercises.mapIndexed { idx, ex ->
            if (idx == index || idx == index + 1) {
                updateSuperset(ex, targetSupersetId, targetNote)
            } else {
                ex
            }
        }
    }

    private fun <T> unlinkItemFromSuperset(
        exercises: List<T>,
        index: Int,
        getSupersetId: (T) -> String?,
        getSupersetNote: (T) -> String?,
        updateSuperset: (T, String?, String?) -> T
    ): List<T> {
        if (index < 0 || index >= exercises.size) return exercises
        val sid = getSupersetId(exercises[index]) ?: return exercises

        val unlinked = exercises.mapIndexed { idx, ex ->
            if (idx == index) updateSuperset(ex, null, null) else ex
        }

        // If only 1 exercise remains with that supersetId, ungroup it as well
        val remaining = unlinked.filter { getSupersetId(it) == sid }
        return if (remaining.size == 1) {
            unlinked.map { if (getSupersetId(it) == sid) updateSuperset(it, null, null) else it }
        } else {
            unlinked
        }
    }

    private fun <T> updateItemsSupersetNote(
        exercises: List<T>,
        supersetId: String,
        note: String,
        getSupersetId: (T) -> String?,
        updateSuperset: (T, String?, String?) -> T
    ): List<T> {
        val cleanNote = note.trim().ifBlank { null }
        return exercises.map { ex ->
            if (getSupersetId(ex) == supersetId) {
                updateSuperset(ex, supersetId, cleanNote)
            } else {
                ex
            }
        }
    }

    // --- WorkoutExercise actions ---

    fun linkWithNext(exercises: List<WorkoutExercise>, index: Int): List<WorkoutExercise> =
        linkItemsWithNext(
            exercises = exercises,
            index = index,
            getSupersetId = { it.supersetId },
            getSupersetNote = { it.supersetNote },
            updateSuperset = { ex, sid, note -> ex.copy(supersetId = sid, supersetNote = note) }
        )

    fun unlinkFromSuperset(exercises: List<WorkoutExercise>, index: Int): List<WorkoutExercise> =
        unlinkItemFromSuperset(
            exercises = exercises,
            index = index,
            getSupersetId = { it.supersetId },
            getSupersetNote = { it.supersetNote },
            updateSuperset = { ex, sid, note -> ex.copy(supersetId = sid, supersetNote = note) }
        )

    fun updateSupersetNote(exercises: List<WorkoutExercise>, supersetId: String, note: String): List<WorkoutExercise> =
        updateItemsSupersetNote(
            exercises = exercises,
            supersetId = supersetId,
            note = note,
            getSupersetId = { it.supersetId },
            updateSuperset = { ex, sid, n -> ex.copy(supersetId = sid, supersetNote = n) }
        )

    // --- TemplateExercise actions ---

    fun linkTemplateWithNext(exercises: List<TemplateExercise>, index: Int): List<TemplateExercise> =
        linkItemsWithNext(
            exercises = exercises,
            index = index,
            getSupersetId = { it.supersetId },
            getSupersetNote = { it.supersetNote },
            updateSuperset = { ex, sid, note -> ex.copy(supersetId = sid, supersetNote = note) }
        )

    fun unlinkTemplateFromSuperset(exercises: List<TemplateExercise>, index: Int): List<TemplateExercise> =
        unlinkItemFromSuperset(
            exercises = exercises,
            index = index,
            getSupersetId = { it.supersetId },
            getSupersetNote = { it.supersetNote },
            updateSuperset = { ex, sid, note -> ex.copy(supersetId = sid, supersetNote = note) }
        )

    fun updateTemplateSupersetNote(exercises: List<TemplateExercise>, supersetId: String, note: String): List<TemplateExercise> =
        updateItemsSupersetNote(
            exercises = exercises,
            supersetId = supersetId,
            note = note,
            getSupersetId = { it.supersetId },
            updateSuperset = { ex, sid, n -> ex.copy(supersetId = sid, supersetNote = n) }
        )
}
