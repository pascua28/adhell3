package com.fusionjack.adhell3.adapter;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.databinding.DialogQuestionBinding;
import com.fusionjack.adhell3.databinding.ItemBlockUrlProviderBinding;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.BlockUrlUtils;
import com.google.android.material.snackbar.Snackbar;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BlockUrlProviderAdapter extends ArrayAdapter<BlockUrlProvider> {

    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private final WeakReference<ContentResolver> contentResolverWeakReference;

    public BlockUrlProviderAdapter(Context context, List<BlockUrlProvider> blockUrlProviders) {
        super(context, 0, blockUrlProviders);
        this.contentResolverWeakReference = new WeakReference<>(context.getContentResolver());
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        BlockUrlProviderViewHolder holder;
        if (convertView == null) {
            ItemBlockUrlProviderBinding itemBinding = ItemBlockUrlProviderBinding.inflate(LayoutInflater.from(parent.getContext()));

            holder = new BlockUrlProviderViewHolder(itemBinding);
            holder.view = itemBinding.getRoot();
            holder.view.setTag(holder);
        } else {
            holder = (BlockUrlProviderViewHolder) convertView.getTag();
        }

        BlockUrlProvider blockUrlProvider = getItem(position);
        if (blockUrlProvider == null) {
            return holder.view;
        }

        holder.binding.urlProviderCheckBox.setTag(position);
        holder.binding.deleteUrlProviderImageView.setTag(position);

        holder.binding.blockUrlProviderTextView.setText(getFileName(blockUrlProvider.url));
        holder.binding.blockUrlCountTextView.setText(String.valueOf(blockUrlProvider.count));

        holder.binding.urlProviderCheckBox.setChecked(blockUrlProvider.selected);
        holder.binding.urlProviderCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int position2 = (Integer) buttonView.getTag();
            BlockUrlProvider provider = getItem(position2);
            new GetAllBlockedUrlsAsyncTask(provider, isChecked, getContext()).execute();
        });

        Date lastUpdated = blockUrlProvider.lastUpdated == null ? new Date() : blockUrlProvider.lastUpdated;
        if (blockUrlProvider.lastUpdated != null) {
            holder.binding.lastUpdatedTextView.setText(dateFormatter.format(lastUpdated));
        } else {
            holder.binding.lastUpdatedTextView.setText("N/A");
        }
        if (blockUrlProvider.deletable) {
            holder.binding.deleteUrlProviderImageView.setVisibility(View.VISIBLE);
        } else {
            holder.binding.deleteUrlProviderImageView.setVisibility(View.INVISIBLE);
        }
        holder.binding.deleteUrlProviderImageView.setOnClickListener(imageView -> {
            DialogQuestionBinding dialogQuestionBinding = DialogQuestionBinding.inflate(LayoutInflater.from(getContext()));
            dialogQuestionBinding.titleTextView.setText(R.string.delete_provider_dialog_title);
            dialogQuestionBinding.questionTextView.setText(R.string.delete_provider_dialog_text);

            AlertDialog alertDialog = new AlertDialog.Builder(getContext(), R.style.AlertDialogStyle)
                    .setView(dialogQuestionBinding.getRoot())
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        int position2 = (Integer) imageView.getTag();
                        BlockUrlProvider provider = getItem(position2);
                        new DeleteProviderAsyncTask(provider).execute();
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .create();

            alertDialog.show();
        });

        return holder.view;
    }

    public void updatedBlockUrlProviderList(List<BlockUrlProvider> itemsArrayList) {
        this.clear();
        if (itemsArrayList != null){
            for (BlockUrlProvider blockUrlProvider : itemsArrayList) {
                this.insert(blockUrlProvider, this.getCount());
            }
        }
    }

    private String getFileName(String url) {
        String result = url;
        Uri uri = Uri.parse(url);
        if (uri != null && uri.getScheme() != null && uri.getScheme().equals("content")) {
            ContentResolver contentResolver = contentResolverWeakReference.get();
            if (contentResolver != null) {
                try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } catch(Exception e) {
                    int cut = result.lastIndexOf('/');
                    if (cut != -1) {
                        result = result.substring(cut + 1);
                    }
                }
            }
        }
        return result;
    }

    private static class DeleteProviderAsyncTask extends AsyncTask<Void, Void, Void> {
        private final BlockUrlProvider provider;

        DeleteProviderAsyncTask(BlockUrlProvider provider) {
            this.provider = provider;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            AppDatabase appDatabase = AppDatabase.getAppDatabase(App.get().getApplicationContext());
            appDatabase.blockUrlProviderDao().delete(provider);
            return null;
        }
    }

    private static class GetAllBlockedUrlsAsyncTask extends AsyncTask<Void, Void, Integer> {
        private final WeakReference<Context> contextReference;
        private final boolean isChecked;
        private final BlockUrlProvider provider;

        GetAllBlockedUrlsAsyncTask(BlockUrlProvider provider, boolean isChecked, Context context) {
            this.provider = provider;
            this.isChecked = isChecked;
            this.contextReference = new WeakReference<>(context);
        }

        @Override
        protected Integer doInBackground(Void... o) {
            provider.selected = isChecked;
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            appDatabase.blockUrlProviderDao().updateBlockUrlProviders(provider);
            int totalUrls = BlockUrlUtils.getAllBlockedUrlsCount(appDatabase);
            if (totalUrls > AdhellAppIntegrity.BLOCK_URL_LIMIT) {
                provider.selected = false;
                appDatabase.blockUrlProviderDao().updateBlockUrlProviders(provider);
            }
            return totalUrls;
        }

        @Override
        protected void onPostExecute(Integer totalUrls) {
            Context context = contextReference.get();
            if (context != null) {
                if (totalUrls > AdhellAppIntegrity.BLOCK_URL_LIMIT) {
                    String message = String.format(Locale.getDefault(), "The total number of unique domains %d exceeds the maximum limit of %d",
                            totalUrls, AdhellAppIntegrity.BLOCK_URL_LIMIT);
                    if (context instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) context;
                        mainActivity.makeSnackbar(message, Snackbar.LENGTH_LONG)
                                .show();
                    }
                }
            }
        }
    }

    private static class BlockUrlProviderViewHolder {
        private View view;
        private final ItemBlockUrlProviderBinding binding;

        BlockUrlProviderViewHolder(ItemBlockUrlProviderBinding binding) {
            this.view = binding.getRoot();
            this.binding = binding;
        }
    }
}
