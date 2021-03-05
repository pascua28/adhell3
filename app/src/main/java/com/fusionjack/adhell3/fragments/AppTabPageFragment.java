package com.fusionjack.adhell3.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.tasks.ToggleAppInfoRxTask;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.dialog.QuestionDialogBuilder;
import com.fusionjack.adhell3.utils.rx.RxCompletableIoBuilder;
import com.fusionjack.adhell3.viewmodel.AppViewModel;

import io.reactivex.Completable;

import static com.fusionjack.adhell3.db.repository.AppRepository.Type.UNKNOWN;

public class AppTabPageFragment extends AppFragment {
    private static final String ARG_PAGE = "page";
    private int page;

    public static final int PACKAGE_DISABLER_PAGE = 0;
    public static final int MOBILE_RESTRICTER_PAGE = 1;
    public static final int WIFI_RESTRICTER_PAGE = 2;
    public static final int WHITELIST_PAGE = 3;

    public static AppTabPageFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        AppTabPageFragment fragment = new AppTabPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected AppRepository.Type getType() {
        AppRepository.Type type = UNKNOWN;
        Bundle bundle = getArguments();
        if (bundle != null) {
            this.page = bundle.getInt(ARG_PAGE);
            switch (page) {
                case PACKAGE_DISABLER_PAGE:
                    type = AppRepository.Type.DISABLER;
                    break;

                case MOBILE_RESTRICTER_PAGE:
                    type = AppRepository.Type.MOBILE_RESTRICTED;
                    break;

                case WIFI_RESTRICTER_PAGE:
                    type = AppRepository.Type.WIFI_RESTRICTED;
                    break;

                case WHITELIST_PAGE:
                    type = AppRepository.Type.WHITELISTED;
                    break;
            }
        }
        return type;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = null;
        switch (page) {
            case PACKAGE_DISABLER_PAGE:
                view = inflateFragment(R.layout.fragment_package_disabler, inflater, container, AppFlag.createDisablerFlag());
                break;

            case MOBILE_RESTRICTER_PAGE:
                view = inflateFragment(R.layout.fragment_mobile_restricter, inflater, container, AppFlag.createMobileRestrictedFlag());
                break;

            case WIFI_RESTRICTER_PAGE:
                view = inflateFragment(R.layout.fragment_wifi_restricter, inflater, container, AppFlag.createWifiRestrictedFlag());
                break;

            case WHITELIST_PAGE:
                view = inflateFragment(R.layout.fragment_whitelisted_app, inflater, container, AppFlag.createWhitelistedFlag());
                break;
        }
        return view;
    }

    @Override
    protected void listOnItemClickListener(AdapterView<?> adView, View view2, int position, long id, AppFlag appFlag) {
        if (page != PACKAGE_DISABLER_PAGE || AppPreferences.getInstance().isAppDisablerToggleEnabled()) {
            AppInfoAdapter adapter = (AppInfoAdapter) adView.getAdapter();
            ToggleAppInfoRxTask.run(adapter.getItem(position), appFlag, adapter);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_enable_all) {
            enableAllPackages();
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableAllPackages() {
        Runnable onPositiveButton = () -> {
            Runnable callback = () -> Toast.makeText(getContext(), getString(R.string.enabled_all_apps), Toast.LENGTH_SHORT).show();
            new RxCompletableIoBuilder().async(Completable.fromAction(this::enableRespectiveApps), callback);
        };
        new QuestionDialogBuilder(getView())
                .setTitle(R.string.enable_apps_dialog_title)
                .setQuestion(R.string.enable_apps_dialog_text)
                .show(onPositiveButton);
    }

    private void enableRespectiveApps() {
        AppViewModel viewModel = new ViewModelProvider(this).get(AppViewModel.class);
        switch (page) {
            case PACKAGE_DISABLER_PAGE:
                viewModel.enableAllDisablerApps();
                break;

            case MOBILE_RESTRICTER_PAGE:
                viewModel.enableAllMobileApps();
                break;

            case WIFI_RESTRICTER_PAGE:
                viewModel.enableAllWifiApps();
                break;

            case WHITELIST_PAGE:
                viewModel.enableAllWhitelistApps();
                break;
        }
    }
}
