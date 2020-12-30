package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fusionjack.adhell3.databinding.ItemContentProviderInfoBinding;
import com.fusionjack.adhell3.model.ProviderInfo;
import com.fusionjack.adhell3.model.IComponentInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;

import java.util.List;

public class ContentProviderInfoAdapter extends ComponentAdapter {

    public ContentProviderInfoAdapter(@NonNull Context context, @NonNull List<IComponentInfo> componentInfos) {
        super(context, componentInfos);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ContentProviderInfoViewHolder holder;
        if (convertView == null) {
            ItemContentProviderInfoBinding itemBinding = ItemContentProviderInfoBinding.inflate(LayoutInflater.from(parent.getContext()));

            holder = new ContentProviderInfoViewHolder(itemBinding);
            holder.view = itemBinding.getRoot();
            holder.view.setTag(holder);
        } else {
            holder = (ContentProviderInfoViewHolder) convertView.getTag();
        }

        IComponentInfo componentInfo = getItem(position);
        if (componentInfo == null) {
            return holder.view;
        }

        if (componentInfo instanceof ProviderInfo) {
            ProviderInfo providerInfo = (ProviderInfo) componentInfo;
            String packageName = providerInfo.getPackageName();
            String providerName = providerInfo.getName();
            holder.binding.providerNameTextView.setText(providerName);
            holder.binding.switchDisable.setChecked(AdhellFactory.getInstance().getComponentState(packageName, providerName));

            boolean enabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
            holder.binding.switchDisable.setEnabled(enabled);
        }

        return holder.view;
    }

    private static class ContentProviderInfoViewHolder {
        private View view;
        private final ItemContentProviderInfoBinding binding;

        ContentProviderInfoViewHolder(ItemContentProviderInfoBinding binding) {
            this.view = binding.getRoot();
            this.binding = binding;
        }
    }
}
