package com.sagun12.vozemcena.data.remote

import com.sagun12.vozemcena.data.settings.CartesiaSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cliente de alto nível da API do cron-job.org.
 *
 * Lê a chave via [CartesiaSettingsStore.getEffectiveCronJobApiKey] (secrets do
 * build ou valor salvo pelo usuário) e expõe as operações principais de
 * tarefas: listar, criar, habilitar/desabilitar e excluir.
 */
class CronJobClient(
    private val api: CronJobApi,
    private val settingsStore: CartesiaSettingsStore
) {

    val isConfigured: Boolean
        get() = settingsStore.hasValidCronJobApiKey()

    /** Lista todas as tarefas da conta. */
    suspend fun listJobs(): Result<List<CronJobDto>> = withContext(Dispatchers.IO) {
        if (!settingsStore.hasValidCronJobApiKey()) {
            return@withContext Result.failure(missingKeyError())
        }

        runCatching {
            val response = api.listJobs()
            if (response.isSuccessful) {
                response.body()?.jobs ?: emptyList()
            } else {
                throw httpError(response.code(), response.errorBody()?.string() ?: response.message())
            }
        }
    }

    /**
     * Cria uma tarefa habilitada com o preset de agendamento informado.
     * Retorna o `jobId` criado.
     */
    suspend fun createJob(
        title: String,
        url: String,
        schedule: CronJobScheduleDto
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!settingsStore.hasValidCronJobApiKey()) {
            return@withContext Result.failure(missingKeyError())
        }

        runCatching {
            val request = CronJobCreateRequest(
                job = CronJobCreateBody(
                    url = url.trim(),
                    title = title.trim(),
                    enabled = true,
                    schedule = schedule
                )
            )
            val response = api.createJob(request)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.jobId
            } else {
                throw httpError(response.code(), response.errorBody()?.string() ?: response.message())
            }
        }
    }

    /** Habilita ou desabilita uma tarefa existente. */
    suspend fun setJobEnabled(jobId: Int, enabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        if (!settingsStore.hasValidCronJobApiKey()) {
            return@withContext Result.failure(missingKeyError())
        }

        runCatching {
            val request = CronJobUpdateRequest(job = CronJobUpdateBody(enabled = enabled))
            val response = api.updateJob(jobId, request)
            if (!response.isSuccessful) {
                throw httpError(response.code(), response.errorBody()?.string() ?: response.message())
            }
        }
    }

    /** Exclui uma tarefa existente. */
    suspend fun deleteJob(jobId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        if (!settingsStore.hasValidCronJobApiKey()) {
            return@withContext Result.failure(missingKeyError())
        }

        runCatching {
            val response = api.deleteJob(jobId)
            if (!response.isSuccessful) {
                throw httpError(response.code(), response.errorBody()?.string() ?: response.message())
            }
        }
    }

    private fun missingKeyError() = IllegalStateException(
        "Chave da API do cron-job.org não configurada. Adicione CRONJOB_API_KEY no painel de Secrets/Keys."
    )

    private fun httpError(code: Int, detail: String?) =
        IllegalStateException("Erro cron-job.org ($code): ${detail ?: "sem detalhes"}")
}
