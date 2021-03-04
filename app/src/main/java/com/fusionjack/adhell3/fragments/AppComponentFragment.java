package com.fusionjack.adhell3.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.utils.AppComponentFactory;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.UiUtils;
import com.fusionjack.adhell3.utils.rx.RxCompletableIoBuilder;
import com.fusionjack.adhell3.utils.rx.RxSingleIoBuilder;
import com.fusionjack.adhell3.viewmodel.AppComponentViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class AppComponentFragment extends AppFragment {

    private boolean isWarningShown;
    private boolean isDisabledComponentMode;

    @Override
    protected AppRepository.Type getType() {
        return AppRepository.Type.COMPONENT;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        this.isWarningShown = false;
        this.isDisabledComponentMode = false;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflateFragment(R.layout.fragment_app_component, inflater, container, AppFlag.createComponentFlag());

        if (BuildConfig.SHOW_SYSTEM_APP_COMPONENT && !isWarningShown) {
            View rootView = view.findViewById(R.id.appComponentCoordinatorLayout);
            Snackbar snackbar = Snackbar.make(rootView, R.string.dialog_system_app_components_info, Snackbar.LENGTH_LONG);
            View snackBarView = snackbar.getView();
            snackBarView.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.round_corner, null));
            TextView snackTextView = snackBarView.findViewById(com.google.android.material.R.id.snackbar_text);
            snackTextView.setMaxLines(3);
            snackbar.setDuration(10000);
            snackbar.setAction("Close", v -> snackbar.dismiss());
            snackbar.show();
            isWarningShown = true;
        }

        return view;
    }

    @Override
    protected void listOnItemClickListener(AdapterView<?> adView, View view2, int position, long id, AppFlag appFlag) {
        AppInfoAdapter adapter = (AppInfoAdapter) adView.getAdapter();
        AppInfo appInfo = adapter.getItem(position);

        Consumer<List<Integer>> callback = dbTypes -> {
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();

            Bundle bundle = new Bundle();
            bundle.putString("packageName", appInfo.packageName);
            bundle.putString("appName", appInfo.appName);
            bundle.putBoolean("isDisabledComponentMode", isDisabledComponentMode);

            if (dbTypes != null) {
                int[] pages = ComponentTabPageFragment.toPages(dbTypes);
                LogUtils.info("pages: " + Arrays.toString(pages));
                bundle.putIntArray("pages", pages);
            }

            ComponentTabFragment fragment = new ComponentTabFragment();
            fragment.setArguments(bundle);

            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragmentContainer, fragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        };

        if (isDisabledComponentMode) {
            AppComponentViewModel viewModel = new ViewModelProvider(this).get(AppComponentViewModel.class);
            new RxSingleIoBuilder().async(viewModel.getComponentTypes(appInfo.packageName), callback);
        } else {
            callback.accept(null);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        Optional.ofNullable(menu.findItem(R.id.action_options)).ifPresent(item -> {
            SubMenu subMenu = item.getSubMenu();
            subMenu.add(0, R.id.action_batch, Menu.NONE, R.string.menu_batch);
            subMenu.add(0, R.id.action_show_disabled_only, Menu.NONE, R.string.menu_show_disabled_only);
            Optional.ofNullable(menu.findItem(R.id.action_batch)).ifPresent(it -> it.setIcon(R.drawable.ic_batch));
            Optional.ofNullable(menu.findItem(R.id.action_show_disabled_only)).ifPresent(this::initDisabledOnlyMenu);
        });

        UiUtils.setMenuIconColor(menu, getContext());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_enable_all) {
            enableAllAppComponents();
        } else if (itemId == R.id.action_batch) {
            batchOperation();
        } else if (itemId == R.id.action_show_disabled_only) {
            toggleDisabledOnlyMode(item);
        }
        return super.onOptionsItemSelected(item);
    }

    private void initDisabledOnlyMenu(MenuItem item) {
        if (isDisabledComponentMode) {
            item.setTitle(R.string.menu_show_all);
            item.setIcon(R.drawable.ic_show_all);
            showDisabledOnly();
        } else {
            item.setTitle(R.string.menu_show_disabled_only);
            item.setIcon(R.drawable.ic_show_disabled_only);
        }
        UiUtils.tintMenuIcon(item, getContext());
    }

    private void toggleDisabledOnlyMode(MenuItem item) {
        isDisabledComponentMode = !isDisabledComponentMode;
        initDisabledOnlyMenu(item);
        if (isDisabledComponentMode) {
            showDisabledOnly();
        } else {
            restoreAppList();
        }
    }

    private void showDisabledOnly() {
        AppComponentViewModel viewModel = new ViewModelProvider(this).get(AppComponentViewModel.class);
        Consumer<List<AppInfo>> callback = this::setCurrentAppList;
        new RxSingleIoBuilder().async(viewModel.getDisabledComponentApps(), callback);
    }

    private void batchOperation() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
        TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
        titleTextView.setText(R.string.dialog_appcomponent_batch_title);
        TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
        questionTextView.setText(R.string.dialog_appcomponent_batch_summary);

        Runnable callback = () -> Toast.makeText(getContext(), "Success", Toast.LENGTH_LONG).show();
        new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton(R.string.button_enable, (dialog, whichButton) -> {
                    new RxCompletableIoBuilder()
                            .setShowDialog(getString(R.string.dialog_appcomponent_enable_summary), getContext())
                            .async(AppComponentFactory.getInstance().processAppComponentInBatch(true), callback);
                })
                .setNegativeButton(R.string.button_disable, (dialog, whichButton) -> {
                    new RxCompletableIoBuilder()
                            .setShowDialog(getString(R.string.dialog_appcomponent_disable_summary), getContext())
                            .async(AppComponentFactory.getInstance().processAppComponentInBatch(false), callback);
                })
                .setNeutralButton(android.R.string.no, null).show();
    }

    private void enableAllAppComponents() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
        TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
        titleTextView.setText(R.string.dialog_enable_components_title);
        TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
        questionTextView.setText(R.string.dialog_enable_components_info);
        new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    Runnable callback = () -> {
                        if (isDisabledComponentMode) {
                            showDisabledOnly();
                        }
                    };
                    AppComponentViewModel viewModel = new ViewModelProvider(this).get(AppComponentViewModel.class);
                    new RxCompletableIoBuilder().async(viewModel.enableAllAppComponents(), callback);
                })
                .setNegativeButton(android.R.string.no, null).show();
    }
}
