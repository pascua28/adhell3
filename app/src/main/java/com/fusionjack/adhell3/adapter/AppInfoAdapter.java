package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;

import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.databinding.DialogQuestionBinding;
import com.fusionjack.adhell3.databinding.ItemAppInfoBinding;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppCache;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.google.android.material.snackbar.Snackbar;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppInfoAdapter extends BaseAdapter {

    public static final String RUNNING_TAG = "@@@RUNNING@@@";
    private final List<AppInfo> applicationInfoList;
    private final WeakReference<Context> contextReference;
    private final AppRepository.Type appType;
    private final ApplicationPolicy appPolicy;
    private final Map<String, Drawable> appIcons;
    private final Map<String, String> versionNames;

    public AppInfoAdapter(List<AppInfo> appInfoList, AppRepository.Type appType, Context context) {
        this.applicationInfoList = appInfoList;
        this.contextReference = new WeakReference<>(context);
        this.appType = appType;
        this.appPolicy = AdhellFactory.getInstance().getAppPolicy();

        //CompletableObserver observer = AppCacheDialog.createObserver(context, this);
        AppCache appCache = AppCache.getInstance(null);
        this.appIcons = appCache.getIcons();
        this.versionNames = appCache.getVersionNames();
    }

    @Override
    public int getCount() {
        return this.applicationInfoList.size();
    }

    @Override
    public AppInfo getItem(int position) {
        return this.applicationInfoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        boolean isAppRunning = false;
        String appName;
        Context context = contextReference.get();
        AppInfoViewHolder holder;
        if (convertView == null) {
            ItemAppInfoBinding itemBinding = ItemAppInfoBinding.inflate(LayoutInflater.from(parent.getContext()));

            holder = new AppInfoViewHolder(itemBinding);
            holder.view = itemBinding.getRoot();
            holder.view.setTag(holder);
        } else {
            holder = (AppInfoViewHolder) convertView.getTag();
        }

        AppInfo appInfo = applicationInfoList.get(position);
        appName = appInfo.appName;
        if (appInfo.appName.contains(RUNNING_TAG)) {
            isAppRunning = true;
            appName = appInfo.appName.replace(RUNNING_TAG, "");
        }
        holder.binding.appName.setText(appName);
        holder.binding.appName.setTextColor(context.getResources().getColor(R.color.colorText, context.getTheme()));
        holder.binding.packName.setText(appInfo.packageName);
        holder.binding.stopButton.setVisibility(View.GONE);
        boolean checked = false;
        switch (appType) {
            case DISABLER:
                boolean enabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
                if (enabled) {
                    checked = !appInfo.disabled;
                } else {
                    checked = true;
                }
                holder.binding.switchDisable.setEnabled(enabled);
                break;
            case MOBILE_RESTRICTED:
                checked = !appInfo.mobileRestricted;
                break;
            case WIFI_RESTRICTED:
                checked = !appInfo.wifiRestricted;
                break;
            case WHITELISTED:
                checked = appInfo.adhellWhitelisted;
                break;
            case COMPONENT:
                holder.binding.switchDisable.setVisibility(View.GONE);
            case DNS:
                boolean isDnsNotEmpty = AppPreferences.getInstance().isDnsNotEmpty();
                holder.binding.switchDisable.setEnabled(isDnsNotEmpty);
                checked = appInfo.hasCustomDns;
                break;
        }
        holder.binding.switchDisable.setChecked(checked);

        if (isAppRunning) {
            holder.binding.appName.setTextColor(context.getResources().getColor(R.color.colorAccent, context.getTheme()));
            holder.binding.stopButton.setVisibility(View.VISIBLE);
                String finalAppName = appName;
            holder.binding.stopButton.setOnClickListener(v -> {
                DialogQuestionBinding dialogQuestionBinding = DialogQuestionBinding.inflate(LayoutInflater.from(context));
                dialogQuestionBinding.titleTextView.setText(context.getResources().getString(R.string.stop_app_dialog_title));
                dialogQuestionBinding.questionTextView.setText(String.format(context.getResources().getString(R.string.stop_app_dialog_text), finalAppName));

                AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setView(dialogQuestionBinding.getRoot())
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            try {
                                appPolicy.stopApp(appInfo.packageName);
                                if (context instanceof MainActivity) {
                                    MainActivity mainActivity = (MainActivity) context;
                                    mainActivity.makeSnackbar(String.format(Locale.getDefault(), context.getResources().getString(R.string.stopped_app), finalAppName), Snackbar.LENGTH_SHORT)
                                            .show();
                                }
                                holder.binding.appName.setTextColor(context.getResources().getColor(R.color.colorText, context.getTheme()));
                                holder.binding.stopButton.setVisibility(View.GONE);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create();

                    alertDialog.show();
                });
        }

        String info = appInfo.system ? "System" : "User";
        String versionName = versionNames.get(appInfo.packageName);
        holder.binding.systemOrNot.setText(String.format("%s (%s)", info, versionName == null ? "0.0.0" : versionName));

        Drawable icon = appIcons.get(appInfo.packageName);
        if (icon == null) {
            icon = ResourcesCompat.getDrawable(context.getResources(), android.R.drawable.sym_def_app_icon, context.getTheme());
        }
        holder.binding.appIcon.setImageDrawable(icon);

        return holder.view;
    }

    private static class AppInfoViewHolder {
        private View view;
        private final ItemAppInfoBinding binding;

        AppInfoViewHolder(ItemAppInfoBinding binding) {
            this.view = binding.getRoot();
            this.binding = binding;
        }
    }
}
