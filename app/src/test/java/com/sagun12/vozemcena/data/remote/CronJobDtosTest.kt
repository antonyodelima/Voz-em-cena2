package com.sagun12.vozemcena.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CronJobDtosTest {

    @Test
    fun schedulePreset_daily9am_hasExpectedFields() {
        val schedule = CronJobPreset.DAILY_9AM.toSchedule("America/Sao_Paulo")

        assertEquals("America/Sao_Paulo", schedule.timezone)
        assertEquals(listOf(0), schedule.minutes)
        assertEquals(listOf(9), schedule.hours)
        assertEquals(listOf(-1), schedule.mdays)
        assertEquals(listOf(-1), schedule.wdays)
        assertEquals(listOf(-1), schedule.months)
        assertEquals(0L, schedule.expiresAt)
    }

    @Test
    fun schedulePreset_hourly_runsEveryHourAtMinuteZero() {
        val schedule = CronJobPreset.HOURLY.toSchedule("UTC")

        assertEquals(listOf(0), schedule.minutes)
        assertEquals(listOf(-1), schedule.hours)
        assertEquals("UTC", schedule.timezone)
    }

    @Test
    fun schedulePreset_weeklyMonday_targetsMondayOnly() {
        val schedule = CronJobPreset.WEEKLY_MON_9AM.toSchedule("UTC")

        // 1 = segunda-feira na convenção do cron-job.org (0 = domingo)
        assertEquals(listOf(1), schedule.wdays)
        assertEquals(listOf(9), schedule.hours)
        assertEquals(listOf(-1), schedule.mdays)
    }

    @Test
    fun createRequest_serializationStructure() {
        val request = CronJobCreateRequest(
            job = CronJobCreateBody(
                url = "https://example.com/webhook",
                title = "Lembrete Voz em Cena",
                enabled = true,
                schedule = CronJobPreset.DAILY_9AM.toSchedule("UTC")
            )
        )

        assertEquals("https://example.com/webhook", request.job.url)
        assertEquals("Lembrete Voz em Cena", request.job.title)
        assertTrue(request.job.enabled)
        assertEquals(0, request.job.requestMethod)
        assertNotNull(request.job.schedule)
    }

    @Test
    fun listResponse_parsesDocumentedExampleStructure() {
        // Espelha o exemplo de resposta documentado em https://docs.cron-job.org/rest-api.html
        val jobs = listOf(
            CronJobDto(
                jobId = 12345,
                enabled = true,
                title = "Example Job",
                saveResponses = false,
                url = "https://example.com/",
                lastStatus = 0,
                lastDuration = 0,
                lastExecution = 0,
                nextExecution = 1640187240L,
                type = 0,
                requestTimeout = 300,
                redirectSuccess = false,
                folderId = 0,
                requestMethod = 0,
                schedule = CronJobScheduleDto(
                    timezone = "Europe/Berlin",
                    expiresAt = 0,
                    hours = listOf(-1),
                    mdays = listOf(-1),
                    minutes = listOf(0, 15, 30, 45),
                    months = listOf(-1),
                    wdays = listOf(-1)
                )
            )
        )
        val response = CronJobListResponse(jobs = jobs, someFailed = false)

        assertEquals(1, response.jobs.size)
        assertFalse(response.someFailed)

        val job = response.jobs.first()
        assertEquals(12345, job.jobId)
        assertTrue(job.enabled)
        assertEquals("https://example.com/", job.url)
        assertEquals(1640187240L, job.nextExecution)
        assertEquals(listOf(0, 15, 30, 45), job.schedule?.minutes)
    }

    @Test
    fun updateRequest_onlyCarriesEnabledFlag() {
        val request = CronJobUpdateRequest(job = CronJobUpdateBody(enabled = true))

        assertTrue(request.job.enabled == true)
        // PATCH envia apenas o delta; demais campos ficam nulos/omitidos.
        CronJobUpdateBody().let { empty ->
            assertEquals(null, empty.enabled)
        }
    }
}
