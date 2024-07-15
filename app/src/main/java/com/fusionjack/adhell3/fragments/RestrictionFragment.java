package com.fusionjack.adhell3.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.LogUtils;
import com.samsung.android.knox.restriction.RestrictionPolicy;

import java.util.function.Function;
import java.util.function.Supplier;

public class RestrictionFragment extends PreferenceFragmentCompat {

    private final RestrictionPolicy policy;
    private SharedPreferences preferences;

    // Google
    private static final String GOOGLE_SYNC_PREFERENCE = "google_sync_preference";
    private static final String GOOGLE_CRASH_PREFERENCE = "google_crash_preference";
    private static final String GOOGLE_BACKUP_PREFERENCE = "google_backup_preference";
    private static final String NON_MARKET_PREFERENCE = "non_market_app_preference";

    // Hardware
    private static final String CAMERA_PREFERENCE = "camera_preference";
    private static final String MICROPHONE_PREFERENCE = "microphone_preference";
    private static final String AUDIO_RECORD_PREFERENCE = "audio_record_preference";
    private static final String VIDEO_RECORD_PREFERENCE = "video_record_preference";
    private static final String USB_HOST_PREFERENCE = "usb_host_preference";

    // Connections
    private static  final String AIRPLANE_MODE_PREFERENCE = "airplane_mode_preference";
    private static final String MOBILE_DATA_PREFERENCE = "mobile_data_preference";
    private static final String WIFI_ACCESS_PREFERENCE = "wifi_access_preference";
    private static final String BLUETOOTH_PREFERENCE = "bluetooth_preference";
    private static final String TETHERING_PREFERENCE = "tethering_preference";

    // Miscellaneous
    private static final String CLIPBOARD_PREFERENCE = "clipboard_preference";
    private static final String CLIPBOARD_SHARE_PREFERENCE = "clipboard_share_preference";
    private static final String FAST_ENCRYPTION_PREFERENCE = "fast_encryption";
    private static final String POWER_OFF_PREFERENCE = "poweroff_preference";
    private static final String SCREEN_CAPTURE_PREFERENCE = "screen_capture_preference";
    private static final String SETTINGS_CHANGE_PREFERENCE = "settings_change_preference";
    private static final String SYSTEM_UPDATES_PREFERENCE = "system_update_preference";
    private static final String WALLPAPER_CHANGE_PREFERENCE = "wallpaper_change_preference";

    public RestrictionFragment() {
        this.policy = AdhellFactory.getInstance().getRestrictionPolicy();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        this.preferences = getPreferenceManager().getSharedPreferences();
        setPreferencesFromResource(R.xml.restrictions_preference, rootKey);

        // Google
        init(GOOGLE_SYNC_PREFERENCE, policy::isGoogleAccountsAutoSyncAllowed);
        init(GOOGLE_CRASH_PREFERENCE, policy::isGoogleCrashReportAllowed);
        init2(GOOGLE_BACKUP_PREFERENCE, policy::isBackupAllowed);
        init(NON_MARKET_PREFERENCE, policy::isNonMarketAppAllowed);

        // Hardware
        init2(CAMERA_PREFERENCE, policy::isCameraEnabled);
        init2(MICROPHONE_PREFERENCE, policy::isMicrophoneEnabled);
        init(AUDIO_RECORD_PREFERENCE, policy::isAudioRecordAllowed);
        init(VIDEO_RECORD_PREFERENCE, policy::isVideoRecordAllowed);
        init(USB_HOST_PREFERENCE, policy::isUsbHostStorageAllowed);

        // Connections
        init(AIRPLANE_MODE_PREFERENCE, policy::isAirplaneModeAllowed);
        init(MOBILE_DATA_PREFERENCE, policy::isCellularDataAllowed);
        init2(WIFI_ACCESS_PREFERENCE, policy::isWiFiEnabled);
        init2(BLUETOOTH_PREFERENCE, policy::isBluetoothEnabled);
        init(TETHERING_PREFERENCE, policy::isTetheringEnabled);

        // Miscellaneous
        init2(CLIPBOARD_PREFERENCE, policy::isClipboardAllowed);
        init(CLIPBOARD_SHARE_PREFERENCE, policy::isClipboardShareAllowed);
        init2(FAST_ENCRYPTION_PREFERENCE, policy::isFastEncryptionAllowed);
        init(POWER_OFF_PREFERENCE, policy::isPowerOffAllowed);
        init2(SCREEN_CAPTURE_PREFERENCE, policy::isScreenCaptureEnabled);
        init2(SETTINGS_CHANGE_PREFERENCE, policy::isSettingsChangesAllowed);
        init(SYSTEM_UPDATES_PREFERENCE, policy::isOTAUpgradeAllowed);
        init(WALLPAPER_CHANGE_PREFERENCE, policy::isWallpaperChangeAllowed);

    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        switch (preference.getKey()) {
            case GOOGLE_SYNC_PREFERENCE:
                execute(GOOGLE_SYNC_PREFERENCE, policy::allowGoogleAccountsAutoSync);
                break;

            case GOOGLE_CRASH_PREFERENCE:
                execute(GOOGLE_CRASH_PREFERENCE, policy::allowGoogleCrashReport);
                break;

            case GOOGLE_BACKUP_PREFERENCE:
                execute(GOOGLE_BACKUP_PREFERENCE, policy::setBackup);
                break;

            case NON_MARKET_PREFERENCE:
                execute(NON_MARKET_PREFERENCE, policy::setAllowNonMarketApps);
                break;

            case CAMERA_PREFERENCE:
                execute(CAMERA_PREFERENCE, policy::setCameraState);
                break;

            case MICROPHONE_PREFERENCE:
                execute(MICROPHONE_PREFERENCE, policy::setMicrophoneState);
                break;

            case AUDIO_RECORD_PREFERENCE:
                execute(AUDIO_RECORD_PREFERENCE, policy::allowAudioRecord);
                break;

            case VIDEO_RECORD_PREFERENCE:
                execute(VIDEO_RECORD_PREFERENCE, policy::allowVideoRecord);
                break;

            case USB_HOST_PREFERENCE:
                execute(USB_HOST_PREFERENCE, policy::allowUsbHostStorage);
                break;

            case AIRPLANE_MODE_PREFERENCE:
                execute(AIRPLANE_MODE_PREFERENCE, policy::allowAirplaneMode);
                break;

            case MOBILE_DATA_PREFERENCE:
                execute(MOBILE_DATA_PREFERENCE, policy::setCellularData);
                break;

            case WIFI_ACCESS_PREFERENCE:
                execute(WIFI_ACCESS_PREFERENCE, policy::allowWiFi);
                break;

            case BLUETOOTH_PREFERENCE:
                execute(BLUETOOTH_PREFERENCE, policy::allowBluetooth);
                break;

            case TETHERING_PREFERENCE:
                execute(TETHERING_PREFERENCE, policy::setTethering);
                break;

            case CLIPBOARD_PREFERENCE:
                execute(CLIPBOARD_PREFERENCE, policy::setClipboardEnabled);
                break;

            case CLIPBOARD_SHARE_PREFERENCE:
                execute(CLIPBOARD_SHARE_PREFERENCE, policy::allowClipboardShare);
                break;

            case FAST_ENCRYPTION_PREFERENCE:
                execute(FAST_ENCRYPTION_PREFERENCE, policy::allowFastEncryption);
                break;

            case POWER_OFF_PREFERENCE:
                execute(POWER_OFF_PREFERENCE, policy::allowPowerOff);

            case SCREEN_CAPTURE_PREFERENCE:
                execute(SCREEN_CAPTURE_PREFERENCE, policy::setScreenCapture);
                break;

            case SETTINGS_CHANGE_PREFERENCE:
                execute(SETTINGS_CHANGE_PREFERENCE, policy::allowSettingsChanges);
                break;

            case SYSTEM_UPDATES_PREFERENCE:
                execute(SYSTEM_UPDATES_PREFERENCE, policy::allowOTAUpgrade);
                break;

            case WALLPAPER_CHANGE_PREFERENCE:
                execute(WALLPAPER_CHANGE_PREFERENCE, policy::allowWallpaperChange);
                break;
        }
        return super.onPreferenceTreeClick(preference);
    }

    private void init(String preferenceName, Supplier<Boolean> func) {
        SwitchPreference switchPreference = (SwitchPreference) findPreference(preferenceName);
        switchPreference.setChecked(func.get());
    }

    private void init2(String preferenceName, Function<Boolean, Boolean> func) {
        SwitchPreference switchPreference = (SwitchPreference) findPreference(preferenceName);
        switchPreference.setChecked(func.apply(false));
    }

    private void execute(String preferenceName, Function<Boolean, Boolean> func) {
        boolean isAllowed = preferences.getBoolean(preferenceName, true);
        LogUtils.info(preferenceName + " is " + (isAllowed ? "enabled" : "disabled"));
        boolean success = func.apply(isAllowed);
        if (!success) {
            Toast.makeText(getContext(), "Failed to change " + preferenceName, Toast.LENGTH_LONG).show();
        }
    }

}
