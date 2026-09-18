package com.example.data.model

enum class ProjectType(val displayName: String, val iconName: String) {
    ANDROID("Android", "android"),
    WEB("Web", "web"),
    DESKTOP("Desktop", "desktop_windows"),
    PYTHON("Python", "code"),
    OTHER("Lainnya", "folder_special")
}

enum class WorkStatus(val label: String) {
    NEW("NEW"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    ERROR("ERROR"),
    PAUSED("PAUSED")
}

enum class BuildStatus(val label: String) {
    PASS("BUILD PASS"),
    FAIL("BUILD FAIL"),
    UNTESTED("UNTESTED"),
    NONE("NONE")
}

enum class TestStatus(val label: String) {
    PASS("TEST PASS"),
    FAIL("TEST FAIL"),
    UNTESTED("UNTESTED"),
    NONE("NONE")
}

enum class ProviderAvailability(val label: String) {
    AVAILABLE("AVAILABLE"),
    UNAVAILABLE("UNAVAILABLE"),
    NOT_CONFIGURED("NOT CONFIGURED"),
    ERROR("ERROR"),
    EXTERNAL("EXTERNAL")
}

data class PromptTask(
    val taskNumber: Int,
    val instruction: String,
    val timestamp: Long
)

data class Project(
    val id: String,
    val name: String,
    val type: ProjectType,
    val location: String,
    val createdAt: Long,
    val lastActivity: Long,
    val lastAiProvider: String, // e.g., "Hermes/Copilot", "Hermes", "Gemini", "kosong"
    val currentTask: String,
    val workStatus: WorkStatus,
    val buildStatus: BuildStatus,
    val testStatus: TestStatus,
    val lastError: String = "",
    val template: String = "Kosong",
    val autoPermission: Boolean = true
)

data class ProjectPrompt(
    val projectId: String,
    val initialPrompt: String,
    val tasks: List<PromptTask> = emptyList()
) {
    fun getFullAccumulatedText(): String {
        val builder = StringBuilder()
        builder.append("INITIAL PROJECT PROMPT\n")
        builder.append(if (initialPrompt.isNotBlank()) initialPrompt else "[Belum ada prompt awal]")
        builder.append("\n\n")

        tasks.forEach { task ->
            val formattedNum = String.format("%03d", task.taskNumber)
            builder.append("TASK $formattedNum\n")
            builder.append(task.instruction)
            builder.append("\n\n")
        }
        return builder.toString().trimEnd()
    }
}

data class SharedCheckpoint(
    val projectId: String,
    val projectState: String,
    val currentTask: String,
    val currentPhase: String,
    val workStatus: WorkStatus,
    val buildStatus: BuildStatus,
    val testStatus: TestStatus,
    val gitModifiedState: String,
    val lastError: String,
    val lastSuccessfulAction: String,
    val lastAiProvider: String,
    val lastActivityTime: Long,
    val recoveryInformation: String
)

data class WorkHistoryItem(
    val id: Long = 0,
    val projectId: String,
    val timestamp: Long,
    val aiProvider: String,
    val task: String,
    val action: String,
    val result: String,
    val buildResult: String,
    val testResult: String,
    val error: String = ""
)

data class AiProviderInfo(
    val id: String,
    val name: String,
    val description: String,
    val availability: ProviderAvailability,
    val supportsYolo: Boolean,
    val cliCommand: String,
    val configKeyOrUrl: String,
    val notes: String
)

data class WorkstationSettings(
    val preferredAiProvider: String = "Gemini",
    val autoPermissionDefault: Boolean = true,
    val termuxBridgePath: String = "/data/data/com.termux/files/home",
    val defaultProjectRoot: String = "/storage/emulated/0/AI_Workstation_Projects",
    val checkConsistencyBeforeResume: Boolean = true,
    val enableRealTelemetry: Boolean = true,
    val themeMode: String = "DARK",
    val accentColor: String = "CYAN"
)
