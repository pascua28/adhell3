package com.fusionjack.adhell3.utils;

import android.content.ComponentName;

import androidx.documentfile.provider.DocumentFile;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.model.AppComponent;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.core.Single;

public final class AppComponentFactory {

    private static final String STORAGE_FOLDERS = "Adhell3/BatchOp";
    private static final String COMPONENTS_FILENAME = "adhell3_components.txt";
    private static AppComponentFactory instance;

    private final ApplicationPolicy appPolicy;
    private final AppDatabase appDatabase;

    private AppComponentFactory() {
        this.appPolicy = AdhellFactory.getInstance().getAppPolicy();
        this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
    }

    public static AppComponentFactory getInstance() {
        if (instance == null) {
            instance = new AppComponentFactory();
        }
        return instance;
    }

    public Single<String> processAppComponentInBatch(boolean enabled) {
        DocumentFile componentsFile;

        try {
            componentsFile = FileUtils.getDocumentFile(STORAGE_FOLDERS, COMPONENTS_FILENAME, FileUtils.FileCreationType.IF_NOT_EXIST);
        } catch (Exception e) {
            return Single.error(e);
        }

        Set<String> componentNames;
        try {
            componentNames = getFileContent(componentsFile);
        } catch (IOException e) {
            return Single.error(e);
        }

        return Single.create(emitter -> {
            if (enabled) {
                enableServices(componentNames);
                enableReceivers(componentNames);
                enableActivities(componentNames);
            } else {
                disableServices(componentNames);
                disableReceivers(componentNames);
                disableActivities(componentNames);
            }
            emitter.onSuccess("Success!");
        });
    }

    private Set<String> getFileContent(DocumentFile file) throws IOException {
        if (file.length() == 0) {
            throw new IOException("File '" + file.getName() + "' is empty !");
        }

        Set<String> lines = new HashSet<>();
        InputStream input = App.getAppContext().getContentResolver().openInputStream(file.getUri());

        if (input != null) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if ((!line.trim().startsWith("#")) && (line.trim().length() > 0)) {
                        lines.add(line.trim());
                    }
                }
            }
            input.close();
        }

        return lines;
    }

    private void enableServices(Set<String> compNames) {
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserAndDisabledApps();
        for (AppInfo app : apps) {
            String packageName = app.packageName;
            Set<String> availableServiceNames = AppComponent.getServiceNames(packageName);
            for (String compName : compNames) {
                if (availableServiceNames.contains(compName)) {
                    boolean compState = AdhellFactory.getInstance().getComponentState(packageName, compName);
                    if (!compState) {
                        ComponentName componentName = new ComponentName(packageName, compName);
                        boolean success = appPolicy.setApplicationComponentState(componentName, true);
                        if (success) {
                            appDatabase.appPermissionDao().delete(packageName, compName);
                        }
                    }
                }
            }
        }
    }

    private void disableServices(Set<String> compNames) {
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserAndDisabledApps();
        for (AppInfo app : apps) {
            String packageName = app.packageName;
            Set<String> availableServiceNames = AppComponent.getServiceNames(packageName);
            for (String compName : compNames) {
                if (availableServiceNames.contains(compName)) {
                    boolean compState = AdhellFactory.getInstance().getComponentState(packageName, compName);
                    if (compState) {
                        ComponentName componentName = new ComponentName(packageName, compName);
                        boolean success = appPolicy.setApplicationComponentState(componentName, false);
                        if (success) {
                            AppPermission appService = new AppPermission();
                            appService.packageName = packageName;
                            appService.permissionName = compName;
                            appService.permissionStatus = AppPermission.STATUS_SERVICE;
                            appService.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                            appDatabase.appPermissionDao().insert(appService);
                        }
                    }
                }
            }
        }
    }

    private void enableReceivers(Set<String> compNames) {
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserAndDisabledApps();
        for (AppInfo app : apps) {
            String packageName = app.packageName;
            Set<String> availableServiceNames = AppComponent.getReceiverNames(packageName);
            for (String compName : compNames) {
                if (availableServiceNames.contains(compName)) {
                    boolean compState = AdhellFactory.getInstance().getComponentState(packageName, compName);
                    if (!compState) {
                        ComponentName componentName = new ComponentName(packageName, compName);
                        boolean success = appPolicy.setApplicationComponentState(componentName, true);
                        if (success) {
                            String receiverPair = compName + "|Auto";
                            appDatabase.appPermissionDao().delete(packageName, receiverPair);
                        }
                    }
                }
            }
        }
    }

    private void disableReceivers(Set<String> compNames) {
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserAndDisabledApps();
        for (AppInfo app : apps) {
            String packageName = app.packageName;
            Set<String> availableServiceNames = AppComponent.getReceiverNames(packageName);
            for (String compName : compNames) {
                if (availableServiceNames.contains(compName)) {
                    boolean compState = AdhellFactory.getInstance().getComponentState(packageName, compName);
                    if (compState) {
                        ComponentName componentName = new ComponentName(packageName, compName);
                        boolean success = appPolicy.setApplicationComponentState(componentName, false);
                        if (success) {
                            AppPermission appReceiver = new AppPermission();
                            appReceiver.packageName = packageName;
                            appReceiver.permissionName = compName + "|Auto";
                            appReceiver.permissionStatus = AppPermission.STATUS_RECEIVER;
                            appReceiver.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                            appDatabase.appPermissionDao().insert(appReceiver);
                        }
                    }
                }
            }
        }
    }

    private void enableActivities(Set<String> compNames) {
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserAndDisabledApps();
        for (AppInfo app : apps) {
            String packageName = app.packageName;
            Set<String> availableActivityNames = AppComponent.getActivityNames(packageName);
            for (String compName : compNames) {
                if (availableActivityNames.contains(compName)) {
                    boolean compState = AdhellFactory.getInstance().getComponentState(packageName, compName);
                    if (!compState) {
                        ComponentName componentName = new ComponentName(packageName, compName);
                        boolean success = appPolicy.setApplicationComponentState(componentName, true);
                        if (success) {
                            appDatabase.appPermissionDao().delete(packageName, compName);
                        }
                    }
                }
            }
        }
    }

    private void disableActivities(Set<String> compNames) {
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserAndDisabledApps();
        for (AppInfo app : apps) {
            String packageName = app.packageName;
            Set<String> availableActivityNames = AppComponent.getActivityNames(packageName);
            for (String compName : compNames) {
                if (availableActivityNames.contains(compName)) {
                    boolean compState = AdhellFactory.getInstance().getComponentState(packageName, compName);
                    if (compState) {
                        ComponentName componentName = new ComponentName(packageName, compName);
                        boolean success = appPolicy.setApplicationComponentState(componentName, false);
                        if (success) {
                            AppPermission appActivity = new AppPermission();
                            appActivity.packageName = packageName;
                            appActivity.permissionName = compName;
                            appActivity.permissionStatus = AppPermission.STATUS_ACTIVITY;
                            appActivity.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                            appDatabase.appPermissionDao().insert(appActivity);
                        }
                    }
                }
            }
        }
    }
}
