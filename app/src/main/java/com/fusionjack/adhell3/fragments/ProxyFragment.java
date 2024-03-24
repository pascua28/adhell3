package com.fusionjack.adhell3.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.StaticProxyInfoAdapter;
import com.fusionjack.adhell3.db.entity.StaticProxy;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.dialog.LayoutDialogBuilder;
import com.fusionjack.adhell3.utils.rx.RxSingleIoBuilder;
import com.fusionjack.adhell3.viewmodel.StaticProxyViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import toan.android.floatingactionmenu.FloatingActionButton;
import toan.android.floatingactionmenu.FloatingActionsMenu;
import toan.android.floatingactionmenu.ScrollDirectionListener;

public class ProxyFragment extends Fragment {
    protected Context context;
    protected SingleObserver<StaticProxy> addObserver;
    protected SingleObserver<String> deleteObserver;

    protected SingleObserver<String> renameObserver;

    protected SingleObserver<StaticProxy> updateObserver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = getContext();

        addObserver = new SingleObserver<StaticProxy>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onSuccess(StaticProxy item) {
                Toast.makeText(context, "Proxy has been added", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        deleteObserver = new SingleObserver<String>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onSuccess(String item) {
                Toast.makeText(context, "Proxy has been deleted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        renameObserver = new SingleObserver<String>() {
            @Override
            public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
            }

            @Override
            public void onSuccess(@io.reactivex.rxjava3.annotations.NonNull String s) {
            }

            @Override
            public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        updateObserver = new SingleObserver<StaticProxy>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onSuccess(StaticProxy item) {
                Toast.makeText(context, "Proxy has been edited", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_proxy, container, false);

        StaticProxyViewModel viewModel = new ViewModelProvider(this).get(StaticProxyViewModel.class);

        Consumer<View> onPositiveButton = (dialogView) -> {
            TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
            TextView originNameTextView = dialogView.findViewById(R.id.originName);
            EditText nameEditText = dialogView.findViewById(R.id.nameEditText);
            EditText hostnameEditText = dialogView.findViewById(R.id.hostnameEditText);
            EditText portEditText = dialogView.findViewById(R.id.portEditText);
            EditText exclusionEditText = dialogView.findViewById(R.id.exclusionEditText);
            EditText userEditText = dialogView.findViewById(R.id.userEditText);
            EditText passwordEditText = dialogView.findViewById(R.id.passwordEditText);

            String originName = originNameTextView.getText().toString();
            String name = nameEditText.getText().toString();
            if (portEditText.getText().toString().isEmpty()) {
                Toast.makeText(context, "Port cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            String hostname = hostnameEditText.getText().toString();
            int port = Integer.parseInt(portEditText.getText().toString());
            String exclusionList = exclusionEditText.getText().toString();
            String user = userEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (!originName.isEmpty() && !originName.equals(name)) {
                try {
                    StaticProxy staticProxy = new StaticProxy(name, hostname, port, exclusionList, user, password);
                    viewModel.updateItem(originName, staticProxy, updateObserver);
                } catch (IllegalArgumentException e) {
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                try {
                    StaticProxy staticProxy = new StaticProxy(name, hostname, port, exclusionList, user, password);

                    if (titleTextView.getText().toString().equals(getString(R.string.dialog_static_proxy_title))) {
                        viewModel.addItem(staticProxy, addObserver);
                    } else {
                        viewModel.updateItem(staticProxy.name, staticProxy, updateObserver);
                    }

                } catch (IllegalArgumentException e) {
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        List<StaticProxy> items = new ArrayList<>();
        StaticProxyInfoAdapter adapter = new StaticProxyInfoAdapter(context, items, onPositiveButton);

        Consumer<LiveData<List<StaticProxy>>> callback = liveData -> {
            if (getView() == null) {
                LogUtils.error("View is null");
                return;
            }
            liveData.observe(getViewLifecycleOwner(), proxyItems -> {
                items.clear();
                items.addAll(proxyItems);
                adapter.notifyDataSetChanged();
            });
        };
        new RxSingleIoBuilder().async(viewModel.getItems(), callback);

        ListView proxylistView = view.findViewById(R.id.proxyListView);
        proxylistView.setAdapter(adapter);

        FloatingActionsMenu proxyFloatMenu = view.findViewById(R.id.proxy_actions);

        proxyFloatMenu.attachToListView(proxylistView, new ScrollDirectionListener() {
            @Override
            public void onScrollDown() {
                proxyFloatMenu.setVisibleWithAnimation(true);
            }
            @Override
            public void onScrollUp() {
                proxyFloatMenu.setVisibleWithAnimation(false);
            }
        });

        FloatingActionButton actionAddProxy = view.findViewById(R.id.action_add_proxy);
        actionAddProxy.setOnClickListener(v -> {
            proxyFloatMenu.collapse();

            new LayoutDialogBuilder(getView())
                    .setLayout(R.layout.dialog_static_proxy)
                    .show(onPositiveButton);
        });

        return view;
    }
}
