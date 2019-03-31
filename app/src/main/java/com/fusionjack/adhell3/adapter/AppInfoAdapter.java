package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppCache;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

public class AppInfoAdapter extends BaseAdapter {

    public static final String RUNNING_TAG = "@@@RUNNING@@@";
    private final List<AppInfo> applicationInfoList;
    private final WeakReference<Context> contextReference;
    private final AppRepository.Type appType;
    private final Map<String, Drawable> appIcons;
    private Map<String, String> versionNames;
    private final ApplicationPolicy appPolicy;


    public AppInfoAdapter(List<AppInfo> appInfoList, AppRepository.Type appType, boolean reload, Context context) {
        this.applicationInfoList = appInfoList;
        this.contextReference = new WeakReference<>(context);
        this.appType = appType;
        this.appPolicy = AdhellFactory.getInstance().getAppPolicy();
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                notifyDataSetChanged();
            }
        };
        if (reload) {
            this.appIcons = AppCache.reload(context, handler).getIcons();
        } else {
            this.appIcons = AppCache.getInstance(context, handler).getIcons();
            this.versionNames = AppCache.getInstance(context, handler).getVersionNames();
        }
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
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_app_info, parent, false);
            holder = new ViewHolder();
            holder.nameH = convertView.findViewById(R.id.appName);
            holder.packageH = convertView.findViewById(R.id.packName);
            holder.infoH = convertView.findViewById(R.id.systemOrNot);
            holder.switchH = convertView.findViewById(R.id.switchDisable);
            holder.imageH = convertView.findViewById(R.id.appIcon);
            holder.stopH = convertView.findViewById(R.id.stopButton);
            holder.stopIvH = convertView.findViewById(R.id.stopButtonImageView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AppInfo appInfo = applicationInfoList.get(position);
        appName = appInfo.appName;
        if (appInfo.appName.contains(RUNNING_TAG)) {
            isAppRunning = true;
            appName = appInfo.appName.replace(RUNNING_TAG, "");
        }
        holder.nameH.setText(appName);
        holder.nameH.setTextColor(context.getResources().getColor(R.color.colorText, context.getTheme()));
        holder.packageH.setText(appInfo.packageName);
        holder.stopH.setVisibility(View.GONE);
        boolean checked = false;
        switch (appType) {
            case DISABLER:
                checked = !appInfo.disabled;
                boolean enabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
                holder.switchH.setEnabled(enabled);
                if (isAppRunning) {
                    View finalConvertView = convertView;
                    holder.nameH.setTextColor(context.getResources().getColor(R.color.colorAccent, context.getTheme()));
                    if (!appInfo.disabled) {
                        holder.stopH.setVisibility(View.VISIBLE);
                        String finalAppName = appName;
                        holder.stopH.setOnClickListener(v -> {
                            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_question, parent, false);
                            TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
                            titleTextView.setText(context.getResources().getString(R.string.stop_app_dialog_title));
                            TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
                            questionTextView.setText(String.format(context.getResources().getString(R.string.stop_app_dialog_text), finalAppName));

                            new AlertDialog.Builder(context)
                                    .setView(dialogView)
                                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                                        try {
                                            appPolicy.stopApp(appInfo.packageName);
                                            Snackbar snackBar = Snackbar.make(finalConvertView, String.format(context.getResources().getString(R.string.stopped_app), finalAppName), Snackbar.LENGTH_SHORT);
                                            TextView tv = snackBar.getView().findViewById(android.support.design.R.id.snackbar_text);
                                            tv.setTextColor(Color.WHITE);
                                            snackBar.show();
                                            holder.nameH.setTextColor(context.getResources().getColor(R.color.colorText, context.getTheme()));
                                            holder.stopH.setVisibility(View.GONE);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, null).show();
                            holder.switchH.setClickable(true);
                            holder.switchH.setFocusable(true);
                            holder.switchH.setFocusableInTouchMode(false);
                        });
                    }
                }
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
                holder.switchH.setVisibility(View.GONE);
            case DNS:
                boolean isDnsNotEmpty = AppPreferences.getInstance().isDnsNotEmpty();
                if (isDnsNotEmpty) {
                    holder.switchH.setEnabled(true);
                } else {
                    holder.switchH.setEnabled(false);
                }
                checked = appInfo.hasCustomDns;
                break;
        }
        holder.switchH.setChecked(checked);

        String info = appInfo.system ? "System" : "User";
        holder.infoH.setText(String.format("%s (%s)", info, versionNames.get(appInfo.packageName)));

        Drawable icon = appIcons.get(appInfo.packageName);
        if (icon == null) {
            icon = context.getResources().getDrawable(android.R.drawable.sym_def_app_icon, context.getTheme());
        }
        holder.imageH.setImageDrawable(icon);

        return convertView;
    }

    private static class ViewHolder {
        TextView nameH;
        TextView packageH;
        TextView infoH;
        Switch switchH;
        ImageView imageH;
        RelativeLayout stopH;
        ImageView stopIvH;
    }
}
