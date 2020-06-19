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

public class WhitelistFragment extends UserListFragment {
    private ArrayAdapter<String> adapter;
    private UserListViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<String> items = new ArrayList<>();
        adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, items);
        viewModel = new ViewModelProvider(this, new UserListViewModel.WhiteListFactory()).get(UserListViewModel.class);
        viewModel.getItems().observe(this, whiteItems -> {
            items.clear();
            items.addAll(whiteItems);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_whitelist, container, false);

        ListView whiteListView = view.findViewById(R.id.whiteListView);
        whiteListView.setAdapter(adapter);
        whiteListView.setOnItemClickListener((parent, view1, position, id) -> {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_question, parent, false);
            TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
            titleTextView.setText(R.string.delete_domain_dialog_title);
            TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
            questionTextView.setText(R.string.delete_domain_dialog_text);

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

        SpeedDialView speedDialView = view.findViewById(R.id.whitelist_actions);
        speedDialView.addActionItem(new SpeedDialActionItem.Builder(R.id.action_add_domain, getResources().getDrawable(R.drawable.ic_public_white_24dp, requireContext().getTheme()))
                .setLabel(getString(R.string.dialog_whitelist_domain_title))
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
                View dialogView = inflater.inflate(R.layout.dialog_whitelist_domain, container, false);
                AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setView(dialogView)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            EditText domainEditText = dialogView.findViewById(R.id.domainEditText);
                            String domainToAdd = domainEditText.getText().toString().trim();
                            if (domainToAdd.indexOf('|') == -1) {
                                if (!BlockUrlPatternsMatch.isUrlValid(domainToAdd)) {
                                    Snackbar.make(MainActivity.getAppRootView(), "Url not valid. Please check", Snackbar.LENGTH_SHORT)
                                            .setAnchorView(R.id.bottomBar)
                                            .show();
                                    return;
                                }
                            } else {
                                // packageName|url
                                StringTokenizer tokens = new StringTokenizer(domainToAdd, "|");
                                if (tokens.countTokens() != 2) {
                                    Snackbar.make(MainActivity.getAppRootView(), "Rule not valid. Please check", Snackbar.LENGTH_SHORT)
                                            .setAnchorView(R.id.bottomBar)
                                            .show();
                                    return;
                                }
                            }
                            viewModel.addItem(domainToAdd, addObserver);
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create();

                alertDialog.show();
                return true;
            }
            return false;
        });

        view.findViewById(R.id.loadingBar).setVisibility(View.GONE);
        if (whiteListView.getVisibility() == View.GONE) {
            AlphaAnimation animation = new AlphaAnimation(0f, 1f);
            animation.setDuration(500);
            animation.setStartOffset(50);
            animation.setFillAfter(true);

            whiteListView.setVisibility(View.VISIBLE);
            whiteListView.startAnimation(animation);
        }

        return view;
    }
}
