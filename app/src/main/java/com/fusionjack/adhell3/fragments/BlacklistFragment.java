package com.fusionjack.adhell3.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.utils.BlockUrlPatternsMatch;
import com.fusionjack.adhell3.utils.dialog.LayoutDialogBuilder;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.dialog.QuestionDialogBuilder;
import com.fusionjack.adhell3.utils.rx.RxSingleIoBuilder;
import com.fusionjack.adhell3.viewmodel.UserListViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import toan.android.floatingactionmenu.FloatingActionButton;
import toan.android.floatingactionmenu.FloatingActionsMenu;
import toan.android.floatingactionmenu.ScrollDirectionListener;

public class BlacklistFragment extends UserListFragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blacklist, container, false);

        List<String> items = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, items);

        UserListViewModel viewModel = new ViewModelProvider(this, new UserListViewModel.BlackListFactory()).get(UserListViewModel.class);

        Consumer<LiveData<List<String>>> callback = liveData -> {
            if (getView() == null) {
                LogUtils.error("View is null");
                return;
            }
            liveData.observe(getViewLifecycleOwner(), blackItems -> {
                List<String> blackList = blackItems.stream().filter(i -> !i.contains("|")).collect(Collectors.toList());
                items.clear();
                items.addAll(blackList);
                adapter.notifyDataSetChanged();
            });
        };
        new RxSingleIoBuilder().async(viewModel.getItems(), callback);

        ListView blacklistView = view.findViewById(R.id.blackListView);
        blacklistView.setAdapter(adapter);
        blacklistView.setOnItemClickListener((parent, view1, position, id) -> {
            Runnable onPositiveButton = () -> {
                String item = (String) parent.getItemAtPosition(position);
                viewModel.removeItem(item, deleteObserver);
            };
            new QuestionDialogBuilder(getView())
                    .setTitle(R.string.delete_domain_dialog_title)
                    .setQuestion(R.string.delete_domain_dialog_text)
                    .show(onPositiveButton);
        });

        FloatingActionsMenu blackFloatMenu = view.findViewById(R.id.blacklist_actions);
        blackFloatMenu.attachToListView(blacklistView, new ScrollDirectionListener() {
            @Override
            public void onScrollDown() {
                blackFloatMenu.setVisibleWithAnimation(true);
            }
            @Override
            public void onScrollUp() {
                blackFloatMenu.setVisibleWithAnimation(false);
            }
        });

        FloatingActionButton actionAddBlackDomain = view.findViewById(R.id.action_add_domain);
        actionAddBlackDomain.setOnClickListener(v -> {
            blackFloatMenu.collapse();
            Consumer<View> onPositiveButton = dialogView -> {
                EditText domainEditText = dialogView.findViewById(R.id.domainEditText);
                String domainToAdd = domainEditText.getText().toString().trim().toLowerCase();
                if (!BlockUrlPatternsMatch.isUrlValid(domainToAdd)) {
                    Toast.makeText(context, "Url not valid. Please check", Toast.LENGTH_SHORT).show();
                } else {
                    viewModel.addItem(domainToAdd, addObserver);
                }
            };
            new LayoutDialogBuilder(getView())
                    .setLayout(R.layout.dialog_blacklist_domain)
                    .show(onPositiveButton);
        });

        return view;
    }
}
