package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fusionjack.adhell3.databinding.ItemReceiverInfoBinding;
import com.fusionjack.adhell3.model.IComponentInfo;
import com.fusionjack.adhell3.model.ReceiverInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;

import java.util.List;

public class ReceiverInfoAdapter extends ComponentAdapter {

    public ReceiverInfoAdapter(@NonNull Context context, @NonNull List<IComponentInfo> componentInfos) {
        super(context, componentInfos);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ReceiverInfoViewHolder holder;
        if (convertView == null) {
            ItemReceiverInfoBinding itemBinding = ItemReceiverInfoBinding.inflate(LayoutInflater.from(parent.getContext()));

            holder = new ReceiverInfoViewHolder(itemBinding);
            holder.view = itemBinding.getRoot();
            holder.view.setTag(holder);
        } else {
            holder = (ReceiverInfoViewHolder) convertView.getTag();
        }

        IComponentInfo componentInfo = getItem(position);
        if (componentInfo == null) {
            return holder.view;
        }

        if (componentInfo instanceof ReceiverInfo) {
            ReceiverInfo receiverInfo = (ReceiverInfo) componentInfo;
            String packageName = receiverInfo.getPackageName();
            String receiverName = receiverInfo.getName();
            String permission = receiverInfo.getPermission();
            holder.binding.receiverNameTextView.setText(receiverName);
            holder.binding.receiverPermissionTextView.setText(permission);
            holder.binding.switchDisable.setChecked(AdhellFactory.getInstance().getComponentState(packageName, receiverName));

            boolean enabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
            holder.binding.switchDisable.setEnabled(enabled);
        }

        return holder.view;
    }

    private static class ReceiverInfoViewHolder {
        private View view;
        private final ItemReceiverInfoBinding binding;

        ReceiverInfoViewHolder(ItemReceiverInfoBinding binding) {
            this.view = binding.getRoot();
            this.binding = binding;
        }
    }
}
