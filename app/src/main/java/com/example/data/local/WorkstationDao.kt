package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkstationDao {

    // --- Projects ---
    @Query("SELECT * FROM projects ORDER BY lastActivity DESC")
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId LIMIT 1")
    suspend fun getProjectById(projectId: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :projectId LIMIT 1")
    fun getProjectByIdFlow(projectId: String): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: String)

    // --- Project Prompts ---
    @Query("SELECT * FROM project_prompts WHERE projectId = :projectId LIMIT 1")
    suspend fun getPromptByProjectId(projectId: String): ProjectPromptEntity?

    @Query("SELECT * FROM project_prompts WHERE projectId = :projectId LIMIT 1")
    fun getPromptByProjectIdFlow(projectId: String): Flow<ProjectPromptEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePrompt(prompt: ProjectPromptEntity)

    @Query("DELETE FROM project_prompts WHERE projectId = :projectId")
    suspend fun deletePromptByProjectId(projectId: String)

    // --- Shared Checkpoints ---
    @Query("SELECT * FROM shared_checkpoints WHERE projectId = :projectId LIMIT 1")
    suspend fun getCheckpointByProjectId(projectId: String): SharedCheckpointEntity?

    @Query("SELECT * FROM shared_checkpoints WHERE projectId = :projectId LIMIT 1")
    fun getCheckpointByProjectIdFlow(projectId: String): Flow<SharedCheckpointEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCheckpoint(checkpoint: SharedCheckpointEntity)

    @Query("DELETE FROM shared_checkpoints WHERE projectId = :projectId")
    suspend fun deleteCheckpointByProjectId(projectId: String)

    // --- Work History ---
    @Query("SELECT * FROM work_history WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getWorkHistoryByProjectIdFlow(projectId: String): Flow<List<WorkHistoryEntity>>

    @Query("SELECT * FROM work_history WHERE projectId = :projectId ORDER BY timestamp DESC")
    suspend fun getWorkHistoryByProjectId(projectId: String): List<WorkHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkHistory(item: WorkHistoryEntity)

    @Query("DELETE FROM work_history WHERE projectId = :projectId")
    suspend fun deleteWorkHistoryByProjectId(projectId: String)

    // --- Settings ---
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): SettingsEntity?

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<SettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: SettingsEntity)
}
