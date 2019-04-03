package com.fusionjack.adhell3.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;

import java.util.Objects;

public class AppComponentFragment extends AppFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        type = AppRepository.Type.COMPONENT;

        initAppModel(type);

        if (BuildConfig.SHOW_SYSTEM_APP_COMPONENT && !AppPreferences.getInstance().getWarningDialogAppComponentDontShow()) {
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
            CheckBox dontShowAgain = dialogView.findViewById(R.id.questionDontShow);
            dontShowAgain.setVisibility(View.VISIBLE);
            TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
            titleTextView.setText(R.string.dialog_system_app_components_title);
            TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
            questionTextView.setText(R.string.dialog_system_app_components_info);
            new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, ((dialog, which) -> {
                        if (dontShowAgain.isChecked()) {
                            AppPreferences.getInstance().setWarningDialogAppComponentDontShow(true);
                        }
                    })).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_enable_all:
                enableAllAppComponents();
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableAllAppComponents() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
        TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
        titleTextView.setText(R.string.dialog_enable_components_title);
        TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
        questionTextView.setText(R.string.dialog_enable_components_info);
        new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                        AsyncTask.execute(() -> {
                            AdhellFactory.getInstance().setAppComponentState(true);
                            AdhellFactory.getInstance().getAppDatabase().appPermissionDao().deleteAll();
                        })
                )
                .setNegativeButton(android.R.string.no, null).show();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_app_component, container, false);
        ProgressBar loadingBar = view.findViewById(R.id.loadingBar);

        AppFlag appFlag = AppFlag.createComponentFlag();
        ListView listView = view.findViewById(appFlag.getLoadLayout());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
            AppInfoAdapter adapter = (AppInfoAdapter) adView.getAdapter();

            FragmentManager fragmentManager = Objects.requireNonNull(getActivity()).getSupportFragmentManager();
            Bundle bundle = new Bundle();
            AppInfo appInfo = adapter.getItem(position);
            bundle.putString("packageName", appInfo.packageName);
            bundle.putString("appName", appInfo.appName);
            ComponentTabFragment fragment = new ComponentTabFragment();
            fragment.setArguments(bundle);

            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragmentContainer, fragment);
            fragmentTransaction.addToBackStack("appComponents");
            fragmentTransaction.commit();
        });

        SwipeRefreshLayout swipeContainer = view.findViewById(appFlag.getRefreshLayout());
        swipeContainer.setOnRefreshListener(() -> {
            loadAppList(type, loadingBar, listView);
            swipeContainer.setRefreshing(false);
            resetSearchView();
        });

        loadAppList(type, loadingBar, listView);
        return view;
    }
}
