package com.fusionjack.adhell3.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.utils.BlockUrlPatternsMatch;
import com.fusionjack.adhell3.viewmodel.UserListViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class BlacklistFragment extends UserListFragment {
    private ArrayAdapter<String> adapter;
    private UserListViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<String> items = new ArrayList<>();
        adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, items);

        viewModel = new ViewModelProvider(this, new UserListViewModel.BlackListFactory()).get(UserListViewModel.class);
        viewModel.getItems().observe(this, blackItems -> {
            items.clear();
            items.addAll(blackItems);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blacklist, container, false);

        ListView blacklistView = view.findViewById(R.id.blackListView);
        blacklistView.setAdapter(adapter);
        blacklistView.setOnItemClickListener((parent, view1, position, id) -> {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_question, parent, false);
            TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
            titleTextView.setText(R.string.delete_domain_firewall_dialog_title);
            TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
            questionTextView.setText(R.string.delete_domain_firewall_dialog_text);

            AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        String item = (String) parent.getItemAtPosition(position);
                        viewModel.removeItem(item, deleteObserver);
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .create();

            alertDialog.show();
        });

        SpeedDialView speedDialView = view.findViewById(R.id.blacklist_actions);

        speedDialView.addActionItem(new SpeedDialActionItem.Builder(R.id.action_add_domain, getResources().getDrawable(R.drawable.ic_public_white_24dp, requireContext().getTheme()))
                .setLabel(getString(R.string.dialog_blacklist_domain_title))
                .setFabBackgroundColor(getResources().getColor(R.color.colorFab, requireContext().getTheme()))
                .setLabelColor(getResources().getColor(R.color.colorText, requireContext().getTheme()))
                .setLabelBackgroundColor(getResources().getColor(R.color.colorBorder, requireContext().getTheme()))
                .setFabSize(com.google.android.material.floatingactionbutton.FloatingActionButton.SIZE_NORMAL)
                .setFabImageTintColor(Color.WHITE)
                .setLabelClickable(false)
                .create());

        speedDialView.addActionItem(new SpeedDialActionItem.Builder(R.id.action_add_firewall_rule, getResources().getDrawable(R.drawable.ic_whatshot_white_24dp, requireContext().getTheme()))
                .setLabel(getString(R.string.dialog_blacklist_rule_title))
                .setFabBackgroundColor(getResources().getColor(R.color.colorFab, requireContext().getTheme()))
                .setLabelColor(getResources().getColor(R.color.colorText, requireContext().getTheme()))
                .setLabelBackgroundColor(getResources().getColor(R.color.colorBorder, requireContext().getTheme()))
                .setFabSize(com.google.android.material.floatingactionbutton.FloatingActionButton.SIZE_NORMAL)
                .setFabImageTintColor(Color.WHITE)
                .setLabelClickable(false)
                .create());

        speedDialView.setOnActionSelectedListener(actionItem -> {
            speedDialView.close();
            if (actionItem.getId() == R.id.action_add_domain) {
                View dialogView = inflater.inflate(R.layout.dialog_blacklist_domain, container, false);
                AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setView(dialogView)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            EditText domainEditText = dialogView.findViewById(R.id.domainEditText);
                            String domainToAdd = domainEditText.getText().toString().trim();
                            if (!BlockUrlPatternsMatch.isUrlValid(domainToAdd)) {
                                Snackbar.make(MainActivity.getAppRootView(), "Url not valid. Please check", Snackbar.LENGTH_SHORT)
                                        .setAnchorView(R.id.bottomBar)
                                        .show();
                            } else {
                                viewModel.addItem(domainToAdd, addObserver);
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create();

                alertDialog.show();
                return true;
            } else if (actionItem.getId() == R.id.action_add_firewall_rule) {
                View dialogView = inflater.inflate(R.layout.dialog_blacklist_rule, container, false);
                AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setView(dialogView)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            EditText ruleEditText = dialogView.findViewById(R.id.ruleEditText);
                            String ruleToAdd = ruleEditText.getText().toString().trim();
                            StringTokenizer tokens = new StringTokenizer(ruleToAdd, "|");
                            if (tokens.countTokens() != 3) {
                                Snackbar.make(MainActivity.getAppRootView(), "Rule not valid. Please check", Snackbar.LENGTH_SHORT)
                                        .setAnchorView(R.id.bottomBar)
                                        .show();
                            } else {
                                viewModel.addItem(ruleToAdd, addObserver);
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create();

                alertDialog.show();
                return true;
            }
            return false;
        });

        view.findViewById(R.id.loadingBar).setVisibility(View.GONE);
        if (blacklistView.getVisibility() == View.GONE) {
            AlphaAnimation animation = new AlphaAnimation(0f, 1f);
            animation.setDuration(500);
            animation.setStartOffset(50);
            animation.setFillAfter(true);

            blacklistView.setVisibility(View.VISIBLE);
            blacklistView.startAnimation(animation);
        }

        return view;
    }
}
