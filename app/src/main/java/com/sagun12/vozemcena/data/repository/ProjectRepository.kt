package com.sagun12.vozemcena.data.repository

import com.sagun12.vozemcena.data.local.ProjectDao
import com.sagun12.vozemcena.data.local.toDomain
import com.sagun12.vozemcena.data.local.toEntity
import com.sagun12.vozemcena.domain.model.AspectRatio
import com.sagun12.vozemcena.domain.model.MediaType
import com.sagun12.vozemcena.domain.model.Project
import com.sagun12.vozemcena.domain.model.ProjectStatus
import com.sagun12.vozemcena.domain.model.Scene
import com.sagun12.vozemcena.domain.model.TransitionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class ProjectRepository(
    private val projectDao: ProjectDao
) {
    fun getAllProjects(): Flow<List<Project>> {
        return projectDao.getAllProjectsWithScenes().map { list ->
            list.map { it.project.toDomain(it.scenes) }
        }
    }

    fun getProjectById(projectId: String): Flow<Project?> {
        return projectDao.getProjectWithScenesById(projectId).map { projectWithScenes ->
            projectWithScenes?.let { it.project.toDomain(it.scenes) }
        }
    }

    suspend fun getProjectDirect(projectId: String): Project? = withContext(Dispatchers.IO) {
        val result = projectDao.getProjectWithScenesDirect(projectId)
        result?.let { it.project.toDomain(it.scenes) }
    }

    suspend fun createNewProject(
        title: String = "Novo Projeto",
        aspectRatio: AspectRatio = AspectRatio.RATIO_16_9,
        initialScenesCount: Int = 1
    ): Project = withContext(Dispatchers.IO) {
        val projectId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val initialScenes = (0 until initialScenesCount).map { index ->
            Scene(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                sequenceOrder = index,
                mediaUri = null,
                mediaType = MediaType.IMAGE,
                textScript = "",
                voiceId = "cartesia-pt-helena",
                voiceName = "Helena (Narradora)",
                durationSec = 4.0f,
                transition = TransitionType.FADE
            )
        }

        val project = Project(
            id = projectId,
            title = title,
            description = "",
            aspectRatio = aspectRatio,
            status = ProjectStatus.DRAFT,
            scenes = initialScenes,
            createdAt = now,
            updatedAt = now
        )

        projectDao.insertProject(project.toEntity())
        projectDao.insertScenes(initialScenes.map { it.toEntity() })

        project
    }

    suspend fun saveProject(project: Project) = withContext(Dispatchers.IO) {
        val updatedProject = project.copy(updatedAt = System.currentTimeMillis())
        projectDao.insertProject(updatedProject.toEntity())
        // Replace all scenes for clean re-ordering
        projectDao.deleteScenesByProjectId(project.id)
        projectDao.insertScenes(updatedProject.scenes.map { it.toEntity() })
    }

    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        projectDao.deleteProjectById(projectId)
    }

    suspend fun duplicateProject(projectId: String): Project? = withContext(Dispatchers.IO) {
        val original = getProjectDirect(projectId) ?: return@withContext null
        val newProjectId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val duplicatedScenes = original.scenes.map { scene ->
            scene.copy(
                id = UUID.randomUUID().toString(),
                projectId = newProjectId
            )
        }

        val duplicatedProject = original.copy(
            id = newProjectId,
            title = "${original.title} (Cópia)",
            status = ProjectStatus.DRAFT,
            outputVideoUri = null,
            scenes = duplicatedScenes,
            createdAt = now,
            updatedAt = now
        )

        projectDao.insertProject(duplicatedProject.toEntity())
        projectDao.insertScenes(duplicatedScenes.map { it.toEntity() })

        duplicatedProject
    }

    suspend fun updateProjectStatus(projectId: String, status: ProjectStatus, outputUri: String? = null) =
        withContext(Dispatchers.IO) {
            val projectWithScenes = projectDao.getProjectWithScenesDirect(projectId) ?: return@withContext
            val updated = projectWithScenes.project.copy(
                status = status.name,
                outputVideoUri = outputUri ?: projectWithScenes.project.outputVideoUri,
                updatedAt = System.currentTimeMillis()
            )
            projectDao.updateProject(updated)
        }
}
