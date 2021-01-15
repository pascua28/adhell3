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
    private static final String MOBILE_DATA_PREFERENCE = "mobile_data_preference";
    private static final String WIFI_ACCESS_PREFERENCE = "wifi_access_preference";
    private static final String BLUETOOTH_PREFERENCE = "bluetooth_preference";
    private static final String TETHERING_PREFERENCE = "tethering_preference";

    // Miscellaneous
    private static final String CLIPBOARD_PREFERENCE = "clipboard_preference";
    private static final String CLIPBOARD_SHARE_PREFERENCE = "clipboard_share_preference";
    private static final String SCREEN_CAPTURE_PREFERENCE = "screen_capture_preference";

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
        init(MOBILE_DATA_PREFERENCE, policy::isCellularDataAllowed);
        init2(WIFI_ACCESS_PREFERENCE, policy::isWiFiEnabled);
        init2(BLUETOOTH_PREFERENCE, policy::isBluetoothEnabled);
        init(TETHERING_PREFERENCE, policy::isTetheringEnabled);

        // Miscellaneous
        init2(CLIPBOARD_PREFERENCE, policy::isClipboardAllowed);
        init(CLIPBOARD_SHARE_PREFERENCE, policy::isClipboardShareAllowed);
        init2(SCREEN_CAPTURE_PREFERENCE, policy::isScreenCaptureEnabled);

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

            case SCREEN_CAPTURE_PREFERENCE:
                execute(SCREEN_CAPTURE_PREFERENCE, policy::setScreenCapture);
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
