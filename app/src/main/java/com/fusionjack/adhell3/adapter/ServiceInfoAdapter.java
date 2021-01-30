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
import com.fusionjack.adhell3.model.ServiceInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppComponentFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.rx.RxCompletableIoBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ServiceInfoAdapter extends ComponentAdapter {

    public ServiceInfoAdapter(@NonNull Context context, @NonNull List<IComponentInfo> componentInfos) {
        super(context, componentInfos);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_service_info, parent, false);
        }

        IComponentInfo componentInfo = getItem(position);
        if (componentInfo == null) {
            return convertView;
        }

        if (componentInfo instanceof ServiceInfo) {
            ServiceInfo serviceInfo = (ServiceInfo) componentInfo;
            String packageName = serviceInfo.getPackageName();
            String serviceName = serviceInfo.getName();
            TextView serviceNameTextView = convertView.findViewById(R.id.serviceNameTextView);
            SwitchMaterial permissionSwitch = convertView.findViewById(R.id.switchDisable);
            serviceNameTextView.setText(serviceName);

            boolean state = AdhellFactory.getInstance().getComponentState(packageName, serviceName);
            if (!state) {
                Completable action = Completable.fromAction(() -> AppComponentFactory.getInstance().addServiceToDatabaseIfNotExist(packageName, serviceName));
                new RxCompletableIoBuilder().async(action);
            }
            permissionSwitch.setChecked(state);

            boolean enabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
            permissionSwitch.setEnabled(enabled);
        }

        return convertView;
    }
}
