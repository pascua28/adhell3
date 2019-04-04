package com.fusionjack.adhell3;

import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fusionjack.adhell3.dialogfragment.ActivationDialogFragment;
import com.fusionjack.adhell3.fragments.AppTabFragment;
import com.fusionjack.adhell3.fragments.AppTabPageFragment;
import com.fusionjack.adhell3.fragments.DomainTabFragment;
import com.fusionjack.adhell3.fragments.DomainTabPageFragment;
import com.fusionjack.adhell3.fragments.HomeTabFragment;
import com.fusionjack.adhell3.fragments.OtherTabFragment;
import com.fusionjack.adhell3.fragments.OtherTabPageFragment;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.BiometricUtils;
import com.fusionjack.adhell3.utils.CrashHandler;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.PasswordStorage;

import java.util.Objects;

import static com.fusionjack.adhell3.fragments.SettingsFragment.SET_NIGHT_MODE_PREFERENCE;

public class MainActivity extends AppCompatActivity {
    private static final String BACK_STACK_TAB_TAG = "tab_fragment";
    private static boolean selectFileActivityLaunched = false;
    private int SELECTED_APP_TAB = AppTabPageFragment.PACKAGE_DISABLER_PAGE;
    private int SELECTED_DOMAIN_TAB = DomainTabPageFragment.PROVIDER_LIST_PAGE;
    private int SELECTED_OTHER_TAB = OtherTabPageFragment.APP_COMPONENT_PAGE;
    private FragmentManager fragmentManager;
    private ActivationDialogFragment activationDialogFragment;
    public AlertDialog passwordDialog;
    private BottomNavigationView bottomBar;
    private int selectedTabId = -1;
    private boolean doubleBackToExitPressedOnce = false;
    public String themeChange;
    private boolean biometricSupport;

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
        // Close Activity if it's not the root of the task
        if (!isTaskRoot()) {
            finish();
            return;
        }

        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        biometricSupport = BiometricUtils.checkBiometricSupport(this);
        getDelegate();
        if (mPrefs.getBoolean(SET_NIGHT_MODE_PREFERENCE, false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        super.onCreate(savedInstanceState);
        themeChange = getIntent().getStringExtra("settingsFragment");

        // Remove elevation shadow of ActionBar
        Objects.requireNonNull(getSupportActionBar()).setElevation(0);

        // Change status bar icon tint based on theme
        View decor = getWindow().getDecorView();
        if (!mPrefs.getBoolean(SET_NIGHT_MODE_PREFERENCE, false)) {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            decor.setSystemUiVisibility(0);
        }

        // Set the crash handler to log crash's stack trace into a file
        if (!(Thread.getDefaultUncaughtExceptionHandler() instanceof CrashHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler.getInstance());
        }

        fragmentManager = getSupportFragmentManager();
        activationDialogFragment = new ActivationDialogFragment();
        activationDialogFragment.setCancelable(false);
        passwordDialog = createPasswordDialog();

        // Early exit if the device doesn't support Knox
        if (!DeviceAdminInteractor.getInstance().isSupported()) {
            LogUtils.info("Device not supported");
            AdhellFactory.getInstance().createNotSupportedDialog(this);
            return;
        }

        setContentView(R.layout.activity_main);

        bottomBar = findViewById(R.id.bottomBar);
        bottomBar.setOnNavigationItemSelectedListener(item -> {
            onTabSelected(item.getItemId());
            return true;
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                fragmentManager.popBackStack();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!selectFileActivityLaunched) {
            // Show password dialog if password has been set and wait until the user enter the password
            if (isPasswordShowing()) {
                return;
            }

            // Check whether Knox is still valid. Show activation dialog if it is not valid anymore.
            if (!isKnoxValid()) {
                return;
            }
        } else {
            selectFileActivityLaunched = false;
        }

        LogUtils.info("Everything is okay");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtils.info("Destroying activity");
    }

    private void onTabSelected(int tabId) {
        LogUtils.info("Tab '" + tabId + "' is selected");
        fragmentManager.popBackStack(BACK_STACK_TAB_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        Fragment replacing;
        switch (tabId) {
            case R.id.homeTab:
                selectedTabId = R.id.homeTab;
                replacing = new HomeTabFragment();
                break;
            case R.id.appsManagementTab:
                selectedTabId = R.id.appsManagementTab;
                replacing = new AppTabFragment();
                break;
            case R.id.domainsTab:
                selectedTabId = R.id.domainsTab;
                replacing = new DomainTabFragment();
                break;
            case R.id.othersTab:
                selectedTabId = R.id.othersTab;
                replacing = new OtherTabFragment();
                if (themeChange != null) {
                    if (themeChange.matches(SET_NIGHT_MODE_PREFERENCE)) {
                        Bundle bundle = new Bundle();
                        bundle.putString("viewpager_position", "Settings");
                        replacing.setArguments(bundle);
                    }
                }
                break;
            default:
                selectedTabId = -1;
                replacing = new HomeTabFragment();
        }
        fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, replacing)
                .addToBackStack(BACK_STACK_TAB_TAG)
                .commit();
    }

    private boolean isPasswordShowing() {
        String passwordHash = AppPreferences.getInstance().getPasswordHash();
        if (!passwordHash.isEmpty()) {
            if (!passwordDialog.isShowing()) {
                LogUtils.info("Showing password dialog");
                passwordDialog.show();
            }
            return true;
        }
        return false;
    }

    public boolean isKnoxValid() {
        if (!DeviceAdminInteractor.getInstance().isAdminActive()) {
            LogUtils.info("Admin is not active, showing activation dialog");
            if (!isActivationDialogVisible()) {
                activationDialogFragment.show(fragmentManager, ActivationDialogFragment.DIALOG_TAG);
            }
            return false;
        }

        if (!DeviceAdminInteractor.getInstance().isKnoxEnabled(this)) {
            LogUtils.info("Knox is disabled, showing activation dialog");
            LogUtils.info("Check if internet connection exists");
            boolean hasInternetAccess = AdhellFactory.getInstance().hasInternetAccess(this);
            if (!hasInternetAccess) {
                AdhellFactory.getInstance().createNoInternetConnectionDialog(this);
            }
            if (!isActivationDialogVisible()) {
                activationDialogFragment.show(fragmentManager, ActivationDialogFragment.DIALOG_TAG);
            }
            return false;
        }

        // Select the Home tab manually if nothing is selected
        if (selectedTabId == -1) {
            if (themeChange != null) {
                if (themeChange.matches(SET_NIGHT_MODE_PREFERENCE)) {
                    bottomBar.setSelectedItemId(R.id.othersTab);
                    onTabSelected(R.id.othersTab);

                } else {
                    onTabSelected(R.id.homeTab);
                }
            } else {
                onTabSelected(R.id.homeTab);
            }
        }
        return true;
    }

    private boolean isActivationDialogVisible() {
        Fragment activationDialog = getSupportFragmentManager().findFragmentByTag(ActivationDialogFragment.DIALOG_TAG);
        return activationDialog != null;
    }

    private AlertDialog createPasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_enter_password, findViewById(android.R.id.content), false);
        int themeColor = this.getResources().getColor(R.color.colorBottomNavUnselected, this.getTheme());
        AlertDialog passwordDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(android.R.string.yes, null)
                .setCancelable(false)
                .create();

        ImageView icon = dialogView.findViewById(R.id.passwordIcon);
        icon.setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
        ImageButton fingerprintButton = dialogView.findViewById(R.id.fingerprintButton);
        passwordDialog.setOnShowListener(dialogInterface -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && biometricSupport) {
                fingerprintButton.setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
                fingerprintButton.setVisibility(View.VISIBLE);
                fingerprintButton.setOnClickListener(v -> BiometricUtils.authenticateUser(this));
                BiometricUtils.authenticateUser(this);
            }
            Button button = passwordDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                EditText passwordEditText = dialogView.findViewById(R.id.passwordEditText);
                String password = passwordEditText.getText().toString();
                try {
                    TextView infoTextView = dialogView.findViewById(R.id.infoTextView);
                    String passwordHash = AppPreferences.getInstance().getPasswordHash();
                    if (PasswordStorage.verifyPassword(password, passwordHash)) {
                        infoTextView.setText(R.string.dialog_enter_password_summary);
                        passwordEditText.setText("");
                        passwordDialog.dismiss();
                        isKnoxValid();
                    } else {
                        infoTextView.setText(R.string.dialog_wrong_password);
                    }
                } catch (PasswordStorage.CannotPerformOperationException | PasswordStorage.InvalidHashException e) {
                    e.printStackTrace();
                }
            });
        });
        return passwordDialog;
    }

    public void setSelectedAppTab(int selectedTabId) {
        this.SELECTED_APP_TAB = selectedTabId;
    }
    public int getSelectedAppTab() {
        return this.SELECTED_APP_TAB;
    }

    public void setSelectedDomainTab(int selectedTabId) {
        this.SELECTED_DOMAIN_TAB = selectedTabId;
    }
    public int getSelectedDomainTab() {
        return this.SELECTED_DOMAIN_TAB;
    }

    public void setSelectedOtherTab(int selectedTabId) {
        this.SELECTED_OTHER_TAB = selectedTabId;
    }
    public int getSelectedOtherTab() {
        return this.SELECTED_OTHER_TAB;
    }

    public static void setSelectFileActivityLaunched(boolean isLaunched) { selectFileActivityLaunched = isLaunched; }
}
