package org.phenoapps.intercross.brapi.service;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.brapi.v2.model.BrAPIPagination;
import org.phenoapps.intercross.R;
import org.phenoapps.intercross.util.KeyUtil;

public class BrapiPaginationManager {

    private static String TAG = "BrAPI Pager";

    private Integer currentPage = 0;
    private Integer totalPages = 1;
    private Integer pageSize;

    private Button nextBtn;
    private Button prevBtn;
    private TextView pageIndicator;
    private Context context;

    private KeyUtil mKeyUtil;

    public BrapiPaginationManager(Context context){
        this.context = context;
        this.mKeyUtil = new KeyUtil(context);
        // Make our prev and next buttons invisible
        nextBtn = ((Activity)context).findViewById(R.id.next);
        prevBtn = ((Activity)context).findViewById(R.id.prev);
        pageIndicator = ((Activity)context).findViewById(R.id.page_indicator);

        reset();
    }

    public BrapiPaginationManager(Integer page, Integer pageSize){
        this.context = null;

        currentPage = page;
        totalPages = 1;
        this.pageSize = pageSize;
    }

    public void reset() {
        // Initially make next and prev gone until we know there are more than 1 page.

        if (nextBtn != null) nextBtn.setVisibility(View.INVISIBLE);
        if (prevBtn != null) prevBtn.setVisibility(View.INVISIBLE);
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
        refreshPageIndicator();
    }

    public void refreshPageIndicator() {
        if (pageIndicator != null) pageIndicator.setText(String.format("Page %d of %d", currentPage + 1, totalPages));

        // Determine our button visibility. Not necessary if we only have 1 page.
        determineBtnVisibility();
    }

    public void determineBtnVisibility() {
        // Determine what buttons should be visible
        if (currentPage <= 0) {
            if (prevBtn != null) prevBtn.setVisibility(View.INVISIBLE);
        } else {
            if (prevBtn != null) prevBtn.setVisibility(View.VISIBLE);
        }

        if (currentPage >= (totalPages - 1)) {
            if (nextBtn != null) nextBtn.setVisibility(View.INVISIBLE);
        } else {
            if (nextBtn != null) nextBtn.setVisibility(View.VISIBLE);
        }
    }

    public void setNewPage(int id) {
        if(R.id.prev == id){
            if (currentPage > 0){
                currentPage = currentPage - 1;
            }else {
                currentPage = 0;
            }
        }else if (R.id.next == id){
            if (currentPage < totalPages - 1){
                currentPage = currentPage + 1;
            }else {
                currentPage = totalPages - 1;
            }
        }
    }

    public Context getContext(){
        return this.context;
    }
}