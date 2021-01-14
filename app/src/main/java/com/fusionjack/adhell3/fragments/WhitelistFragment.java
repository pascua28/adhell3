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
import androidx.lifecycle.ViewModelProvider;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.utils.BlockUrlPatternsMatch;
import com.fusionjack.adhell3.viewmodel.UserListViewModel;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class WhitelistFragment extends UserListFragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_whitelist, container, false);

        List<String> items = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, items);
        UserListViewModel viewModel = new ViewModelProvider(this, new UserListViewModel.WhiteListFactory()).get(UserListViewModel.class);
        viewModel.getItems().observe(getViewLifecycleOwner(), whiteItems -> {
            items.clear();
            items.addAll(whiteItems);
            adapter.notifyDataSetChanged();
        });

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
        FloatingActionButton actionAddWhiteDomain = view.findViewById(R.id.action_add_domain);
        actionAddWhiteDomain.setIcon(R.drawable.ic_public_white_24dp);
        actionAddWhiteDomain.setOnClickListener(v -> {
            whiteFloatMenu.collapse();
            View dialogView = inflater.inflate(R.layout.dialog_whitelist_domain, container, false);
            new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        EditText domainEditText = dialogView.findViewById(R.id.domainEditText);
                        String domainToAdd = domainEditText.getText().toString().trim().toLowerCase();
                        if (domainToAdd.indexOf('|') == -1) {
                            if (!BlockUrlPatternsMatch.isUrlValid(domainToAdd)) {
                                Toast.makeText(this.getContext(), "Url not valid. Please check", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } else {
                            // packageName|url
                            StringTokenizer tokens = new StringTokenizer(domainToAdd, "|");
                            if (tokens.countTokens() != 2) {
                                Toast.makeText(this.getContext(), "Rule not valid. Please check", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        viewModel.addItem(domainToAdd, addObserver);
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        });
        return view;
    }
}
