package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BuildStatus
import com.example.data.model.TestStatus
import com.example.data.model.WorkStatus
import com.example.ui.components.CyberActionButton
import com.example.ui.components.StatusBadge
import com.example.ui.components.TechnicalStateChip
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BuildPass
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TestPass
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.WorkstationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedCheckpointScreen(
    projectId: String,
    viewModel: WorkstationViewModel
) {
    val project by viewModel.selectedProject.collectAsState()
    val checkpoint by viewModel.currentCheckpoint.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SHARED CHECKPOINT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = project?.name ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.testTag("edit_checkpoint_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Checkpoint",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Checkpoint Header Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CURRENT PHASE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                        checkpoint?.let { StatusBadge(status = it.workStatus) }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = checkpoint?.currentPhase ?: "PHASE 01: INITIALIZATION",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TechnicalStateChip(
                            label = checkpoint?.buildStatus?.label ?: "BUILD: NONE",
                            passed = when (checkpoint?.buildStatus) {
                                BuildStatus.PASS -> true
                                BuildStatus.FAIL -> false
                                else -> null
                            }
                        )
                        TechnicalStateChip(
                            label = checkpoint?.testStatus?.label ?: "TEST: NONE",
                            passed = when (checkpoint?.testStatus) {
                                TestStatus.PASS -> true
                                TestStatus.FAIL -> false
                                else -> null
                            }
                        )
                    }
                }
            }

            // Checkpoint Fields List
            CheckpointDetailItem(
                label = "PROJECT STATE",
                value = checkpoint?.projectState ?: "N/A"
            )

            CheckpointDetailItem(
                label = "CURRENT TASK",
                value = checkpoint?.currentTask ?: "N/A"
            )

            CheckpointDetailItem(
                label = "GIT MODIFIED STATE",
                value = checkpoint?.gitModifiedState ?: "Clean"
            )

            CheckpointDetailItem(
                label = "LAST AI PROVIDER",
                value = checkpoint?.lastAiProvider ?: "kosong"
            )

            CheckpointDetailItem(
                label = "LAST SUCCESSFUL ACTION",
                value = checkpoint?.lastSuccessfulAction ?: "N/A"
            )

            CheckpointDetailItem(
                label = "LAST ERROR",
                value = if (checkpoint?.lastError.isNullOrBlank()) "None (Zero active errors)" else checkpoint?.lastError ?: "",
                isError = !checkpoint?.lastError.isNullOrBlank()
            )

            CheckpointDetailItem(
                label = "RECOVERY INFORMATION",
                value = checkpoint?.recoveryInformation ?: "Standard checkpoint restoration available."
            )

            CheckpointDetailItem(
                label = "LAST ACTIVITY TIME",
                value = formatTimestamp(checkpoint?.lastActivityTime ?: System.currentTimeMillis())
            )

            Spacer(modifier = Modifier.height(8.dp))

            CyberActionButton(
                text = "PERBARUI / SINKRONKAN CHECKPOINT",
                onClick = { showEditDialog = true },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.Refresh,
                isPrimary = false
            )
        }
    }

    if (showEditDialog && checkpoint != null) {
        EditCheckpointDialog(
            checkpoint = checkpoint!!,
            onDismiss = { showEditDialog = false },
            onSave = { state, task, phase, work, build, test, git, err, recovery ->
                viewModel.updateCheckpointData(
                    projectId = projectId,
                    projectState = state,
                    currentTask = task,
                    currentPhase = phase,
                    workStatus = work,
                    buildStatus = build,
                    testStatus = test,
                    gitModifiedState = git,
                    lastError = err,
                    recoveryInfo = recovery
                )
                showEditDialog = false
            }
        )
    }
}

@Composable
fun CheckpointDetailItem(
    label: String,
    value: String,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, if (isError) AlertRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isError) AlertRed else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) AlertRed else MaterialTheme.colorScheme.onBackground,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun EditCheckpointDialog(
    checkpoint: com.example.data.model.SharedCheckpoint,
    onDismiss: () -> Unit,
    onSave: (
        projectState: String,
        currentTask: String,
        currentPhase: String,
        workStatus: WorkStatus,
        buildStatus: BuildStatus,
        testStatus: TestStatus,
        gitModifiedState: String,
        lastError: String,
        recoveryInfo: String
    ) -> Unit
) {
    var state by remember { mutableStateOf(checkpoint.projectState) }
    var task by remember { mutableStateOf(checkpoint.currentTask) }
    var phase by remember { mutableStateOf(checkpoint.currentPhase) }
    var workStatus by remember { mutableStateOf(checkpoint.workStatus) }
    var buildStatus by remember { mutableStateOf(checkpoint.buildStatus) }
    var testStatus by remember { mutableStateOf(checkpoint.testStatus) }
    var gitState by remember { mutableStateOf(checkpoint.gitModifiedState) }
    var lastError by remember { mutableStateOf(checkpoint.lastError) }
    var recovery by remember { mutableStateOf(checkpoint.recoveryInformation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Shared Checkpoint",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = phase,
                    onValueChange = { phase = it },
                    label = { Text("Phase") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = task,
                    onValueChange = { task = it },
                    label = { Text("Current Task") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text("Project State") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = gitState,
                    onValueChange = { gitState = it },
                    label = { Text("Git State") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = lastError,
                    onValueChange = { lastError = it },
                    label = { Text("Last Error") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = recovery,
                    onValueChange = { recovery = it },
                    label = { Text("Recovery Info") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(state, task, phase, workStatus, buildStatus, testStatus, gitState, lastError, recovery)
                }
            ) {
                Text("SIMPAN", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("BATAL", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
