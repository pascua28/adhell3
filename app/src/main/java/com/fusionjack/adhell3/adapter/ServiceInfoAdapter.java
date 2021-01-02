package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fusionjack.adhell3.databinding.ItemServiceInfoBinding;
import com.fusionjack.adhell3.model.IComponentInfo;
import com.fusionjack.adhell3.model.ServiceInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppComponentFactory;
import com.fusionjack.adhell3.utils.AppPreferences;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ServiceInfoAdapter extends ComponentAdapter {

    public ServiceInfoAdapter(@NonNull Context context, @NonNull List<IComponentInfo> componentInfos) {
        super(context, componentInfos);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ServiceInfoViewHolder holder;
        if (convertView == null) {
            ItemServiceInfoBinding itemBinding = ItemServiceInfoBinding.inflate(LayoutInflater.from(parent.getContext()));

            holder = new ServiceInfoViewHolder(itemBinding);
            holder.view = itemBinding.getRoot();
            holder.view.setTag(holder);
        } else {
            holder = (ServiceInfoViewHolder) convertView.getTag();
        }

        IComponentInfo componentInfo = getItem(position);
        if (componentInfo == null) {
            return holder.view;
        }

        if (componentInfo instanceof ServiceInfo) {
            ServiceInfo serviceInfo = (ServiceInfo) componentInfo;
            String packageName = serviceInfo.getPackageName();
            String serviceName = serviceInfo.getName();

            holder.binding.serviceNameTextView.setText(serviceName);

            boolean state = AdhellFactory.getInstance().getComponentState(packageName, serviceName);
            if (!state) {
                Completable.fromAction(() -> AppComponentFactory.getInstance().addServiceToDatabaseIfNotExist(packageName, serviceName))
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

    private static class ServiceInfoViewHolder {
        private View view;
        private final ItemServiceInfoBinding binding;

        ServiceInfoViewHolder(ItemServiceInfoBinding binding) {
            this.view = binding.getRoot();
            this.binding = binding;
        }
    }
}
