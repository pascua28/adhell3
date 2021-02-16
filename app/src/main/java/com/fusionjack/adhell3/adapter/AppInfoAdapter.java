package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.cache.AppCache;
import com.fusionjack.adhell3.cache.AppCacheInfo;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.lang.ref.WeakReference;
import java.util.List;

public class AppInfoAdapter extends BaseAdapter {

    private final List<AppInfo> applicationInfoList;
    private final WeakReference<Context> contextReference;
    private final AppRepository.Type appType;

    private final AppCache appCache;
    private final Drawable defaultIcon;

    private final ApplicationPolicy appPolicy;
    private final int runningColor;
    private final int textColor;

    public AppInfoAdapter(List<AppInfo> appInfoList, AppRepository.Type appType, Context context) {
        this.applicationInfoList = appInfoList;
        this.contextReference = new WeakReference<>(context);
        this.appType = appType;

        this.appCache = AppCache.getInstance();
        this.defaultIcon = ContextCompat.getDrawable(contextReference.get(), android.R.drawable.sym_def_app_icon);

        this.appPolicy = AdhellFactory.getInstance().getAppPolicy();
        this.runningColor = ContextCompat.getColor(context, R.color.colorAccent);
        this.textColor = ContextCompat.getColor(context, R.color.colorText);
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
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AppInfo appInfo = applicationInfoList.get(position);
        holder.nameH.setText(appInfo.appName);
        holder.packageH.setText(appInfo.packageName);
        boolean checked = false;
        switch (appType) {
            case DISABLER:
                checked = !appInfo.disabled;
                boolean enabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
                holder.switchH.setEnabled(enabled);
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
                holder.switchH.setEnabled(isDnsNotEmpty);
                checked = appInfo.hasCustomDns;
                break;
        }
        holder.switchH.setChecked(checked);

        boolean isRunning = appPolicy.isApplicationRunning(appInfo.packageName);
        holder.nameH.setTextColor(isRunning ? runningColor : textColor);
        holder.packageH.setTextColor(isRunning ? runningColor : textColor);
        holder.infoH.setTextColor(isRunning ? runningColor : textColor);

        AppCacheInfo appCacheInfo = appCache.getAppCacheInfo(appInfo.packageName);
        String info = appInfo.system ? "System" : "User";
        String versionName = appCacheInfo.getAppVersion();
        holder.infoH.setText(String.format("%s (%s)", info, versionName));

        Drawable icon = appCacheInfo.getDrawable();
        if (icon == null) {
            icon = defaultIcon;
        }
        holder.imageH.setImageDrawable(icon);

        return convertView;
    }

    private static class ViewHolder {
        TextView nameH;
        TextView packageH;
        TextView infoH;
        SwitchMaterial switchH;
        ImageView imageH;
    }
}
