package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.model.IComponentInfo;
import com.fusionjack.adhell3.model.PermissionInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppComponentFactory;
import com.fusionjack.adhell3.utils.AppPermissionUtils;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.samsung.android.knox.application.ApplicationPolicy.PERMISSION_POLICY_STATE_DENY;

public class PermissionInfoAdapter extends ComponentAdapter {

    private final ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();

    public PermissionInfoAdapter(@NonNull Context context, @NonNull List<IComponentInfo> componentInfos) {
        super(context, componentInfos);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_permission_info, parent, false);
        }

        IComponentInfo componentInfo = getItem(position);
        if (componentInfo == null) {
            return convertView;
        }

        if (componentInfo instanceof PermissionInfo) {
            PermissionInfo permissionInfo = (PermissionInfo) componentInfo;
            String packageName = permissionInfo.getPackageName();
            String permissionName = permissionInfo.getName();
            String permissionLabel = permissionInfo.getLabel();

            TextView permissionLabelTextView = convertView.findViewById(R.id.permissionLabelTextView);
            TextView permissionNameTextView = convertView.findViewById(R.id.permissionNameTextView);
            TextView protectionLevelTextView = convertView.findViewById(R.id.protectionLevelTextView);
            Switch permissionSwitch = convertView.findViewById(R.id.switchDisable);
            permissionLabelTextView.setText(permissionLabel);
            permissionNameTextView.setText(permissionName);
            protectionLevelTextView.setText(AppPermissionUtils.getProtectionLevelLabel(permissionInfo.getLevel()));

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
            permissionSwitch.setChecked(checked);

            boolean enabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
            permissionSwitch.setEnabled(enabled);
        }

        return convertView;
    }
}
