package com.fusionjack.adhell3.tasks;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.DatabaseFactory;
import com.fusionjack.adhell3.utils.DialogUtils;
import com.fusionjack.adhell3.utils.LogUtils;

import java.lang.ref.WeakReference;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class BackupDatabaseRxTask implements Runnable {

    private final WeakReference<Context> contextWeakReference;
    private AlertDialog dialog;

    public BackupDatabaseRxTask(Context context) {
        this.contextWeakReference = new WeakReference<>(context);
        createDialog();
    }

    private void createDialog() {
        Context context = contextWeakReference.get();
        if (context != null) {
            this.dialog = DialogUtils.getProgressDialog("Backing up database ...", context);
            dialog.setCancelable(false);
        }
    }

    @Override
    public void run() {
        Context context = contextWeakReference.get();
        if (context == null) {
            return;
        }
        Completable.fromAction(() -> DatabaseFactory.getInstance().backupDatabase())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        dialog.show();
                    }

                    @Override
                    public void onComplete() {
                        dialog.dismiss();
                        new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                                .setTitle("Info")
                                .setMessage("Backup database is finished.")
                                .show();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dialog.dismiss();
                        LogUtils.error(e.getMessage(), e);
                        new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                                .setTitle("Error")
                                .setMessage(e.getMessage())
                                .show();
                    }
                });
    }
}
