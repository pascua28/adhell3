package com.fusionjack.adhell3.dialog;

import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.utils.DialogUtils;
import com.fusionjack.adhell3.utils.LogUtils;

import java.lang.ref.WeakReference;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.disposables.Disposable;


public class AppCacheDialog {

    private final WeakReference<Context> contextWeakReference;
    private AlertDialog dialog;

    public AppCacheDialog(Context context) {
        this.contextWeakReference = new WeakReference<>(context);
        createDialog("Caching apps, please wait...");
    }

    public AppCacheDialog(Context context, String message) {
        this.contextWeakReference = new WeakReference<>(context);
        createDialog(message);
    }

    private void createDialog(String message) {
        Context context = contextWeakReference.get();
        if (context != null) {
            dialog = DialogUtils.getProgressDialog(message, context);
            dialog.setCancelable(false);
        }
    }

    public void showDialog() {
        if (dialog != null) {
            dialog.show();
        }
    }

    public void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public static CompletableObserver createActivityObserver(Context context, FragmentManager fragmentManager) {
        final AppCacheDialog dialog = new AppCacheDialog(context);
        return new CompletableObserver() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                dialog.showDialog();
            }

            @Override
            public void onComplete() {
                dialog.dismissDialog();
                refreshVisibleFragment(fragmentManager);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                dialog.dismissDialog();
                refreshVisibleFragment(fragmentManager);
                LogUtils.error(e.getMessage(), e);
                new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setTitle("Error")
                        .setMessage("Something went wrong when caching apps, please reopen adhell3. Error: \n\n" + e.getMessage())
                        .show();
            }
        };
    }

    private static void refreshVisibleFragment(FragmentManager fragmentManager) {
        if (fragmentManager != null) {
            for (Fragment fragment: fragmentManager.getFragments()) {
                if (fragment != null && fragment.isVisible()) {
                    fragment.onResume();
                    break;
                }
            }
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
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(@NonNull Throwable e) {
                dialog.dismissDialog();
                LogUtils.error(e.getMessage(), e);
                new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setTitle("Error")
                        .setMessage("Something went wrong when caching apps, please reopen adhell3. Error: \n\n" + e.getMessage())
                        .show();
            }
        };
    }

    public static CompletableObserver createObserver(Context context, BaseExpandableListAdapter adapter) {
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
                new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setTitle("Error")
                        .setMessage("Something went wrong when caching apps, please reopen adhell3. Error: \n\n" + e.getMessage())
                        .show();
            }
        };
    }
}
