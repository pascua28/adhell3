package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.BlockUrlUtils;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        urlProviderCheckBox.setTag(position);
        deleteUrlImageView.setTag(position);

        blockUrlProviderTextView.setText(blockUrlProvider.url);
        blockUrlCountTextView.setText(String.valueOf(blockUrlProvider.count));

        urlProviderCheckBox.setChecked(blockUrlProvider.selected);
        urlProviderCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int position2 = (Integer) buttonView.getTag();
            BlockUrlProvider provider = getItem(position2);
            new GetAllBlockedUrlsAsyncTask(provider, isChecked, this, getContext()).execute();
        });

        Date lastUpdated = blockUrlProvider.lastUpdated == null ? new Date() : blockUrlProvider.lastUpdated;
        lastUpdatedTextView.setText(dateFormatter.format(lastUpdated));
        if (!blockUrlProvider.deletable) {
            deleteUrlImageView.setVisibility(View.GONE);
        }
        deleteUrlImageView.setOnClickListener(imageView -> {
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, parent, false);
            TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
            titleTextView.setText(R.string.delete_provider_dialog_title);
            TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
            questionTextView.setText(R.string.delete_provider_dialog_text);

            AlertDialog alertDialog = new AlertDialog.Builder(getContext(), R.style.ThemeOverlay_AlertDialog)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        int position2 = (Integer) imageView.getTag();
                        BlockUrlProvider provider = getItem(position2);
                        new DeleteProviderAsyncTask(provider, this).execute();
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .create();

            if (alertDialog.getWindow() != null)
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            alertDialog.show();
        });

        return convertView;
    }

    private static class DeleteProviderAsyncTask extends AsyncTask<Void, Void, Void> {
        private final BlockUrlProvider provider;
        private final BlockUrlProviderAdapter adapter;

        DeleteProviderAsyncTask(BlockUrlProvider provider, BlockUrlProviderAdapter adapter) {
            this.provider = provider;
            this.adapter = adapter;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            AppDatabase appDatabase = AppDatabase.getAppDatabase(App.get().getApplicationContext());
            appDatabase.blockUrlProviderDao().delete(provider);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            adapter.remove(provider);
            adapter.notifyDataSetChanged();
        }
    }

    private static class GetAllBlockedUrlsAsyncTask extends AsyncTask<Void, Void, Integer> {
        private final BlockUrlProvider provider;
        private final boolean isChecked;
        private final BlockUrlProviderAdapter adapter;
        private final WeakReference<Context> contextReference;

        GetAllBlockedUrlsAsyncTask(BlockUrlProvider provider, boolean isChecked, BlockUrlProviderAdapter adapter, Context context) {
            this.provider = provider;
            this.isChecked = isChecked;
            this.adapter = adapter;
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
                adapter.notifyDataSetChanged();

                if (totalUrls > AdhellAppIntegrity.BLOCK_URL_LIMIT) {
                    String message = String.format(Locale.getDefault(), "The total number of unique domains %d exceeds the maximum limit of %d",
                            totalUrls, AdhellAppIntegrity.BLOCK_URL_LIMIT);
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
