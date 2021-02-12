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
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.utils.AppCache;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

public class AppInfoAdapter extends BaseAdapter {

    private final List<AppInfo> applicationInfoList;
    private final WeakReference<Context> contextReference;
    private final AppRepository.Type appType;

    private final Map<String, Drawable> appIcons;
    private final Map<String, String> versionNames;

    private final Drawable defaultIcon;

    public AppInfoAdapter(List<AppInfo> appInfoList, AppRepository.Type appType, Context context) {
        this.applicationInfoList = appInfoList;
        this.contextReference = new WeakReference<>(context);
        this.appType = appType;

        AppCache appCache = AppCache.getInstance();
        this.appIcons = appCache.getIcons();
        this.versionNames = appCache.getVersionNames();

        this.defaultIcon = ContextCompat.getDrawable(contextReference.get(), android.R.drawable.sym_def_app_icon);
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
        String versionName = versionNames.get(appInfo.packageName);
        holder.infoH.setText(String.format("%s (%s)", info, versionName == null ? "0.0.0" : versionName));

        Drawable icon = appIcons.get(appInfo.packageName);
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
