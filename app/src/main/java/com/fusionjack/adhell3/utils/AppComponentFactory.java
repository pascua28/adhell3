package com.fusionjack.adhell3.utils;

import android.content.ComponentName;
import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.databinding.DialogQuestionBinding;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.model.AppComponent;
import com.google.android.material.snackbar.Snackbar;
import com.fusionjack.adhell3.model.ReceiverInfo;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static com.samsung.android.knox.application.ApplicationPolicy.ERROR_NONE;
import static com.samsung.android.knox.application.ApplicationPolicy.PERMISSION_POLICY_STATE_DENY;

public final class AppComponentFactory {

    public static final String STORAGE_FOLDERS = "Adhell3/BatchOp";
    public static final String COMPONENTS_FILENAME = "adhell3_components.txt";
    public static final String OLD_SERVICE_FILENAME = "adhell3_services.txt";
    public static final String OLD_RECEIVER_FILENAME = "adhell3_receivers.txt";
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

        AppComponent.getActivityNames(packageName).forEach(activityName -> {
            boolean state = AdhellFactory.getInstance().getComponentState(packageName, activityName);
            if (!state) {
                addActivityToDatabaseIfNotExist(packageName, activityName);
            }
        });

        AppComponent.getProviderNames(packageName).forEach(providerName -> {
            boolean state = AdhellFactory.getInstance().getComponentState(packageName, providerName);
            if (!state) {
                addProviderToDatabaseIfNotExist(packageName, providerName);
            }
        });

        AppComponent.getReceivers(packageName, "").forEach(info -> {
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
                enableAppComponents(componentNames);
            } else {
                disableAppComponents(componentNames);
            }
            emitter.onSuccess("Success!");
        });
    }

    public void processAppComponentInBatchForApp(String packageName, boolean enabled) {
        DocumentFile componentsFile;

        try {
            componentsFile = FileUtils.getDocumentFile(STORAGE_FOLDERS, COMPONENTS_FILENAME, FileUtils.FileCreationType.IF_NOT_EXIST);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Set<String> componentNames;
        try {
            componentNames = getFileContent(componentsFile);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        if (enabled) {
            enableComponentsForApp(packageName, componentNames);
        } else {
            disableComponentsForApp(packageName, componentNames);
        }
    }

    public Set<String> getFileContent(DocumentFile file) throws IOException {
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

    public void checkMigrateOldBatchFiles(Context context) {
        WeakReference<Context> contextReference = new WeakReference<>(context);

        SingleObserver<String> migrateObserver = new SingleObserver<String>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
            }

            @Override
            public void onSuccess(@NonNull String s) {
                if (context instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) context;
                    mainActivity.makeSnackbar(s, Snackbar.LENGTH_LONG)
                            .show();
                }
            }

            @Override
            public void onError(@NonNull Throwable e) {
                if (context instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) context;
                    mainActivity.makeSnackbar(String.format(Locale.getDefault(), "Unable to migrate old batch files! %s", e.getMessage()), Snackbar.LENGTH_LONG)
                            .show();
                }
            }
        };

        SingleObserver<Boolean> checkObserver = new SingleObserver<Boolean>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
            }

            @Override
            public void onSuccess(@NonNull Boolean s) {
                Context context = contextReference.get();
                if (s && context != null) {
                    DialogQuestionBinding dialogQuestionBinding = DialogQuestionBinding.inflate(LayoutInflater.from(context));
                    dialogQuestionBinding.titleTextView.setText(R.string.dialog_appcomponent_batch_migrate_file_title);
                    dialogQuestionBinding.questionTextView.setText(
                            String.format(
                                    Locale.getDefault(),
                                    contextReference.get().getString(R.string.dialog_appcomponent_batch_migrate_file_summary),
                                    OLD_RECEIVER_FILENAME,
                                    OLD_SERVICE_FILENAME,
                                    COMPONENTS_FILENAME
                            )
                    );

                    AlertDialog alertDialog = new AlertDialog.Builder(contextReference.get(), R.style.AlertDialogStyle)
                            .setView(dialogQuestionBinding.getRoot())
                            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> migrateOldBatchFilesToNewSingle()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(migrateObserver))
                            .setNegativeButton(android.R.string.no, null)
                            .create();

                    alertDialog.show();
                }
            }

            @Override
            public void onError(@NonNull Throwable e) {
                if (context instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) context;
                    mainActivity.makeSnackbar(String.format(Locale.getDefault(), "Unable to check if migrate old batch files is needed! %s", e.getMessage()), Snackbar.LENGTH_LONG)
                            .show();
                }
            }
        };

        checkMigrateOldBatchFilesSingle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(checkObserver);
    }

    private Single<Boolean> checkMigrateOldBatchFilesSingle() {
        return Single.create(emitter -> {
            try {
                DocumentFile oldBatchServices = FileUtils.getDocumentFile(STORAGE_FOLDERS, OLD_SERVICE_FILENAME, FileUtils.FileCreationType.NEVER);
                DocumentFile oldBatchReceivers = FileUtils.getDocumentFile(STORAGE_FOLDERS, OLD_RECEIVER_FILENAME, FileUtils.FileCreationType.NEVER);
                DocumentFile newBatchComponents = FileUtils.getDocumentFile(STORAGE_FOLDERS, COMPONENTS_FILENAME, FileUtils.FileCreationType.NEVER);

                // Check if Old batch file exist and new one is empty
                if ((oldBatchServices != null && oldBatchServices.exists() && oldBatchServices.length() > 0) || (oldBatchReceivers != null && oldBatchReceivers.exists() && oldBatchReceivers.length() > 0)) {
                    if (newBatchComponents == null || !newBatchComponents.exists() || newBatchComponents.length() == 0) {
                        emitter.onSuccess(true);
                    }
                }
                emitter.onSuccess(false);
            } catch (Exception e) {
                emitter.onError(e);
                LogUtils.error("Unable to check batch files", e);
            }
        });
    }

    private Single<String> migrateOldBatchFilesToNewSingle() {
        return Single.create(emitter -> {
            try {
                // Get files
                DocumentFile oldBatchServices = FileUtils.getDocumentFile(STORAGE_FOLDERS, OLD_SERVICE_FILENAME, FileUtils.FileCreationType.NEVER);
                DocumentFile oldBatchReceivers = FileUtils.getDocumentFile(STORAGE_FOLDERS, OLD_RECEIVER_FILENAME, FileUtils.FileCreationType.NEVER);
                DocumentFile newBatchComponents = FileUtils.getDocumentFile(STORAGE_FOLDERS, COMPONENTS_FILENAME, FileUtils.FileCreationType.IF_NOT_EXIST);
                DocumentFile[] oldFiles = { oldBatchReceivers, oldBatchServices };

                // Get contents form old files
                List<String> lines = new ArrayList<>();
                lines.add("## This new batch file can contain services, receivers and activity to enable/disable them");
                lines.add("");
                for (DocumentFile file : oldFiles) {
                    if (file != null && file.exists()) {
                        if (file.length() > 0) {
                            lines.add(String.format(Locale.getDefault(), "# Copied from file '%s'", file.getName()));
                            InputStream input = App.getAppContext().getContentResolver().openInputStream(file.getUri());

                            if (input != null) {
                                try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        lines.add(line);
                                    }
                                }
                                input.close();
                            }
                            lines.add("");
                        }
                        file.renameTo(String.format(Locale.getDefault(), "old_%s", file.getName()));
                    }
                }

                // Write old content to new file
                OutputStream out = App.getAppContext().getContentResolver().openOutputStream(newBatchComponents.getUri());
                for(int i = 0; i < lines.size() ; i++) {
                    if (out != null) {
                        out.write(String.format("%s%s", lines.get(i), System.lineSeparator()).getBytes());
                    }
                }
                if (out != null) {
                    out.flush();
                    out.close();
                }
                emitter.onSuccess("Success, old batch files migrated to new one.");
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    private void enableAppComponents(Set<String> compNames) {
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserAndDisabledApps();
        for (AppInfo app : apps) {
            String packageName = app.packageName;
            Set<String> availableServiceNames = AppComponent.getServiceNames(packageName);
            Set<String> availableReceiverNames = AppComponent.getReceiverNames(packageName);
            Set<String> availableActivityNames = AppComponent.getActivityNames(packageName);
            Set<String> availableProviderNames = AppComponent.getProviderNames(packageName);
            for (String compName : compNames) {
                if (availableServiceNames.contains(compName) ||
                        availableActivityNames.contains(compName) ||
                        availableProviderNames.contains(compName)
                ) {
                    boolean compState = AdhellFactory.getInstance().getComponentState(packageName, compName);
                    if (!compState) {
                        ComponentName componentName = new ComponentName(packageName, compName);
                        boolean success = appPolicy.setApplicationComponentState(componentName, true);
                        if (success) {
                            appDatabase.appPermissionDao().delete(packageName, compName);
                        }
                    }
                } else if (availableReceiverNames.contains(compName)) {
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

    private void enableComponentsForApp(String packageName, Set<String> compNames) {
        Set<String> availableServiceNames = AppComponent.getServiceNames(packageName);
        Set<String> availableReceiverNames = AppComponent.getReceiverNames(packageName);
        Set<String> availableActivityNames = AppComponent.getActivityNames(packageName);
        for (String compName : compNames) {
            if (availableServiceNames.contains(compName) ||
                    availableActivityNames.contains(compName)) {
                boolean compState = AdhellFactory.getInstance().getComponentState(packageName, compName);
                if (!compState) {
                    ComponentName componentName = new ComponentName(packageName, compName);
                    boolean success = appPolicy.setApplicationComponentState(componentName, true);
                    if (success) {
                        appDatabase.appPermissionDao().delete(packageName, compName);
                    }
                }
            } else if (availableReceiverNames.contains(compName)) {
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

    private void disableAppComponents(Set<String> compNames) {
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserAndDisabledApps();
        for (AppInfo app : apps) {
            String packageName = app.packageName;
            Set<String> availableServiceNames = AppComponent.getServiceNames(packageName);
            Set<String> availableReceiverNames = AppComponent.getReceiverNames(packageName);
            Set<String> availableActivityNames = AppComponent.getActivityNames(packageName);
            Set<String> availableProviderNames = AppComponent.getProviderNames(packageName);
            for (String compName : compNames) {
                boolean disable = false;
                int permissionStatus = 0;

                if (availableServiceNames.contains(compName)) {
                    disable = true;
                    permissionStatus = AppPermission.STATUS_SERVICE;
                } else if (availableReceiverNames.contains(compName)) {
                    disable = true;
                    permissionStatus = AppPermission.STATUS_RECEIVER;
                } else if (availableActivityNames.contains(compName)) {
                    disable = true;
                    permissionStatus = AppPermission.STATUS_ACTIVITY;
                } else if (availableProviderNames.contains(compName)) {
                    disable = true;
                    permissionStatus = AppPermission.STATUS_PROVIDER;
                }

                if (disable) {
                    boolean compState = AdhellFactory.getInstance().getComponentState(packageName, compName);
                    if (compState) {
                        ComponentName componentName = new ComponentName(packageName, compName);
                        boolean success = appPolicy.setApplicationComponentState(componentName, false);
                        if (success) {
                            AppPermission appService = new AppPermission();
                            appService.packageName = packageName;
                            if (permissionStatus == AppPermission.STATUS_RECEIVER) {
                                appService.permissionName = compName + "|Auto";
                            } else {
                                appService.permissionName = compName;
                            }
                            appService.permissionStatus = permissionStatus;
                            appService.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                            appDatabase.appPermissionDao().insert(appService);
                        }
                    }
                }
            }
        }
    }

    private void disableComponentsForApp(String packageName, Set<String> compNames) {
        Set<String> availableServiceNames = AppComponent.getServiceNames(packageName);
        Set<String> availableReceiverNames = AppComponent.getReceiverNames(packageName);
        Set<String> availableActivityNames = AppComponent.getActivityNames(packageName);
        Set<String> availableProviderNames = AppComponent.getProviderNames(packageName);
        for (String compName : compNames) {
            boolean disable = false;
            int permissionStatus = 0;

            if (availableServiceNames.contains(compName)) {
                disable = true;
                permissionStatus = AppPermission.STATUS_SERVICE;
            } else if (availableReceiverNames.contains(compName)) {
                disable = true;
                permissionStatus = AppPermission.STATUS_RECEIVER;
            } else if (availableActivityNames.contains(compName)) {
                disable = true;
                permissionStatus = AppPermission.STATUS_ACTIVITY;
            } else if (availableProviderNames.contains(compName)) {
                disable = true;
                permissionStatus = AppPermission.STATUS_PROVIDER;
            }

            if (disable) {
                boolean compState = AdhellFactory.getInstance().getComponentState(packageName, compName);
                if (compState) {
                    ComponentName componentName = new ComponentName(packageName, compName);
                    boolean success = appPolicy.setApplicationComponentState(componentName, false);
                    if (success) {
                        AppPermission appService = new AppPermission();
                        appService.packageName = packageName;
                        if (permissionStatus == AppPermission.STATUS_RECEIVER) {
                            appService.permissionName = compName + "|Auto";
                        } else {
                            appService.permissionName = compName;
                        }
                        appService.permissionStatus = permissionStatus;
                        appService.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                        appDatabase.appPermissionDao().insert(appService);
                    }
                }
            }
        }
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
                insertServiceToDatabase(packageName, serviceName);
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

    // Enable all activities for the given app
    public void enableActivities(String packageName) {
        AppComponent.getActivityNames(packageName)
                .forEach(activityName -> {
                    boolean state = AdhellFactory.getInstance().getComponentState(packageName, activityName);
                    if (!state) {
                        setActivityState(true, packageName, activityName);
                    }
                });
    }

    public void toggleActivityState(String packageName, String activityName) {
        boolean state = AdhellFactory.getInstance().getComponentState(packageName, activityName);
        setActivityState(!state, packageName, activityName);
    }

    private void setActivityState(boolean state, String packageName, String activityName) {
        ComponentName serviceCompName = new ComponentName(packageName, activityName);
        boolean success = appPolicy.setApplicationComponentState(serviceCompName, state);
        if (success) {
            if (state) {
                appDatabase.appPermissionDao().delete(packageName, activityName);
            } else {
                insertActivityToDatabase(packageName, activityName);
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
        AppPermission appService = new AppPermission();
        appService.packageName = packageName;
        appService.permissionName = activityName;
        appService.permissionStatus = AppPermission.STATUS_ACTIVITY;
        appService.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
        appDatabase.appPermissionDao().insert(appService);
    }

    // Enable all activities for the given app
    public void enableProviders(String packageName) {
        AppComponent.getActivityNames(packageName)
                .forEach(providerName -> {
                    boolean state = AdhellFactory.getInstance().getComponentState(packageName, providerName);
                    if (!state) {
                        setProviderState(true, packageName, providerName);
                    }
                });
    }

    public void toggleProviderState(String packageName, String providerName) {
        boolean state = AdhellFactory.getInstance().getComponentState(packageName, providerName);
        setProviderState(!state, packageName, providerName);
    }

    private void setProviderState(boolean state, String packageName, String providerName) {
        ComponentName serviceCompName = new ComponentName(packageName, providerName);
        boolean success = appPolicy.setApplicationComponentState(serviceCompName, state);
        if (success) {
            if (state) {
                appDatabase.appPermissionDao().delete(packageName, providerName);
            } else {
                insertProviderToDatabase(packageName, providerName);
            }
        }
    }

    public void addProviderToDatabaseIfNotExist(String packageName, String providerName) {
        AppPermission provider = appDatabase.appPermissionDao().getProvider(packageName, providerName);
        if (provider == null) {
            LogUtils.info("Adding provider name '" + packageName + "|" + providerName + "' to database.");
            insertProviderToDatabase(packageName, providerName);
        }
    }

    private void insertProviderToDatabase(String packageName, String providerName) {
        AppPermission appService = new AppPermission();
        appService.packageName = packageName;
        appService.permissionName = providerName;
        appService.permissionStatus = AppPermission.STATUS_PROVIDER;
        appService.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
        appDatabase.appPermissionDao().insert(appService);
    }

    // Enable all receivers for the given app
    public void enableReceivers(String packageName) {
        AppComponent.getReceivers(packageName, "")
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
                insertReceiverToDatabase(packageName, receiverName, receiverPermission);
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
