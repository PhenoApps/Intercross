package org.phenoapps.intercross.brapi.service;

import android.util.Log;

import org.brapi.client.v2.ApiCallback;
import org.brapi.client.v2.model.exceptions.ApiException;

import java.util.List;
import java.util.Map;

public abstract class BrapiV2ApiCallBack<T> implements ApiCallback<T> {

    @Override
    public void onFailure(ApiException error, int i, Map<String, List<String>> map) {
        Log.e("error-of", error.toString());
    }

    @Override
    public void onUploadProgress(long l, long l1, boolean b) {
    }

    @Override
    public void onDownloadProgress(long l, long l1, boolean b) {
    }
}
