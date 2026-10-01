package ch.simibu.sport.domain.repository

import ch.simibu.sport.core.model.TrainingTemplate
import kotlinx.coroutines.flow.Flow

interface TemplateRepository {
    fun getTemplates(userId: String): Flow<List<TrainingTemplate>>
    fun getTemplate(userId: String, templateId: String): Flow<TrainingTemplate?>
    suspend fun createTemplate(template: TrainingTemplate): Result<String>
    suspend fun updateTemplate(template: TrainingTemplate): Result<Unit>
    suspend fun deleteTemplate(userId: String, templateId: String): Result<Unit>
}
