package com.fusionjack.adhell3.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.google.android.material.snackbar.Snackbar;

import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;

public abstract class UserListFragment extends Fragment {
    Context context;
    SingleObserver<String> addObserver;
    SingleObserver<String> deleteObserver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.context = getContext();

        addObserver = new SingleObserver<String>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onSuccess(String item) {
                if (item.indexOf('|') == -1) {
                    Snackbar.make(MainActivity.getAppRootView(), "Domain has been added", Snackbar.LENGTH_SHORT)
                            .setAnchorView(R.id.bottomBar)
                            .show();
                } else {
                    Snackbar.make(MainActivity.getAppRootView(), "Rule has been added", Snackbar.LENGTH_SHORT)
                            .setAnchorView(R.id.bottomBar)
                            .show();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (e.getMessage() != null) {
                    Snackbar.make(MainActivity.getAppRootView(), e.getMessage(), Snackbar.LENGTH_SHORT)
                            .setAnchorView(R.id.bottomBar)
                            .show();
                }
            }
        };

        deleteObserver = new SingleObserver<String>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onSuccess(String item) {
                if (item.indexOf('|') == -1) {
                    Snackbar.make(MainActivity.getAppRootView(), "Domain has been removed", Snackbar.LENGTH_SHORT)
                            .setAnchorView(R.id.bottomBar)
                            .show();
                } else {
                    Snackbar.make(MainActivity.getAppRootView(), "Rule has been removed", Snackbar.LENGTH_SHORT)
                            .setAnchorView(R.id.bottomBar)
                            .show();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (e.getMessage() != null) {
                    Snackbar.make(MainActivity.getAppRootView(), e.getMessage(), Snackbar.LENGTH_SHORT)
                            .setAnchorView(R.id.bottomBar)
                            .show();
                }
            }
        };
    }
}
