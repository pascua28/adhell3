package com.fusionjack.adhell3.model;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPermissionUtils;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import static com.samsung.android.knox.application.ApplicationPolicy.PERMISSION_POLICY_STATE_DENY;

public class AppComponentDisabled {

    public static List<IComponentInfo> getDisabledPermissions(String searchText) {
        List<PermissionsPair> permissionNameList = new ArrayList<>();
        PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();

        try {
            List<String> packagesList = new ArrayList<>();
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            List<AppPermission> storedPermissions = appDatabase.appPermissionDao().getAll();
            for (AppPermission storedPermission : storedPermissions) {
                if (storedPermission.permissionStatus == -1) {
                    if (!packagesList.contains(storedPermission.packageName)) {
                        ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
                        List<String> deniedPermissions = null;
                        if (appPolicy != null) {
                            deniedPermissions = appPolicy.getRuntimePermissions(storedPermission.packageName, PERMISSION_POLICY_STATE_DENY);
                        }
                        if (deniedPermissions != null) {
                            PackageInfo packageInfo = packageManager.getPackageInfo(storedPermission.packageName, PackageManager.GET_PERMISSIONS);
                            if (packageInfo != null) {
                                List<String> permissions = new ArrayList<>(Arrays.asList(packageInfo.requestedPermissions));
                                for (String deniedPermission : deniedPermissions) {
                                    if (permissions.contains(deniedPermission))
                                        permissionNameList.add(new PermissionsPair(storedPermission.packageName, deniedPermission));
                                }
                            }
                        }
                        packagesList.add(storedPermission.packageName);
                    }
                }
            }

            Collections.sort(permissionNameList, (r1, r2) -> r1.permissionName.compareToIgnoreCase(r2.permissionName));
            Collections.sort(permissionNameList, (r1, r2) -> r1.packageName.compareToIgnoreCase(r2.packageName));
        } catch (Exception ignored) {
        }

        List<IComponentInfo> permissionList = new ArrayList<>();
        for (PermissionsPair permissionName : permissionNameList) {
            if (searchText.length() <= 0
                    || permissionName.packageName.toLowerCase().contains(searchText.toLowerCase())
                    || permissionName.permissionName.toLowerCase().contains(searchText.toLowerCase())) {
                try {
                    android.content.pm.PermissionInfo info = packageManager.getPermissionInfo(permissionName.permissionName, PackageManager.GET_META_DATA);
                    if (AppPermissionUtils.isDangerousLevel(info.protectionLevel)) {
                        CharSequence description = info.loadDescription(packageManager);
                        permissionList.add(new PermissionInfo(permissionName.permissionName,
                                description == null ? "No description" : description.toString(),
                                info.protectionLevel, permissionName.packageName));
                    }
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }
        }

        return permissionList;
    }

    public static List<IComponentInfo> getDisabledServices(String searchText) {
        Set<ServicesPair> serviceNameSet = new HashSet<>();

        // Disabled services won't be appear in the package manager anymore
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        List<AppPermission> storedServices = appDatabase.appPermissionDao().getAll();
        for (AppPermission storedService : storedServices) {
            if (storedService.permissionStatus == 2) {
                serviceNameSet.add(new ServicesPair(storedService.packageName, storedService.permissionName));
            }
        }

        List<ServicesPair> serviceNameList = new ArrayList<>(serviceNameSet);
        Collections.sort(serviceNameList, (r1, r2) -> r1.serviceName.compareToIgnoreCase(r2.serviceName));
        Collections.sort(serviceNameList, (r1, r2) -> r1.packageName.compareToIgnoreCase(r2.packageName));
        List<IComponentInfo> serviceInfoList = new ArrayList<>();
        for (ServicesPair pair : serviceNameList) {
            if (searchText.length() <= 0
                    || pair.packageName.toLowerCase().contains(searchText.toLowerCase())
                    || pair.serviceName.toLowerCase().contains(searchText.toLowerCase())
            ) {
                serviceInfoList.add(new ServiceInfo(pair.packageName, pair.serviceName));
            }
        }

        return serviceInfoList;
    }


    public static List<IComponentInfo> getDisabledReceivers(String searchText) {
        Set<ReceiversPair> receiverNameSet = new HashSet<>();

        // Disabled services won't be appear in the package manager anymore
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        List<AppPermission> storedReceivers = appDatabase.appPermissionDao().getAll();
        for (AppPermission storedReceiver : storedReceivers) {
            if (storedReceiver.permissionStatus == 5) {
                StringTokenizer tokenizer = new StringTokenizer(storedReceiver.permissionName, "|");
                String name = tokenizer.nextToken();
                String permission = tokenizer.nextToken();
                receiverNameSet.add(new ReceiversPair(storedReceiver.packageName, name, permission));
            }
        }

        List<ReceiversPair> receiverNameList = new ArrayList<>(receiverNameSet);
        Collections.sort(receiverNameList, (r1, r2) -> r1.receiverName.compareToIgnoreCase(r2.receiverName));
        Collections.sort(receiverNameList, (r1, r2) -> r1.packageName.compareToIgnoreCase(r2.packageName));
        List<IComponentInfo> receiverInfoList = new ArrayList<>();
        for (ReceiversPair pair : receiverNameList) {
            if (searchText.length() <= 0
                    || pair.packageName.toLowerCase().contains(searchText.toLowerCase())
                    || pair.receiverName.toLowerCase().contains(searchText.toLowerCase())
            ) {
                receiverInfoList.add(new ReceiverInfo(pair.packageName, pair.receiverName, pair.permission));
            }
        }

        return receiverInfoList;
    }

    private static class PermissionsPair {
        private String packageName;
        private String permissionName;

        PermissionsPair(String packageName, String permissionName) {
            this.packageName = packageName;
            this.permissionName = permissionName;
        }
    }

    private static class ServicesPair {
        private String packageName;
        private String serviceName;

        ServicesPair(String packageName, String serviceName) {
            this.packageName = packageName;
            this.serviceName = serviceName;
        }
    }

    private static class ReceiversPair {
        private String packageName;
        private String receiverName;
        private String permission;

        ReceiversPair(String packageName, String receiverName, String permission) {
            this.packageName = packageName;
            this.receiverName = receiverName;
            this.permission = permission;
        }
    }
}