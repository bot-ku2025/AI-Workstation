package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.components.CyberActionButton
import com.example.ui.components.StatusBadge
import com.example.ui.components.TechnicalStateChip
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.AlertRed
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.WorkstationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    viewModel: WorkstationViewModel
) {
    val project by viewModel.selectedProject.collectAsState()
    val checkpoint by viewModel.currentCheckpoint.collectAsState()
    val history by viewModel.currentHistory.collectAsState()
    val bridgeStatus by viewModel.bridgeStatus.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "DETAIL PROJECT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onBackground
                    )
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
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_project_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Project",
                            tint = AlertRed
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
            // Identity Header Card
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
                            text = project?.name ?: "",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        project?.let { StatusBadge(status = it.workStatus) }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "ID: ${project?.id} • Type: ${project?.type?.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Location: ${project?.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TechnicalStateChip(
                            label = project?.buildStatus?.label ?: "BUILD: NONE",
                            passed = when (project?.buildStatus) {
                                BuildStatus.PASS -> true
                                BuildStatus.FAIL -> false
                                else -> null
                            }
                        )
                        TechnicalStateChip(
                            label = project?.testStatus?.label ?: "TEST: NONE",
                            passed = when (project?.testStatus) {
                                TestStatus.PASS -> true
                                TestStatus.FAIL -> false
                                else -> null
                            }
                        )
                    }
                }
            }

            // Technical Attributes Table
            DetailFieldItem(label = "CREATED AT", value = formatTimestamp(project?.createdAt ?: System.currentTimeMillis()))
            DetailFieldItem(label = "LAST ACTIVITY", value = formatTimestamp(project?.lastActivity ?: System.currentTimeMillis()))
            DetailFieldItem(label = "LAST AI USED", value = project?.lastAiProvider ?: "kosong")
            DetailFieldItem(label = "CURRENT TASK", value = project?.currentTask ?: "-")
            DetailFieldItem(label = "TEMPLATE", value = project?.template ?: "-")
            DetailFieldItem(
                label = "LAST ERROR",
                value = if (project?.lastError.isNullOrBlank()) "None (Clean)" else project?.lastError ?: "",
                isError = !project?.lastError.isNullOrBlank()
            )

            // External Execution Availability Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "EXTERNAL EXECUTION AVAILABILITY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "State: ${bridgeStatus.state.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (bridgeStatus.isExecutable) TerminalGreen else WarningAmber,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = bridgeStatus.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Navigations to Checkpoint, History, Prompt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CyberActionButton(
                    text = "CHECKPOINT",
                    onClick = { viewModel.navigateTo(Screen.SharedCheckpointScreen(projectId)) },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Sync,
                    isPrimary = false
                )
                CyberActionButton(
                    text = "HISTORY",
                    onClick = { viewModel.navigateTo(Screen.WorkHistoryScreen(projectId)) },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.History,
                    isPrimary = false
                )
                CyberActionButton(
                    text = "PROMPT",
                    onClick = { viewModel.navigateTo(Screen.ProjectPromptScreen(projectId)) },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Description,
                    isPrimary = false
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            CyberActionButton(
                text = "LANJUTKAN WORKFLOW",
                onClick = { viewModel.navigateTo(Screen.ContinueWorkflow(projectId)) },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.PlayArrow,
                isPrimary = true
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "Hapus Project?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AlertRed
                )
            },
            text = {
                Text(
                    text = "Semua checkpoint, prompt persisten, dan riwayat kerja untuk project '${project?.name}' akan dihapus secara permanen.",
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteProject(projectId)
                    }
                ) {
                    Text("HAPUS", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("BATAL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun DetailFieldItem(
    label: String,
    value: String,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = if (isError) AlertRed else MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
