package com.sagun12.vozemcena.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Endpoints REST do cron-job.org (https://docs.cron-job.org/rest-api.html).
 *
 * A autenticação (`Authorization: Bearer <chave>`) é adicionada por um
 * interceptor em [CronJobModule]. Corpos de resposta vazios (`{}`) são
 * consumidos como [Unit] pelo conversor embutido do Retrofit.
 */
interface CronJobApi {

    @GET("jobs")
    suspend fun listJobs(): Response<CronJobListResponse>

    @PUT("jobs")
    suspend fun createJob(@Body request: CronJobCreateRequest): Response<CronJobCreateResponse>

    @PATCH("jobs/{jobId}")
    suspend fun updateJob(
        @Path("jobId") jobId: Int,
        @Body request: CronJobUpdateRequest
    ): Response<Unit>

    @DELETE("jobs/{jobId}")
    suspend fun deleteJob(@Path("jobId") jobId: Int): Response<Unit>
}
