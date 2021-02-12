package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.cache.AppCache;
import com.fusionjack.adhell3.cache.AppCacheInfo;
import com.fusionjack.adhell3.db.entity.ReportBlockedUrl;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReportBlockedUrlAdapter extends ArrayAdapter<ReportBlockedUrl> {

    private final AppCache appCache;
    private final Drawable defaultIcon;

    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);

    public ReportBlockedUrlAdapter(@NonNull Context context, @NonNull List<ReportBlockedUrl> objects) {
        super(context, 0, objects);

        this.appCache = AppCache.getInstance();
        this.defaultIcon = ContextCompat.getDrawable(getContext(), android.R.drawable.sym_def_app_icon);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_blocked_url_info, parent, false);
        }
        ReportBlockedUrl reportBlockedUrl = getItem(position);
        if (reportBlockedUrl == null) {
            return convertView;
        }

        ImageView blockedDomainIconImageView = convertView.findViewById(R.id.blockedDomainIconImageView);
        TextView blockedDomainAppNameTextView = convertView.findViewById(R.id.blockedDomainAppNameTextView);
        TextView blockedDomainUrlTextView = convertView.findViewById(R.id.blockedDomainUrlTextView);
        TextView blockedDomainTimeTextView = convertView.findViewById(R.id.blockedDomainTimeTextView);

        AppCacheInfo appCacheInfo = appCache.getAppCacheInfo(reportBlockedUrl.packageName);
        String appName = appCacheInfo.getAppName();
        Drawable icon = appCacheInfo.getDrawable();
        if (icon == null) {
            icon = defaultIcon;
        }
        blockedDomainIconImageView.setImageDrawable(icon);
        blockedDomainAppNameTextView.setText(appName);
        blockedDomainUrlTextView.setText(reportBlockedUrl.url);
        blockedDomainTimeTextView.setText(dateFormatter.format(reportBlockedUrl.blockDate));

        return convertView;
    }
}
