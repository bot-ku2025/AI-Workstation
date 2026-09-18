package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.BuildStatus
import com.example.data.model.ProjectType
import com.example.data.model.TestStatus
import com.example.data.model.WorkStatus

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // from ProjectType
    val location: String,
    val createdAt: Long,
    val lastActivity: Long,
    val lastAiProvider: String,
    val currentTask: String,
    val workStatus: String, // from WorkStatus
    val buildStatus: String, // from BuildStatus
    val testStatus: String, // from TestStatus
    val lastError: String,
    val template: String,
    val autoPermission: Boolean
)

@Entity(tableName = "project_prompts")
data class ProjectPromptEntity(
    @PrimaryKey val projectId: String,
    val initialPrompt: String,
    val tasksJson: String // serialized tasks JSON
)

@Entity(tableName = "shared_checkpoints")
data class SharedCheckpointEntity(
    @PrimaryKey val projectId: String,
    val projectState: String,
    val currentTask: String,
    val currentPhase: String,
    val workStatus: String,
    val buildStatus: String,
    val testStatus: String,
    val gitModifiedState: String,
    val lastError: String,
    val lastSuccessfulAction: String,
    val lastAiProvider: String,
    val lastActivityTime: Long,
    val recoveryInformation: String
)

@Entity(tableName = "work_history")
data class WorkHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: String,
    val timestamp: Long,
    val aiProvider: String,
    val task: String,
    val action: String,
    val result: String,
    val buildResult: String,
    val testResult: String,
    val error: String
)

@Entity(tableName = "app_settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val preferredAiProvider: String,
    val autoPermissionDefault: Boolean,
    val termuxBridgePath: String,
    val defaultProjectRoot: String,
    val checkConsistencyBeforeResume: Boolean,
    val enableRealTelemetry: Boolean,
    val themeMode: String,
    val accentColor: String
)
