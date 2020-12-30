package com.fusionjack.adhell3.model;

import android.content.pm.ActivityInfo;
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

    private static final Comparator<PermissionInfo> PERMISSION_COMPARATOR = Comparator.comparing(PermissionInfo::getName);
    private static final Comparator<ServiceInfo> SERVICE_COMPARATOR = Comparator.comparing(ServiceInfo::getName);
    private static final Comparator<ReceiverInfo> RECEIVER_COMPARATOR = Comparator.comparing(ReceiverInfo::getName);

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

    public static List<IComponentInfo> getServices(String packageName) {
        Set<String> serviceNames = getServiceNames(packageName);
        return serviceNames.stream()
                .map(serviceName -> new ServiceInfo(packageName, serviceName))
                .sorted(SERVICE_COMPARATOR)
                .collect(Collectors.toList());
    }

    public static List<IComponentInfo> getReceivers(String packageName) {
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
                ActivityInfo[] receivers = packageInfo.receivers;
                if (receivers != null) {
                    for (ActivityInfo activityInfo : receivers) {
                        receiverNames.add(new ReceiverInfo(packageName, activityInfo.name, activityInfo.permission));
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        return receiverNames.stream()
                .sorted(RECEIVER_COMPARATOR)
                .collect(Collectors.toList());
    }
}
