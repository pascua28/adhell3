package com.fusionjack.adhell3.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.fusionjack.adhell3.MainActivity;
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
            public void onSubscribe(@NonNull Disposable d) {
            }

            @Override
            public void onSuccess(@NonNull String item) {
                if (item.indexOf('|') == -1) {
                    if (getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.makeSnackbar("Domain has been added", Snackbar.LENGTH_SHORT)
                                .show();
                    }
                } else {
                    if (getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.makeSnackbar("Rule has been added", Snackbar.LENGTH_SHORT)
                                .show();
                    }
                }
            }

            @Override
            public void onError(@NonNull Throwable e) {
                if (e.getMessage() != null) {
                    if (getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.makeSnackbar(e.getMessage(), Snackbar.LENGTH_SHORT)
                                .show();
                    }
                }
            }
        };

        deleteObserver = new SingleObserver<String>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
            }

            @Override
            public void onSuccess(@NonNull String item) {
                if (item.indexOf('|') == -1) {
                    if (getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.makeSnackbar("Domain has been removed", Snackbar.LENGTH_SHORT)
                                .show();
                    }
                } else {
                    if (getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.makeSnackbar("Rule has been removed", Snackbar.LENGTH_SHORT)
                                .show();
                    }
                }
            }

            @Override
            public void onError(@NonNull Throwable e) {
                if (e.getMessage() != null) {
                    if (getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.makeSnackbar(e.getMessage(), Snackbar.LENGTH_SHORT)
                                .show();
                    }
                }
            }
        };
    }
}
