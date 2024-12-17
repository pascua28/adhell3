# Device Owner

# Introduction (**READ FIRST**)
## What is Device Owner and how is it related to Adhell3?
Device Owner is a special Android permission designed for enterprise use that enables advanced policy management for work devices.

Due to [changes Samsung introduces in Android 15 and later](https://www.samsungknox.com/en/blog/changes-to-knox-sdk) some Adhell3's features will not function without Device Owner permission.

## Features restricted in Adhell3 without Device Owner
- **Disabling apps**
- **Managing apps' components**
- **Launching apps' activities**
- **Stopping apps**

## Features affected in Android when Device Owner is granted

As the Device Owner is primarily designed for corporate use, certain limitations are imposed when it is active:
- **Google Backup** is disabled by default (can be enabled in Adhell3->Other->Settings->Enable Google Backup)
- **Samsung Backup and Smart Switch** are not functional
- **Secure Folder or work profile** apps (like [Shelter](https://gitea.angry.im/PeterCxy/Shelter)) does not work
- **Adhell3 Uninstallation** is not allowed without clearing Device Owner first
- **Exclusive Permission**, only one app can have Device Owner at the time

## Requirements for granting Device Owner permission
Due to Android's restrictions, the following conditions must be met before granting Device Owner permission:
- **No accounts on the device**. Ensure that there are no accounts on the device under Settings app->Accounts and backup->Manage accounts
- **No Secure Folder, work profile or other user accounts**
- **No other app with Device Owner permission**

Once Device Owner permission is granted, accounts can be re-added.

**Tip**: If you are setting up new device (or after factory reset), simply skip adding any accounts and configure them after granting the permission.

## Should you grant Device Owner?
- **For Android versions below 15** there is no advantage to granting Device Owner permission.
- **For Android 15 and above** consider whether you need the restricted features of Adhell3 and the limitations imposed by Device Owner permission are acceptable.

# Granting Device Owner permission to Adhell3
**Warning!** If granting Device Owner fails (requirements are not met) admin permission is revoked, therefore it is advised to disable all Adhell3's switches before the procedure.

## Method 1: Using Shizuku (Recommended)

Adhell3 can grant Device Owner to itself using [Shizuku](https://shizuku.rikka.app/). This method does not require any additional devices.

1. Download and install Shizuku using any method from here: https://shizuku.rikka.app/download
2. Start Shizuku via wireless debugging as described here: https://shizuku.rikka.app/guide/setup/#start-via-wireless-debugging
3. Open Adhell3 and navigate to Other->Settings->Grant Device Owner
4. Click on the "Grant" button
5. A popup should appear asking for Shizuku's permission. Click "Allow"
6. After short time a toast message should appear indicating result
7. If unsuccessful a detailed error will be shown on the dialog. Make sure you have [met requirements](#requirements-for-granting-device-owner) and followed the steps correctly.

If successful you may uninstall Shizuku afterward or continue using it with other [awesome apps](https://github.com/timschneeb/awesome-shizuku).

## Method 2: Using ADB (PC required)
Device Owner can be granted manually via ADB command.

1. Set up ADB. Follow the guide by Shizuku (skip the final step for starting Shizuku): https://shizuku.rikka.app/guide/setup/#start-by-connecting-to-a-computer

2. Run command. Use this command scheme:

    `adb shell dpm set-device-owner {package_name}/com.fusionjack.adhell3.receiver.CustomDeviceAdminReceiver`

    Replace `{package_name}` with Adhell3's package name configured in [app.properties](https://gitlab.com/fusionjack/adhell3-scripts#appproperties)

    The command output will indicate result. You can double-check if Adhell3 has Device Owner in Adhell3->Other->Settings->About (Is Device Owner)
