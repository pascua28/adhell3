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

public class AppCacheDialog extends ProgressDialog {

    public AppCacheDialog(Context context) {
        super(new WeakReference<>(context).get());
        setCancelable(false);
    }

    public void showDialog(String message) {
        setMessage(message);
        show();
    }

    public void dismissDialog() {
        if (isShowing()) {
            dismiss();
        }
    }

    public static CompletableObserver createObserver(Context context, BaseAdapter adapter) {
        final AppCacheDialog dialog = new AppCacheDialog(context);
        return new CompletableObserver() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                dialog.showDialog("Caching apps's info, please wait ...");
            }

            @Override
            public void onComplete() {
                dialog.dismissDialog();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
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
