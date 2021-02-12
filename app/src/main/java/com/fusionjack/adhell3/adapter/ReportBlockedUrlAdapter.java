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
import com.fusionjack.adhell3.db.entity.ReportBlockedUrl;
import com.fusionjack.adhell3.utils.AppCache;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportBlockedUrlAdapter extends ArrayAdapter<ReportBlockedUrl> {
    private final Map<String, Drawable> appIcons;
    private final Map<String, String> appNames;

    private final Drawable defaultIcon;

    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);

    public ReportBlockedUrlAdapter(@NonNull Context context, @NonNull List<ReportBlockedUrl> objects) {
        super(context, 0, objects);

        AppCache appCache = AppCache.getInstance();
        this.appNames = appCache.getNames();
        this.appIcons = appCache.getIcons();

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

        String appName = appNames.get(reportBlockedUrl.packageName);
        Drawable icon = appIcons.get(reportBlockedUrl.packageName);
        if (icon == null) {
            icon = defaultIcon;
        }
        blockedDomainIconImageView.setImageDrawable(icon);
        blockedDomainAppNameTextView.setText(appName == null ? "(unknown)" : appName);
        blockedDomainUrlTextView.setText(reportBlockedUrl.url);
        blockedDomainTimeTextView.setText(dateFormatter.format(reportBlockedUrl.blockDate));

        return convertView;
    }
}
