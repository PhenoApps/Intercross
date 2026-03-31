package org.phenoapps.intercross.brapi.service;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.arch.core.util.Function;
import androidx.preference.PreferenceManager;

import org.phenoapps.intercross.Constants;
import org.phenoapps.intercross.R;
import org.phenoapps.intercross.brapi.ApiError;
import org.phenoapps.intercross.brapi.BrapiAuthDialog;
import org.phenoapps.intercross.brapi.BrapiControllerResponse;
import org.phenoapps.intercross.brapi.model.BrapiProgram;
import org.phenoapps.intercross.brapi.model.BrapiStudyDetails;
import org.phenoapps.intercross.brapi.model.BrapiTrial;
import org.phenoapps.intercross.brapi.model.FieldBookImage;
import org.phenoapps.intercross.brapi.model.Observation;
import org.phenoapps.intercross.util.KeyUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

//todo create github package for a generalized service maybe also an Android Service implementation?
public interface BrAPIService {

    public static String exportTarget = "export";
    public static String notUniqueFieldMessage = "not_unique";
    public static String notUniqueIdMessage = "not_unique_id";

    public static BrapiControllerResponse authorizeBrAPI(SharedPreferences sharedPreferences, Context context, String target) {
        KeyUtil keyUtil = new KeyUtil(context);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        //TODO
//        editor.putString(keyUtil.getBrapiKeys().getBrapiTokenKey(), null);
//        editor.apply();

        if (target == null) {
            target = "";
        }

        try {
            //TODO parameterize this name or use Application label
            String url = ""; //TODO PreferenceManager.getDefaultSharedPreferences(context).getString(keyUtil.getBrapiKeys().getBrapiUrlKey(), "") + "/brapi/authorize?display_name=Intercross&return_url=intercross://%s";
            url = String.format(url, target);
            try {
                // Go to url with the default browser
                Uri uri = Uri.parse(url);
                Intent i = new Intent(Intent.ACTION_VIEW, uri);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                context.startActivity(i);

                // We require no response since this starts a new activity.
                return new BrapiControllerResponse(null, "");

            } catch (ActivityNotFoundException ex) {
                Log.e("BrAPI", "Error starting BrAPI auth", ex);
                return new BrapiControllerResponse(false, context.getString(R.string.brapi_auth_error_starting));

            }
        } catch (Exception ex) {
            Log.e("BrAPI", "Error starting BrAPI auth", ex);
            return new BrapiControllerResponse(false, context.getString(R.string.brapi_auth_error_starting));

        }
    }

    // Returns true on successful parsing. False otherwise.
    public static BrapiControllerResponse checkBrapiAuth(Activity activity) {

        KeyUtil keyUtil = new KeyUtil(activity);

        Uri data = activity.getIntent().getData();

        if (data != null && data.isHierarchical()) {

            // Clear our data from our deep link so the app doesn't think it is
            // coming from a deep link if it is coming from deep link on pause and resume.
            activity.getIntent().setData(null);

            Integer status = Integer.parseInt(data.getQueryParameter("status"));

            // Check that we actually have the data. If not return failure.
            if (status == null) {
                return new BrapiControllerResponse(false, "No data received from host.");
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
            if (status == 200) {
                SharedPreferences.Editor editor = prefs.edit();
                String token = data.getQueryParameter("token");

                // Check that we received a token.
                if (token == null) {
                    return new BrapiControllerResponse(false, "No access token received in response from host.");
                }

                //TODO editor.putString(keyUtil.getBrapiKeys().getBrapiTokenKey(), token);
                editor.apply();

                return new BrapiControllerResponse(true, activity.getString(R.string.brapi_auth_success));
            } else {
                SharedPreferences.Editor editor = prefs.edit();
                //TODO editor.putString(keyUtil.getBrapiKeys().getBrapiTokenKey(), null);
                editor.apply();

                return new BrapiControllerResponse(false, activity.getString(R.string.brapi_auth_deny));
            }
        } else {
            // Return null status when it is not a brapi response
            return new BrapiControllerResponse(null, "");
        }

    }

    // Helper functions for brapi configurations
    public static Boolean isLoggedIn(Context context) {

        KeyUtil keyUtil = new KeyUtil(context);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String auth_token = ""; //TODO prefs.getString(keyUtil.getBrapiKeys().getBrapiTokenKey(), "");

        if (auth_token == null || auth_token == "") {
            return false;
        }

        return true;
    }

    public static Boolean hasValidBaseUrl(Context context) {
        String url = getBrapiUrl(context);

        return Patterns.WEB_URL.matcher(url).matches();
    }

    public static Boolean checkMatchBrapiUrl(Context context, String dataSource) {

        try {
            URL externalUrl = new URL(getBrapiUrl(context));
            String hostURL = externalUrl.getHost();

            return (hostURL.equals(dataSource));
        } catch (MalformedURLException e) {
            Log.e("error-cmbu", e.toString());
            return false;
        }

    }

    public static String getHostUrl(Context context) {
        try {
            String brapiURL = getBrapiUrl(context);
            URL externalUrl = new URL(brapiURL);
            return externalUrl.getHost();
        } catch (MalformedURLException e) {
            Log.e("error-ghu", e.toString());
            return null;
        }
    }

    public static String getBrapiUrl(Context context) {
        KeyUtil keyUtil = new KeyUtil(context);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String baseURL = prefs.getString(keyUtil.getBrapiUrl(), "https://test-server.brapi.org");
        String path = Constants.BRAPI_PATH_V2;
        return baseURL + path;
    }

    public static boolean isConnectionError(int code) {
        return code == 401 || code == 403 || code == 404;
    }

    public static void handleConnectionError(Context context, int code) {
        ApiError apiError = ApiError.processErrorCode(code);
        String toastMsg = "";

        switch (apiError) {
            case UNAUTHORIZED:
                // Start the login process
                BrapiAuthDialog brapiAuth = new BrapiAuthDialog(context, null);
                brapiAuth.show();
                toastMsg = context.getString(R.string.brapi_auth_deny);
                break;
            case FORBIDDEN:
                toastMsg = context.getString(R.string.brapi_auth_permission_deny);
                break;
            case NOT_FOUND:
                toastMsg = context.getString(R.string.brapi_not_found);
                break;
            default:
                toastMsg = "";
        }
        Toast.makeText(context.getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
    }
    
    public void getPrograms(final BrapiPaginationManager paginationManager, final Function<List<BrapiProgram>, Void> function, final Function<Integer, Void> failFunction);

    public void getTrials(String programDbId, BrapiPaginationManager paginationManager, final Function<List<BrapiTrial>, Void> function, final Function<Integer, Void> failFunction);

    public void getStudies(String programDbId, String trialDbId, BrapiPaginationManager paginationManager, final Function<List<BrapiStudyDetails>, Void> function, final Function<Integer, Void> failFunction);

    //public void getStudyDetails(final String studyDbId, final Function<BrapiStudyDetails, Void> function, final Function<Integer, Void> failFunction);

    //public void getPlotDetails(final String studyDbId, final Function<BrapiStudyDetails, Void> function, final Function<Integer, Void> failFunction);

    //public void getOntology(BrapiPaginationManager paginationManager, final Function<List<TraitObject>, Void> function, final Function<Integer, Void> failFunction);

    public void postPhenotypes(List<Observation> observations,
                               final Function<List<Observation>, Void> function,
                               final Function<Integer, Void> failFunction);

    // will only ever have one study in current architecture
    public void putObservations(List<Observation> observations,
                                final Function<List<Observation>, Void> function,
                                final Function<Integer, Void> failFunction);

    //public void getTraits(final String studyDbId, final Function<BrapiStudyDetails, Void> function, final Function<Integer, Void> failFunction);

    //public BrapiControllerResponse saveStudyDetails(BrapiStudyDetails studyDetails);
}