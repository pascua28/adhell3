package com.fusionjack.adhell3.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;

import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.databinding.DialogBlacklistDomainBinding;
import com.fusionjack.adhell3.databinding.DialogBlacklistRuleBinding;
import com.fusionjack.adhell3.databinding.DialogQuestionBinding;
import com.fusionjack.adhell3.databinding.FragmentBlacklistBinding;
import com.fusionjack.adhell3.utils.BlockUrlPatternsMatch;
import com.fusionjack.adhell3.viewmodel.UserListViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.base.Splitter;
import com.leinardi.android.speeddial.SpeedDialActionItem;

import java.util.ArrayList;
import java.util.List;

public class BlacklistFragment extends UserListFragment {
    private ArrayAdapter<String> adapter;
    private UserListViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<String> items = new ArrayList<>();
        adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, items);

        viewModel = new ViewModelProvider(getActivity() != null ? getActivity() : this, new UserListViewModel.BlackListFactory()).get(UserListViewModel.class);
        viewModel.getItems().observe(this, blackItems -> {
            items.clear();
            items.addAll(blackItems);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentBlacklistBinding binding = FragmentBlacklistBinding.inflate(inflater);

        binding.blackListView.setAdapter(adapter);
        binding.blackListView.setOnItemClickListener((parent, view1, position, id) -> {
            DialogQuestionBinding dialogQuestionBinding = DialogQuestionBinding.inflate(inflater);
            dialogQuestionBinding.titleTextView.setText(R.string.delete_domain_firewall_dialog_title);
            dialogQuestionBinding.questionTextView.setText(R.string.delete_domain_firewall_dialog_text);

            AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                    .setView(dialogQuestionBinding.getRoot())
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        String item = (String) parent.getItemAtPosition(position);
                        viewModel.removeItem(item, deleteObserver);
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .create();

            alertDialog.show();
        });

        binding.blacklistActions.addActionItem(new SpeedDialActionItem.Builder(R.id.action_add_domain, ResourcesCompat.getDrawable(getResources(), R.drawable.ic_public_white_24dp, requireContext().getTheme()))
                .setLabel(getString(R.string.dialog_blacklist_domain_title))
                .setFabBackgroundColor(getResources().getColor(R.color.colorFab, requireContext().getTheme()))
                .setLabelColor(getResources().getColor(R.color.colorText, requireContext().getTheme()))
                .setLabelBackgroundColor(getResources().getColor(R.color.colorBorder, requireContext().getTheme()))
                .setFabSize(com.google.android.material.floatingactionbutton.FloatingActionButton.SIZE_NORMAL)
                .setFabImageTintColor(Color.WHITE)
                .setLabelClickable(false)
                .create());

        binding.blacklistActions.addActionItem(new SpeedDialActionItem.Builder(R.id.action_add_firewall_rule, ResourcesCompat.getDrawable(getResources(), R.drawable.ic_whatshot_white_24dp, requireContext().getTheme()))
                .setLabel(getString(R.string.dialog_blacklist_rule_title))
                .setFabBackgroundColor(getResources().getColor(R.color.colorFab, requireContext().getTheme()))
                .setLabelColor(getResources().getColor(R.color.colorText, requireContext().getTheme()))
                .setLabelBackgroundColor(getResources().getColor(R.color.colorBorder, requireContext().getTheme()))
                .setFabSize(com.google.android.material.floatingactionbutton.FloatingActionButton.SIZE_NORMAL)
                .setFabImageTintColor(Color.WHITE)
                .setLabelClickable(false)
                .create());

        binding.blacklistActions.setOnActionSelectedListener(actionItem -> {
            binding.blacklistActions.close();
            if (actionItem.getId() == R.id.action_add_domain) {
                DialogBlacklistDomainBinding dialogBlacklistDomainBinding = DialogBlacklistDomainBinding.inflate(inflater);
                AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setView(dialogBlacklistDomainBinding.getRoot())
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            Editable domainEditText = dialogBlacklistDomainBinding.domainEditText.getText();
                            String domainToAdd = (domainEditText != null) ? domainEditText.toString().trim() : "";
                            if (!BlockUrlPatternsMatch.isUrlValid(domainToAdd)) {
                                if (getActivity() instanceof MainActivity) {
                                    MainActivity mainActivity = (MainActivity) getActivity();
                                    mainActivity.makeSnackbar("Url not valid. Please check", Snackbar.LENGTH_SHORT)
                                            .show();
                                }
                            } else {
                                viewModel.addItem(domainToAdd, addObserver);
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create();

                alertDialog.show();
                return true;
            } else if (actionItem.getId() == R.id.action_add_firewall_rule) {
                DialogBlacklistRuleBinding dialogBlacklistRuleBinding = DialogBlacklistRuleBinding.inflate(inflater);
                AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setView(dialogBlacklistRuleBinding.getRoot())
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            Editable ruleEditText = dialogBlacklistRuleBinding.ruleEditText.getText();
                            String ruleToAdd = (ruleEditText != null) ? ruleEditText.toString().trim() : "";
                            List<String> splittedRule = Splitter.on('|').omitEmptyStrings().trimResults().splitToList(ruleToAdd);
                            if (splittedRule.size() != 3) {
                                if (getActivity() instanceof MainActivity) {
                                    MainActivity mainActivity = (MainActivity) getActivity();
                                    mainActivity.makeSnackbar("Rule not valid. Please check", Snackbar.LENGTH_SHORT)
                                            .show();
                                }
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

        final boolean[] noScroll = { false };
        final int[] previousDistanceFromFirstCellToTop = {0};
        binding.blackListView.setOnScrollListener(new ExpandableListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL && noScroll[0]) {
                    if (binding.blacklistActions.isShown()) binding.blacklistActions.hide();
                    else binding.blacklistActions.show();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem == 0 && visibleItemCount == totalItemCount) {
                    noScroll[0] = true;
                } else {
                    noScroll[0] = false;
                    View firstCell = binding.blackListView.getChildAt(0);
                    if (firstCell == null) {
                        return;
                    }
                    int distanceFromFirstCellToTop = firstVisibleItem * firstCell.getHeight() - firstCell.getTop();
                    if (distanceFromFirstCellToTop < previousDistanceFromFirstCellToTop[0]) {
                        binding.blacklistActions.show();
                    } else if (distanceFromFirstCellToTop > previousDistanceFromFirstCellToTop[0]) {
                        binding.blacklistActions.hide();
                    }
                    previousDistanceFromFirstCellToTop[0] = distanceFromFirstCellToTop;
                }
            }
        });

        binding.loadingBar.setVisibility(View.GONE);
        if (binding.blackListView.getVisibility() == View.GONE) {
            AlphaAnimation animation = new AlphaAnimation(0f, 1f);
            animation.setDuration(500);
            animation.setStartOffset(50);
            animation.setFillAfter(true);

            binding.blackListView.setVisibility(View.VISIBLE);
            binding.blackListView.startAnimation(animation);
        }

        return binding.getRoot();
    }
}
