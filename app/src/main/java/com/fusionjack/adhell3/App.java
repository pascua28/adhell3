package com.fusionjack.adhell3;


import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.preference.PreferenceManager;

import com.fusionjack.adhell3.dagger.component.AppComponent;
import com.fusionjack.adhell3.dagger.component.DaggerAppComponent;
import com.fusionjack.adhell3.dagger.module.AppModule;

import timber.log.Timber;

import static com.fusionjack.adhell3.fragments.SettingsFragment.SET_NIGHT_MODE_PREFERENCE;

public class App extends Application implements DefaultLifecycleObserver {
    private static App instance;
    private static Context appContext;
    private AppComponent appComponent;

    public static App get() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        // Set the Night Mode according to saved preferences
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (mPrefs.getBoolean(SET_NIGHT_MODE_PREFERENCE, false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        instance = this;
        appComponent = initDagger(instance);
        appContext = getApplicationContext();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        // Clear resources to prevent memory leak
        instance = null;
        appContext = null;
        appComponent = null;
    }

    private AppComponent initDagger(App application) {
        return DaggerAppComponent.builder()
                .appModule(new AppModule(application))
                .build();
    }

    public static Context getAppContext() {
        return appContext;
    }


    public AppComponent getAppComponent() {
        return appComponent;
    }
}
