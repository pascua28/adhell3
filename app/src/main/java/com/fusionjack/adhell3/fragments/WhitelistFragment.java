package com.fusionjack.adhell3.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.rx.RxSingleIoBuilder;
import com.fusionjack.adhell3.viewmodel.UserListViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import toan.android.floatingactionmenu.FloatingActionButton;
import toan.android.floatingactionmenu.FloatingActionsMenu;
import toan.android.floatingactionmenu.ScrollDirectionListener;

public class WhitelistFragment extends UserListFragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_whitelist, container, false);

        List<String> items = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, items);
        UserListViewModel viewModel = new ViewModelProvider(this, new UserListViewModel.WhiteListFactory()).get(UserListViewModel.class);

        Consumer<LiveData<List<String>>> callback = liveData -> {
            if (getView() == null) {
                LogUtils.error("View is null");
                return;
            }
            liveData.observe(getViewLifecycleOwner(), whiteItems -> {
                items.clear();
                items.addAll(whiteItems);
                adapter.notifyDataSetChanged();
            });
        };
        new RxSingleIoBuilder().async(viewModel.getItems(), callback);

        ListView whiteListView = view.findViewById(R.id.whiteListView);
        whiteListView.setAdapter(adapter);
        whiteListView.setOnItemClickListener((parent, view1, position, id) -> {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_question, parent, false);
            TextView titlTextView = dialogView.findViewById(R.id.titleTextView);
            titlTextView.setText(R.string.delete_domain_dialog_title);
            TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
            questionTextView.setText(R.string.delete_domain_dialog_text);

            new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        String item = (String) parent.getItemAtPosition(position);
                        viewModel.removeItem(item, deleteObserver);
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        });

        FloatingActionsMenu whiteFloatMenu = view.findViewById(R.id.whitelist_actions);
        whiteFloatMenu.attachToListView(whiteListView, new ScrollDirectionListener() {
            @Override
            public void onScrollDown() {
                whiteFloatMenu.setVisibleWithAnimation(true);
            }
            @Override
            public void onScrollUp() {
                whiteFloatMenu.setVisibleWithAnimation(false);
            }
        });

        FloatingActionButton actionAddWhiteDomain = view.findViewById(R.id.action_add_domain);
        actionAddWhiteDomain.setIcon(R.drawable.ic_add_domain);
        actionAddWhiteDomain.setOnClickListener(v -> {
            whiteFloatMenu.collapse();
            View dialogView = inflater.inflate(R.layout.dialog_whitelist_domain, container, false);
            new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        EditText domainEditText = dialogView.findViewById(R.id.domainEditText);
                        String domainToAdd = domainEditText.getText().toString();
                        try {
                            viewModel.validateDomain(domainToAdd);
                            viewModel.addItem(domainToAdd, addObserver);
                        } catch (IllegalArgumentException e) {
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        });
        return view;
    }
}
