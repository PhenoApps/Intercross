package org.phenoapps.intercross.application;

import android.app.Application;

import org.phenoapps.intercross.BuildConfig;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class Intercross extends Application {

    public Intercross() {
        if (BuildConfig.DEBUG) {
            //StrictMode.enableDefaults();
            //un-comment to enable strict warnings in logcat
        }
    }
}
