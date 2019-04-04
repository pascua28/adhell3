package com.fusionjack.adhell3;


import android.app.Application;
import android.content.Context;

import com.fusionjack.adhell3.dagger.component.AppComponent;
import com.fusionjack.adhell3.dagger.component.DaggerAppComponent;
import com.fusionjack.adhell3.dagger.module.AppModule;

public class App extends Application {
    private static App instance;
    private AppComponent appComponent;
    private static Context appContext;

    public static App get() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        appComponent = initDagger(instance);
        appContext = getApplicationContext();
    }

    private AppComponent initDagger(App application) {
        return DaggerAppComponent.builder()
                .appModule(new AppModule(application))
                .build();
    }

    public static Context getAppContext() {
        return appContext;
    }

    public static void setAppContext(Context context) {
        App.appContext = context;
    }


    public AppComponent getAppComponent() {
        return appComponent;
    }
}
