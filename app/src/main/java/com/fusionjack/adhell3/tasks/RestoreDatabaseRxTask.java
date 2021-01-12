package com.fusionjack.adhell3.tasks;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppDatabaseFactory;
import com.fusionjack.adhell3.utils.DialogUtils;
import com.fusionjack.adhell3.utils.LogUtils;

import java.lang.ref.WeakReference;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class RestoreDatabaseRxTask implements Runnable {
    private final WeakReference<Context> contextWeakReference;
    private AlertDialog dialog;

    public RestoreDatabaseRxTask(Context context) {
        this.contextWeakReference = new WeakReference<>(context);
        createDialog();
    }

    private void createDialog() {
        Context context = contextWeakReference.get();
        if (context != null) {
            this.dialog = DialogUtils.getProgressDialog("Restoring database, please wait ...", context);
            dialog.setCancelable(false);
        }
    }

    @Override
    public void run() {
        Context context = contextWeakReference.get();
        if (context == null) {
            return;
        }

        boolean hasInternetAccess = AdhellFactory.getInstance().hasInternetAccess(context);
        Observable.create((ObservableOnSubscribe<String>) emitter -> {
            try {
                AppDatabaseFactory.restoreDatabase(emitter, hasInternetAccess);
            } catch (Exception e) {
                emitter.onError(e);
            }
            emitter.onComplete();
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        dialog.show();
                    }

                    @Override
                    public void onNext(@NonNull String message) {
                        DialogUtils.setProgressDialogMessage(dialog, message);
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

                    @Override
                    public void onComplete() {
                        dialog.dismiss();
                        new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                                .setTitle("Info")
                                .setMessage("Restore database is finished.\nGo to 'Home' tab and turn on Knox functionality.")
                                .show();
                    }
                });
    }
}
