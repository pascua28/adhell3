package com.fusionjack.adhell3.dialogfragment;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.model.CustomSwitchPreference;
import com.fusionjack.adhell3.tasks.AutoUpdateWorker;
import com.fusionjack.adhell3.utils.AppPreferences;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AutoUpdateDialogFragment extends DialogFragment {
    public static final String AUTO_UPDATE_WORK_TAG = "adhell_auto_update";
    public static final int[] intervalArray = new int[] {1,2,3,4,5,6,7,14,21,28};
    private final CustomSwitchPreference customSwitchPreference;
    private final WorkManager workManager;
    private Switch globalSwitch;
    private TextView seekLabelTextView;
    private SeekBar intervalSeekBar;
    private CheckBox appComponentsCheckBox;
    private CheckBox cleanDBCheckBox;
    private CheckBox logCheckBox;
    private CheckBox lowBatteryCheckBox;
    private CheckBox mobileDataCheckBox;
    private TimePicker startTimePicker;

    public AutoUpdateDialogFragment(Preference preference) {
        this.customSwitchPreference = (CustomSwitchPreference) preference;
        this.workManager = WorkManager.getInstance(App.getAppContext());
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.9);
            if (dialog.getWindow() != null)
                dialog.getWindow().setLayout(width, height);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return inflater.inflate(R.layout.dialog_fragment_auto_update, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        globalSwitch = view.findViewById(R.id.globalSwitch);
        globalSwitch.setChecked(customSwitchPreference.isChecked());
        intervalSeekBar = view.findViewById(R.id.intervalSeekBar);
        intervalSeekBar.setMax(intervalArray.length-1);
        intervalSeekBar.setProgress(AppPreferences.getInstance().getAutoUpdateInterval());
        intervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekLabelTextView.setText(getSeekBarText(getValueFromSeekBar(progress)));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar){}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar){}
        });
        seekLabelTextView = view.findViewById(R.id.seekLabelTextView);
        seekLabelTextView.setText(getSeekBarText(getValueFromSeekBar(intervalSeekBar.getProgress())));
        appComponentsCheckBox = view.findViewById(R.id.appComponentsCheckBox);
        appComponentsCheckBox.setChecked(AppPreferences.getInstance().getAppComponentsAutoUpdate());
        cleanDBCheckBox = view.findViewById(R.id.cleanDBCheckBox);
        cleanDBCheckBox.setChecked(AppPreferences.getInstance().getCleanDatabaseAutoUpdate());
        logCheckBox = view.findViewById(R.id.logCheckBox);
        logCheckBox.setChecked(AppPreferences.getInstance().getCreateLogOnAutoUpdate());
        lowBatteryCheckBox = view.findViewById(R.id.lowBatteryCheckBox);
        lowBatteryCheckBox.setChecked(AppPreferences.getInstance().getAutoUpdateConstraintLowBattery());
        mobileDataCheckBox = view.findViewById(R.id.mobileDataCheckBox);
        mobileDataCheckBox.setChecked(AppPreferences.getInstance().getAutoUpdateConstraintMobileData());
        startTimePicker = view.findViewById(R.id.startTimePicker);
        startTimePicker.setIs24HourView(true);
        startTimePicker.setHour(AppPreferences.getInstance().getStartHourAutoUpdate());
        startTimePicker.setMinute(AppPreferences.getInstance().getStartMinuteAutoUpdate());
        Button saveButton = view.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            saveAutoUpdateSettings();
            dismiss();
        });
    }

    public static Constraints getAutoUpdateConstraints() {
        Constraints.Builder workerConstraints = new Constraints.Builder();
        if (AppPreferences.getInstance().getAutoUpdateConstraintMobileData()) {
            workerConstraints.setRequiredNetworkType(NetworkType.CONNECTED);
        } else {
            workerConstraints.setRequiredNetworkType(NetworkType.UNMETERED);
        }
        workerConstraints.setRequiresBatteryNotLow(!AppPreferences.getInstance().getAutoUpdateConstraintLowBattery());
        workerConstraints.setRequiresCharging(false);
        workerConstraints.setRequiresDeviceIdle(false);
        workerConstraints.setRequiresStorageNotLow(false);

        return workerConstraints.build();
    }

    private void enqueuePeriodicWork() {
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(AutoUpdateWorker.class)
                .setConstraints(getAutoUpdateConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .setInitialDelay(
                        getInitialDelayForFirstScheduleWork(
                            AppPreferences.getInstance().getStartHourAutoUpdate(),
                            AppPreferences.getInstance().getStartMinuteAutoUpdate()),
                        TimeUnit.MILLISECONDS
                )
                .build();
        cancelPeriodicWork();
        workManager.enqueueUniqueWork(AUTO_UPDATE_WORK_TAG, ExistingWorkPolicy.REPLACE , workRequest);
    }

    private void cancelPeriodicWork() {
        workManager.cancelUniqueWork(AUTO_UPDATE_WORK_TAG);
    }

    private void saveAutoUpdateSettings() {
        AppPreferences.getInstance().setAutoUpdateInterval(intervalSeekBar.getProgress());
        AppPreferences.getInstance().setAppComponentsAutoUpdate(appComponentsCheckBox.isChecked());
        AppPreferences.getInstance().setCleanDatabaseAutoUpdate(cleanDBCheckBox.isChecked());
        AppPreferences.getInstance().setCreateLogOnAutoUpdate(logCheckBox.isChecked());
        AppPreferences.getInstance().setAutoUpdateConstraintLowBattery(lowBatteryCheckBox.isChecked());
        AppPreferences.getInstance().setAutoUpdateConstraintMobileData(mobileDataCheckBox.isChecked());
        AppPreferences.getInstance().setStartHourAutoUpdate(startTimePicker.getHour());
        AppPreferences.getInstance().setStartMinuteAutoUpdate(startTimePicker.getMinute());
        customSwitchPreference.setChecked(globalSwitch.isChecked());

        if (globalSwitch.isChecked()) {
            enqueuePeriodicWork();
            Toast.makeText(getContext(), "Auto update enabled", Toast.LENGTH_LONG).show();
        } else {
            cancelPeriodicWork();
            Toast.makeText(getContext(), "Auto update disabled", Toast.LENGTH_LONG).show();
        }
    }

    private long getInitialDelayForFirstScheduleWork(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        long nowMillis = calendar.getTimeInMillis();

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (nowMillis > calendar.getTimeInMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return calendar.getTimeInMillis() - nowMillis;
    }

    private String getSeekBarText(int value) {
        return (value < 168) ?
                    ((value/24) == 1) ?
                        String.format(Locale.getDefault(), "Interval: %d day", value)
                        : String.format(Locale.getDefault(), "Interval: %d days", value)
                    : ((value/168) == 1) ?
                        String.format(Locale.getDefault(), "Interval: %d week", value/7)
                        : String.format(Locale.getDefault(), "Interval: %d weeks", value/7);
    }

    private int getValueFromSeekBar(int progress) {
        return intervalArray[progress];
    }
}
