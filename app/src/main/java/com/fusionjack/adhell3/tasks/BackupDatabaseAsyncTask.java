package com.fusionjack.adhell3.tasks;

import android.content.Context;
import android.os.AsyncTask;

import androidx.appcompat.app.AlertDialog;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.DatabaseFactory;
import com.fusionjack.adhell3.utils.DialogUtils;

public class BackupDatabaseAsyncTask extends AsyncTask<Void, Void, String> {
    private final AlertDialog dialog;
    private AlertDialog.Builder builder;

    public BackupDatabaseAsyncTask(Context context) {
        this.dialog = DialogUtils.getProgressDialog("Backup database is running...", context);
        this.dialog.setCancelable(false);
        this.builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
    }

    @Override
    protected void onPreExecute() {
        dialog.show();
    }

    @Override
    protected String doInBackground(Void... args) {
        try {
            DatabaseFactory.getInstance().backupDatabase();
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @Override
    protected void onPostExecute(String message) {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }

        if (message == null) {
            builder.setMessage("Backup database is finished");
            builder.setTitle("Info");
        } else {
            builder.setMessage(message);
            builder.setTitle("Error");
        }
        AlertDialog dialog = builder.create();

        dialog.show();

        // Clean resource to prevent memory leak
        this.builder = null;
    }
}
