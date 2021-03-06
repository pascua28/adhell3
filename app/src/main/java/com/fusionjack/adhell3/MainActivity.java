package com.fusionjack.adhell3;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.fusionjack.adhell3.dialogfragment.ActivationDialogFragment;
import com.fusionjack.adhell3.fragments.AppTabFragment;
import com.fusionjack.adhell3.fragments.DomainTabFragment;
import com.fusionjack.adhell3.fragments.HomeTabFragment;
import com.fusionjack.adhell3.fragments.OtherTabFragment;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.CrashHandler;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.dialog.EnterPasswordDialog;
import com.fusionjack.adhell3.utils.dialog.HostsFileDialog;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import static com.fusionjack.adhell3.fragments.SettingsFragment.SET_NIGHT_MODE_PREFERENCE;

public class MainActivity extends AppCompatActivity {
    private static final String BACK_STACK_TAB_TAG = "tab_fragment";

    private FragmentManager fragmentManager;
    private int selectedTabId = -1;
    private boolean doubleBackToExitPressedOnce = false;

    private ActivationDialogFragment activationDialogFragment;

    @Override
    public void onBackPressed() {
        int count = fragmentManager.getBackStackEntryCount();
        if (count <= 1) {
            if (doubleBackToExitPressedOnce) {
                finish();
            }

            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Press once again to exit", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
        } else {
            fragmentManager.popBackStackImmediate();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isNightMode = sharedPreferences.getBoolean(SET_NIGHT_MODE_PREFERENCE, false);
        AppCompatDelegate.setDefaultNightMode(isNightMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);

        // Remove elevation shadow of ActionBar
        getSupportActionBar().setElevation(0);

        // Change status bar icon tint based on theme
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(isNightMode ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        // Set the crash handler to log crash's stack trace into a file
        if (!(Thread.getDefaultUncaughtExceptionHandler() instanceof CrashHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler.getInstance());
        }

        fragmentManager = getSupportFragmentManager();
        activationDialogFragment = new ActivationDialogFragment();
        activationDialogFragment.setCancelable(false);

        // Early exit if the device doesn't support Knox
        if (!DeviceAdminInteractor.getInstance().isSupported()) {
            LogUtils.info("Device not supported");
            AdhellFactory.getInstance().createNotSupportedDialog(this);
            return;
        }

        setContentView(R.layout.activity_main);

        BottomNavigationView bottomBar = findViewById(R.id.bottomBar);
        bottomBar.setOnNavigationItemSelectedListener(item -> {
            onTabSelected(item.getItemId());
            return true;
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            fragmentManager.popBackStack();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Show password dialog if password has been set and wait until the user enter the password
        if (showPasswordDialog()) {
            return;
        }

        // Check whether Knox is still valid. Show activation dialog if it is not valid anymore.
        if (!isKnoxValid()) {
            return;
        }

        LogUtils.info("Everything is okay");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EnterPasswordDialog.destroy();
        activationDialogFragment = null;
        HostsFileDialog.destroy();
        LogUtils.info("onDestroy()");
    }

    private void onTabSelected(int tabId) {
        fragmentManager.popBackStack(BACK_STACK_TAB_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        Fragment replacing;
        if (tabId == R.id.homeTab) {
            selectedTabId = R.id.homeTab;
            replacing = new HomeTabFragment();
            LogUtils.info( "Home tab is selected");
        } else if (tabId == R.id.appsManagementTab) {
            selectedTabId = R.id.appsManagementTab;
            replacing = new AppTabFragment();
            LogUtils.info( "App tab is selected");
        } else if (tabId == R.id.domainsTab) {
            selectedTabId = R.id.domainsTab;
            replacing = new DomainTabFragment();
            LogUtils.info( "Domain tab is selected");
        } else if (tabId == R.id.othersTab) {
            selectedTabId = R.id.othersTab;
            replacing = new OtherTabFragment();
            LogUtils.info( "Other tab is selected");
        } else {
            selectedTabId = -1;
            replacing = new HomeTabFragment();
        }

        fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, replacing)
                .addToBackStack(BACK_STACK_TAB_TAG)
                .commit();
    }

    private boolean showPasswordDialog() {
        String passwordHash = AppPreferences.getInstance().getPasswordHash();
        if (!passwordHash.isEmpty()) {
            EnterPasswordDialog.getInstance(findViewById(android.R.id.content), this::isKnoxValid).show();
            return true;
        }
        return false;
    }

    private boolean isKnoxValid() {
        if (!DeviceAdminInteractor.getInstance().isAdminActive()) {
            LogUtils.info( "Admin is not active, showing activation dialog");
            if (!isActivationDialogVisible()) {
                activationDialogFragment.show(fragmentManager, ActivationDialogFragment.DIALOG_TAG);
            }
            return false;
        }

        if (!DeviceAdminInteractor.getInstance().isKnoxEnabled(this)) {
            LogUtils.info( "Knox is disabled, showing activation dialog");
            boolean hasInternetAccess = AdhellFactory.getInstance().hasInternetAccess(this);
            if (!hasInternetAccess) {
                AdhellFactory.getInstance().createNoInternetConnectionDialog(this);
            } else if (!isActivationDialogVisible()) {
                activationDialogFragment.show(fragmentManager, ActivationDialogFragment.DIALOG_TAG);
            }
            return false;
        }

        // Select the Home tab manually if nothing is selected
        if (selectedTabId == -1) {
            onTabSelected(R.id.homeTab);
        }

        return true;
    }

    private boolean isActivationDialogVisible() {
        Fragment activationDialog = getSupportFragmentManager().findFragmentByTag(ActivationDialogFragment.DIALOG_TAG);
        return activationDialog != null;
    }
    
}
