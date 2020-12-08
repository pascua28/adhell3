package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.fusionjack.adhell3.databinding.GroupBlockedUrlInfoBinding;
import com.fusionjack.adhell3.databinding.ItemBlockedUrlInfoBinding;
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
    private static Map<String, Drawable> appIcons;
    private static Map<String, String> appNames;
    private final Context context;
    private final List<String> expandableListTitle;
    private final HashMap<String, List<ReportBlockedUrl>> expandableListDetail;

    public ReportBlockedUrlAdapter(@NonNull Context context, @NonNull HashMap<String, List<ReportBlockedUrl>> objects, Handler handler) {
        AppCache appCache = AppCache.getInstance(context, handler);
        if (appCache.getNames().size() > 0) {
            appIcons = appCache.getIcons();
            appNames = appCache.getNames();
        }
        this.context = context;
        this.expandableListTitle = new ArrayList<>(objects.keySet());
        this.expandableListDetail = objects;
    }

    @Override
    public int getGroupCount() {
        if (appNames.size() > 0) {
            return this.expandableListTitle.size();
        } else {
            return 0;
        }
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        List<ReportBlockedUrl> children = this.expandableListDetail.get(this.expandableListTitle.get(groupPosition));
        if (children != null && appNames.size() > 0) {
            return children.size();
        } else {
            return 0;
        }
    }

    @Override
    public String getGroup(int groupPosition) {
        return this.expandableListTitle.get(groupPosition);
    }

    @Override
    public ReportBlockedUrl getChild(int groupPosition, int childPosition) {
        List<ReportBlockedUrl> children = this.expandableListDetail.get(this.expandableListTitle.get(groupPosition));
        if (children != null) {
            return children.get(childPosition);
        } else {
            return null;
        }
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
        ReportBlockedUrlGroupViewHolder holder;
        String packageName = getGroup(groupPosition);
        String appName = appNames.get(packageName);
        int childCount = getChildrenCount(groupPosition);
        if (convertView == null) {
            GroupBlockedUrlInfoBinding groupBinding = GroupBlockedUrlInfoBinding.inflate(LayoutInflater.from(parent.getContext()));

            holder = new ReportBlockedUrlGroupViewHolder(groupBinding);
            holder.view = groupBinding.getRoot();
            holder.view.setTag(holder);
        } else {
            holder = (ReportBlockedUrlGroupViewHolder) convertView.getTag();
        }

        Drawable icon = appIcons.get(packageName);
        if (icon == null) {
            icon = ResourcesCompat.getDrawable(context.getResources(), android.R.drawable.sym_def_app_icon, context.getTheme());
        }
        holder.binding.blockedDomainIconImageView.setImageDrawable(icon);
        holder.binding.blockedDomainCountTextView.setText(String.valueOf(childCount));
        if (appName == null) {
            holder.binding.blockedDomainAppNameTextView.setText(packageName);
            holder.binding.blockedDomainPackageNameTextView.setText("");
        } else {
            holder.binding.blockedDomainAppNameTextView.setText(appName);
            holder.binding.blockedDomainPackageNameTextView.setText(packageName);
        }
        return holder.view;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ReportBlockedUrlItemInfoViewHolder holder;
        final ReportBlockedUrl reportBlockedUrlItem = getChild(groupPosition, childPosition);
        if (convertView == null) {
            ItemBlockedUrlInfoBinding itemBinding = ItemBlockedUrlInfoBinding.inflate(LayoutInflater.from(parent.getContext()));

            holder = new ReportBlockedUrlItemInfoViewHolder(itemBinding);
            holder.view = itemBinding.getRoot();
            holder.view.setTag(holder);
        } else {
            holder = (ReportBlockedUrlItemInfoViewHolder) convertView.getTag();
        }

        Drawable icon = appIcons.get(reportBlockedUrlItem.packageName);
        if (icon == null) {
            icon = ResourcesCompat.getDrawable(context.getResources(), android.R.drawable.sym_def_app_icon, context.getTheme());
        }
        holder.binding.blockedDomainIconImageView.setImageDrawable(icon);
        holder.binding.blockedDomainUrlTextView.setText(reportBlockedUrlItem.url);
        holder.binding.blockedDomainTimeTextView.setText(dateFormatter.format(reportBlockedUrlItem.blockDate));

        return holder.view;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    private static class ReportBlockedUrlGroupViewHolder {
        private View view;
        private final GroupBlockedUrlInfoBinding binding;

        ReportBlockedUrlGroupViewHolder(GroupBlockedUrlInfoBinding binding) {
            this.view = binding.getRoot();
            this.binding = binding;
        }
    }

    private static class ReportBlockedUrlItemInfoViewHolder {
        private View view;
        private final ItemBlockedUrlInfoBinding binding;

        ReportBlockedUrlItemInfoViewHolder(ItemBlockedUrlInfoBinding binding) {
            this.view = binding.getRoot();
            this.binding = binding;
        }
    }
}
