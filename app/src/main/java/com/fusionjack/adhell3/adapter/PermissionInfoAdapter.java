package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fusionjack.adhell3.databinding.ItemPermissionInfoBinding;
import com.fusionjack.adhell3.model.IComponentInfo;
import com.fusionjack.adhell3.model.PermissionInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPermissionUtils;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.util.List;

import static com.samsung.android.knox.application.ApplicationPolicy.PERMISSION_POLICY_STATE_DENY;

public class PermissionInfoAdapter extends ComponentAdapter {

    public PermissionInfoAdapter(@NonNull Context context, @NonNull List<IComponentInfo> componentInfos) {
        super(context, componentInfos);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        PermissionInfoViewHolder holder;
        if (convertView == null) {
            ItemPermissionInfoBinding itemBinding = ItemPermissionInfoBinding.inflate(LayoutInflater.from(parent.getContext()));

            holder = new PermissionInfoViewHolder(itemBinding);
            holder.view = itemBinding.getRoot();
            holder.view.setTag(holder);
        } else {
            holder = (PermissionInfoViewHolder) convertView.getTag();
        }

        IComponentInfo componentInfo = getItem(position);
        if (componentInfo == null) {
            return holder.view;
        }

        if (componentInfo instanceof PermissionInfo) {
            PermissionInfo permissionInfo = (PermissionInfo) componentInfo;
            holder.binding.permissionLabelTextView.setText(permissionInfo.label);
            holder.binding.permissionNameTextView.setText(permissionInfo.name);
            holder.binding.protectionLevelTextView.setText(AppPermissionUtils.getProtectionLevelLabel(permissionInfo.getLevel()));

            boolean checked = false;
            ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
            if (appPolicy != null) {
                List<String> deniedPermissions = appPolicy.getRuntimePermissions(permissionInfo.getPackageName(), PERMISSION_POLICY_STATE_DENY);
                if (!deniedPermissions.contains(permissionInfo.name)) {
                    checked = true;
                }
                holder.binding.switchDisable.setChecked(checked);

                boolean enabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
                holder.binding.switchDisable.setEnabled(enabled);
            }
        }

        return holder.view;
    }

    private static class PermissionInfoViewHolder {
        private View view;
        private final ItemPermissionInfoBinding binding;

        PermissionInfoViewHolder(ItemPermissionInfoBinding binding) {
            this.view = binding.getRoot();
            this.binding = binding;
        }
    }
}
