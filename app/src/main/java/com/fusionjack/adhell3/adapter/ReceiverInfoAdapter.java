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
import com.fusionjack.adhell3.utils.AppComponentFactory;
import com.fusionjack.adhell3.utils.AppPreferences;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

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
            String receiverPermission = receiverInfo.getPermission();

            holder.binding.receiverNameTextView.setText(receiverName);
            holder.binding.receiverPermissionTextView.setText(permission);

            boolean state = AdhellFactory.getInstance().getComponentState(packageName, receiverName);
            if (!state) {
                Completable.fromAction(() -> AppComponentFactory.getInstance().addReceiverToDatabaseIfNotExist(packageName, receiverName, receiverPermission))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe();
            }
            holder.binding.switchDisable.setChecked(state);

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
