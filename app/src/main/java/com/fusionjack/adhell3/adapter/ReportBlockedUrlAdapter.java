package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.entity.ReportBlockedUrl;
import com.fusionjack.adhell3.utils.AppCache;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportBlockedUrlAdapter extends BaseExpandableListAdapter {
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss", Locale.US);
    private final Map<String, Drawable> appIcons;
    private final Map<String, String> appNames;
    private final Context context;
    private final List<String> expandableListTitle;
    private final HashMap<String, List<ReportBlockedUrl>> expandableListDetail;

    public ReportBlockedUrlAdapter(@NonNull Context context, @NonNull HashMap<String, List<ReportBlockedUrl>> objects, Handler handler) {
        AppCache appCache = AppCache.getInstance(context, handler);
        appIcons = appCache.getIcons();
        appNames = appCache.getNames();
        this.context = context;
        this.expandableListTitle = new ArrayList<>(objects.keySet());
        this.expandableListDetail = objects;
    }

    @Override
    public int getGroupCount() {
        return this.expandableListTitle.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.expandableListDetail.get(this.expandableListTitle.get(groupPosition)).size();
    }

    @Override
    public String getGroup(int groupPosition) {
        return this.expandableListTitle.get(groupPosition);
    }

    @Override
    public ReportBlockedUrl getChild(int groupPosition, int childPosition) {
        return this.expandableListDetail.get(this.expandableListTitle.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String packageName = getGroup(groupPosition);
        String appName = appNames.get(packageName);
        int childCount = getChildrenCount(groupPosition);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.group_blocked_url_info, parent, false);
        }
        ImageView blockedDomainIconImageView = convertView.findViewById(R.id.blockedDomainIconImageView);
        TextView appNameTextView = convertView.findViewById(R.id.blockedDomainAppNameTextView);
        TextView packageNameTextView = convertView.findViewById(R.id.blockedDomainPackageNameTextView);
        TextView countTextView = convertView.findViewById(R.id.blockedDomainCountTextView);
        Drawable icon = appIcons.get(packageName);
        if (icon == null) {
            icon = context.getResources().getDrawable(android.R.drawable.sym_def_app_icon, context.getTheme());
        }
        blockedDomainIconImageView.setImageDrawable(icon);
        countTextView.setText(String.valueOf(childCount));
        if (appName == null) {
            appNameTextView.setText(packageName);
            packageNameTextView.setText("");
        } else {
            appNameTextView.setText(appName);
            packageNameTextView.setText(packageName);
        }
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        final ReportBlockedUrl reportBlockedUrlItem = getChild(groupPosition, childPosition);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_blocked_url_info, parent, false);
        }
        TextView expandedListUrlTextView = convertView.findViewById(R.id.blockedDomainUrlTextView);
        TextView expandedListTimeTextView = convertView.findViewById(R.id.blockedDomainTimeTextView);
        ImageView blockedDomainIconImageView = convertView.findViewById(R.id.blockedDomainIconImageView);
        Drawable icon = appIcons.get(reportBlockedUrlItem.packageName);
        if (icon == null) {
            icon = context.getResources().getDrawable(android.R.drawable.sym_def_app_icon, context.getTheme());
        }
        blockedDomainIconImageView.setImageDrawable(icon);
        expandedListUrlTextView.setText(reportBlockedUrlItem.url);
        expandedListTimeTextView.setText(dateFormatter.format(reportBlockedUrlItem.blockDate));
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
