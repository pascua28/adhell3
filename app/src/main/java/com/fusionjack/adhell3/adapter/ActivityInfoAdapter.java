package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.model.ActivityInfo;
import com.fusionjack.adhell3.model.IComponentInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

public class ActivityInfoAdapter extends ComponentAdapter {

    public ActivityInfoAdapter(@NonNull Context context, @NonNull List<IComponentInfo> componentInfos) {
        super(context, componentInfos);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_activity_info, parent, false);
        }

        IComponentInfo componentInfo = getItem(position);
        if (componentInfo == null) {
            return convertView;
        }

        if (componentInfo instanceof ActivityInfo) {
            ActivityInfo activityInfo = (ActivityInfo) componentInfo;
            String packageName = activityInfo.getPackageName();
            String activityName = activityInfo.getName();
            TextView activityNameTextView = convertView.findViewById(R.id.activityNameTextView);
            SwitchMaterial permissionSwitch = convertView.findViewById(R.id.switchDisable);
            activityNameTextView.setText(activityName);
            permissionSwitch.setChecked(AdhellFactory.getInstance().getComponentState(packageName, activityName));

            boolean enabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
            permissionSwitch.setEnabled(enabled);
        }

        return convertView;
    }
}
