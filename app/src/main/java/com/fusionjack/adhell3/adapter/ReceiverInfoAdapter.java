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
import com.fusionjack.adhell3.model.ReceiverInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppComponentFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.rx.RxCompletableIoBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

import io.reactivex.Completable;

public class ReceiverInfoAdapter extends ComponentAdapter {

    private final boolean toggleIsEnabled;

    public ReceiverInfoAdapter(@NonNull Context context, @NonNull List<IComponentInfo> componentInfos) {
        super(context, componentInfos);
        this.toggleIsEnabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_receiver_info, parent, false);
        }

        IComponentInfo componentInfo = getItem(position);
        if (componentInfo == null) {
            return convertView;
        }

        if (componentInfo instanceof ReceiverInfo) {
            ReceiverInfo receiverInfo = (ReceiverInfo) componentInfo;

            TextView receiverNameTextView = convertView.findViewById(R.id.receiverNameTextView);
            TextView receiverPackageTextView = convertView.findViewById(R.id.receiverPackageTextView);
            TextView receiverPermissionTextView = convertView.findViewById(R.id.receiverPermissionTextView);
            SwitchMaterial permissionSwitch = convertView.findViewById(R.id.switchDisable);

            String receiverName = receiverInfo.getName();
            String receiverPermission = receiverInfo.getPermission();
            int lastIndex = receiverName.lastIndexOf('.');
            if (lastIndex != -1) {
                String nameStr = receiverName.substring(lastIndex + 1);
                String packageStr = receiverName.substring(0, lastIndex);
                receiverNameTextView.setText(nameStr);
                receiverPackageTextView.setText(packageStr);
            } else {
                receiverNameTextView.setText("Unknown");
                receiverPackageTextView.setText(receiverName);
            }
            receiverPermissionTextView.setText(receiverPermission);

            String packageName = receiverInfo.getPackageName();
            boolean state = AdhellFactory.getInstance().getComponentState(packageName, receiverName);
            if (!state) {
                Completable action = Completable.fromAction(() -> AppComponentFactory.getInstance().addReceiverToDatabaseIfNotExist(packageName, receiverName, receiverPermission));
                new RxCompletableIoBuilder().async(action);
            }
            permissionSwitch.setChecked(state);

            permissionSwitch.setEnabled(toggleIsEnabled);
        }

        return convertView;
    }
}
