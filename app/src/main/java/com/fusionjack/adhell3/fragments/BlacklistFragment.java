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
import com.fusionjack.adhell3.utils.BlockUrlPatternsMatch;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.rx.RxSingleIoBuilder;
import com.fusionjack.adhell3.viewmodel.UserListViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Consumer;

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
                items.clear();
                items.addAll(blackItems);
                adapter.notifyDataSetChanged();
            });
        };
        new RxSingleIoBuilder().async(viewModel.getItems(), callback);

        ListView blacklistView = view.findViewById(R.id.blackListView);
        blacklistView.setAdapter(adapter);
        blacklistView.setOnItemClickListener((parent, view1, position, id) -> {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_question, parent, false);
            TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
            titleTextView.setText(R.string.delete_domain_firewall_dialog_title);
            TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
            questionTextView.setText(R.string.delete_domain_firewall_dialog_text);

            new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        String item = (String) parent.getItemAtPosition(position);
                        viewModel.removeItem(item, deleteObserver);
                    })
                    .setNegativeButton(android.R.string.no, null).show();
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
            View dialogView = inflater.inflate(R.layout.dialog_blacklist_domain, container, false);
            new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        EditText domainEditText = dialogView.findViewById(R.id.domainEditText);
                        String domainToAdd = domainEditText.getText().toString().trim().toLowerCase();
                        if (!BlockUrlPatternsMatch.isUrlValid(domainToAdd)) {
                            Toast.makeText(context, "Url not valid. Please check", Toast.LENGTH_SHORT).show();
                        } else {
                            viewModel.addItem(domainToAdd, addObserver);
                        }
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        });

        FloatingActionButton actionAddFirewallRule = view.findViewById(R.id.action_add_firewall_rule);
        actionAddFirewallRule.setOnClickListener(v -> {
            blackFloatMenu.collapse();
            View dialogView = inflater.inflate(R.layout.dialog_blacklist_rule, container, false);
            new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        EditText ruleEditText = dialogView.findViewById(R.id.ruleEditText);
                        String ruleToAdd = ruleEditText.getText().toString().trim().toLowerCase();
                        StringTokenizer tokens = new StringTokenizer(ruleToAdd, "|");
                        if (tokens.countTokens() != 3) {
                            Toast.makeText(context, "Rule not valid. Please check", Toast.LENGTH_SHORT).show();
                        } else {
                            viewModel.addItem(ruleToAdd, addObserver);
                        }
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        });

        return view;
    }
}
