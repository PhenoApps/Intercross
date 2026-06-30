package org.phenoapps.intercross.brapi.service;

import android.content.Context;
import android.util.Log;

import org.brapi.v2.model.BrAPIPagination;

public class BrapiPaginationManager {

    private static final String TAG = "BrAPI Pager";

    private Integer currentPage = 0;
    private Integer totalPages = 1;
    private Integer pageSize;
    private final Context context;

    public BrapiPaginationManager(Context context){
        this.context = context;

        reset();
    }

    public BrapiPaginationManager(Integer page, Integer pageSize){
        this.context = null;

        currentPage = page;
        totalPages = 1;
        this.pageSize = pageSize;
    }

    public void reset() {
        //set defaults
        currentPage = 0;
        totalPages = 1;
        pageSize = getDefaultPageSize();
    }

    public Integer getDefaultPageSize(){
        String pageSizeStr = "1000";

        int pageSize = 1000;

        try {
            if (pageSizeStr != null) {
                pageSize = Integer.parseInt(pageSizeStr);
            }
        } catch (NumberFormatException nfe) {
            String message = nfe.getLocalizedMessage();
            if (message != null) {
                Log.d("FieldBookError", nfe.getLocalizedMessage());
            } else {
                Log.d("FieldBookError", "Pagination Preference number format error.");
            }
            nfe.printStackTrace();
        }

        return pageSize;
    }

    public Integer getPage() {
        return currentPage;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void update(BrAPIPagination pagination) {
        //BrAPI metadata was setting totalPages to 0. PageMan. default should be 1.
        this.totalPages = 1;
        try {
            this.totalPages = Math.max(1, pagination.getTotalPages());
        } catch (Exception e) {
            Log.e(TAG, "Pagination update failed.", e);
        }
    }

    public Context getContext(){
        return this.context;
    }
}
