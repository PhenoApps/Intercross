package org.phenoapps.intercross.brapi.service;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.arch.core.util.Function;
import androidx.preference.PreferenceManager;

import org.brapi.client.v2.BrAPIClient;
import org.brapi.client.v2.model.exceptions.ApiException;
import org.brapi.client.v2.model.queryParams.germplasm.CrossQueryParams;
import org.brapi.client.v2.model.queryParams.germplasm.CrossingProjectQueryParams;
import org.brapi.client.v2.model.queryParams.germplasm.PlannedCrossQueryParams;
import org.brapi.client.v2.modules.core.ProgramsApi;
import org.brapi.client.v2.modules.germplasm.CrossesApi;
import org.brapi.client.v2.modules.germplasm.CrossingProjectsApi;
import org.brapi.client.v2.modules.germplasm.GermplasmApi;
import org.brapi.client.v2.modules.phenotype.ObservationUnitsApi;
import org.brapi.v2.model.BrAPIMetadata;
import org.brapi.v2.model.germ.BrAPICross;
import org.brapi.v2.model.germ.BrAPICrossingProject;
import org.brapi.v2.model.germ.BrAPIPlannedCross;
import org.brapi.v2.model.germ.response.BrAPICrossesListResponse;
import org.brapi.v2.model.germ.response.BrAPICrossingProjectsListResponse;
import org.brapi.v2.model.germ.response.BrAPIPlannedCrossesListResponse;
import org.phenoapps.brapi.account.BrapiAccountRepository;
import org.phenoapps.brapi.account.BrapiPreferenceKeys;
import org.phenoapps.intercross.util.KeyUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BrAPIServiceV2 implements BrAPIService {

    private final static String TAG = "BrAPI V2";
    private final ProgramsApi programsApi;
    private final CrossingProjectsApi crossingProjectsApi;
    private final CrossesApi crossesApi;
    private final GermplasmApi germplasmApi;

    public final ObservationUnitsApi observationUnitsApi;

    private final KeyUtil mKeyUtil;

    public BrAPIServiceV2(Context context) {
        this.mKeyUtil = new KeyUtil(context);

        // Make timeout longer. Set it to 60 seconds for now
        BrAPIClient apiClient = new BrAPIClient(BrAPIService.getBrapiUrl(context), 60*1000*10);

        String token = getTokenFromRepository(context);

        try {
            apiClient.authenticate(t -> token);
        } catch (ApiException e) {
            Log.e(TAG, "Authentication error", e);
        }

        this.programsApi = new ProgramsApi(apiClient);
        this.crossingProjectsApi = new CrossingProjectsApi(apiClient);
        this.germplasmApi = new GermplasmApi(apiClient);
        this.crossesApi = new CrossesApi(apiClient);
        this.observationUnitsApi = new ObservationUnitsApi(apiClient);
    }

    /**
     * Retrieves the BrAPI access token, trying BrapiAccountRepository first
     * and falling back to SharedPreferences if the repository is unavailable.
     */
    private static String getTokenFromRepository(Context context) {
        try {
            KeyUtil keyUtil = new KeyUtil(context);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            BrapiPreferenceKeys keys = new BrapiPreferenceKeys(
                keyUtil.getBrapiEnabled(),
                keyUtil.getBrapiUrl(),
                keyUtil.getBrapiDisplayName(),
                keyUtil.getBrapiToken(),
                keyUtil.getBrapiId(),
                "v2",
                keyUtil.getBrapiOidcUrl(),
                keyUtil.getBrapiOidcFlow(),
                keyUtil.getBrapiOidcClientId(),
                keyUtil.getBrapiOidcScope()
            );
            BrapiAccountRepository repository = new BrapiAccountRepository(context, prefs, keys);
            String token = repository.peekToken();
            if (token != null && !token.isEmpty()) {
                return token;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get token from repository, falling back to SharedPreferences", e);
        }
        // Fallback to SharedPreferences
        KeyUtil keyUtil = new KeyUtil(context);
        return PreferenceManager.getDefaultSharedPreferences(context).getString(keyUtil.getBrapiToken(), "");
    }

    private void updatePageInfo(BrapiPaginationManager paginationManager, BrAPIMetadata metadata){
        if(paginationManager.getContext() != null) { //null check for JUnits
            ((Activity) paginationManager.getContext())
                    .runOnUiThread(() -> paginationManager.update(metadata.getPagination()));
        }
    }

    /**
     * a planned cross in brapi is a wishlist in intercross
     * just male/female pairs with a min/max and wish type (cross, seeds, fruits)
     * @param function
     * @param failFunction
     */
    public void postPlannedCross(List<BrAPIPlannedCross> plan, final Function<BrAPIPlannedCrossesListResponse, Void> function,
                                    final Function<Integer, Void> failFunction) {
        try {
            BrapiV2ApiCallBack<BrAPIPlannedCrossesListResponse> callback = new BrapiV2ApiCallBack<BrAPIPlannedCrossesListResponse>() {
                @Override
                public void onSuccess(BrAPIPlannedCrossesListResponse phenotypesResponse, int i, Map<String, List<String>> map) {

                    function.apply(phenotypesResponse);
                }

                @Override
                public void onFailure(ApiException error, int statusCode, Map<String, List<String>> responseHeaders) {
                    failFunction.apply(error.getCode());
                }
            };

            crossesApi.plannedcrossesPostAsync(plan, callback);

        } catch (ApiException e) {
            failFunction.apply(e.getCode());
            e.printStackTrace();
        }
    }

    public void postCrosses(List<BrAPICross> crosses, final Function<BrAPICrossesListResponse, Void> function,
                            final Function<Integer, Void> failFunction) {
        try {
            BrapiV2ApiCallBack<BrAPICrossesListResponse> callback = new BrapiV2ApiCallBack<BrAPICrossesListResponse>() {
                @Override
                public void onSuccess(BrAPICrossesListResponse response, int i, Map<String, List<String>> map) {

                    function.apply(response);
                }

                @Override
                public void onFailure(ApiException error, int statusCode, Map<String, List<String>> responseHeaders) {
                    failFunction.apply(error.getCode());
                }
            };

            crossesApi.crossesPostAsync(crosses, callback);

        } catch (ApiException e) {
            failFunction.apply(e.getCode());
            e.printStackTrace();
        }
    }

    public void postCrossingProject(String name, final Function<List<BrAPICrossingProject>, Void> function,
                                    final Function<Integer, Void> failFunction) {
        try {
            BrapiV2ApiCallBack<BrAPICrossingProjectsListResponse> callback = new BrapiV2ApiCallBack<BrAPICrossingProjectsListResponse>() {
                @Override
                public void onSuccess(BrAPICrossingProjectsListResponse response, int i, Map<String, List<String>> map) {
                    function.apply(new ArrayList<>(response.getResult().getData()));
                }

                @Override
                public void onFailure(ApiException error, int statusCode, Map<String, List<String>> responseHeaders) {
                    failFunction.apply(error.getCode());
                }
            };

            List<BrAPICrossingProject> list = new ArrayList<>();

            BrAPICrossingProject body = new BrAPICrossingProject();
            body.crossingProjectName(name);

            list.add(body);

            crossingProjectsApi.crossingprojectsPostAsync(list, callback);

        } catch (ApiException e) {
            failFunction.apply(e.getCode());
            e.printStackTrace();
        }
    }

    public void getPlannedCrosses(String crossProjectDbId, BrapiPaginationManager paginationManager,
                            final Function<List<BrAPIPlannedCross>, Void> function,
                            final Function<Integer, Void> failFunction) {

        Integer initPage = paginationManager.getPage();

        try {
            BrapiV2ApiCallBack<BrAPIPlannedCrossesListResponse> callback = new BrapiV2ApiCallBack<BrAPIPlannedCrossesListResponse>() {

                @Override
                public void onSuccess(BrAPIPlannedCrossesListResponse response, int i, Map<String, List<String>> map) {
                    // Cancel processing if the page that was processed is not the page
                    // that we are currently on. For Example: User taps "Next Page" before brapi call returns data
                    if (initPage.equals(paginationManager.getPage())) {
                        updatePageInfo(paginationManager, response.getMetadata());
                        List<BrAPIPlannedCross> crosses = response.getResult().getData();
                        function.apply(crosses);
                    }
                }

                @Override
                public void onFailure(ApiException error, int i, Map<String, List<String>> map) {
                    failFunction.apply(error.getCode());
                }
            };

            PlannedCrossQueryParams request = new PlannedCrossQueryParams();
            request.page(paginationManager.getPage()).pageSize(paginationManager.getPageSize());

            //.crossingProjectDbId(crossProjectDbId)

            crossesApi.plannedcrossesGetAsync(request, callback);

        } catch (ApiException e) {
            failFunction.apply(e.getCode());
            e.printStackTrace();
        }
    }

    public void getCrosses(String crossProjectDbId, BrapiPaginationManager paginationManager,
                            final Function<List<BrAPICross>, Void> function,
                            final Function<Integer, Void> failFunction) {

        Integer initPage = paginationManager.getPage();

        try {
            BrapiV2ApiCallBack<BrAPICrossesListResponse> callback = new BrapiV2ApiCallBack<BrAPICrossesListResponse>() {

                @Override
                public void onSuccess(BrAPICrossesListResponse response, int i, Map<String, List<String>> map) {
                    if (initPage.equals(paginationManager.getPage())) {
                        updatePageInfo(paginationManager, response.getMetadata());
                        List<BrAPICross> crosses = response.getResult().getData();
                        function.apply(crosses);
                    }
                }

                @Override
                public void onFailure(ApiException error, int i, Map<String, List<String>> map) {
                    failFunction.apply(error.getCode());
                }
            };

            CrossQueryParams params = new CrossQueryParams();
            params.crossingProjectDbId(crossProjectDbId).page(paginationManager.getPage()).pageSize(paginationManager.getPageSize());
            crossesApi.crossesGetAsync(params, callback);

        } catch (ApiException e) {
            failFunction.apply(e.getCode());
            e.printStackTrace();
        }
    }

    public void getCrossingProjects(BrapiPaginationManager paginationManager,
                             final Function<List<BrAPICrossingProject>, Void> function,
                             final Function<Integer, Void> failFunction) {
        Integer initPage = paginationManager.getPage();
        try {
            BrapiV2ApiCallBack<BrAPICrossingProjectsListResponse> callback = new BrapiV2ApiCallBack<BrAPICrossingProjectsListResponse>() {
                @Override
                public void onSuccess(BrAPICrossingProjectsListResponse projectsListResponse, int i, Map<String, List<String>> map) {
                    if (initPage.equals(paginationManager.getPage())) {
                        updatePageInfo(paginationManager, projectsListResponse.getMetadata());
                        List<BrAPICrossingProject> projects = projectsListResponse.getResult().getData();
                        function.apply(projects);
                    }
                }

                @Override
                public void onFailure(ApiException error, int i, Map<String, List<String>> map) {
                    failFunction.apply(error.getCode());
                }
            };

            CrossingProjectQueryParams request = new CrossingProjectQueryParams();
            request.page(paginationManager.getPage()).pageSize(paginationManager.getPageSize());
            crossingProjectsApi.crossingprojectsGetAsync(request, callback);

        } catch (ApiException e) {
            failFunction.apply(e.getCode());
            e.printStackTrace();
        }
    }

    public void getCrossingProject(String crossProjectDbId,
                                    final Function<List<BrAPICrossingProject>, Void> function,
                                    final Function<Integer, Void> failFunction) {
        try {
            BrapiV2ApiCallBack<BrAPICrossingProjectsListResponse> callback = new BrapiV2ApiCallBack<BrAPICrossingProjectsListResponse>() {
                @Override
                public void onSuccess(BrAPICrossingProjectsListResponse projectsListResponse, int i, Map<String, List<String>> map) {
                    List<BrAPICrossingProject> projects = projectsListResponse.getResult().getData();
                    function.apply(projects);
                }

                @Override
                public void onFailure(ApiException error, int i, Map<String, List<String>> map) {
                    failFunction.apply(error.getCode());
                }
            };

            CrossingProjectQueryParams request = new CrossingProjectQueryParams();
            request.crossingProjectDbId(crossProjectDbId);
            crossingProjectsApi.crossingprojectsGetAsync(request, callback);

        } catch (ApiException e) {
            failFunction.apply(e.getCode());
            e.printStackTrace();
        }
    }
}