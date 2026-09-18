package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bridge.AiProviderRegistry
import com.example.bridge.BridgeState
import com.example.bridge.BridgeStatus
import com.example.bridge.CommandRequest
import com.example.bridge.ExecutionBridge
import com.example.bridge.ExecutionLog
import com.example.bridge.RealExecutionBridge
import com.example.data.local.WorkstationDatabase
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
import com.example.data.repository.WorkstationRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    object Dashboard : Screen()
    object ProjectManager : Screen()
    object CreateProject : Screen()
    data class StartProject(val projectId: String) : Screen()
    data class ProjectMenu(val projectId: String) : Screen()
    data class ContinueWorkflow(val projectId: String) : Screen()
    data class ProjectPromptScreen(val projectId: String) : Screen()
    data class SharedCheckpointScreen(val projectId: String) : Screen()
    data class WorkHistoryScreen(val projectId: String) : Screen()
    data class ProjectDetail(val projectId: String) : Screen()
    data class AiProviderSelection(val projectId: String) : Screen()
    object SettingsScreen : Screen()
    object Integrations : Screen()
    object About : Screen()
}

class WorkstationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WorkstationRepository
    private val bridge: ExecutionBridge = RealExecutionBridge(application)

    init {
        val db = WorkstationDatabase.getDatabase(application)
        repository = WorkstationRepository(db.workstationDao())
    }

    val projects: StateFlow<List<Project>> = repository.getProjectsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<WorkstationSettings> = repository.getSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkstationSettings())

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val screenBackStack = mutableListOf<Screen>()

    private val _selectedProject = MutableStateFlow<Project?>(null)
    val selectedProject: StateFlow<Project?> = _selectedProject.asStateFlow()

    private val _currentPrompt = MutableStateFlow<ProjectPrompt?>(null)
    val currentPrompt: StateFlow<ProjectPrompt?> = _currentPrompt.asStateFlow()

    private val _currentCheckpoint = MutableStateFlow<SharedCheckpoint?>(null)
    val currentCheckpoint: StateFlow<SharedCheckpoint?> = _currentCheckpoint.asStateFlow()

    private val _currentHistory = MutableStateFlow<List<WorkHistoryItem>>(emptyList())
    val currentHistory: StateFlow<List<WorkHistoryItem>> = _currentHistory.asStateFlow()

    private val _bridgeStatus = MutableStateFlow<BridgeStatus>(
        BridgeStatus(
            state = BridgeState.STANDALONE_METADATA_MODE,
            bridgePath = "/data/data/com.termux/files/home",
            message = "Bridge initialized in local workstation mode",
            isExecutable = false
        )
    )
    val bridgeStatus: StateFlow<BridgeStatus> = _bridgeStatus.asStateFlow()

    private val _executionLogs = MutableStateFlow<List<ExecutionLog>>(emptyList())
    val executionLogs: StateFlow<List<ExecutionLog>> = _executionLogs.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _consistencyIssues = MutableStateFlow<List<String>>(emptyList())
    val consistencyIssues: StateFlow<List<String>> = _consistencyIssues.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        checkBridge()
        // No synthetic projects are seeded. Empty means the device has no real projects yet.
    }

    fun checkBridge() {
        viewModelScope.launch {
            val s = repository.getSettings()
            val status = bridge.checkAvailability(s.termuxBridgePath)
            _bridgeStatus.value = status
        }
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            screenBackStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack() {
        if (screenBackStack.isNotEmpty()) {
            val prev = screenBackStack.removeAt(screenBackStack.size - 1)
            _currentScreen.value = prev
            // Refresh loaded data for previous screen if it has projectId
            when (prev) {
                is Screen.ProjectMenu -> loadProjectData(prev.projectId)
                is Screen.ProjectDetail -> loadProjectData(prev.projectId)
                is Screen.ProjectPromptScreen -> loadProjectData(prev.projectId)
                is Screen.SharedCheckpointScreen -> loadProjectData(prev.projectId)
                is Screen.WorkHistoryScreen -> loadProjectData(prev.projectId)
                is Screen.ContinueWorkflow -> loadProjectData(prev.projectId)
                is Screen.AiProviderSelection -> loadProjectData(prev.projectId)
                else -> {}
            }
        } else {
            _currentScreen.value = Screen.Dashboard
        }
    }

    fun loadProjectData(projectId: String) {
        viewModelScope.launch {
            val p = repository.getProject(projectId)
            _selectedProject.value = p
            val pr = repository.getPrompt(projectId)
            _currentPrompt.value = pr
            val cp = repository.getCheckpoint(projectId)
            _currentCheckpoint.value = cp

            if (p != null) {
                val issues = bridge.verifyConsistency(p, cp)
                _consistencyIssues.value = issues
            }

            repository.getWorkHistoryFlow(projectId).collect { items ->
                _currentHistory.value = items
            }
        }
    }

    fun createProject(
        name: String,
        type: ProjectType,
        firstAi: String,
        template: String,
        customPath: String? = null
    ): String {
        val projectId = UUID.randomUUID().toString().take(8)
        val safeName = name.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "_")
        val location = customPath?.takeIf { it.isNotBlank() }
            ?: "/storage/emulated/0/AI_Workstation_Projects/$safeName"
        val now = System.currentTimeMillis()

        val newProject = Project(
            id = projectId,
            name = name.trim(),
            type = type,
            location = location,
            createdAt = now,
            lastActivity = now,
            lastAiProvider = firstAi,
            currentTask = "Project Initialized",
            workStatus = WorkStatus.NEW,
            buildStatus = BuildStatus.NONE,
            testStatus = TestStatus.NONE,
            lastError = "",
            template = template,
            autoPermission = true
        )

        val defaultPromptText = when (template) {
            "Android + Compose" -> "Arsitektur Android Native dengan Kotlin, Jetpack Compose, Clean Architecture, Material 3, dan Room Database."
            "Sesuai instruksi AI" -> "Silakan analisa struktur project dan rekomendasikan arsitektur terbaik."
            else -> ""
        }

        val initialPrompt = ProjectPrompt(
            projectId = projectId,
            initialPrompt = defaultPromptText,
            tasks = emptyList()
        )

        val initialCheckpoint = SharedCheckpoint(
            projectId = projectId,
            projectState = "Project initialized via template [$template]",
            currentTask = "Project setup & configuration",
            currentPhase = "PHASE 01: INITIALIZATION",
            workStatus = WorkStatus.NEW,
            buildStatus = BuildStatus.NONE,
            testStatus = TestStatus.NONE,
            gitModifiedState = "Repository initialized",
            lastError = "",
            lastSuccessfulAction = "Created project '$name' ($type)",
            lastAiProvider = firstAi,
            lastActivityTime = now,
            recoveryInformation = "Initial snapshot created cleanly."
        )

        val initialHistory = WorkHistoryItem(
            projectId = projectId,
            timestamp = now,
            aiProvider = firstAi,
            task = "Project creation",
            action = "Initialized $name ($type) using template $template",
            result = "SUCCESS",
            buildResult = "N/A",
            testResult = "N/A",
            error = ""
        )

        viewModelScope.launch {
            repository.saveProject(newProject)
            repository.savePrompt(initialPrompt)
            repository.saveCheckpoint(initialCheckpoint)
            repository.addWorkHistoryItem(initialHistory)
            loadProjectData(projectId)
        }

        return projectId
    }

    fun startProjectWithOption(projectId: String, option: Int, inputPrompt: String = "") {
        viewModelScope.launch {
            val project = repository.getProject(projectId) ?: return@launch
            val now = System.currentTimeMillis()

            val finalInitialPrompt = when (option) {
                1 -> "Project ${project.name} (${project.type.displayName}): Buat modul terstruktur dengan standar arsitektur clean code, type safety, dan dokumentasi komprehensif."
                2 -> ""
                3 -> inputPrompt.trim()
                else -> ""
            }

            val updatedPrompt = ProjectPrompt(
                projectId = projectId,
                initialPrompt = finalInitialPrompt,
                tasks = emptyList()
            )
            repository.savePrompt(updatedPrompt)

            val updatedCheckpoint = (repository.getCheckpoint(projectId) ?: SharedCheckpoint(
                projectId = projectId,
                projectState = "Ready",
                currentTask = "Ready for instructions",
                currentPhase = "PHASE 01: READY",
                workStatus = WorkStatus.NEW,
                buildStatus = BuildStatus.NONE,
                testStatus = TestStatus.NONE,
                gitModifiedState = "Clean",
                lastError = "",
                lastSuccessfulAction = "Project started",
                lastAiProvider = project.lastAiProvider,
                lastActivityTime = now,
                recoveryInformation = "Clean startup state"
            )).copy(
                projectState = "Project prompt configured. Ready for first task execution.",
                currentTask = if (finalInitialPrompt.isNotBlank()) "Execute initial prompt instructions" else "Waiting for instructions",
                currentPhase = "PHASE 01: READY",
                workStatus = WorkStatus.NEW,
                lastActivityTime = now
            )
            repository.saveCheckpoint(updatedCheckpoint)

            repository.addWorkHistoryItem(
                WorkHistoryItem(
                    projectId = projectId,
                    timestamp = now,
                    aiProvider = project.lastAiProvider,
                    task = "Start Project",
                    action = when (option) {
                        1 -> "Started project with standard specialized prompt"
                        2 -> "Started project without initial prompt"
                        3 -> "Started project with custom initial prompt"
                        else -> "Started project"
                    },
                    result = "SUCCESS",
                    buildResult = "N/A",
                    testResult = "N/A"
                )
            )

            loadProjectData(projectId)
            navigateTo(Screen.ProjectMenu(projectId))
        }
    }

    fun appendInstruction(projectId: String, instructionText: String, taskName: String = "") {
        if (instructionText.isBlank()) return
        viewModelScope.launch {
            val project = repository.getProject(projectId) ?: return@launch
            val updatedPrompt = repository.appendTaskToPrompt(projectId, instructionText.trim())
            val now = System.currentTimeMillis()
            val taskNum = updatedPrompt.tasks.size
            val taskLabel = if (taskName.isNotBlank()) taskName else "Task #${String.format("%03d", taskNum)}"

            // Update project
            val updatedProject = project.copy(
                currentTask = taskLabel,
                workStatus = WorkStatus.IN_PROGRESS,
                lastActivity = now
            )
            repository.saveProject(updatedProject)

            // Update checkpoint
            val checkpoint = repository.getCheckpoint(projectId)
            val updatedCheckpoint = (checkpoint ?: SharedCheckpoint(
                projectId = projectId,
                projectState = "Active",
                currentTask = taskLabel,
                currentPhase = "PHASE 02: EXECUTION",
                workStatus = WorkStatus.IN_PROGRESS,
                buildStatus = BuildStatus.NONE,
                testStatus = TestStatus.NONE,
                gitModifiedState = "Modified files pending execution",
                lastError = "",
                lastSuccessfulAction = "Appended $taskLabel",
                lastAiProvider = project.lastAiProvider,
                lastActivityTime = now,
                recoveryInformation = "State synced after appending task."
            )).copy(
                currentTask = taskLabel,
                workStatus = WorkStatus.IN_PROGRESS,
                lastSuccessfulAction = "Instruksi $taskLabel ditambahkan ke persistent prompt",
                lastActivityTime = now
            )
            repository.saveCheckpoint(updatedCheckpoint)

            // Add history
            repository.addWorkHistoryItem(
                WorkHistoryItem(
                    projectId = projectId,
                    timestamp = now,
                    aiProvider = project.lastAiProvider,
                    task = taskLabel,
                    action = "Menambahkan instruksi baru (TASK ${String.format("%03d", taskNum)})",
                    result = "PROMPT APPENDED",
                    buildResult = "N/A",
                    testResult = "N/A"
                )
            )

            loadProjectData(projectId)
            _statusMessage.value = "Instruksi TASK ${String.format("%03d", taskNum)} berhasil ditambahkan ke Prompt."
        }
    }

    fun switchAiProvider(projectId: String, newProviderId: String) {
        viewModelScope.launch {
            val project = repository.getProject(projectId) ?: return@launch
            val oldProvider = project.lastAiProvider
            val now = System.currentTimeMillis()

            val updatedProject = project.copy(
                lastAiProvider = newProviderId,
                lastActivity = now
            )
            repository.saveProject(updatedProject)

            val checkpoint = repository.getCheckpoint(projectId)
            if (checkpoint != null) {
                val updatedCheckpoint = checkpoint.copy(
                    lastAiProvider = newProviderId,
                    lastActivityTime = now,
                    lastSuccessfulAction = "Dialihkan dari AI '$oldProvider' ke '$newProviderId' dengan shared context dipertahankan."
                )
                repository.saveCheckpoint(updatedCheckpoint)
            }

            repository.addWorkHistoryItem(
                WorkHistoryItem(
                    projectId = projectId,
                    timestamp = now,
                    aiProvider = newProviderId,
                    task = "Switch AI Provider",
                    action = "Beralih provider dari [$oldProvider] -> [$newProviderId]. Shared Checkpoint dimuat.",
                    result = "CONTEXT PRESERVED",
                    buildResult = "N/A",
                    testResult = "N/A"
                )
            )

            loadProjectData(projectId)
            _statusMessage.value = "AI Provider aktif diganti ke $newProviderId (Shared context terjaga)."
        }
    }

    fun updateCheckpointData(
        projectId: String,
        projectState: String,
        currentTask: String,
        currentPhase: String,
        workStatus: WorkStatus,
        buildStatus: BuildStatus,
        testStatus: TestStatus,
        gitModifiedState: String,
        lastError: String,
        recoveryInfo: String
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val project = repository.getProject(projectId) ?: return@launch
            val oldCheckpoint = repository.getCheckpoint(projectId)

            val updatedCheckpoint = SharedCheckpoint(
                projectId = projectId,
                projectState = projectState,
                currentTask = currentTask,
                currentPhase = currentPhase,
                workStatus = workStatus,
                buildStatus = buildStatus,
                testStatus = testStatus,
                gitModifiedState = gitModifiedState,
                lastError = lastError,
                lastSuccessfulAction = "Checkpoint manual sync / update",
                lastAiProvider = project.lastAiProvider,
                lastActivityTime = now,
                recoveryInformation = recoveryInfo
            )
            repository.saveCheckpoint(updatedCheckpoint)

            val updatedProject = project.copy(
                currentTask = currentTask,
                workStatus = workStatus,
                buildStatus = buildStatus,
                testStatus = testStatus,
                lastError = lastError,
                lastActivity = now
            )
            repository.saveProject(updatedProject)

            repository.addWorkHistoryItem(
                WorkHistoryItem(
                    projectId = projectId,
                    timestamp = now,
                    aiProvider = project.lastAiProvider,
                    task = currentTask,
                    action = "Update Shared Checkpoint [Phase: $currentPhase, Status: ${workStatus.label}]",
                    result = "CHECKPOINT UPDATED",
                    buildResult = buildStatus.label,
                    testResult = testStatus.label,
                    error = lastError
                )
            )

            loadProjectData(projectId)
            _statusMessage.value = "Shared Checkpoint berhasil diperbarui dan disinkronkan."
        }
    }

    fun executeResumeWorkflow(projectId: String) {
        viewModelScope.launch {
            val project = repository.getProject(projectId) ?: return@launch
            val prompt = repository.getPrompt(projectId) ?: ProjectPrompt(projectId, "", emptyList())
            val checkpoint = repository.getCheckpoint(projectId) ?: SharedCheckpoint(
                projectId = projectId,
                projectState = "Resume State",
                currentTask = project.currentTask,
                currentPhase = "PHASE 02: RESUME",
                workStatus = WorkStatus.IN_PROGRESS,
                buildStatus = project.buildStatus,
                testStatus = project.testStatus,
                gitModifiedState = "Synced",
                lastError = project.lastError,
                lastSuccessfulAction = "Resumed workflow",
                lastAiProvider = project.lastAiProvider,
                lastActivityTime = System.currentTimeMillis(),
                recoveryInformation = "Resumed from stored checkpoint"
            )

            _isExecuting.value = true
            _executionLogs.value = emptyList()

            val request = CommandRequest(
                projectId = projectId,
                projectPath = project.location,
                providerId = project.lastAiProvider,
                instruction = project.currentTask,
                yoloEnabled = project.autoPermission,
                accumulatedPrompt = prompt.getFullAccumulatedText(),
                checkpoint = checkpoint
            )

            var realExecutionSucceeded = false
            bridge.execute(request).collect { log ->
                _executionLogs.value = _executionLogs.value + log
                if (log.tag == "SUCCESS" && !log.isError) {
                    realExecutionSucceeded = true
                }
            }

            // Never mutate the project/checkpoint to a completed workflow unless the bridge
            // received an actual Termux exit code 0 from the dispatched CLI process.
            if (realExecutionSucceeded) {
                val now = System.currentTimeMillis()
                val updatedProject = project.copy(
                    lastActivity = now,
                    workStatus = WorkStatus.IN_PROGRESS
                )
                repository.saveProject(updatedProject)

                val updatedCp = checkpoint.copy(
                    lastActivityTime = now,
                    lastSuccessfulAction = "Workflow dioperasikan via provider [${project.lastAiProvider}]"
                )
                repository.saveCheckpoint(updatedCp)

                repository.addWorkHistoryItem(
                    WorkHistoryItem(
                        projectId = projectId,
                        timestamp = now,
                        aiProvider = project.lastAiProvider,
                        task = project.currentTask,
                        action = "Melanjutkan pengerjaan task via ${project.lastAiProvider}",
                        result = "WORKFLOW STEP COMPLETED",
                        buildResult = project.buildStatus.label,
                        testResult = project.testStatus.label
                    )
                )
            }

            _isExecuting.value = false
            loadProjectData(projectId)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            if (_selectedProject.value?.id == projectId) {
                _selectedProject.value = null
            }
            navigateTo(Screen.Dashboard)
        }
    }

    fun saveSettings(newSettings: WorkstationSettings) {
        viewModelScope.launch {
            repository.saveSettings(newSettings)
            checkBridge()
            _statusMessage.value = "Pengaturan workstation berhasil disimpan."
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }


}
