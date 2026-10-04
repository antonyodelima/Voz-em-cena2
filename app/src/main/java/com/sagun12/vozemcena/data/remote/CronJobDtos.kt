package com.sagun12.vozemcena.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.TimeZone

/**
 * Modelos de requisição/resposta da API REST do cron-job.org.
 * Seguem o formato documentado em https://docs.cron-job.org/rest-api.html
 *
 * Endpoints usados:
 * - GET    /jobs       lista tarefas
 * - PUT    /jobs       cria tarefa
 * - PATCH  /jobs/{id}  atualiza tarefa (ex.: habilitar/desabilitar)
 * - DELETE /jobs/{id}  exclui tarefa
 */

/** Agendamento de uma tarefa (formato `JobSchedule` da documentação). */
@JsonClass(generateAdapter = true)
data class CronJobScheduleDto(
    @Json(name = "timezone") val timezone: String = "UTC",
    @Json(name = "expiresAt") val expiresAt: Long = 0,
    /** Horas (0-23; [-1] = todas as horas). */
    @Json(name = "hours") val hours: List<Int> = emptyList(),
    /** Dias do mês (1-31; [-1] = todos). */
    @Json(name = "mdays") val mdays: List<Int> = emptyList(),
    /** Minutos (0-59; [-1] = todos os minutos). */
    @Json(name = "minutes") val minutes: List<Int> = emptyList(),
    /** Meses (1-12; [-1] = todos). */
    @Json(name = "months") val months: List<Int> = emptyList(),
    /** Dias da semana (0=Domingo..6=Sábado; [-1] = todos). */
    @Json(name = "wdays") val wdays: List<Int> = emptyList()
)

/** Tarefa cron (`Job` da documentação). Campos de leitura são opcionais. */
@JsonClass(generateAdapter = true)
data class CronJobDto(
    @Json(name = "jobId") val jobId: Int? = null,
    @Json(name = "enabled") val enabled: Boolean = false,
    @Json(name = "title") val title: String = "",
    @Json(name = "saveResponses") val saveResponses: Boolean = false,
    @Json(name = "url") val url: String = "",
    /** Status da última execução (0=desconhecido, 1=OK, 2..10=falhas). */
    @Json(name = "lastStatus") val lastStatus: Int = 0,
    @Json(name = "lastDuration") val lastDuration: Long = 0,
    @Json(name = "lastExecution") val lastExecution: Long = 0,
    @Json(name = "sslCertExpiry") val sslCertExpiry: Long? = null,
    /** Próxima execução prevista (epoch seconds); null quando não há previsão. */
    @Json(name = "nextExecution") val nextExecution: Long? = null,
    @Json(name = "type") val type: Int = 0,
    @Json(name = "requestTimeout") val requestTimeout: Int = -1,
    @Json(name = "redirectSuccess") val redirectSuccess: Boolean = false,
    @Json(name = "folderId") val folderId: Int = 0,
    @Json(name = "schedule") val schedule: CronJobScheduleDto? = null,
    /** 0=GET, 1=POST, ... (RequestMethod da documentação). */
    @Json(name = "requestMethod") val requestMethod: Int = 0
)

/** Resposta de GET /jobs. */
@JsonClass(generateAdapter = true)
data class CronJobListResponse(
    @Json(name = "jobs") val jobs: List<CronJobDto> = emptyList(),
    @Json(name = "someFailed") val someFailed: Boolean = false
)

/** Corpo de criação de tarefa (PUT /jobs) — apenas `url` é obrigatório. */
@JsonClass(generateAdapter = true)
data class CronJobCreateBody(
    @Json(name = "url") val url: String,
    @Json(name = "title") val title: String = "",
    @Json(name = "enabled") val enabled: Boolean = true,
    @Json(name = "saveResponses") val saveResponses: Boolean = false,
    @Json(name = "requestMethod") val requestMethod: Int = 0,
    @Json(name = "schedule") val schedule: CronJobScheduleDto
)

@JsonClass(generateAdapter = true)
data class CronJobCreateRequest(
    @Json(name = "job") val job: CronJobCreateBody
)

/** Resposta de PUT /jobs. */
@JsonClass(generateAdapter = true)
data class CronJobCreateResponse(
    @Json(name = "jobId") val jobId: Int = 0
)

/** Corpo parcial de atualização (PATCH /jobs/{jobId}) — apenas campos alterados. */
@JsonClass(generateAdapter = true)
data class CronJobUpdateBody(
    @Json(name = "enabled") val enabled: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class CronJobUpdateRequest(
    @Json(name = "job") val job: CronJobUpdateBody
)

/**
 * Presets de agendamento disponíveis na UI, convertidos para o formato
 * `JobSchedule` do cron-job.org ([-1] = "todos").
 */
enum class CronJobPreset(val label: String) {
    EVERY_15_MIN("A cada 15 minutos") {
        override fun toSchedule(timeZone: String) = CronJobScheduleDto(
            timezone = timeZone,
            minutes = listOf(0, 15, 30, 45),
            hours = listOf(-1),
            mdays = listOf(-1),
            months = listOf(-1),
            wdays = listOf(-1)
        )
    },
    HOURLY("De hora em hora (no minuto 0)") {
        override fun toSchedule(timeZone: String) = CronJobScheduleDto(
            timezone = timeZone,
            minutes = listOf(0),
            hours = listOf(-1),
            mdays = listOf(-1),
            months = listOf(-1),
            wdays = listOf(-1)
        )
    },
    DAILY_9AM("Diariamente às 09:00") {
        override fun toSchedule(timeZone: String) = CronJobScheduleDto(
            timezone = timeZone,
            minutes = listOf(0),
            hours = listOf(9),
            mdays = listOf(-1),
            months = listOf(-1),
            wdays = listOf(-1)
        )
    },
    WEEKLY_MON_9AM("Toda segunda-feira às 09:00") {
        override fun toSchedule(timeZone: String) = CronJobScheduleDto(
            timezone = timeZone,
            minutes = listOf(0),
            hours = listOf(9),
            mdays = listOf(-1),
            months = listOf(-1),
            wdays = listOf(1)
        )
    };

    abstract fun toSchedule(timeZone: String): CronJobScheduleDto

    fun toSchedule(): CronJobScheduleDto = toSchedule(TimeZone.getDefault().id)
}
