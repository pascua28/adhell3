package com.fusionjack.adhell3.model;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPermissionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class AppComponent {

    private static final Comparator<IComponentInfo> APP_COMPONENT_COMPARATOR = Comparator.comparing(IComponentInfo::getName);

    public static List<IComponentInfo> combinePermissionsList(String packageName, List<AppPermission> appComponentList) {
        return getPermissions(packageName);
    }

    public static List<IComponentInfo> combineActivitiesList(String packageName, List<AppPermission> appComponentList) {
        Set<IComponentInfo> combinedList = new TreeSet<>(APP_COMPONENT_COMPARATOR);
        combinedList.addAll(appComponentList.stream()
                .map(appComponent -> new com.fusionjack.adhell3.model.ActivityInfo(packageName, appComponent.permissionName))
                .collect(Collectors.toList()));
        combinedList.addAll(getActivitiesFromSystem(packageName).stream()
                .map(serviceName -> new com.fusionjack.adhell3.model.ActivityInfo(packageName, serviceName))
                .collect(Collectors.toList()));
        return new ArrayList<>(combinedList);
    }

    public static List<IComponentInfo> combineServicesList(String packageName, List<AppPermission> appComponentList) {
        Set<IComponentInfo> combinedList = new TreeSet<>(APP_COMPONENT_COMPARATOR);
        combinedList.addAll(appComponentList.stream()
                .map(appComponent -> new ServiceInfo(packageName, appComponent.permissionName))
                .collect(Collectors.toList()));
        combinedList.addAll(getServicesFromSystem(packageName).stream()
                .map(serviceName -> new ServiceInfo(packageName, serviceName))
                .collect(Collectors.toList()));
        return new ArrayList<>(combinedList);
    }

    public static List<IComponentInfo> combineReceiversList(String packageName, List<AppPermission> appComponentList) {
        Set<IComponentInfo> combinedList = new TreeSet<>(APP_COMPONENT_COMPARATOR);
        combinedList.addAll(appComponentList.stream()
                .map(appComponent -> {
                    StringTokenizer tokenizer = new StringTokenizer(appComponent.permissionName, "|");
                    String name = tokenizer.nextToken();
                    String permission = tokenizer.nextToken();
                    return new ReceiverInfo(packageName, name, permission);
                })
                .collect(Collectors.toList()));
        combinedList.addAll(getReceiversFromSystem(packageName));
        return new ArrayList<>(combinedList);
    }

    public static List<IComponentInfo> getPermissions(String packageName) {
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
                            .filter(info -> !info.equals(PermissionInfo.EMPTY_PERMISSION))
                            .sorted(APP_COMPONENT_COMPARATOR)
                            .collect(Collectors.toList());
                }
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        return permissionList;
    }

    public static Set<String> getActivities(String packageName) {
        Set<String> activityNameSet = new TreeSet<>();

        // Disabled activities won't be appear in the package manager anymore
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        List<AppPermission> storedActivities = appDatabase.appPermissionDao().getActivities(packageName);
        for (AppPermission storedActivity : storedActivities) {
            activityNameSet.add(storedActivity.permissionName);
        }

        activityNameSet.addAll(getActivitiesFromSystem(packageName));

        return activityNameSet;
    }

    public static Set<String> getServices(String packageName) {
        Set<String> serviceNameSet = new TreeSet<>();

        // Disabled services won't be appear in the package manager anymore
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        List<AppPermission> storedServices = appDatabase.appPermissionDao().getServices(packageName);
        for (AppPermission storedService : storedServices) {
            serviceNameSet.add(storedService.permissionName);
        }

        serviceNameSet.addAll(getServicesFromSystem(packageName));

        return serviceNameSet;
    }

    public static List<IComponentInfo> getReceivers(String packageName) {
        Set<IComponentInfo> receiverNames = new TreeSet<>(APP_COMPONENT_COMPARATOR);

        // Disabled services won't be appear in the package manager anymore
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        List<AppPermission> storedReceivers = appDatabase.appPermissionDao().getReceivers(packageName);
        for (AppPermission storedReceiver : storedReceivers) {
            StringTokenizer tokenizer = new StringTokenizer(storedReceiver.permissionName, "|");
            String name = tokenizer.nextToken();
            String permission = tokenizer.nextToken();
            receiverNames.add(new ReceiverInfo(packageName, name, permission));
        }

        receiverNames.addAll(getReceiversFromSystem(packageName));

        return new ArrayList<>(receiverNames);
    }

    private static List<String> getActivitiesFromSystem(String packageName) {
        List<String> list = new ArrayList<>();
        try {
            PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            if (packageInfo != null) {
                Optional.ofNullable(packageInfo.activities).ifPresent(activities -> {
                    Arrays.stream(activities).forEach(activityInfo -> list.add(activityInfo.name));
                });
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return list;
    }

    private static List<String> getServicesFromSystem(String packageName) {
        List<String> list = new ArrayList<>();
        try {
            PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SERVICES);
            if (packageInfo != null) {
                android.content.pm.ServiceInfo[] services = packageInfo.services;
                if (services != null) {
                    for (android.content.pm.ServiceInfo serviceInfo : services) {
                        list.add(serviceInfo.name);
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return list;
    }

    private static List<IComponentInfo> getReceiversFromSystem(String packageName) {
        List<IComponentInfo> list = new ArrayList<>();
        try {
            PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_RECEIVERS);
            if (packageInfo != null) {
                ActivityInfo[] receivers = packageInfo.receivers;
                if (receivers != null) {
                    for (ActivityInfo activityInfo : receivers) {
                        list.add(new ReceiverInfo(packageName, activityInfo.name, activityInfo.permission));
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return list;
    }

}
