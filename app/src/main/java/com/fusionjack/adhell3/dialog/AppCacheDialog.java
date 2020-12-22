package com.fusionjack.adhell3.dialog;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.widget.BaseAdapter;

import com.fusionjack.adhell3.utils.LogUtils;

import java.lang.ref.WeakReference;

import io.reactivex.CompletableObserver;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;

public class AppCacheDialog {

    private final WeakReference<Context> contextWeakReference;
    private ProgressDialog dialog;

    public AppCacheDialog(Context context) {
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

    public void showDialog() {
        if (dialog != null) {
            dialog.setMessage("Caching apps, please wait...");
            dialog.show();
        }
    }

    public void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public static CompletableObserver createObserver(Context context, BaseAdapter adapter) {
        final AppCacheDialog dialog = new AppCacheDialog(context);
        return new CompletableObserver() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                dialog.showDialog();
            }

            @Override
            public void onComplete() {
                dialog.dismissDialog();
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(@NonNull Throwable e) {
                dialog.dismissDialog();
                LogUtils.error(e.getMessage(), e);
                new AlertDialog.Builder(context)
                        .setTitle("Error")
                        .setMessage("Something went wrong when caching apps, please reopen adhell3. Error: \n\n" + e.getMessage())
                        .show();
            }
        };
    }
}
