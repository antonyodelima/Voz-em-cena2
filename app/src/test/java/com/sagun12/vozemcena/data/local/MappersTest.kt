package com.sagun12.vozemcena.data.local

import com.sagun12.vozemcena.domain.model.AspectRatio
import com.sagun12.vozemcena.domain.model.MediaType
import com.sagun12.vozemcena.domain.model.Project
import com.sagun12.vozemcena.domain.model.ProjectStatus
import com.sagun12.vozemcena.domain.model.Scene
import com.sagun12.vozemcena.domain.model.TransitionType
import com.sagun12.vozemcena.domain.model.Voice
import com.sagun12.vozemcena.domain.model.VoiceProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MappersTest {

    @Test
    fun projectMapping_preservesFields() {
        val domainProject = Project(
            id = "proj-1",
            title = "Test Project",
            description = "Desc",
            aspectRatio = AspectRatio.RATIO_9_16,
            status = ProjectStatus.DRAFT,
            scenes = emptyList()
        )

        val entity = domainProject.toEntity()
        assertEquals("proj-1", entity.id)
        assertEquals("Test Project", entity.title)
        assertEquals(AspectRatio.RATIO_9_16.name, entity.aspectRatio)

        val convertedBack = entity.toDomain()
        assertEquals(domainProject.id, convertedBack.id)
        assertEquals(domainProject.title, convertedBack.title)
        assertEquals(domainProject.aspectRatio, convertedBack.aspectRatio)
    }

    @Test
    fun sceneMapping_preservesFields() {
        val scene = Scene(
            id = "scene-1",
            projectId = "proj-1",
            sequenceOrder = 0,
            mediaUri = "file:///image.jpg",
            mediaType = MediaType.IMAGE,
            textScript = "Narrativa de teste",
            voiceId = "cartesia-pt-helena",
            durationSec = 5.0f,
            transition = TransitionType.FADE
        )

        val entity = scene.toEntity()
        val backToDomain = entity.toDomain()

        assertEquals(scene.id, backToDomain.id)
        assertEquals(scene.textScript, backToDomain.textScript)
        assertEquals(scene.durationSec, backToDomain.durationSec, 0.01f)
    }

    @Test
    fun voiceMapping_preservesTagsAndProperties() {
        val voice = Voice(
            id = "voice-test",
            name = "Voz Teste",
            description = "Desc",
            language = "pt-BR",
            gender = "Masculino",
            provider = VoiceProvider.CARTESIA,
            tags = listOf("Narração", "Grave"),
            isCustom = true
        )

        val entity = voice.toEntity()
        val backToDomain = entity.toDomain()

        assertEquals(voice.id, backToDomain.id)
        assertEquals(voice.name, backToDomain.name)
        assertEquals(2, backToDomain.tags.size)
        assertEquals("Narração", backToDomain.tags[0])
    }
}
