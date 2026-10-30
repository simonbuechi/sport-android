package ch.simibu.sport.domain.repository

import ch.simibu.sport.core.model.Program
import kotlinx.coroutines.flow.Flow

interface ProgramRepository {
    fun getPrograms(userId: String): Flow<List<Program>>
    fun getProgram(userId: String, programId: String): Flow<Program?>
    suspend fun createProgram(program: Program): Result<String>
    suspend fun updateProgram(program: Program): Result<Unit>
    suspend fun deleteProgram(userId: String, programId: String): Result<Unit>
    suspend fun setActiveProgram(userId: String, programId: String?): Result<Unit>
}
