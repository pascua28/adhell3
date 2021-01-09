package com.fusionjack.adhell3.utils;

import android.content.ComponentName;
import android.os.Environment;

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

    private static final String SERVICE_FILENAME = "adhell3_services.txt";
    private static final String RECEIVER_FILENAME = "adhell3_receivers.txt";
    private static AppComponentFactory instance;

    private final ApplicationPolicy appPolicy;
    private final AppDatabase appDatabase;

    private AppComponentFactory() {
        this.appPolicy = AdhellFactory.getInstance().getAppPolicy();
        this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
    }

    private Set<String> readTxtServices() {
        Set<String> serviceNames;
        try {
            serviceNames = getFileContent(SERVICE_FILENAME);
        } catch (IOException e) {
            serviceNames = Collections.emptySet();
        }
        return serviceNames;
    }

    private Set<String> readTxtReceivers() {
        Set<String> receiverNames;
        try {
            receiverNames = getFileContent(RECEIVER_FILENAME);
        } catch (IOException e) {
            receiverNames = Collections.emptySet();
        }
        return receiverNames;
    }

    private Set<String> getFileContent(String fileName) throws IOException {
        File serviceFile = new File(Environment.getExternalStorageDirectory(), fileName);
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

        AppComponent.getServiceNames(packageName).forEach(serviceName -> {
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

    // Enable all permissions for the given app
    public void enablePermissions(String packageName) {
        List<String> deniedPermissions = appPolicy.getRuntimePermissions(packageName, PERMISSION_POLICY_STATE_DENY);
        int errorCode = AdhellFactory.getInstance().setAppPermission(packageName, deniedPermissions, true);
        if (errorCode == ERROR_NONE) {
            appDatabase.appPermissionDao().deletePermissions(packageName);
        }
        //deniedPermissions.forEach(permissionName -> setPermissionState(state, packageName, permissionName));
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
        Set<String> serviceNames = readTxtServices();
        Set<String> receiverNames = readTxtReceivers();
        if (serviceNames.isEmpty() || receiverNames.isEmpty()) {
            return Single.error(new FileNotFoundException("File name '" + SERVICE_FILENAME + "' or '" + RECEIVER_FILENAME + "' cannot be found."));
        }
        return Single.create(emitter -> {
            if (enabled) {
                enableTxtServices();
                enableTxtReceivers();
            } else {
                disableTxtServices();
                disableTxtReceivers();
            }
            emitter.onSuccess("Success!");
        });
    }

    // Enable services from 'adhell3_services.txt' for all apps
    private void enableTxtServices() {
        Set<String> serviceNames = readTxtServices();
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserAndDisabledApps();
        for (AppInfo app : apps) {
            String packageName = app.packageName;
            setTxtServicesState(true, packageName, serviceNames);
        }
    }

    // Disable services from 'adhell3_services.txt' for all apps
    private void disableTxtServices() {
        Set<String> serviceNames = readTxtServices();
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserAndDisabledApps();
        for (AppInfo app : apps) {
            setTxtServicesState(false, app.packageName, serviceNames);
        }
    }

    // Disable services from 'adhell3_services.txt' for the given app
    public void disableTxtServices(String packageName) {
        Set<String> serviceNames = readTxtServices();
        setTxtServicesState(false, packageName, serviceNames);
    }

    // Only services from 'adhell3_services.txt' will be enabled/disabled
    private void setTxtServicesState(boolean state, String packageName, Set<String> serviceNames) {
        AppComponent.getServiceNames(packageName).stream()
                .filter(serviceNames::contains)
                .forEach(serviceName -> {
                    boolean currentState = AdhellFactory.getInstance().getComponentState(packageName, serviceName);
                    if (state != currentState) {
                        setServiceState(state, packageName, serviceName);
                    }
                });
    }

    // Enable all services for the given app
    public void enableServices(String packageName) {
        AppComponent.getServiceNames(packageName)
                .forEach(serviceName -> {
                    boolean state = AdhellFactory.getInstance().getComponentState(packageName, serviceName);
                    if (!state) {
                        setServiceState(true, packageName, serviceName);
                    }
                });
    }

    public void toggleServiceState(String packageName, String serviceName) {
        boolean state = AdhellFactory.getInstance().getComponentState(packageName, serviceName);
        setServiceState(!state, packageName, serviceName);
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
        Set<String> receiverNames = readTxtReceivers();
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserAndDisabledApps();
        for (AppInfo app : apps) {
            setTxtReceiversState(true, app.packageName, receiverNames);
        }
    }

    // Disable services from 'adhell3_receivers.txt' for all apps
    private void disableTxtReceivers() {
        Set<String> receiverNames = readTxtReceivers();
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserAndDisabledApps();
        for (AppInfo app : apps) {
            setTxtReceiversState(false, app.packageName, receiverNames);
        }
    }

    // Disable services from 'adhell3_receivers.txt' for the given app
    public void disableTxtReceivers(String packageName) {
        Set<String> receiverNames = readTxtReceivers();
        setTxtReceiversState(false, packageName, receiverNames);
    }

    // Only receivers from 'adhell3_services.txt' will be enabled/disabled
    private void setTxtReceiversState(boolean state, String packageName, Set<String> receiverNames) {
        AppComponent.getReceivers(packageName).stream()
                .filter(info -> receiverNames.contains(((ReceiverInfo)info).getName()))
                .forEach(info -> {
                    String receiverName = ((ReceiverInfo) info).getName();
                    String receiverPermission = ((ReceiverInfo) info).getPermission();
                    boolean currentState = AdhellFactory.getInstance().getComponentState(packageName, receiverName);
                    if (state != currentState) {
                        setReceiverState(state, packageName, receiverName, receiverPermission);
                    }
                });
    }

    // Enable all receivers for the given app
    public void enableReceivers(String packageName) {
        AppComponent.getReceivers(packageName)
                .forEach(info -> {
                    String receiverName = ((ReceiverInfo) info).getName();
                    String receiverPermission = ((ReceiverInfo) info).getPermission();
                    boolean state = AdhellFactory.getInstance().getComponentState(packageName, receiverName);
                    if (!state) {
                        setReceiverState(true, packageName, receiverName, receiverPermission);
                    }
                });
    }

    public void toggleReceiverState(String packageName, String receiverName, String receiverPermission) {
        boolean state = AdhellFactory.getInstance().getComponentState(packageName, receiverName);
        setReceiverState(!state, packageName, receiverName, receiverPermission);
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
