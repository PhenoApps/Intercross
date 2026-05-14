package org.phenoapps.intercross.fragments.brapi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.brapi.client.v2.model.exceptions.ApiException
import org.brapi.client.v2.model.queryParams.phenotype.ObservationQueryParams
import org.brapi.client.v2.model.queryParams.phenotype.ObservationUnitQueryParams
import org.brapi.client.v2.modules.phenotype.ObservationUnitsApi
import org.brapi.client.v2.modules.phenotype.ObservationsApi
import org.brapi.v2.model.core.BrAPIStudy
import org.brapi.v2.model.germ.BrAPICrossingProject
import org.brapi.v2.model.germ.BrAPIPlannedCross
import org.brapi.v2.model.pheno.BrAPIObservation
import org.brapi.v2.model.pheno.BrAPIObservationUnit
import org.brapi.v2.model.pheno.BrAPIObservationVariable
import org.phenoapps.intercross.brapi.service.BrAPIServiceV2
import org.phenoapps.intercross.brapi.service.BrapiPaginationManager
import org.brapi.v2.model.germ.BrAPICross
import org.brapi.v2.model.pheno.response.BrAPIObservationListResponse
import org.brapi.v2.model.pheno.response.BrAPIObservationUnitListResponse
import org.phenoapps.intercross.brapi.service.BrapiV2ApiCallBack
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


/**
 * BrAPI date/time encoding uses ISO 8601 timestamps **without** fractional seconds for query params
 * (see Date_Time_Encoding: `yyyy-MM-ddThh:mm:ssZ` for UTC). Many servers reject `.SSS` in range filters.
 */
private fun normalizeBrApiObservationTimestampQueryParam(value: String): String =
    runCatching {
        val instant = try {
            OffsetDateTime.parse(value).toInstant()
        } catch (_: Exception) {
            Instant.parse(value)
        }
        Instant.ofEpochMilli(instant.toEpochMilli())
            .atOffset(ZoneOffset.UTC)
            .truncatedTo(ChronoUnit.SECONDS)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
    }.getOrElse { value }

/**
 * Temp helper coroutines TODO
 */

class BrapiRequestException(val code: Int) : Exception("BrAPI request failed with code $code")

suspend fun BrAPIServiceV2.awaitCrossingProjects(
    paginationManager: BrapiPaginationManager
): List<BrAPICrossingProject> = suspendCancellableCoroutine { continuation ->
    getCrossingProjects(
        paginationManager,
        { projects ->
            if (continuation.isActive) {
                continuation.resume(projects)
            }
            null
        },
        { fail ->
            if (continuation.isActive) {
                continuation.resumeWithException(BrapiRequestException(fail))
            }
            null
        }
    )
}

suspend fun BrAPIServiceV2.awaitPlannedCrosses(
    crossingProjectDbId: String,
    paginationManager: BrapiPaginationManager
): List<BrAPIPlannedCross> = suspendCancellableCoroutine { continuation ->
    getPlannedCrosses(
        crossingProjectDbId,
        paginationManager,
        { plannedCrosses ->
            if (continuation.isActive) {
                continuation.resume(plannedCrosses)
            }
            null
        },
        { fail ->
            if (continuation.isActive) {
                continuation.resumeWithException(BrapiRequestException(fail))
            }
            null
        }
    )
}

suspend fun BrAPIServiceV2.awaitCrosses(
    crossingProjectDbId: String,
    paginationManager: BrapiPaginationManager
): List<BrAPICross> = suspendCancellableCoroutine { continuation ->
    getCrosses(
        crossingProjectDbId,
        paginationManager,
        { crosses ->
            if (continuation.isActive) {
                continuation.resume(crosses)
            }
            null
        },
        { fail ->
            if (continuation.isActive) {
                continuation.resumeWithException(BrapiRequestException(fail))
            }
            null
        }
    )
}

suspend fun BrAPIServiceV2.awaitStudies(
    programDbId: String,
    paginationManager: BrapiPaginationManager
): List<BrAPIStudy> = suspendCancellableCoroutine { continuation ->
    getStudies(
        programDbId,
        paginationManager,
        { studies ->
            if (continuation.isActive) {
                continuation.resume(studies)
            }
            null
        },
        { fail ->
            if (continuation.isActive) {
                continuation.resumeWithException(BrapiRequestException(fail))
            }
            null
        }
    )
}

suspend fun fetchAllPagesParallelWithProgress(
    pageSize: Int = 100,
    maxParallel: Int = 4,
    onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> },
    fetchPage: suspend (page: Int) -> BrAPIObservationUnitListResponse
): List<BrAPIObservationUnit> = coroutineScope {

    val first = fetchPage(0)
    val pagination = first.metadata.pagination
    val totalPages = pagination.totalPages

    val results = mutableListOf<BrAPIObservationUnit>()
    results.addAll(first.result.data)

    // Report initial progress
    onProgress(1, totalPages)

    if (totalPages <= 1) return@coroutineScope results

    val semaphore = Semaphore(maxParallel)
    var completedPages = 1

    // parallel fetches for pages 2..N


    //TODO TEMP TO END EARLY
    val deferred = (2..10).map { page ->

//    val deferred = (2..totalPages).map { page ->
        async(Dispatchers.IO) {
            semaphore.withPermit {
                try {
                    val response = fetchPage(page)
                    val data = response.result.data

                    synchronized(results) {
                        results.addAll(data)
                    }

                } catch (e: Exception) {
                    // Log or collect errors if needed
                    println("Page $page failed: ${e.message}")
                } finally {
                    synchronized(results) {
                        completedPages++
                        onProgress(completedPages, totalPages)
                    }
                }
            }
        }
    }

    //wait for all pages to finish (success or failure)
    deferred.awaitAll()

    results
}

suspend fun ObservationUnitsApi.fetchObservationUnitsPage(
    programDbId: String,
    page: Int,
    pageSize: Int
): BrAPIObservationUnitListResponse = suspendCancellableCoroutine { cont ->

    val params: ObservationUnitQueryParams = ObservationUnitQueryParams()
        .programDbId(programDbId).also {
            it.page(page)
            it.pageSize(pageSize)
        }

    observationunitsGetAsync(
        params,
        object : BrapiV2ApiCallBack<BrAPIObservationUnitListResponse>() {
            override fun onSuccess(
                response: BrAPIObservationUnitListResponse,
                i: Int,
                map: Map<String, List<String>>
            ) {
                cont.resume(response)
            }

            override fun onFailure(
                error: ApiException,
                i: Int,
                map: Map<String, List<String>>
            ) {
                cont.resumeWithException(error)
            }
        })
}

suspend fun ObservationUnitsApi.getAllObservationUnits(
    programDbId: String,
    pageSize: Int = 15,
    maxParallel: Int = 4,
    onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
): List<BrAPIObservationUnit> {

    return fetchAllPagesParallelWithProgress(
        pageSize = pageSize,
        maxParallel = maxParallel,
        onProgress = onProgress
    ) { page ->
        fetchObservationUnitsPage(programDbId, page, pageSize)
    }
}

suspend fun BrAPIServiceV2.awaitObservationVariables(
    programDbId: String,
    studyDbId: String,
    paginationManager: BrapiPaginationManager
): List<BrAPIObservationVariable> = suspendCancellableCoroutine { continuation ->
    getObservationVariables(
        programDbId,
        studyDbId,
        paginationManager,
        { variables ->
            if (continuation.isActive) {
                continuation.resume(variables)
            }
            null
        },
        { fail ->
            if (continuation.isActive) {
                continuation.resumeWithException(BrapiRequestException(fail))
            }
            null
        }
    )
}

suspend fun ObservationsApi.fetchObservationsPage(
    studyDbId: String,
    observationVariableDbId: String,
    observationTimeStampRangeStart: String,
    observationTimeStampRangeEnd: String,
    page: Int,
    pageSize: Int
): BrAPIObservationListResponse = suspendCancellableCoroutine { cont ->
    val rangeStart = normalizeBrApiObservationTimestampQueryParam(observationTimeStampRangeStart)
    val rangeEnd = normalizeBrApiObservationTimestampQueryParam(observationTimeStampRangeEnd)
    val params = ObservationQueryParams()
        .studyDbId(studyDbId)
        .observationVariableDbId(observationVariableDbId)
        .observationTimeStampRangeStart(rangeStart)
        .observationTimeStampRangeEnd(rangeEnd)
    params.page(page)
    params.pageSize(pageSize)

    observationsGetAsync(
        params,
        object : BrapiV2ApiCallBack<BrAPIObservationListResponse>() {
            override fun onSuccess(
                response: BrAPIObservationListResponse,
                i: Int,
                map: Map<String, List<String>>
            ) {
                cont.resume(response)
            }

            override fun onFailure(
                error: ApiException,
                i: Int,
                map: Map<String, List<String>>
            ) {
                cont.resumeWithException(error)
            }
        })
}

suspend fun ObservationsApi.getAllObservationsForStudyVariableInTimestampRange(
    studyDbId: String,
    observationVariableDbId: String,
    observationTimeStampRangeStart: String,
    observationTimeStampRangeEnd: String,
    pageSize: Int = 100
): List<BrAPIObservation> {
    val first = fetchObservationsPage(
        studyDbId,
        observationVariableDbId,
        observationTimeStampRangeStart,
        observationTimeStampRangeEnd,
        0,
        pageSize
    )
    val results = first.result?.data?.toMutableList() ?: mutableListOf()
    val totalPages = first.metadata?.pagination?.totalPages?.takeIf { it > 0 } ?: 1
    for (page in 1 until totalPages) {
        val pageResponse = fetchObservationsPage(
            studyDbId,
            observationVariableDbId,
            observationTimeStampRangeStart,
            observationTimeStampRangeEnd,
            page,
            pageSize
        )
        pageResponse.result?.data?.let { results.addAll(it) }
    }
    return results
}