package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.tasks.DomainRxTaskFactory;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.PathUtils;
import com.fusionjack.adhell3.utils.dialog.QuestionDialogBuilder;
import com.fusionjack.adhell3.utils.rx.RxCompletableIoBuilder;
import com.fusionjack.adhell3.utils.rx.RxSingleIoBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class BlockUrlProviderAdapter extends ArrayAdapter<BlockUrlProvider> {

    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    public BlockUrlProviderAdapter(Context context, List<BlockUrlProvider> blockUrlProviders) {
        super(context, 0, blockUrlProviders);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_block_url_provider, parent, false);
        }

        BlockUrlProvider blockUrlProvider = getItem(position);
        if (blockUrlProvider == null) {
            return convertView;
        }

        TextView blockUrlProviderTextView = convertView.findViewById(R.id.blockUrlProviderTextView);
        TextView blockUrlCountTextView = convertView.findViewById(R.id.blockUrlCountTextView);
        CheckBox urlProviderCheckBox = convertView.findViewById(R.id.urlProviderCheckBox);
        ImageView deleteUrlImageView = convertView.findViewById(R.id.deleteUrlProviderImageView);
        TextView lastUpdatedTextView = convertView.findViewById(R.id.lastUpdatedTextView);

        blockUrlProviderTextView.setText(getPath(blockUrlProvider.url));
        blockUrlCountTextView.setText(String.valueOf(blockUrlProvider.count));

        Date lastUpdated = blockUrlProvider.lastUpdated;
        urlProviderCheckBox.setChecked(blockUrlProvider.selected);
        urlProviderCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            BlockUrlProvider provider = getItem(position);
            selectProvider(isChecked, provider);
        });

        lastUpdatedTextView.setText(lastUpdated == null ? "Never" : dateFormatter.format(lastUpdated));

        deleteUrlImageView.setVisibility(blockUrlProvider.deletable ? View.VISIBLE : View.INVISIBLE);
        deleteUrlImageView.setOnClickListener(imageView -> {
            new QuestionDialogBuilder(parent)
                    .setTitle(R.string.delete_provider_dialog_title)
                    .setQuestion(R.string.delete_provider_dialog_text)
                    .show(() -> deleteProvider(getItem(position)));
        });

        return convertView;
    }

    private String getPath(String uriStr) {
        if (URLUtil.isContentUrl(uriStr)) {
            return PathUtils.getPath(Uri.parse(uriStr));
        }
        return uriStr;
    }

    private void selectProvider(boolean isChecked, BlockUrlProvider provider) {
        boolean hasInternetAccess = AdhellFactory.getInstance().hasInternetAccess(getContext());
        if (provider.lastUpdated == null && !hasInternetAccess) {
            Toast.makeText(getContext(), "There is no internet access and it is never updated", Toast.LENGTH_LONG).show();
            return;
        }

        Consumer<Boolean> onSuccessCallback = isValid -> {
            if (!isValid) {
                String message = String.format(Locale.ENGLISH, "The total number of unique domains exceeds the maximum limit of %d",
                        AdhellAppIntegrity.BLOCK_URL_LIMIT);
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        };

        if (isChecked) {
            new RxSingleIoBuilder()
                    .setShowDialog("Loading provider ...", getContext())
                    .async(DomainRxTaskFactory.selectProvider(isChecked, provider), onSuccessCallback);
        } else {
            new RxSingleIoBuilder()
                    .async(DomainRxTaskFactory.selectProvider(isChecked, provider), onSuccessCallback);
        }
    }

    private void deleteProvider(BlockUrlProvider provider) {
        new RxCompletableIoBuilder().async(DomainRxTaskFactory.deleteProvider(provider));
    }

}
