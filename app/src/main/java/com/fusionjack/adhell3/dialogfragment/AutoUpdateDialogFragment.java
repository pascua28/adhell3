package com.fusionjack.adhell3.dialogfragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.model.CustomSwitchPreference;
import com.fusionjack.adhell3.tasks.AppComponentsUpdateWorker;
import com.fusionjack.adhell3.tasks.ReScheduleUpdateWorker;
import com.fusionjack.adhell3.tasks.RulesUpdateWorker;
import com.fusionjack.adhell3.utils.AppComponentFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AutoUpdateDialogFragment extends DialogFragment {
    public static int MAX_RETRY = 5;
    public static final int[] intervalArray = new int[] {1,2,3,4,5,6,7,14,21,28};
    private final CustomSwitchPreference customSwitchPreference;
    private static WorkManager workManager;
    private Switch globalSwitch;
    private TextView seekLabelTextView;
    private SeekBar intervalSeekBar;
    private CheckBox appComponentsCheckBox;
    private CheckBox logCheckBox;
    private CheckBox lowBatteryCheckBox;
    private CheckBox mobileDataCheckBox;
    private TimePicker startTimePicker;

    public AutoUpdateDialogFragment(Preference preference) {
        this.customSwitchPreference = (CustomSwitchPreference) preference;
        workManager = WorkManager.getInstance(App.getAppContext());
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
            getDialog().getWindow().getAttributes().windowAnimations = R.style.FragmentDialogAnimation;
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

    public static void migrateOldAutoUpdateJob(Activity activity) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean autoUpdateEnabled = sharedPref.getBoolean("auto_update_preference", false);
        if (autoUpdateEnabled && !AppPreferences.getInstance().getAutoUpdateMigrated()) {
            enqueueAutoUpdateWork(WorkManager.getInstance(App.getAppContext()));
            AppPreferences.getInstance().setAutoUpdateMigrated(true);
        }
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

    public static void enqueueNextAutoUpdateWork() {
        OneTimeWorkRequest reScheduleWorkRequest = new OneTimeWorkRequest.Builder(ReScheduleUpdateWorker.class)
                .setConstraints(AutoUpdateDialogFragment.getAutoUpdateConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build();

        // Enqueue new job with readjusting initial delay
        WorkManager.getInstance(App.getAppContext()).enqueue(reScheduleWorkRequest);
    }

    private static void enqueueAutoUpdateWork(WorkManager workManager) {
        OneTimeWorkRequest rulesWorkRequest = new OneTimeWorkRequest.Builder(RulesUpdateWorker.class)
                .setConstraints(AutoUpdateDialogFragment.getAutoUpdateConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .setInitialDelay(
                        getInitialDelayForFirstScheduleWork(
                                AppPreferences.getInstance().getStartHourAutoUpdate(),
                                AppPreferences.getInstance().getStartMinuteAutoUpdate()),
                        TimeUnit.MILLISECONDS)
                .build();

        OneTimeWorkRequest appComponentsWorkRequest = new OneTimeWorkRequest.Builder(AppComponentsUpdateWorker.class)
                .setConstraints(AutoUpdateDialogFragment.getAutoUpdateConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build();

        OneTimeWorkRequest reScheduleWorkRequest = new OneTimeWorkRequest.Builder(ReScheduleUpdateWorker.class)
                .setConstraints(AutoUpdateDialogFragment.getAutoUpdateConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build();

        // Cancel previous job
        cancelAllWork(workManager);

        // Enqueue new job with readjusting initial delay
        workManager.beginWith(rulesWorkRequest)
                .then(appComponentsWorkRequest)
                .then(reScheduleWorkRequest)
                .enqueue();
    }

    private static void cancelAllWork(WorkManager workManager) {
        workManager.cancelAllWork();
    }

    private void saveAutoUpdateSettings() {
        if (appComponentsCheckBox.isChecked()) {
            if (getView() != null) {
                AppComponentFactory.getInstance().checkMigrateOldBatchFiles(getContext(), getView());
            }
        }

        AppPreferences.getInstance().setAutoUpdateInterval(intervalSeekBar.getProgress());
        AppPreferences.getInstance().setAppComponentsAutoUpdate(appComponentsCheckBox.isChecked());
        AppPreferences.getInstance().setCreateLogOnAutoUpdate(logCheckBox.isChecked());
        AppPreferences.getInstance().setAutoUpdateConstraintLowBattery(lowBatteryCheckBox.isChecked());
        AppPreferences.getInstance().setAutoUpdateConstraintMobileData(mobileDataCheckBox.isChecked());
        AppPreferences.getInstance().setStartHourAutoUpdate(startTimePicker.getHour());
        AppPreferences.getInstance().setStartMinuteAutoUpdate(startTimePicker.getMinute());
        customSwitchPreference.setChecked(globalSwitch.isChecked());

        if (globalSwitch.isChecked()) {
            enqueueAutoUpdateWork(workManager);
            if (getActivity() != null && getActivity().findViewById(R.id.bottomBar) != null) {
                MainActivity.makeSnackbar("Auto update enabled", Snackbar.LENGTH_LONG)
                        .show();
            }
        } else {
            cancelAllWork(workManager);
            if (getActivity() != null && getActivity().findViewById(R.id.bottomBar) != null) {
                MainActivity.makeSnackbar("Auto update disabled", Snackbar.LENGTH_LONG)
                        .show();
            }
        }
        AppPreferences.getInstance().setAutoUpdateMigrated(true);
    }

    private static long getInitialDelayForFirstScheduleWork(int hour, int minute) {
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
