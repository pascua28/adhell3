package com.fusionjack.adhell3.utils;

import android.content.ComponentName;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.model.AppComponent;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

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

    public void checkMigrateOldBatchFiles(WeakReference<Context> contextReference, WeakReference<ViewGroup> viewGroupReference) {
        SingleObserver<String> migrateObserver = new SingleObserver<String>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onSuccess(String s) {
                Toast.makeText(contextReference.get(), s, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(contextReference.get(), "Unable to migrate old batch files!" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        };

        SingleObserver<Boolean> checkObserver = new SingleObserver<Boolean>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onSuccess(Boolean s) {
                if (s) {
                    View dialogView = LayoutInflater.from(contextReference.get()).inflate(R.layout.dialog_question, viewGroupReference.get(), false);
                    TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
                    titleTextView.setText(R.string.dialog_appcomponent_batch_migrate_file_title);
                    TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
                    questionTextView.setText(
                            String.format(
                                    Locale.getDefault(),
                                    contextReference.get().getString(R.string.dialog_appcomponent_batch_migrate_file_summary),
                                    OLD_RECEIVER_FILENAME,
                                    OLD_SERVICE_FILENAME,
                                    COMPONENTS_FILENAME
                            )
                    );

                    AlertDialog alertDialog = new AlertDialog.Builder(contextReference.get(), R.style.AlertDialogStyle)
                            .setView(dialogView)
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
            public void onError(Throwable e) {
                Toast.makeText(contextReference.get(), "Unable to check if migrate old batch files is needed!" + e.getMessage(), Toast.LENGTH_LONG).show();
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
