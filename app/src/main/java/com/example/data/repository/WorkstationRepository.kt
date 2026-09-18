package com.example.data.repository

import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectPromptEntity
import com.example.data.local.SettingsEntity
import com.example.data.local.SharedCheckpointEntity
import com.example.data.local.WorkHistoryEntity
import com.example.data.local.WorkstationDao
import com.example.data.model.BuildStatus
import com.example.data.model.Project
import com.example.data.model.ProjectPrompt
import com.example.data.model.ProjectType
import com.example.data.model.PromptTask
import com.example.data.model.SharedCheckpoint
import com.example.data.model.TestStatus
import com.example.data.model.WorkHistoryItem
import com.example.data.model.WorkStatus
import com.example.data.model.WorkstationSettings
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkstationRepository(private val dao: WorkstationDao) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val taskListType = Types.newParameterizedType(List::class.java, PromptTask::class.java)
    private val taskListAdapter = moshi.adapter<List<PromptTask>>(taskListType)

    // --- Projects ---
    fun getProjectsFlow(): Flow<List<Project>> {
        return dao.getAllProjectsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getProject(projectId: String): Project? {
        return dao.getProjectById(projectId)?.toDomain()
    }

    fun getProjectFlow(projectId: String): Flow<Project?> {
        return dao.getProjectByIdFlow(projectId).map { it?.toDomain() }
    }

    suspend fun saveProject(project: Project) {
        dao.insertProject(project.toEntity())
    }

    suspend fun deleteProject(projectId: String) {
        dao.deleteProjectById(projectId)
        dao.deletePromptByProjectId(projectId)
        dao.deleteCheckpointByProjectId(projectId)
        dao.deleteWorkHistoryByProjectId(projectId)
    }

    // --- Prompts ---
    suspend fun getPrompt(projectId: String): ProjectPrompt? {
        return dao.getPromptByProjectId(projectId)?.let { entity ->
            val tasks = parseTasksJson(entity.tasksJson)
            ProjectPrompt(entity.projectId, entity.initialPrompt, tasks)
        }
    }

    fun getPromptFlow(projectId: String): Flow<ProjectPrompt?> {
        return dao.getPromptByProjectIdFlow(projectId).map { entity ->
            entity?.let {
                val tasks = parseTasksJson(it.tasksJson)
                ProjectPrompt(it.projectId, it.initialPrompt, tasks)
            }
        }
    }

    suspend fun savePrompt(prompt: ProjectPrompt) {
        val tasksJson = taskListAdapter.toJson(prompt.tasks)
        dao.insertOrUpdatePrompt(
            ProjectPromptEntity(
                projectId = prompt.projectId,
                initialPrompt = prompt.initialPrompt,
                tasksJson = tasksJson
            )
        )
    }

    suspend fun appendTaskToPrompt(projectId: String, instruction: String): ProjectPrompt {
        val existing = getPrompt(projectId) ?: ProjectPrompt(projectId, "", emptyList())
        val nextTaskNum = existing.tasks.size + 1
        val newTask = PromptTask(
            taskNumber = nextTaskNum,
            instruction = instruction,
            timestamp = System.currentTimeMillis()
        )
        val updatedTasks = existing.tasks + newTask
        val updatedPrompt = existing.copy(tasks = updatedTasks)
        savePrompt(updatedPrompt)
        return updatedPrompt
    }

    // --- Checkpoints ---
    suspend fun getCheckpoint(projectId: String): SharedCheckpoint? {
        return dao.getCheckpointByProjectId(projectId)?.toDomain()
    }

    fun getCheckpointFlow(projectId: String): Flow<SharedCheckpoint?> {
        return dao.getCheckpointByProjectIdFlow(projectId).map { it?.toDomain() }
    }

    suspend fun saveCheckpoint(checkpoint: SharedCheckpoint) {
        dao.insertOrUpdateCheckpoint(checkpoint.toEntity())
    }

    // --- Work History ---
    fun getWorkHistoryFlow(projectId: String): Flow<List<WorkHistoryItem>> {
        return dao.getWorkHistoryByProjectIdFlow(projectId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun addWorkHistoryItem(item: WorkHistoryItem) {
        dao.insertWorkHistory(item.toEntity())
    }

    // --- Settings ---
    suspend fun getSettings(): WorkstationSettings {
        return dao.getSettings()?.toDomain() ?: WorkstationSettings()
    }

    fun getSettingsFlow(): Flow<WorkstationSettings> {
        return dao.getSettingsFlow().map { it?.toDomain() ?: WorkstationSettings() }
    }

    suspend fun saveSettings(settings: WorkstationSettings) {
        dao.saveSettings(settings.toEntity())
    }

    // --- Helper JSON conversion ---
    private fun parseTasksJson(json: String): List<PromptTask> {
        return try {
            if (json.isBlank()) emptyList() else taskListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- Mappings ---
    private fun ProjectEntity.toDomain(): Project {
        return Project(
            id = id,
            name = name,
            type = runCatching { ProjectType.valueOf(type) }.getOrDefault(ProjectType.OTHER),
            location = location,
            createdAt = createdAt,
            lastActivity = lastActivity,
            lastAiProvider = lastAiProvider,
            currentTask = currentTask,
            workStatus = runCatching { WorkStatus.valueOf(workStatus) }.getOrDefault(WorkStatus.NEW),
            buildStatus = runCatching { BuildStatus.valueOf(buildStatus) }.getOrDefault(BuildStatus.NONE),
            testStatus = runCatching { TestStatus.valueOf(testStatus) }.getOrDefault(TestStatus.NONE),
            lastError = lastError,
            template = template,
            autoPermission = autoPermission
        )
    }

    private fun Project.toEntity(): ProjectEntity {
        return ProjectEntity(
            id = id,
            name = name,
            type = type.name,
            location = location,
            createdAt = createdAt,
            lastActivity = lastActivity,
            lastAiProvider = lastAiProvider,
            currentTask = currentTask,
            workStatus = workStatus.name,
            buildStatus = buildStatus.name,
            testStatus = testStatus.name,
            lastError = lastError,
            template = template,
            autoPermission = autoPermission
        )
    }

    private fun SharedCheckpointEntity.toDomain(): SharedCheckpoint {
        return SharedCheckpoint(
            projectId = projectId,
            projectState = projectState,
            currentTask = currentTask,
            currentPhase = currentPhase,
            workStatus = runCatching { WorkStatus.valueOf(workStatus) }.getOrDefault(WorkStatus.NEW),
            buildStatus = runCatching { BuildStatus.valueOf(buildStatus) }.getOrDefault(BuildStatus.NONE),
            testStatus = runCatching { TestStatus.valueOf(testStatus) }.getOrDefault(TestStatus.NONE),
            gitModifiedState = gitModifiedState,
            lastError = lastError,
            lastSuccessfulAction = lastSuccessfulAction,
            lastAiProvider = lastAiProvider,
            lastActivityTime = lastActivityTime,
            recoveryInformation = recoveryInformation
        )
    }

    private fun SharedCheckpoint.toEntity(): SharedCheckpointEntity {
        return SharedCheckpointEntity(
            projectId = projectId,
            projectState = projectState,
            currentTask = currentTask,
            currentPhase = currentPhase,
            workStatus = workStatus.name,
            buildStatus = buildStatus.name,
            testStatus = testStatus.name,
            gitModifiedState = gitModifiedState,
            lastError = lastError,
            lastSuccessfulAction = lastSuccessfulAction,
            lastAiProvider = lastAiProvider,
            lastActivityTime = lastActivityTime,
            recoveryInformation = recoveryInformation
        )
    }

    private fun WorkHistoryEntity.toDomain(): WorkHistoryItem {
        return WorkHistoryItem(
            id = id,
            projectId = projectId,
            timestamp = timestamp,
            aiProvider = aiProvider,
            task = task,
            action = action,
            result = result,
            buildResult = buildResult,
            testResult = testResult,
            error = error
        )
    }

    private fun WorkHistoryItem.toEntity(): WorkHistoryEntity {
        return WorkHistoryEntity(
            id = id,
            projectId = projectId,
            timestamp = timestamp,
            aiProvider = aiProvider,
            task = task,
            action = action,
            result = result,
            buildResult = buildResult,
            testResult = testResult,
            error = error
        )
    }

    private fun SettingsEntity.toDomain(): WorkstationSettings {
        return WorkstationSettings(
            preferredAiProvider = preferredAiProvider,
            autoPermissionDefault = autoPermissionDefault,
            termuxBridgePath = termuxBridgePath,
            defaultProjectRoot = defaultProjectRoot,
            checkConsistencyBeforeResume = checkConsistencyBeforeResume,
            enableRealTelemetry = enableRealTelemetry,
            themeMode = themeMode,
            accentColor = accentColor
        )
    }

    private fun WorkstationSettings.toEntity(): SettingsEntity {
        return SettingsEntity(
            id = 1,
            preferredAiProvider = preferredAiProvider,
            autoPermissionDefault = autoPermissionDefault,
            termuxBridgePath = termuxBridgePath,
            defaultProjectRoot = defaultProjectRoot,
            checkConsistencyBeforeResume = checkConsistencyBeforeResume,
            enableRealTelemetry = enableRealTelemetry,
            themeMode = themeMode,
            accentColor = accentColor
        )
    }
}
