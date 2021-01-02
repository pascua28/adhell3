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
import com.fusionjack.adhell3.model.ReceiverInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppComponentFactory;
import com.fusionjack.adhell3.utils.AppPreferences;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ReceiverInfoAdapter extends ComponentAdapter {

    public ReceiverInfoAdapter(@NonNull Context context, @NonNull List<IComponentInfo> componentInfos) {
        super(context, componentInfos);
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
            String packageName = receiverInfo.getPackageName();
            String receiverName = receiverInfo.getName();
            String receiverPermission = receiverInfo.getPermission();
            TextView receiverNameTextView = convertView.findViewById(R.id.receiverNameTextView);
            TextView receiverPermissionTextView = convertView.findViewById(R.id.receiverPermissionTextView);
            Switch permissionSwitch = convertView.findViewById(R.id.switchDisable);
            receiverNameTextView.setText(receiverName);
            receiverPermissionTextView.setText(receiverPermission);

            boolean state = AdhellFactory.getInstance().getComponentState(packageName, receiverName);
            if (!state) {
                Completable.fromAction(() -> AppComponentFactory.getInstance().addReceiverToDatabaseIfNotExist(packageName, receiverName, receiverPermission))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe();
            }
            permissionSwitch.setChecked(state);

            boolean enabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
            permissionSwitch.setEnabled(enabled);
        }

        return convertView;
    }
}
