package com.fusionjack.adhell3.tasks;

import android.app.ProgressDialog;
import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.fusionjack.adhell3.db.DatabaseFactory;
import com.fusionjack.adhell3.utils.rx.RxCompletableIoBuilder;

import java.lang.ref.WeakReference;

import io.reactivex.Completable;
import io.reactivex.functions.Action;

public class BackupDatabaseRxTask implements Runnable {

    private final WeakReference<Context> contextWeakReference;
    private ProgressDialog dialog;

    public BackupDatabaseRxTask(Context context) {
        this.contextWeakReference = new WeakReference<>(context);
        createDialog();
    }

    private void createDialog() {
        Context context = contextWeakReference.get();
        if (context != null) {
            this.dialog = new ProgressDialog(context);
            dialog.setCancelable(false);
        }
    }

    @Override
    public void run() {
        Context context = contextWeakReference.get();
        if (context == null) {
            return;
        }

        Runnable onSubscribeCallback = () -> {
            dialog.setMessage("Backing up database ...");
            dialog.show();
        };

        Runnable onCompleteCallback = () -> {
            dialog.dismiss();
            new AlertDialog.Builder(context)
                    .setTitle("Info")
                    .setMessage("Backup database is finished.")
                    .show();
        };

        Runnable onErrorCallback = () -> dialog.dismiss();

        Action action = () -> DatabaseFactory.getInstance().backupDatabase();

        new RxCompletableIoBuilder()
                .showErrorAlert(context)
                .async(Completable.fromAction(action), onSubscribeCallback, onCompleteCallback, onErrorCallback);
    }
}
