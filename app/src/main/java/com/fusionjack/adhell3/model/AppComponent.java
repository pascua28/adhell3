package com.fusionjack.adhell3.model;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPermissionUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class AppComponent {

    /*public static List<IComponentInfo> getPermissions(String packageName, String searchText) {
        List<String> permissionNameList = new ArrayList<>();
        PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();*/

    private static final Comparator<PermissionInfo> PERMISSION_COMPARATOR = Comparator.comparing(PermissionInfo::getName);
    private static final Comparator<ServiceInfo> SERVICE_COMPARATOR = Comparator.comparing(ServiceInfo::getName);
    private static final Comparator<ReceiverInfo> RECEIVER_COMPARATOR = Comparator.comparing(ReceiverInfo::getName);
    private static final Comparator<ActivityInfo> ACTIVITY_COMPARATOR = Comparator.comparing(ActivityInfo::getName);
    private static final Comparator<ProviderInfo> PROVIDER_COMPARATOR = Comparator.comparing(ProviderInfo::getName);

    public static List<IComponentInfo> getPermissions(String packageName, String searchText) {
        List<IComponentInfo> permissionList = Collections.emptyList();

        try {
            PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            if (packageInfo != null) {
                String[] permissions = packageInfo.requestedPermissions;
                if (permissions != null) {
                    permissionList = Arrays.stream(permissions)
                            .map(permissionName -> {
                                PermissionInfo permissionInfo = PermissionInfo.EMPTY_PERMISSION;
                                try {
                                    android.content.pm.PermissionInfo info = packageManager.getPermissionInfo(permissionName, PackageManager.GET_META_DATA);
                                    if (AppPermissionUtils.isDangerousLevel(info.protectionLevel)) {
                                        CharSequence description = info.loadDescription(packageManager);
                                        permissionInfo = new PermissionInfo(permissionName, description, info.protectionLevel, packageName);
                                    }
                                } catch (PackageManager.NameNotFoundException ignored) {
                                }
                                return permissionInfo;
                            })
                            .filter(info -> {
                                if (searchText != null && !searchText.isEmpty()) {
                                    return !info.equals(PermissionInfo.EMPTY_PERMISSION) &&
                                            (info.getName().toLowerCase().contains(searchText.toLowerCase()) ||
                                            info.getPackageName().toLowerCase().contains(searchText.toLowerCase()));
                                } else {
                                    return !info.equals(PermissionInfo.EMPTY_PERMISSION);
                                }
                            })
                            .sorted(PERMISSION_COMPARATOR)
                            .collect(Collectors.toList());
                }
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return permissionList;
    }

    public static Set<String> getServiceNames(String packageName) {
        Set<String> serviceNameSet = new HashSet<>();
        PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();

        // Disabled services won't be appear in the package manager anymore
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        List<AppPermission> storedServices = appDatabase.appPermissionDao().getServices(packageName);
        for (AppPermission storedService : storedServices) {
            serviceNameSet.add(storedService.permissionName);
        }

        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SERVICES);
            if (packageInfo != null) {
                android.content.pm.ServiceInfo[] services = packageInfo.services;
                if (services != null) {
                    for (android.content.pm.ServiceInfo serviceInfo : services) {
                        serviceNameSet.add(serviceInfo.name);
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        return serviceNameSet;
    }

    public static List<IComponentInfo> getServices(String packageName, String searchText) {
        Set<String> serviceNames = getServiceNames(packageName);
        if (searchText != null && !searchText.isEmpty()) {
            return serviceNames.stream()
                    .map(serviceName -> new ServiceInfo(packageName, serviceName))
                    .filter(info -> info.getName().toLowerCase().contains(searchText.toLowerCase()) || info.getPackageName().toLowerCase().contains(searchText.toLowerCase()))
                    .sorted(SERVICE_COMPARATOR)
                    .collect(Collectors.toList());
        } else {
            return serviceNames.stream()
                    .map(serviceName -> new ServiceInfo(packageName, serviceName))
                    .sorted(SERVICE_COMPARATOR)
                    .collect(Collectors.toList());
        }
    }

    public static List<IComponentInfo> getReceivers(String packageName, String searchText) {
        Set<ReceiverInfo> receiverNames = new HashSet<>();
        PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();

        // Disabled services won't be appear in the package manager anymore
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        List<AppPermission> storedReceivers = appDatabase.appPermissionDao().getReceivers(packageName);
        for (AppPermission storedReceiver : storedReceivers) {
            StringTokenizer tokenizer = new StringTokenizer(storedReceiver.permissionName, "|");
            String name = tokenizer.nextToken();
            String permission = tokenizer.nextToken();
            receiverNames.add(new ReceiverInfo(packageName, name, permission));
        }

        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_RECEIVERS);
            if (packageInfo != null) {
                android.content.pm.ActivityInfo[] receivers = packageInfo.receivers;
                if (receivers != null) {
                    for (android.content.pm.ActivityInfo activityInfo : receivers) {
                        receiverNames.add(new ReceiverInfo(packageName, activityInfo.name, activityInfo.permission));
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        if (searchText != null && !searchText.isEmpty()) {
            return receiverNames.stream()
                    .filter(info -> info.getName().toLowerCase().contains(searchText.toLowerCase()) || info.getPackageName().toLowerCase().contains(searchText.toLowerCase()))
                    .sorted(RECEIVER_COMPARATOR)
                    .collect(Collectors.toList());
        } else {
            return receiverNames.stream()
                    .sorted(RECEIVER_COMPARATOR)
                    .collect(Collectors.toList());
        }
    }

    public static Set<String> getReceiverNames(String packageName) {
        List<IComponentInfo> componentInfoList = getReceivers(packageName, "");
        return componentInfoList.stream()
                .map(receiverName -> ((android.content.pm.ActivityInfo) receiverName).name)
                .collect(Collectors.toSet());
    }

    public static Set<String> getActivityNames(String packageName) {
        Set<String> activityNameSet = new HashSet<>();
        PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();

        // Disabled activities won't be appear in the package manager anymore
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        List<AppPermission> storedActivities = appDatabase.appPermissionDao().getActivities(packageName);
        for (AppPermission storedActivity : storedActivities) {
            activityNameSet.add(storedActivity.permissionName);
        }

        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            if (packageInfo != null) {
                android.content.pm.ActivityInfo[] activities = packageInfo.activities;
                if (activities != null) {
                    for (android.content.pm.ActivityInfo activityInfo : activities) {
                        activityNameSet.add(activityInfo.name);
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        return activityNameSet;
    }

    public static List<IComponentInfo> getActivities(String packageName, String searchText) {
        Set<String> activityNames = getActivityNames(packageName);
        if (searchText != null && !searchText.isEmpty()) {
            return activityNames.stream()
                    .map(activityName -> new ActivityInfo(packageName, activityName))
                    .filter(info -> info.getName().toLowerCase().contains(searchText.toLowerCase()) || info.getPackageName().toLowerCase().contains(searchText.toLowerCase()))
                    .sorted(ACTIVITY_COMPARATOR)
                    .collect(Collectors.toList());
        } else {
            return activityNames.stream()
                    .map(activityName -> new ActivityInfo(packageName, activityName))
                    .sorted(ACTIVITY_COMPARATOR)
                    .collect(Collectors.toList());
        }
    }

    public static Set<String> getProviderNames(String packageName) {
        Set<String> providerNameSet = new HashSet<>();
        PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();

        // Disabled provider won't be appear in the package manager anymore
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        List<AppPermission> storedProviders = appDatabase.appPermissionDao().getContentProviders(packageName);
        for (AppPermission storedProvider : storedProviders) {
            providerNameSet.add(storedProvider.permissionName);
        }

        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PROVIDERS);
            if (packageInfo != null) {
                android.content.pm.ProviderInfo[] providers = packageInfo.providers;
                if (providers != null) {
                    for (android.content.pm.ProviderInfo providerInfo : providers) {
                        providerNameSet.add(providerInfo.name);
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        return providerNameSet;
    }

    public static List<IComponentInfo> getProviders(String packageName, String searchText) {
        Set<String> providerNames = getProviderNames(packageName);
        if (searchText != null && !searchText.isEmpty()) {
            return providerNames.stream()
                    .map(providerName -> new ProviderInfo(packageName, providerName))
                    .filter(info -> info.getName().toLowerCase().contains(searchText.toLowerCase()) || info.getPackageName().toLowerCase().contains(searchText.toLowerCase()))
                    .sorted(PROVIDER_COMPARATOR)
                    .collect(Collectors.toList());
        } else {
            return providerNames.stream()
                    .map(providerName -> new ProviderInfo(packageName, providerName))
                    .sorted(PROVIDER_COMPARATOR)
                    .collect(Collectors.toList());
        }
    }
}