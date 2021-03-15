package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.model.IComponentInfo;
import com.fusionjack.adhell3.model.PermissionInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppComponentFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.rx.RxCompletableIoBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;

import static com.samsung.android.knox.application.ApplicationPolicy.PERMISSION_POLICY_STATE_DENY;

public class PermissionInfoAdapter extends ComponentAdapter {

    private final ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
    private final boolean toggleIsEnabled;

    public PermissionInfoAdapter(@NonNull Context context, @NonNull List<IComponentInfo> componentInfos) {
        super(context, componentInfos);
        this.toggleIsEnabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
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
            SwitchMaterial permissionSwitch = convertView.findViewById(R.id.switchDisable);

            String prefix = "android.permission.";
            String name = permissionName;
            if (name.startsWith(prefix)) {
                name = name.substring(prefix.length());
            }
            permissionNameTextView.setText(name);
            permissionLabelTextView.setText(permissionLabel);

            boolean checked = false;
            List<String> deniedPermissions = appPolicy.getRuntimePermissions(packageName, PERMISSION_POLICY_STATE_DENY);
            if (!deniedPermissions.contains(permissionName)) {
                checked = true;
            }
            if (!checked) {
                Completable action = Completable.fromAction(() -> AppComponentFactory.getInstance().addPermissionToDatabaseIfNotExist(packageName, permissionName));
                new RxCompletableIoBuilder().async(action);
            }
            permissionSwitch.setChecked(checked);
            permissionSwitch.setEnabled(toggleIsEnabled);
        }

        return convertView;
    }
}
