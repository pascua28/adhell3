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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.utils.AppComponentFactory;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.UiUtils;
import com.fusionjack.adhell3.utils.dialog.AppComponentDialog;
import com.fusionjack.adhell3.utils.dialog.QuestionDialogBuilder;
import com.fusionjack.adhell3.utils.rx.RxCompletableIoBuilder;
import com.fusionjack.adhell3.utils.rx.RxSingleIoBuilder;
import com.fusionjack.adhell3.viewmodel.AppComponentViewModel;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class AppComponentFragment extends AppFragment {

    private boolean isDisabledComponentMode;

    @Override
    protected AppRepository.Type getType() {
        return AppRepository.Type.COMPONENT;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        this.isDisabledComponentMode = false;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflateFragment(R.layout.fragment_app_component, inflater, container, AppFlag.createComponentFlag());
        AppComponentDialog.getInstance(view).show();
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
        Runnable callback = () -> Toast.makeText(getContext(), "Success", Toast.LENGTH_LONG).show();
        Runnable onPositiveButton = () -> {
            new RxCompletableIoBuilder()
                    .setShowDialog(getString(R.string.dialog_appcomponent_enable_summary), getContext())
                    .async(AppComponentFactory.getInstance().processAppComponentInBatch(true), callback);
        };
        Runnable onNegativeButton = () -> {
            new RxCompletableIoBuilder()
                    .setShowDialog(getString(R.string.dialog_appcomponent_disable_summary), getContext())
                    .async(AppComponentFactory.getInstance().processAppComponentInBatch(false), callback);
        };
        new QuestionDialogBuilder(getView())
                .setTitle(R.string.dialog_appcomponent_batch_title)
                .setQuestion(R.string.dialog_appcomponent_batch_summary)
                .setPositiveButtonText(R.string.button_enable)
                .setNegativeButtonText(R.string.button_disable)
                .show(onPositiveButton, onNegativeButton, () -> {});
    }

    private void enableAllAppComponents() {
        Runnable onPositiveButton = () -> {
            Runnable callback = () -> {
                if (isDisabledComponentMode) {
                    showDisabledOnly();
                }
            };
            AppComponentViewModel viewModel = new ViewModelProvider(this).get(AppComponentViewModel.class);
            new RxCompletableIoBuilder().async(viewModel.enableAllAppComponents(), callback);
        };
        new QuestionDialogBuilder(getView())
                .setTitle(R.string.dialog_enable_components_title)
                .setQuestion(R.string.dialog_enable_components_info)
                .show(onPositiveButton);
    }
}
