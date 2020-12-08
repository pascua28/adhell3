package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.fusionjack.adhell3.databinding.GroupAppComponentInfoBinding;
import com.fusionjack.adhell3.databinding.ItemReceiverDisabledInfoBinding;
import com.fusionjack.adhell3.model.IComponentInfo;
import com.fusionjack.adhell3.model.ReceiverInfo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReceiverDisabledInfoAdapter extends ComponentDisabledAdapter {

    private final WeakReference<Context> contextReference;
    private final Map<String, Drawable> appIcons;
    private final List<String> expandableListTitle;
    private final Map<String, List<IComponentInfo>> expandableListDetail;
    
    public ReceiverDisabledInfoAdapter(@NonNull Context context,
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
        ReceiverDisabledInfoGroupViewHolder holder;
        if (convertView == null) {
            GroupAppComponentInfoBinding groupBinding = GroupAppComponentInfoBinding.inflate(LayoutInflater.from(parent.getContext()));

            holder = new ReceiverDisabledInfoGroupViewHolder(groupBinding);
            holder.view = groupBinding.getRoot();
            holder.view.setTag(holder);
        } else {
            holder = (ReceiverDisabledInfoGroupViewHolder) convertView.getTag();
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
        ReceiverDisabledInfoItemViewHolder holder;
        IComponentInfo componentInfo = getChild(groupPosition, childPosition);

        if (convertView == null) {
            ItemReceiverDisabledInfoBinding itemBinding = ItemReceiverDisabledInfoBinding.inflate(LayoutInflater.from(parent.getContext()));

            holder = new ReceiverDisabledInfoItemViewHolder(itemBinding);
            holder.view = itemBinding.getRoot();
            holder.view.setTag(holder);
        } else {
            holder = (ReceiverDisabledInfoItemViewHolder) convertView.getTag();
        }

        if (componentInfo != null) {
            if (componentInfo instanceof ReceiverInfo) {
                ReceiverInfo receiverInfo = (ReceiverInfo) componentInfo;

                holder.binding.receiverNameTextView.setText(receiverInfo.getName());
                holder.binding.receiverPermissionTextView.setText(receiverInfo.getPermission());
            }
        }

        return holder.view;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    private static class ReceiverDisabledInfoGroupViewHolder {
        private View view;
        private final GroupAppComponentInfoBinding binding;

        ReceiverDisabledInfoGroupViewHolder(GroupAppComponentInfoBinding binding) {
            this.view = binding.getRoot();
            this.binding = binding;
        }
    }

    private static class ReceiverDisabledInfoItemViewHolder {
        private View view;
        private final ItemReceiverDisabledInfoBinding binding;

        ReceiverDisabledInfoItemViewHolder(ItemReceiverDisabledInfoBinding binding) {
            this.view = binding.getRoot();
            this.binding = binding;
        }
    }
}
