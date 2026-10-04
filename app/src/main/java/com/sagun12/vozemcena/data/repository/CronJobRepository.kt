package com.sagun12.vozemcena.data.repository

import com.sagun12.vozemcena.data.remote.CronJobClient
import com.sagun12.vozemcena.data.remote.CronJobDto
import com.sagun12.vozemcena.data.remote.CronJobPreset

/**
 * Repositório de agendamentos HTTP do cron-job.org para o aplicativo.
 *
 * Permite ao usuário gerenciar tarefas recorrentes (webhooks/lembretes)
 * diretamente da tela de Ajustes.
 */
class CronJobRepository(
    private val cronJobClient: CronJobClient
) {

    val isConfigured: Boolean
        get() = cronJobClient.isConfigured

    /** Lista as tarefas agendadas da conta. */
    suspend fun listJobs(): Result<List<CronJobDto>> = cronJobClient.listJobs()

    /**
     * Cria uma tarefa habilitada a partir de um preset de agendamento.
     * Retorna o `jobId` criado.
     */
    suspend fun createJob(title: String, url: String, preset: CronJobPreset): Result<Int> =
        cronJobClient.createJob(title = title, url = url, schedule = preset.toSchedule())

    /** Habilita ou desabilita uma tarefa existente. */
    suspend fun setJobEnabled(jobId: Int, enabled: Boolean): Result<Unit> =
        cronJobClient.setJobEnabled(jobId, enabled)

    /** Exclui uma tarefa existente. */
    suspend fun deleteJob(jobId: Int): Result<Unit> = cronJobClient.deleteJob(jobId)
}
