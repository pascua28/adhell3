package com.fusionjack.adhell3;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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
import com.fusionjack.adhell3.utils.CrashHandler;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;
import com.fusionjack.adhell3.utils.LogUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;

import static com.fusionjack.adhell3.fragments.SettingsFragment.SET_NIGHT_MODE_PREFERENCE;

public class MainActivity extends AppCompatActivity {
    public String themeChange;
    private static final String BACK_STACK_TAB_TAG = "tab_fragment";
    private static final int ADMIN_PERMISSION_REQUEST_CODE = 41;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 42;
    private static boolean selectFileActivityLaunched = false;
    private static boolean restoreBackStack = false;
    private int SELECTED_APP_TAB = AppTabPageFragment.PACKAGE_DISABLER_PAGE;
    private int SELECTED_DOMAIN_TAB = DomainTabPageFragment.PROVIDER_LIST_PAGE;
    private int SELECTED_OTHER_TAB = OtherTabPageFragment.APP_COMPONENT_PAGE;
    private FragmentManager fragmentManager;
    private ActivationDialogFragment activationDialogFragment;
    private boolean doubleBackToExitPressedOnce = false;
    private int previousSelectedTab = -1;
    private AlertDialog permissionDialog;
    private BottomNavigationView bottomBar;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Set the crash handler to log crash's stack trace into a file
        if (!(Thread.getDefaultUncaughtExceptionHandler() instanceof CrashHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler.getInstance());
        }

        // Remove elevation shadow of ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setElevation(0);
        }

        // Change status bar icon tint based on theme
        View decor = getWindow().getDecorView();
        if (!mPrefs.getBoolean(SET_NIGHT_MODE_PREFERENCE, false)) {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            decor.setSystemUiVisibility(0);
        }

        fragmentManager = getSupportFragmentManager();
        activationDialogFragment = new ActivationDialogFragment();
        activationDialogFragment.setCancelable(false);

        themeChange = getIntent().getStringExtra("settingsFragment");

        setContentView(R.layout.activity_main);

        bottomBar = findViewById(R.id.bottomBar);
        bottomBar.setOnNavigationItemSelectedListener(item -> {
            onTabSelected(item.getItemId());
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        themeChange = getIntent().getStringExtra("settingsFragment");

        if (!selectFileActivityLaunched) {
            if (!getIntent().getBooleanExtra("START", false) && themeChange == null) {
                Intent splashIntent = new Intent(this, SplashScreenActivity.class);
                splashIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(splashIntent);
            }

            finishOnResume();
        } else {
            selectFileActivityLaunched = false;
        }

        LogUtils.info("Everything is okay");
    }

    @Override
    protected void onPause() {
        onNewIntent(new Intent());
        if (fragmentManager.getBackStackEntryCount() > 1) {
            restoreBackStack = true;
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtils.info("Destroying activity");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData){
        super.onActivityResult(requestCode, resultCode, resultData);

        if(resultCode == RESULT_OK && requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            Uri treeUri = resultData.getData();
            if (treeUri != null) {
                AppPreferences.getInstance().setStorageTreePath(treeUri.toString());
                this.grantUriPermission(this.getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                this.getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            if (permissionDialog != null) {
                permissionDialog.dismiss();
            }
            finishOnResume();
        } else if (resultCode == RESULT_OK && requestCode == ADMIN_PERMISSION_REQUEST_CODE) {
            Toast.makeText(this, "Admin OK!", Toast.LENGTH_LONG).show();
            finishOnResume();
        }
    }

    @Override
    public void onBackPressed() {
        int count = fragmentManager.getBackStackEntryCount();
        if (count <= 1) {
            if (doubleBackToExitPressedOnce) {
                Intent intent = new Intent(this, SplashScreenActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra("EXIT", true);
                startActivity(intent);
                overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_out);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            fragmentManager.popBackStack();
            return true;
        }
        return super.onOptionsItemSelected(item);
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

    public void finishOnResume() {
        // Check whether Knox is still valid. Show activation dialog if it is not valid anymore.
        if (!isKnoxValid()) {
            return;
        }

        // Check for storage permission
        requestStoragePermission();

        // Select Other tab if theme has been changed or select Home tab by default
        if (themeChange != null && themeChange.contains(SET_NIGHT_MODE_PREFERENCE)) {
            bottomBar.setSelectedItemId(R.id.othersTab);
        } else {
            if (restoreBackStack){
                restoreBackStack = false;
            } else {
                bottomBar.setSelectedItemId(bottomBar.getSelectedItemId());
            }
        }
    }

    private boolean isKnoxValid() {
        if (!DeviceAdminInteractor.getInstance().isAdminActive()) {
            LogUtils.info("Admin is not active, showing activation dialog");
            if (isActivationDialogNotVisible()) {
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
            if (isActivationDialogNotVisible()) {
                activationDialogFragment.show(fragmentManager, ActivationDialogFragment.DIALOG_TAG);
            }
            return false;
        }

        if (!isActivationDialogNotVisible()) {
            activationDialogFragment.dismiss();
        }
        return true;
    }

    private void requestStoragePermission() {
        if (AppPreferences.getInstance().getStorageTreePath().equals("") || this.getContentResolver().getPersistedUriPermissions().size() <= 0) {
            View dialogView = this.getLayoutInflater().inflate(R.layout.dialog_question, findViewById(android.R.id.content), false);
            TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
            titleTextView.setText(R.string.dialog_storage_permission_title);
            TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
            questionTextView.setText(R.string.dialog_storage_permission_summary);

            permissionDialog = new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                        setSelectFileActivityLaunched(true);

                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                        intent.addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);

                        startActivityForResult(intent, STORAGE_PERMISSION_REQUEST_CODE);
                    }
                )
                .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> finish())
                    .create();

            if (!permissionDialog.isShowing()) {
                permissionDialog.show();
            }
        }
    }

    private void onTabSelected(int tabId) {
        LogUtils.info("Tab '" + tabId + "' is selected");
        if (previousSelectedTab != getTabIndex(tabId) || fragmentManager.getBackStackEntryCount() > 1) {
            fragmentManager.popBackStack(BACK_STACK_TAB_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            Fragment replacing;
            switch (tabId) {
                case R.id.homeTab:
                    replacing = new HomeTabFragment();
                    break;
                case R.id.appsManagementTab:
                    replacing = new AppTabFragment();
                    break;
                case R.id.domainsTab:
                    replacing = new DomainTabFragment();
                    break;
                case R.id.othersTab:
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
                    replacing = new Fragment();
                    break;
            }
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            if (previousSelectedTab != -1) {
                fragmentTransaction.setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_out, R.anim.fragment_fade_in, R.anim.fragment_fade_out);
            } else {
                fragmentTransaction.setCustomAnimations(0, R.anim.fragment_fade_out, R.anim.fragment_fade_in, R.anim.fragment_fade_out);
            }
            fragmentTransaction
                    .replace(R.id.fragmentContainer, replacing)
                    .addToBackStack(BACK_STACK_TAB_TAG)
                    .commit();
        }
        previousSelectedTab = getTabIndex(tabId);
    }

    private boolean isActivationDialogNotVisible() {
        Fragment activationDialog = getSupportFragmentManager().findFragmentByTag(ActivationDialogFragment.DIALOG_TAG);
        return activationDialog == null;
    }

    private int getTabIndex(int tabId) {
        HashMap<Integer, Integer> tabsIndex = new HashMap<>();
        tabsIndex.put(R.id.homeTab, 0);
        tabsIndex.put(R.id.appsManagementTab, 1);
        tabsIndex.put(R.id.domainsTab, 2);
        tabsIndex.put(R.id.othersTab, 3);

        if (tabsIndex.get(tabId) != null) {
            return tabsIndex.get(tabId);
        } else {
            return 0;
        }
    }
}
