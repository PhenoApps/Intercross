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
import org.brapi.client.v2.model.queryParams.phenotype.ObservationUnitQueryParams
import org.brapi.client.v2.modules.phenotype.ObservationUnitsApi
import org.brapi.v2.model.germ.BrAPICrossingProject
import org.brapi.v2.model.germ.BrAPIPlannedCross
import org.brapi.v2.model.pheno.BrAPIObservationUnit
import org.phenoapps.intercross.brapi.service.BrAPIServiceV2
import org.phenoapps.intercross.brapi.service.BrapiPaginationManager
import org.brapi.v2.model.germ.BrAPICross
import org.brapi.v2.model.pheno.response.BrAPIObservationUnitListResponse
import org.phenoapps.intercross.brapi.service.BrapiV2ApiCallBack


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