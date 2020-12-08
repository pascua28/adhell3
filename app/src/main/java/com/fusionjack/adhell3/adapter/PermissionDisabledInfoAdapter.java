package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.fusionjack.adhell3.databinding.GroupAppComponentInfoBinding;
import com.fusionjack.adhell3.databinding.ItemPermissionDisabledInfoBinding;
import com.fusionjack.adhell3.model.IComponentInfo;
import com.fusionjack.adhell3.model.PermissionInfo;
import com.fusionjack.adhell3.utils.AppPermissionUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PermissionDisabledInfoAdapter extends ComponentDisabledAdapter {

    private final WeakReference<Context> contextReference;
    private final Map<String, Drawable> appIcons;
    private final List<String> expandableListTitle;
    private final Map<String, List<IComponentInfo>> expandableListDetail;

    public PermissionDisabledInfoAdapter(@NonNull Context context,
                                         @NonNull Map<String, List<IComponentInfo>> componentInfos,
                                         Map<String, Drawable> appIcons)
    {
        super();

        this.contextReference = new WeakReference<>(context);
        this.appIcons = appIcons;

        this.expandableListTitle = new ArrayList<>(componentInfos.keySet());
        this.expandableListDetail = componentInfos;
    }

    @Override
    public int getGroupCount() {
        return this.expandableListTitle.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        List<IComponentInfo> children = this.expandableListDetail.get(this.expandableListTitle.get(groupPosition));
        if (children != null) {
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
    public IComponentInfo getChild(int groupPosition, int childPosition) {
        List<IComponentInfo> children = this.expandableListDetail.get(this.expandableListTitle.get(groupPosition));
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
        PermissionDisabledInfoGroupViewHolder holder;
        if (convertView == null) {
            GroupAppComponentInfoBinding groupBinding = GroupAppComponentInfoBinding.inflate(LayoutInflater.from(parent.getContext()));

            holder = new PermissionDisabledInfoGroupViewHolder(groupBinding);
            holder.view = groupBinding.getRoot();
            holder.view.setTag(holder);
        } else {
            holder = (PermissionDisabledInfoGroupViewHolder) convertView.getTag();
        }

        String packageName = getChild(groupPosition, 0).getPackageName();
        String appName = getGroup(groupPosition);
        Drawable icon = appIcons.get(packageName);
        if (icon == null) {
            icon = ResourcesCompat.getDrawable(contextReference.get().getResources(), android.R.drawable.sym_def_app_icon, contextReference.get().getTheme());
        }

        holder.binding.appIconImageView.setImageDrawable(icon);
        if (appName.equals(packageName)) {
            holder.binding.AppNameTextView.setText(packageName);
            holder.binding.PackageNameTextView.setText("");
        } else {
            holder.binding.AppNameTextView.setText(appName);
            holder.binding.PackageNameTextView.setText(packageName);
        }
        holder.binding.countTextView.setText(String.valueOf(getChildrenCount(groupPosition)));

        return holder.view;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        PermissionDisabledInfoItemViewHolder holder;
        IComponentInfo componentInfo = getChild(groupPosition, childPosition);

        if (convertView == null) {
            ItemPermissionDisabledInfoBinding itemBinding = ItemPermissionDisabledInfoBinding.inflate(LayoutInflater.from(parent.getContext()));

            holder = new PermissionDisabledInfoItemViewHolder(itemBinding);
            holder.view = itemBinding.getRoot();
            holder.view.setTag(holder);
        } else {
            holder = (PermissionDisabledInfoItemViewHolder) convertView.getTag();
        }

        if (componentInfo != null) {
            if (componentInfo instanceof PermissionInfo) {
                PermissionInfo permissionInfo = (PermissionInfo) componentInfo;

                holder.binding.permissionNameTextView.setText(permissionInfo.name);
                holder.binding.protectionLevelTextView.setText(AppPermissionUtils.getProtectionLevelLabel(permissionInfo.getLevel()));
                holder.binding.permissionLabelTextView.setText(permissionInfo.label);
            }
        }

        return holder.view;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    private static class PermissionDisabledInfoGroupViewHolder {
        private View view;
        private final GroupAppComponentInfoBinding binding;

        PermissionDisabledInfoGroupViewHolder(GroupAppComponentInfoBinding binding) {
            this.view = binding.getRoot();
            this.binding = binding;
        }
    }

    private static class PermissionDisabledInfoItemViewHolder {
        private View view;
        private final ItemPermissionDisabledInfoBinding binding;

        PermissionDisabledInfoItemViewHolder(ItemPermissionDisabledInfoBinding binding) {
            this.view = binding.getRoot();
            this.binding = binding;
        }
    }
}
