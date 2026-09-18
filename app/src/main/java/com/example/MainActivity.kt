package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.AiProviderSelectionScreen
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.ContinueWorkflowScreen
import com.example.ui.screens.CreateProjectScreen
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.screens.IntegrationsScreen
import com.example.ui.screens.ProjectDetailScreen
import com.example.ui.screens.ProjectManagerScreen
import com.example.ui.screens.ProjectMenuScreen
import com.example.ui.screens.ProjectPromptScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SharedCheckpointScreen
import com.example.ui.screens.StartProjectScreen
import com.example.ui.screens.WorkHistoryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.WorkstationViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: WorkstationViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val settings by viewModel.settings.collectAsState()
      MyApplicationTheme(settings = settings) {
        WorkstationApp(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun WorkstationApp(viewModel: WorkstationViewModel) {
  val currentScreen by viewModel.currentScreen.collectAsState()

  BackHandler(enabled = currentScreen != Screen.Dashboard) {
    viewModel.navigateBack()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    when (val screen = currentScreen) {
      is Screen.Dashboard -> MainDashboardScreen(viewModel = viewModel)
      is Screen.ProjectManager -> ProjectManagerScreen(viewModel = viewModel)
      is Screen.CreateProject -> CreateProjectScreen(viewModel = viewModel)
      is Screen.StartProject -> StartProjectScreen(projectId = screen.projectId, viewModel = viewModel)
      is Screen.ProjectMenu -> ProjectMenuScreen(projectId = screen.projectId, viewModel = viewModel)
      is Screen.ContinueWorkflow -> ContinueWorkflowScreen(projectId = screen.projectId, viewModel = viewModel)
      is Screen.ProjectPromptScreen -> ProjectPromptScreen(projectId = screen.projectId, viewModel = viewModel)
      is Screen.SharedCheckpointScreen -> SharedCheckpointScreen(projectId = screen.projectId, viewModel = viewModel)
      is Screen.WorkHistoryScreen -> WorkHistoryScreen(projectId = screen.projectId, viewModel = viewModel)
      is Screen.ProjectDetail -> ProjectDetailScreen(projectId = screen.projectId, viewModel = viewModel)
      is Screen.AiProviderSelection -> AiProviderSelectionScreen(projectId = screen.projectId, viewModel = viewModel)
      is Screen.SettingsScreen -> SettingsScreen(viewModel = viewModel)
      is Screen.Integrations -> IntegrationsScreen(viewModel = viewModel)
      is Screen.About -> AboutScreen(viewModel = viewModel)
    }
  }
}

