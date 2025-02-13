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
- **Lockscreen Message** about organization owned device is displayed
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
4. (Optional but recommended) Check Requirements
    1. Click on the "Check Requirements" Button
    2. A popup should appear asking for Shizuku's permission. Click "Allow"
    3. After a short time, status and details of individual requirements will be shown
    4. If requirements are not met, a button to open accounts setting will be shown. 
    **It is not advised to continue the process if any of the check failed**
5. Click on the "Grant" button
6. A popup should appear asking for Shizuku's permission. Click "Allow" (Not needed if requirements were checked)
7. After short time a toast message should appear indicating result
8. If unsuccessful a detailed error will be shown on the dialog. Make sure you have [met requirements](#requirements-for-granting-device-owner) and followed the steps correctly.

If successful you may uninstall Shizuku afterward or continue using it with other [awesome apps](https://github.com/timschneeb/awesome-shizuku).

## Method 2: Using ADB (PC required)
Device Owner can be granted manually via ADB command.

1. Set up ADB. Follow the guide by Shizuku (skip the final step for starting Shizuku): https://shizuku.rikka.app/guide/setup/#start-by-connecting-to-a-computer

2. Run command. Use this command scheme:

    `adb shell dpm set-device-owner {package_name}/com.fusionjack.adhell3.receiver.CustomDeviceAdminReceiver`

    Replace `{package_name}` with Adhell3's package name configured in [app.properties](https://gitlab.com/fusionjack/adhell3-scripts#appproperties)

    The command output will indicate result. You can double-check if Adhell3 has Device Owner in Adhell3->Other->Settings->About (Is Device Owner)

3. If granting failed you can use these commands to check requirements:
    - `adb shell dumpsys account` - list accounts
    - `adb shell pm list users` - list profiles. Only one (main) profile should be listed
    - `adb shell dpm list-owners` - list owners

# Transfer Device Owner to another app
Device Owner permission can be transferred to another app.

Adhell3 need to have Knox license deactivated.

The target app must support this and have admin activated.

If the app can transfer back, this feature can be used to uninstall Adhell3 without clearing Device Owner.

An example app that support this is [OwnDroid](https://github.com/BinTianqi/OwnDroid). The steps below will apply to it, but can be adapted for others.

1. Install OwnDroid from [its GitHub](https://github.com/BinTianqi/OwnDroid/releases)
2. Open OwnDroid and activate admin for it in Click to activate->Device admin->Activate
3. Open Adhell3, go to Other->Settings->Change license key
4. Click on deactivate button. If it fails make sure to enter the same key that was used in activation
5. License activation dialog will appear. Click on "Transfer Device Owner" button
6. A new dialog will appear. In text box enter `com.bintianqi.owndroid/.Receiver` and click on "Transfer" button
7. A toast message will appear indicating result. If successful OwnDroid will show active Device Owner
8. You can now reinstall Adhell3. After it's activated you can continue to transfer Device Owner back
9. In OwnDroid click on "Activated" then "Transfer Ownership"
10. In text box enter:
    
    `{package_name}/com.fusionjack.adhell3.receiver.CustomDeviceAdminReceiver`

    Replace `{package_name}` with Adhell3's package name configured in [app.properties](https://gitlab.com/fusionjack/adhell3-scripts#appproperties)
11. Click on "Transfer" button
12. A toast message will appear indicating result. You can double-check if Adhell3 has Device Owner in Adhell3->Other->Settings->About (Is Device Owner)
