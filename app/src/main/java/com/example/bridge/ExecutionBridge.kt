package com.example.bridge

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import com.example.data.model.BuildStatus
import com.example.data.model.Project
import com.example.data.model.SharedCheckpoint
import com.example.data.model.TestStatus
import com.example.data.model.WorkStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class BridgeState {
    BRIDGE_ONLINE,
    BRIDGE_OFFLINE,
    TERMUX_NOT_INSTALLED,
    PERMISSION_DENIED,
    STANDALONE_METADATA_MODE
}

data class BridgeStatus(
    val state: BridgeState,
    val bridgePath: String,
    val message: String,
    val isExecutable: Boolean
)

data class CommandRequest(
    val projectId: String,
    val projectPath: String,
    val providerId: String,
    val instruction: String,
    val yoloEnabled: Boolean,
    val accumulatedPrompt: String,
    val checkpoint: SharedCheckpoint
)

data class ExecutionLog(
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val isError: Boolean = false
)

data class ExecutionResult(
    val isSuccess: Boolean,
    val output: String,
    val errorMessage: String = "",
    val suggestedBuildStatus: BuildStatus = BuildStatus.NONE,
    val suggestedTestStatus: TestStatus = TestStatus.NONE,
    val updatedCheckpoint: SharedCheckpoint? = null
)

interface ExecutionBridge {
    fun checkAvailability(termuxPath: String): BridgeStatus
    fun execute(request: CommandRequest): Flow<ExecutionLog>
    suspend fun verifyConsistency(project: Project, checkpoint: SharedCheckpoint?): List<String>
}

class RealExecutionBridge(private val context: Context) : ExecutionBridge {

    companion object {
        private const val TERMUX_RUN_COMMAND = "com.termux.RUN_COMMAND"
        private const val EXTRA_PATH = "com.termux.RUN_COMMAND_PATH"
        private const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        private const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
        private const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
        private const val EXTRA_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"
        private const val EXTRA_COMMAND_LABEL = "com.termux.RUN_COMMAND_COMMAND_LABEL"

        private const val RESULT_EXTRA = "com.termux.RUN_COMMAND_RESULT"
        private const val RESULT_STDOUT = "stdout"
        private const val RESULT_STDERR = "stderr"
        private const val RESULT_EXIT_CODE = "exitCode"

        private val pending = ConcurrentHashMap<String, CompletableDeferred<TermuxResult>>()

        fun deliverResult(requestId: String, result: TermuxResult) {
            pending.remove(requestId)?.complete(result)
        }
    }

    data class TermuxResult(
        val stdout: String,
        val stderr: String,
        val exitCode: Int
    )

    override fun checkAvailability(termuxPath: String): BridgeStatus {
        val termuxInstalled = try {
            context.packageManager.getApplicationInfo("com.termux", PackageManager.GET_META_DATA)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

        return if (termuxInstalled) {
            BridgeStatus(
                state = BridgeState.BRIDGE_ONLINE,
                bridgePath = termuxPath,
                message = "Termux terdeteksi. RUN_COMMAND IPC nyata siap; akses external-app harus diaktifkan di Termux.",
                isExecutable = true
            )
        } else {
            BridgeStatus(
                state = BridgeState.TERMUX_NOT_INSTALLED,
                bridgePath = termuxPath,
                message = "Termux tidak terpasang. Local execution belum tersedia.",
                isExecutable = false
            )
        }
    }

    override suspend fun verifyConsistency(project: Project, checkpoint: SharedCheckpoint?): List<String> {
        val issues = mutableListOf<String>()
        if (checkpoint == null) {
            issues.add("Shared Checkpoint belum dibuat untuk project ini.")
            return issues
        }
        if (checkpoint.projectId != project.id) {
            issues.add("Checkpoint mismatch: ID checkpoint (${checkpoint.projectId}) berbeda dengan ID project (${project.id}).")
        }
        if (project.workStatus == WorkStatus.IN_PROGRESS && checkpoint.workStatus == WorkStatus.COMPLETED) {
            issues.add("Inkonsistensi status: Project bertanda IN_PROGRESS tetapi Checkpoint tercatat COMPLETED.")
        }
        if (project.buildStatus != checkpoint.buildStatus) {
            issues.add("Build status mismatch: Project [${project.buildStatus.label}] vs Checkpoint [${checkpoint.buildStatus.label}].")
        }
        return issues
    }

    override fun execute(request: CommandRequest): Flow<ExecutionLog> = flow {
        emit(ExecutionLog("BRIDGE", "Android → Termux RUN_COMMAND: memulai task [${request.projectId}]"))
        emit(ExecutionLog("CONTEXT", "Checkpoint phase=${request.checkpoint.currentPhase}; status=${request.checkpoint.workStatus.label}"))
        emit(ExecutionLog("PROMPT", "Persistent Prompt: ${request.accumulatedPrompt.lines().size} baris"))
        emit(ExecutionLog("PROVIDER", "Provider=${request.providerId.uppercase()} YOLO=${request.yoloEnabled}"))

        val status = checkAvailability("/data/data/com.termux/files/home")
        if (!status.isExecutable) {
            emit(ExecutionLog("BRIDGE_ERROR", status.message, true))
            return@flow
        }

        val command = buildProviderCommand(request)
        if (command == null) {
            emit(ExecutionLog("PROVIDER_ERROR", "Provider ${request.providerId} belum memiliki adapter CLI yang aman/terdefinisi.", true))
            return@flow
        }

        val requestId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<TermuxResult>()
        pending[requestId] = deferred

        val promptB64 = Base64.encodeToString(buildFullPrompt(request).toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

        // The prompt is encoded before crossing the Android→Termux boundary, avoiding shell
        // interpolation of arbitrary user text. Termux writes it to the selected project and
        // the provider CLI consumes that file.
        val shellScript = """
            set -e
            cd ${shellQuote(request.projectPath)}
            printf '%s' '$promptB64' | base64 -d > .ai-workstation-prompt.txt
            $command
            rc=$?
            rm -f .ai-workstation-prompt.txt
            exit $rc
        """.trimIndent()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestId.hashCode(),
            Intent(context, TermuxResultReceiver::class.java).apply {
                putExtra(TermuxResultReceiver.EXTRA_REQUEST_ID, requestId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val intent = Intent(TERMUX_RUN_COMMAND).apply {
            setPackage("com.termux")
            putExtra(EXTRA_PATH, "/data/data/com.termux/files/usr/bin/bash")
            putExtra(EXTRA_ARGUMENTS, arrayOf("-lc", shellScript))
            putExtra(EXTRA_WORKDIR, request.projectPath)
            putExtra(EXTRA_BACKGROUND, true)
            putExtra(EXTRA_PENDING_INTENT, pendingIntent)
            putExtra(EXTRA_COMMAND_LABEL, "AI Workstation: ${request.providerId}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.sendBroadcast(intent)
        } catch (e: SecurityException) {
            pending.remove(requestId)
            emit(ExecutionLog("PERMISSION_ERROR", "Termux menolak RUN_COMMAND: ${e.message}", true))
            return@flow
        } catch (e: Exception) {
            pending.remove(requestId)
            emit(ExecutionLog("IPC_ERROR", "Gagal mengirim command ke Termux: ${e.message}", true))
            return@flow
        }

        emit(ExecutionLog("DISPATCHED", "Command benar-benar dikirim ke Termux. Menunggu exit code..."))

        val result = try {
            deferred.await()
        } catch (e: Exception) {
            pending.remove(requestId)
            emit(ExecutionLog("IPC_ERROR", "Menunggu hasil Termux gagal: ${e.message}", true))
            return@flow
        }

        if (result.stdout.isNotBlank()) {
            emit(ExecutionLog("STDOUT", result.stdout.trimEnd()))
        }
        if (result.stderr.isNotBlank()) {
            emit(ExecutionLog("STDERR", result.stderr.trimEnd(), result.exitCode != 0))
        }

        if (result.exitCode == 0) {
            emit(ExecutionLog("SUCCESS", "Termux exit code=0. Provider process selesai tanpa error."))
        } else {
            emit(ExecutionLog("FAILED", "Termux exit code=${result.exitCode}. SUCCESS tidak dicatat.", true))
        }
    }

    private fun buildFullPrompt(request: CommandRequest): String =
        buildString {
            appendLine("AI WORKSTATION EXECUTION CONTEXT")
            appendLine("Project ID: ${request.projectId}")
            appendLine("Project path: ${request.projectPath}")
            appendLine("Provider: ${request.providerId}")
            appendLine("YOLO enabled: ${request.yoloEnabled}")
            appendLine()
            appendLine("PROJECT PROMPT:")
            appendLine(request.accumulatedPrompt)
            appendLine()
            appendLine("CURRENT CHECKPOINT:")
            appendLine(request.checkpoint.toString())
            appendLine()
            appendLine("CURRENT TASK:")
            appendLine(request.instruction)
            appendLine()
            appendLine("AUTONOMOUS LOOP:")
            appendLine("INSPECT → IMPLEMENT → BUILD → ERROR? → READ ACTUAL ERROR → FIX → BUILD AGAIN → TEST → VERIFY → CONTINUE")
        }

    private fun buildProviderCommand(request: CommandRequest): String? {
        val promptFile = ".ai-workstation-prompt.txt"
        return when (request.providerId.lowercase()) {
            "copilot" -> "copilot ${if (request.yoloEnabled) "--yolo " else ""}-p \"$(cat $promptFile)\""
            "claude" -> "claude ${if (request.yoloEnabled) "--dangerously-skip-permissions " else ""}\"$(cat $promptFile)\""
            "gemini" -> "gemini ${if (request.yoloEnabled) "--yolo " else ""}\"$(cat $promptFile)\""
            "hermes" -> "hermes-agent ${if (request.yoloEnabled) "-y " else ""}\"$(cat $promptFile)\""
            else -> null
        }
    }

    private fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"
}

class TermuxResultReceiver : android.content.BroadcastReceiver() {
    companion object {
        const val EXTRA_REQUEST_ID = "ai_workstation.request_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_REQUEST_ID) ?: return
        val resultBundle = intent.getBundleExtra("com.termux.RUN_COMMAND_RESULT") ?: Bundle.EMPTY
        val stdout = resultBundle.getString("stdout").orEmpty()
        val stderr = resultBundle.getString("stderr").orEmpty()
        val exitCode = resultBundle.getInt("exitCode", -1)
        RealExecutionBridge.deliverResult(id, RealExecutionBridge.TermuxResult(stdout, stderr, exitCode))
    }
}
