package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.model.ActivityInfo;
import com.fusionjack.adhell3.model.IComponentInfo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ActivityDisabledInfoAdapter extends ComponentDisabledAdapter {

    private final WeakReference<Context> contextReference;
    private final Map<String, Drawable> appIcons;
    private final List<String> expandableListTitle;
    private final Map<String, List<IComponentInfo>> expandableListDetail;

    public ActivityDisabledInfoAdapter(@NonNull Context context,
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
        ViewHolderGroup holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(contextReference.get()).inflate(R.layout.group_app_component_info, parent, false);
            holder = new ViewHolderGroup();

            holder.appIconImageView = convertView.findViewById(R.id.appIconImageView);
            holder.appNameTextView = convertView.findViewById(R.id.AppNameTextView);
            holder.packageNameTextView = convertView.findViewById(R.id.PackageNameTextView);
            holder.countTextView = convertView.findViewById(R.id.countTextView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolderGroup) convertView.getTag();
        }

        String packageName = getChild(groupPosition, 0).getPackageName();
        String appName = getGroup(groupPosition);
        Drawable icon = appIcons.get(packageName);
        if (icon == null) {
            icon = contextReference.get().getResources().getDrawable(android.R.drawable.sym_def_app_icon, contextReference.get().getTheme());
        }

        holder.appIconImageView.setImageDrawable(icon);
        if (appName.equals(packageName)) {
            holder.appNameTextView.setText(packageName);
            holder.packageNameTextView.setText("");
        } else {
            holder.appNameTextView.setText(appName);
            holder.packageNameTextView.setText(packageName);
        }
        holder.countTextView.setText(String.valueOf(getChildrenCount(groupPosition)));

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ViewHolderChild holder;
        Context context = contextReference.get();
        IComponentInfo componentInfo = getChild(groupPosition, childPosition);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_activity_disabled_info, parent, false);
            holder = new ViewHolderChild();


            holder.activityNameTextView = convertView.findViewById(R.id.activityNameTextView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolderChild) convertView.getTag();
        }

        if (componentInfo instanceof ActivityInfo) {
            ActivityInfo activityInfo = (ActivityInfo) componentInfo;

            holder.activityNameTextView.setText(activityInfo.getName());
        }

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    private static class ViewHolderGroup {
        ImageView appIconImageView;
        TextView appNameTextView;
        TextView packageNameTextView;
        TextView countTextView;
    }

    private static class ViewHolderChild {
        TextView activityNameTextView;
    }
}
