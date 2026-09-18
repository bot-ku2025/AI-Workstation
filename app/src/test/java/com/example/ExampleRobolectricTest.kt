package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectPromptEntity
import com.example.data.local.SharedCheckpointEntity
import com.example.data.local.WorkstationDao
import com.example.data.local.WorkstationDatabase
import com.example.data.model.BuildStatus
import com.example.data.model.ProjectType
import com.example.data.model.TestStatus
import com.example.data.model.WorkStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  private lateinit var db: WorkstationDatabase
  private lateinit var dao: WorkstationDao

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, WorkstationDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    dao = db.workstationDao()
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun read_appName_from_context() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AI Workstation", appName)
  }

  @Test
  fun test_project_and_checkpoint_persistence() = runBlocking {
    val now = System.currentTimeMillis()
    val project = ProjectEntity(
      id = "test-proj-01",
      name = "Test Workstation Project",
      type = ProjectType.ANDROID.name,
      location = "/projects/test",
      createdAt = now,
      lastActivity = now,
      lastAiProvider = "Hermes",
      currentTask = "Initial Setup",
      workStatus = WorkStatus.IN_PROGRESS.name,
      buildStatus = BuildStatus.PASS.name,
      testStatus = TestStatus.PASS.name,
      lastError = "",
      template = "Android + Compose",
      autoPermission = true
    )
    dao.insertProject(project)

    val fetched = dao.getProjectById("test-proj-01")
    assertNotNull(fetched)
    assertEquals("Test Workstation Project", fetched?.name)
    assertEquals("Hermes", fetched?.lastAiProvider)

    val checkpoint = SharedCheckpointEntity(
      projectId = "test-proj-01",
      projectState = "Active",
      currentTask = "Initial Setup",
      currentPhase = "PHASE 01: READY",
      workStatus = WorkStatus.IN_PROGRESS.name,
      buildStatus = BuildStatus.PASS.name,
      testStatus = TestStatus.PASS.name,
      gitModifiedState = "Clean",
      lastError = "",
      lastSuccessfulAction = "Setup completed",
      lastAiProvider = "Hermes",
      lastActivityTime = now,
      recoveryInformation = "Stable"
    )
    dao.insertOrUpdateCheckpoint(checkpoint)

    val fetchedCp = dao.getCheckpointByProjectId("test-proj-01")
    assertNotNull(fetchedCp)
    assertEquals("PHASE 01: READY", fetchedCp?.currentPhase)
  }
}

