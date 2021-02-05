package com.fusionjack.adhell3.utils;

import android.content.ComponentName;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.model.AppComponent;
import com.fusionjack.adhell3.model.ReceiverInfo;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.Single;

import static com.samsung.android.knox.application.ApplicationPolicy.ERROR_NONE;
import static com.samsung.android.knox.application.ApplicationPolicy.PERMISSION_POLICY_STATE_DENY;

public final class AppComponentFactory {

    private static final String ACTIVITY_FILENAME = "adhell3_activities.txt";
    private static final String SERVICE_FILENAME = "adhell3_services.txt";
    private static final String RECEIVER_FILENAME = "adhell3_receivers.txt";
    private static AppComponentFactory instance;

    private final ApplicationPolicy appPolicy;
    private final AppDatabase appDatabase;

    private AppComponentFactory() {
        this.appPolicy = AdhellFactory.getInstance().getAppPolicy();
        this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
    }

    private Set<String> readTxtComponent(String fileName) {
        Set<String> componentNames;
        try {
            componentNames = getFileContent(fileName);
        } catch (IOException e) {
            componentNames = Collections.emptySet();
        }
        return componentNames;
    }

    private Set<String> getFileContent(String fileName) throws IOException {
        File serviceFile = FileUtils.toFile(fileName);
        if (!serviceFile.exists()) {
            throw new FileNotFoundException("File name " + fileName + " cannot be found");
        }

        Set<String> lines = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(serviceFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line.trim());
            }
        }

        return lines;
    }

    public static AppComponentFactory getInstance() {
        if (instance == null) {
            instance = new AppComponentFactory();
        }
        return instance;
    }

    // If an app component is disabled, it should be existed in the database
    // If it is not the case, insert it into database
    public void checkAppComponentConsistency(String packageName) {
        List<String> deniedPermissions = appPolicy.getRuntimePermissions(packageName, PERMISSION_POLICY_STATE_DENY);
        deniedPermissions.forEach(permissionName -> addPermissionToDatabaseIfNotExist(packageName, permissionName));

        AppComponent.getActivities(packageName).forEach(activityName -> {
            boolean state = AdhellFactory.getInstance().getComponentState(packageName, activityName);
            if (!state) {
                addActivityToDatabaseIfNotExist(packageName, activityName);
            }
        });

        AppComponent.getServices(packageName).forEach(serviceName -> {
            boolean state = AdhellFactory.getInstance().getComponentState(packageName, serviceName);
            if (!state) {
                addServiceToDatabaseIfNotExist(packageName, serviceName);
            }
        });

        AppComponent.getReceivers(packageName).forEach(info -> {
            ReceiverInfo receiverInfo = ((ReceiverInfo) info);
            String receiverName = receiverInfo.getName();
            String receiverPermission = receiverInfo.getPermission();
            boolean state = AdhellFactory.getInstance().getComponentState(packageName, receiverName);
            if (!state) {
                addReceiverToDatabaseIfNotExist(packageName, receiverName, receiverPermission);
            }
        });
    }

    public void togglePermissionState(String packageName, String permissionName) {
        List<String> deniedPermissions = appPolicy.getRuntimePermissions(packageName, PERMISSION_POLICY_STATE_DENY);
        boolean state = deniedPermissions.contains(permissionName);
        setPermissionState(state, packageName, permissionName);
    }

    public void toggleActivityState(String packageName, String activityName) {
        boolean state = AdhellFactory.getInstance().getComponentState(packageName, activityName);
        setActivityState(!state, packageName, activityName);
    }

    public void toggleServiceState(String packageName, String serviceName) {
        boolean state = AdhellFactory.getInstance().getComponentState(packageName, serviceName);
        setServiceState(!state, packageName, serviceName);
    }

    public void toggleReceiverState(String packageName, String receiverName, String receiverPermission) {
        boolean state = AdhellFactory.getInstance().getComponentState(packageName, receiverName);
        setReceiverState(!state, packageName, receiverName, receiverPermission);
    }

    // Enable all permissions for the given app
    public void enablePermissions(String packageName) {
        List<String> deniedPermissions = appPolicy.getRuntimePermissions(packageName, PERMISSION_POLICY_STATE_DENY);
        int errorCode = AdhellFactory.getInstance().setAppPermission(packageName, deniedPermissions, true);
        if (errorCode == ERROR_NONE) {
            appDatabase.appPermissionDao().deletePermissions(packageName);
        }
        //deniedPermissions.forEach(permissionName -> setPermissionState(state, packageName, permissionName));
    }

    // Enable all services for the given app
    public void enableActivities(String packageName) {
        AppComponent.getActivities(packageName)
                .forEach(activityName -> {
                    boolean state = AdhellFactory.getInstance().getComponentState(packageName, activityName);
                    if (!state) {
                        setActivityState(true, packageName, activityName);
                    }
                });
    }

    // Enable all services for the given app
    public void enableServices(String packageName) {
        AppComponent.getServices(packageName)
                .forEach(serviceName -> {
                    boolean state = AdhellFactory.getInstance().getComponentState(packageName, serviceName);
                    if (!state) {
                        setServiceState(true, packageName, serviceName);
                    }
                });
    }

    // Enable all receivers for the given app
    public void enableReceivers(String packageName) {
        AppComponent.getReceivers(packageName)
                .forEach(info -> {
                    String receiverName = info.getName();
                    String receiverPermission = ((ReceiverInfo) info).getPermission();
                    boolean state = AdhellFactory.getInstance().getComponentState(packageName, receiverName);
                    if (!state) {
                        setReceiverState(true, packageName, receiverName, receiverPermission);
                    }
                });
    }

    private void setPermissionState(boolean state, String packageName, String permissionName) {
        int errorCode = AdhellFactory.getInstance().setAppPermission(packageName, Collections.singletonList(permissionName), state);
        if (errorCode == ApplicationPolicy.ERROR_NONE) {
            if (state) {
                List<String> siblingPermissionNames = AppPermissionUtils.getSiblingPermissions(permissionName);
                for (String name : siblingPermissionNames) {
                    appDatabase.appPermissionDao().delete(packageName, name);
                }
            } else {
                insertPermissionToDatabase(packageName, permissionName);
            }
        }
    }

    public void addPermissionToDatabaseIfNotExist(String packageName, String permissionName) {
        AppPermission permission = appDatabase.appPermissionDao().getPermission(packageName, permissionName);
        if (permission == null) {
            LogUtils.info("Adding permission name '" + packageName + "|" + permissionName + "' to database.");
            insertPermissionToDatabase(packageName, permissionName);
        }
    }

    private void insertPermissionToDatabase(String packageName, String permissionName) {
        AppPermission appPermission = new AppPermission();
        appPermission.packageName = packageName;
        appPermission.permissionName = permissionName;
        appPermission.permissionStatus = AppPermission.STATUS_PERMISSION;
        appPermission.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
        appDatabase.appPermissionDao().insert(appPermission);
    }

    public Single<String> processAppComponentInBatch(boolean enabled) {
        return Single.create(emitter -> {
            if (enabled) {
                enableTxtActivities();
                enableTxtServices();
                enableTxtReceivers();
            } else {
                disableTxtActivities();
                disableTxtServices();
                disableTxtReceivers();
            }
            emitter.onSuccess("Success!");
        });
    }

    private void setActivityState(boolean state, String packageName, String activityName) {
        ComponentName activityCompName = new ComponentName(packageName, activityName);
        boolean success = appPolicy.setApplicationComponentState(activityCompName, state);
        if (success) {
            if (state) {
                appDatabase.appPermissionDao().delete(packageName, activityName);
            } else {
                addActivityToDatabaseIfNotExist(packageName, activityName);
            }
        }
    }

    public void addActivityToDatabaseIfNotExist(String packageName, String activityName) {
        AppPermission activity = appDatabase.appPermissionDao().getActivity(packageName, activityName);
        if (activity == null) {
            LogUtils.info("Adding activity name '" + packageName + "|" + activityName + "' to database.");
            insertActivityToDatabase(packageName, activityName);
        }
    }

    private void insertActivityToDatabase(String packageName, String activityName) {
        AppPermission appActivity = new AppPermission();
        appActivity.packageName = packageName;
        appActivity.permissionName = activityName;
        appActivity.permissionStatus = AppPermission.STATUS_ACTIVITY;
        appActivity.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
        appDatabase.appPermissionDao().insert(appActivity);
    }

    // Enable activities from 'adhell3_activities.txt' for all apps
    private void enableTxtActivities() {
        Set<String> activityNames = readTxtComponent(ACTIVITY_FILENAME);
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserAndDisabledApps();
        for (AppInfo app : apps) {
            String packageName = app.packageName;
            setTxtActivitiesState(true, packageName, activityNames);
        }
    }

    // Disable activities from 'adhell3_activities.txt' for all apps
    private void disableTxtActivities() {
        Set<String> activityNames = readTxtComponent(ACTIVITY_FILENAME);
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserAndDisabledApps();
        for (AppInfo app : apps) {
            setTxtActivitiesState(false, app.packageName, activityNames);
        }
    }

    // Disable activities from 'adhell3_activities.txt' for the given app
    public void disableTxtActivities(String packageName) {
        Set<String> activityNames = readTxtComponent(ACTIVITY_FILENAME);
        setTxtActivitiesState(false, packageName, activityNames);
    }

    // Only activities from 'adhell3_activities.txt' will be enabled/disabled
    private void setTxtActivitiesState(boolean state, String packageName, Set<String> activityNames) {
        AppComponent.getActivities(packageName).stream()
                .filter(activityNames::contains)
                .forEach(activityName -> {
                    boolean currentState = AdhellFactory.getInstance().getComponentState(packageName, activityName);
                    if (state != currentState) {
                        setActivityState(state, packageName, activityName);
                    }
                });
    }

    // Enable services from 'adhell3_services.txt' for all apps
    private void enableTxtServices() {
        Set<String> serviceNames = readTxtComponent(SERVICE_FILENAME);
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserAndDisabledApps();
        for (AppInfo app : apps) {
            String packageName = app.packageName;
            setTxtServicesState(true, packageName, serviceNames);
        }
    }

    // Disable services from 'adhell3_services.txt' for all apps
    private void disableTxtServices() {
        Set<String> serviceNames = readTxtComponent(SERVICE_FILENAME);
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserAndDisabledApps();
        for (AppInfo app : apps) {
            setTxtServicesState(false, app.packageName, serviceNames);
        }
    }

    // Disable services from 'adhell3_services.txt' for the given app
    public void disableTxtServices(String packageName) {
        Set<String> serviceNames = readTxtComponent(SERVICE_FILENAME);
        setTxtServicesState(false, packageName, serviceNames);
    }

    // Only services from 'adhell3_services.txt' will be enabled/disabled
    private void setTxtServicesState(boolean state, String packageName, Set<String> serviceNames) {
        AppComponent.getServices(packageName).stream()
                .filter(serviceNames::contains)
                .forEach(serviceName -> {
                    boolean currentState = AdhellFactory.getInstance().getComponentState(packageName, serviceName);
                    if (state != currentState) {
                        setServiceState(state, packageName, serviceName);
                    }
                });
    }

    private void setServiceState(boolean state, String packageName, String serviceName) {
        ComponentName serviceCompName = new ComponentName(packageName, serviceName);
        boolean success = appPolicy.setApplicationComponentState(serviceCompName, state);
        if (success) {
            if (state) {
                appDatabase.appPermissionDao().delete(packageName, serviceName);
            } else {
                addServiceToDatabaseIfNotExist(packageName, serviceName);
            }
        }
    }

    public void addServiceToDatabaseIfNotExist(String packageName, String serviceName) {
        AppPermission service = appDatabase.appPermissionDao().getService(packageName, serviceName);
        if (service == null) {
            LogUtils.info("Adding service name '" + packageName + "|" + serviceName + "' to database.");
            insertServiceToDatabase(packageName, serviceName);
        }
    }

    private void insertServiceToDatabase(String packageName, String serviceName) {
        AppPermission appService = new AppPermission();
        appService.packageName = packageName;
        appService.permissionName = serviceName;
        appService.permissionStatus = AppPermission.STATUS_SERVICE;
        appService.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
        appDatabase.appPermissionDao().insert(appService);
    }

    // Enable services from 'adhell3_receivers.txt' for all apps
    private void enableTxtReceivers() {
        Set<String> receiverNames = readTxtComponent(RECEIVER_FILENAME);
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserAndDisabledApps();
        for (AppInfo app : apps) {
            setTxtReceiversState(true, app.packageName, receiverNames);
        }
    }

    // Disable services from 'adhell3_receivers.txt' for all apps
    private void disableTxtReceivers() {
        Set<String> receiverNames = readTxtComponent(RECEIVER_FILENAME);
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserAndDisabledApps();
        for (AppInfo app : apps) {
            setTxtReceiversState(false, app.packageName, receiverNames);
        }
    }

    // Disable services from 'adhell3_receivers.txt' for the given app
    public void disableTxtReceivers(String packageName) {
        Set<String> receiverNames = readTxtComponent(RECEIVER_FILENAME);
        setTxtReceiversState(false, packageName, receiverNames);
    }

    // Only receivers from 'adhell3_services.txt' will be enabled/disabled
    private void setTxtReceiversState(boolean state, String packageName, Set<String> receiverNames) {
        AppComponent.getReceivers(packageName).stream()
                .filter(info -> receiverNames.contains(info.getName()))
                .forEach(info -> {
                    String receiverName = info.getName();
                    String receiverPermission = ((ReceiverInfo) info).getPermission();
                    boolean currentState = AdhellFactory.getInstance().getComponentState(packageName, receiverName);
                    if (state != currentState) {
                        setReceiverState(state, packageName, receiverName, receiverPermission);
                    }
                });
    }

    private void setReceiverState(boolean state, String packageName, String receiverName, String receiverPermission) {
        int indexOfPipe = receiverName.indexOf('|');
        if (indexOfPipe != -1) {
            receiverName = receiverName.substring(0, indexOfPipe);
        }
        ComponentName receiverCompName = new ComponentName(packageName, receiverName);
        boolean success = appPolicy.setApplicationComponentState(receiverCompName, state);
        if (success) {
            if (state) {
                deleteReceiverFromDatabase(packageName, receiverName, receiverPermission);
            } else {
                addReceiverToDatabaseIfNotExist(packageName, receiverName, receiverPermission);
            }
        }
    }

    public void addReceiverToDatabaseIfNotExist(String packageName, String receiverName, String receiverPermission) {
        AppPermission receiver = appDatabase.appPermissionDao().getReceiver(packageName, buildReceiverPair(receiverName, receiverPermission));
        if (receiver == null) {
            LogUtils.info("Adding receiver name '" + packageName + "|" + receiverName + "|" + receiverPermission + "' to database.");
            insertReceiverToDatabase(packageName, receiverName, receiverPermission);
        }
    }

    private void insertReceiverToDatabase(String packageName, String receiverName, String receiverPermission) {
        AppPermission appReceiver = new AppPermission();
        appReceiver.packageName = packageName;
        appReceiver.permissionName = buildReceiverPair(receiverName, receiverPermission);
        appReceiver.permissionStatus = AppPermission.STATUS_RECEIVER;
        appReceiver.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
        appDatabase.appPermissionDao().insert(appReceiver);
    }

    private void deleteReceiverFromDatabase(String packageName, String receiverName, String receiverPermission) {
        String receiverPair = buildReceiverPair(receiverName, receiverPermission);
        appDatabase.appPermissionDao().delete(packageName, receiverPair);
        receiverPair = buildReceiverPair(receiverName,"Auto");
        appDatabase.appPermissionDao().delete(packageName, receiverPair);
    }

    private String buildReceiverPair(String receiverName, String receiverPermission) {
        return receiverName + "|" + receiverPermission;
    }
}
