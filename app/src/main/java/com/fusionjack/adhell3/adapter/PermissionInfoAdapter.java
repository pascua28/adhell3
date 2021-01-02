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
import com.fusionjack.adhell3.utils.AppComponentFactory;
import com.fusionjack.adhell3.utils.AppPermissionUtils;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static com.samsung.android.knox.application.ApplicationPolicy.PERMISSION_POLICY_STATE_DENY;

public class PermissionInfoAdapter extends ComponentAdapter {

    private final ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();

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

            String packageName = permissionInfo.getPackageName();
            String permissionName = permissionInfo.getName();
            String permissionLabel = permissionInfo.getLabel();

            holder.binding.permissionLabelTextView.setText(permissionLabel);
            holder.binding.permissionNameTextView.setText(permissionName);
            holder.binding.protectionLevelTextView.setText(AppPermissionUtils.getProtectionLevelLabel(permissionInfo.getLevel()));

            boolean checked = false;
            List<String> deniedPermissions = appPolicy.getRuntimePermissions(packageName, PERMISSION_POLICY_STATE_DENY);
            if (!deniedPermissions.contains(permissionName)) {
                checked = true;
            }
            if (!checked) {
                Completable.fromAction(() -> AppComponentFactory.getInstance().addPermissionToDatabaseIfNotExist(packageName, permissionName))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe();
            }
            holder.binding.switchDisable.setChecked(checked);

            boolean enabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
            holder.binding.switchDisable.setEnabled(enabled);
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
