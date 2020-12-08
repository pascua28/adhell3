package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fusionjack.adhell3.databinding.ItemActivityInfoBinding;
import com.fusionjack.adhell3.model.ActivityInfo;
import com.fusionjack.adhell3.model.IComponentInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;

import java.util.List;

public class ActivityInfoAdapter extends ComponentAdapter {

    public ActivityInfoAdapter(@NonNull Context context, @NonNull List<IComponentInfo> componentInfos) {
        super(context, componentInfos);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ActivityInfoViewHolder holder;
        if (convertView == null) {
            ItemActivityInfoBinding itemBinding = ItemActivityInfoBinding.inflate(LayoutInflater.from(parent.getContext()));

            holder = new ActivityInfoViewHolder(itemBinding);
            holder.view = itemBinding.getRoot();
            holder.view.setTag(holder);
        } else {
            holder = (ActivityInfoViewHolder) convertView.getTag();
        }

        IComponentInfo componentInfo = getItem(position);
        if (componentInfo == null) {
            return holder.view;
        }

        if (componentInfo instanceof ActivityInfo) {
            ActivityInfo activityInfo = (ActivityInfo) componentInfo;
            String packageName = activityInfo.getPackageName();
            String activityName = activityInfo.getName();
            holder.binding.activityNameTextView.setText(activityName);
            holder.binding.switchDisable.setChecked(AdhellFactory.getInstance().getComponentState(packageName, activityName));

            boolean enabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
            holder.binding.switchDisable.setEnabled(enabled);
        }

        return holder.view;
    }

    private static class ActivityInfoViewHolder {
        private View view;
        private final ItemActivityInfoBinding binding;

        ActivityInfoViewHolder(ItemActivityInfoBinding binding) {
            this.view = binding.getRoot();
            this.binding = binding;
        }
    }
}
