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
                if (storedPermission.permissionStatus == AppPermission.STATUS_PERMISSION) {
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

            permissionNameList.sort((r1, r2) -> r1.permissionName.compareToIgnoreCase(r2.permissionName));
            permissionNameList.sort((r1, r2) -> r1.packageName.compareToIgnoreCase(r2.packageName));
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
            if (storedService.permissionStatus == AppPermission.STATUS_SERVICE) {
                serviceNameSet.add(new ServicesPair(storedService.packageName, storedService.permissionName));
            }
        }

        List<ServicesPair> serviceNameList = new ArrayList<>(serviceNameSet);
        serviceNameList.sort((r1, r2) -> r1.serviceName.compareToIgnoreCase(r2.serviceName));
        serviceNameList.sort((r1, r2) -> r1.packageName.compareToIgnoreCase(r2.packageName));
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
            if (storedReceiver.permissionStatus == AppPermission.STATUS_RECEIVER) {
                StringTokenizer tokenizer = new StringTokenizer(storedReceiver.permissionName, "|");
                String name = tokenizer.nextToken();
                String permission = tokenizer.nextToken();
                receiverNameSet.add(new ReceiversPair(storedReceiver.packageName, name, permission));
            }
        }

        List<ReceiversPair> receiverNameList = new ArrayList<>(receiverNameSet);
        receiverNameList.sort((r1, r2) -> r1.receiverName.compareToIgnoreCase(r2.receiverName));
        receiverNameList.sort((r1, r2) -> r1.packageName.compareToIgnoreCase(r2.packageName));
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

    public static List<IComponentInfo> getDisabledActivities(String searchText) {
        Set<ActivitiesPair> activityNameSet = new HashSet<>();

        // Disabled services won't be appear in the package manager anymore
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        List<AppPermission> storedActivities = appDatabase.appPermissionDao().getAll();
        for (AppPermission storedActivity : storedActivities) {
            if (storedActivity.permissionStatus == AppPermission.STATUS_ACTIVITY) {
                activityNameSet.add(new ActivitiesPair(storedActivity.packageName, storedActivity.permissionName));
            }
        }

        List<ActivitiesPair> activityNameList = new ArrayList<>(activityNameSet);
        activityNameList.sort((r1, r2) -> r1.activityName.compareToIgnoreCase(r2.activityName));
        activityNameList.sort((r1, r2) -> r1.packageName.compareToIgnoreCase(r2.packageName));
        List<IComponentInfo> activityInfoList = new ArrayList<>();
        for (ActivitiesPair pair : activityNameList) {
            if (searchText.length() <= 0
                    || pair.packageName.toLowerCase().contains(searchText.toLowerCase())
                    || pair.activityName.toLowerCase().contains(searchText.toLowerCase())
            ) {
                activityInfoList.add(new ActivityInfo(pair.packageName, pair.activityName));
            }
        }

        return activityInfoList;
    }

    public static List<IComponentInfo> getDisabledProviders(String searchText) {
        Set<ProvidersPair> providerNameSet = new HashSet<>();

        // Disabled services won't be appear in the package manager anymore
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        List<AppPermission> storedProviders = appDatabase.appPermissionDao().getAll();
        for (AppPermission storedProvider : storedProviders) {
            if (storedProvider.permissionStatus == AppPermission.STATUS_PROVIDER) {
                providerNameSet.add(new ProvidersPair(storedProvider.packageName, storedProvider.permissionName));
            }
        }

        List<ProvidersPair> providerNameList = new ArrayList<>(providerNameSet);
        providerNameList.sort((r1, r2) -> r1.activityName.compareToIgnoreCase(r2.activityName));
        providerNameList.sort((r1, r2) -> r1.packageName.compareToIgnoreCase(r2.packageName));
        List<IComponentInfo> providerInfoList = new ArrayList<>();
        for (ProvidersPair pair : providerNameList) {
            if (searchText.length() <= 0
                    || pair.packageName.toLowerCase().contains(searchText.toLowerCase())
                    || pair.activityName.toLowerCase().contains(searchText.toLowerCase())
            ) {
                providerInfoList.add(new ContentProviderInfo(pair.packageName, pair.activityName));
            }
        }

        return providerInfoList;
    }

    private static class PermissionsPair {
        private final String packageName;
        private final String permissionName;

        PermissionsPair(String packageName, String permissionName) {
            this.packageName = packageName;
            this.permissionName = permissionName;
        }
    }

    private static class ServicesPair {
        private final String packageName;
        private final String serviceName;

        ServicesPair(String packageName, String serviceName) {
            this.packageName = packageName;
            this.serviceName = serviceName;
        }
    }

    private static class ReceiversPair {
        private final String packageName;
        private final String receiverName;
        private final String permission;

        ReceiversPair(String packageName, String receiverName, String permission) {
            this.packageName = packageName;
            this.receiverName = receiverName;
            this.permission = permission;
        }
    }

    private static class ActivitiesPair {
        private final String packageName;
        private final String activityName;

        ActivitiesPair(String packageName, String activityName) {
            this.packageName = packageName;
            this.activityName = activityName;
        }
    }

    private static class ProvidersPair {
        private final String packageName;
        private final String activityName;

        ProvidersPair(String packageName, String activityName) {
            this.packageName = packageName;
            this.activityName = activityName;
        }
    }
}