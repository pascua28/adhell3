package com.fusionjack.adhell3;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.fusionjack.adhell3.databinding.ActivityMainBinding;
import com.fusionjack.adhell3.databinding.DialogQuestionBinding;
import com.fusionjack.adhell3.dialogfragment.ActivationDialogFragment;
import com.fusionjack.adhell3.dialogfragment.AutoUpdateDialogFragment;
import com.fusionjack.adhell3.fragments.AppTabFragment;
import com.fusionjack.adhell3.fragments.AppTabPageFragment;
import com.fusionjack.adhell3.fragments.DomainTabFragment;
import com.fusionjack.adhell3.fragments.DomainTabPageFragment;
import com.fusionjack.adhell3.fragments.FilterAppInfo;
import com.fusionjack.adhell3.fragments.HomeTabFragment;
import com.fusionjack.adhell3.fragments.OtherTabFragment;
import com.fusionjack.adhell3.fragments.OtherTabPageFragment;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppCache;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.CrashHandler;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;
import com.fusionjack.adhell3.utils.LogUtils;
import com.google.android.material.snackbar.Snackbar;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.fusionjack.adhell3.fragments.SettingsFragment.SET_NIGHT_MODE_PREFERENCE;

public class MainActivity extends AppCompatActivity {
    public static boolean themeChanged = false;
    public static AtomicBoolean appCacheReady = new AtomicBoolean(false);
    private static final String BACK_STACK_TAB_TAG = "tab_fragment";
    private static boolean selectFileActivityLaunched = false;
    private static boolean restoreBackStack = false;
    private static int SELECTED_APP_TAB = AppTabPageFragment.PACKAGE_DISABLER_PAGE;
    private static int SELECTED_DOMAIN_TAB = DomainTabPageFragment.PROVIDER_LIST_PAGE;
    private static int SELECTED_OTHER_TAB = OtherTabPageFragment.APP_COMPONENT_PAGE;
    private static boolean doubleBackToExitPressedOnce = false;
    private static int previousSelectedTabId = -1;
    private static FilterAppInfo filterAppInfo;
    private final Handler snackbarDelayedHandler = new Handler();
    private AlertDialog permissionDialog;
    private Snackbar snackbar;
    private FragmentManager fragmentManager;
    private ActivityMainBinding binding;

    private final ActivityResultLauncher<Uri> openDocumentTreeLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), result -> {
        AppPreferences.getInstance().setStorageTreePath(result.toString());
        this.grantUriPermission(this.getPackageName(), result, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        this.getContentResolver().takePersistableUriPermission(result, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if (permissionDialog != null) {
            permissionDialog.dismiss();
        }
        finishOnResume();
    });

    public final ActivityResultLauncher<Intent> adminPermissionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            makeSnackbar("Admin OK!", Snackbar.LENGTH_LONG)
                    .show();
            finishOnResume();
        }
    });

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());

        // Set the crash handler to log crash's stack trace into a file
        if (!(Thread.getDefaultUncaughtExceptionHandler() instanceof CrashHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler.getInstance());
        }

        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setElevation(0);
            String versionInfo = getResources().getString(R.string.version);
            actionBar.setSubtitle(String.format(versionInfo, BuildConfig.VERSION_NAME));
        }

        fragmentManager = getSupportFragmentManager();

        setTheme();

        binding.bottomBar.setOnNavigationItemSelectedListener(item -> {
            onTabSelected(item.getItemId());
            return true;
        });

        setContentView(binding.getRoot());

        // Migrate auto update job to new class if needed
        AutoUpdateDialogFragment.migrateOldAutoUpdateJob(this);
    }

    @Override
    protected void onResume() {
        if (!selectFileActivityLaunched) {
            if (!getIntent().getBooleanExtra("START", false) && !themeChanged && !AppPreferences.getInstance().getPasswordHash().isEmpty()) {
                Intent splashIntent = new Intent(this, SplashScreenActivity.class);
                splashIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(splashIntent);
                super.onResume();
                return;
            }
            finishOnResume();
        } else {
            selectFileActivityLaunched = false;
        }
        super.onResume();
        setTheme();
        LogUtils.info("Everything is okay");
    }

    @Override
    protected void onPause() {
        onNewIntent(new Intent());
        restoreBackStack = fragmentManager.getBackStackEntryCount() > 1;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        LogUtils.info("Destroying activity");
        this.binding = null;
        closeActivity(false);

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        int count = fragmentManager.getBackStackEntryCount();
        if (count <= 1) {
            if (doubleBackToExitPressedOnce) {
                closeActivity(true);
                return;
            }

            doubleBackToExitPressedOnce = true;
            makeSnackbar("Press once again to exit", Snackbar.LENGTH_SHORT)
                    .show();

            new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
        } else {
            super.onBackPressed();
        }
    }

    private void closeActivity(boolean finish) {
        previousSelectedTabId = -1;
        themeChanged = false;
        restoreBackStack = false;

        setFilterAppInfo(new FilterAppInfo());

        setSelectedAppTab(AppTabPageFragment.PACKAGE_DISABLER_PAGE);
        setSelectedDomainTab(DomainTabPageFragment.PROVIDER_LIST_PAGE);
        setSelectedOtherTab(OtherTabPageFragment.APP_COMPONENT_PAGE);

        onNewIntent(new Intent());

        Intent intent = new Intent(this, SplashScreenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("EXIT", true);
        startActivity(intent);
        overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_out);

        if (finish) {
            finish();
            finishAffinity();
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

    public static void setSelectedAppTab(int selectedTabId) {
        SELECTED_APP_TAB = selectedTabId;
    }
    public static int getSelectedAppTab() {
        return SELECTED_APP_TAB;
    }

    public static void setSelectedDomainTab(int selectedTabId) {
        SELECTED_DOMAIN_TAB = selectedTabId;
    }
    public static int getSelectedDomainTab() {
        return SELECTED_DOMAIN_TAB;
    }

    public static void setSelectedOtherTab(int selectedTabId) {
        SELECTED_OTHER_TAB = selectedTabId;
    }
    public static int getSelectedOtherTab() {
        return SELECTED_OTHER_TAB;
    }

    public static void setSelectFileActivityLaunched(boolean isLaunched) { selectFileActivityLaunched = isLaunched; }

    public ActivityResultLauncher<Intent> getAdminPermissionLauncher() {
        return adminPermissionLauncher;
    }

    public Snackbar makeSnackbar(String message, int showDelay) {
        int duration = showDelay == Snackbar.LENGTH_LONG ? 2750 : 1500;
        ViewGroup rootView = binding.getRoot();
        this.snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_INDEFINITE)
                .setAnchorView(binding.bottomBar)
                .setDuration(duration);

        snackbarDelayedHandler.removeCallbacksAndMessages(null);

        List<View> fabList = getViewsByTag(rootView, "fab");
        for (View fab : fabList) {
            new Handler().postDelayed(() -> {
                snackbarDelayedHandler.postDelayed(() -> fab.animate().translationY(0).setDuration(75), (duration + 150));
                if (snackbar.getView().getHeight() > 1) {
                    fab.animate().translationY(-(snackbar.getView().getHeight() + (18 * ((float) getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT)))).setDuration(75);
                } else {
                    fab.animate().translationY(-(65 * ((float) getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT))).setDuration(100);
                }
            }, 30);
        }

        return snackbar;
    }

    private static ArrayList<View> getViewsByTag(ViewGroup root, String tag){
        ArrayList<View> views = new ArrayList<>();
        final int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = root.getChildAt(i);
            if (child instanceof ViewGroup) {
                views.addAll(getViewsByTag((ViewGroup) child, tag));
            }

            final Object tagObj = child.getTag();
            if (tagObj != null && tagObj.equals(tag)) {
                views.add(child);
            }
        }
        return views;
    }

    public static FilterAppInfo getFilterAppInfo() {
        if (MainActivity.filterAppInfo == null) {
            return new FilterAppInfo();
        } else {
            return MainActivity.filterAppInfo;
        }
    }

    public static void setFilterAppInfo(FilterAppInfo filterAppInfo) {
        MainActivity.filterAppInfo = filterAppInfo;
    }

    public void finishOnResume() {
        // Check whether Knox is still valid. Show activation dialog if it is not valid anymore.
        if (!isKnoxValid()) {
            return;
        }

        // Reload AppCache if needed
        new ReloadAppCacheIfNeeded(this, getSupportFragmentManager()).execute();

        // Check for storage permission
        requestStoragePermission();

        // Select Other tab if theme has been changed or select Home tab by default
        if (themeChanged) {
            binding.bottomBar.setSelectedItemId(R.id.othersTab);
        }

        if (!themeChanged && !restoreBackStack){
            if (previousSelectedTabId == -1) {
                binding.bottomBar.setSelectedItemId(R.id.homeTab);
            } else {
                binding.bottomBar.setSelectedItemId(binding.bottomBar.getSelectedItemId());
            }
        }
    }

    private boolean isKnoxValid() {
        ActivationDialogFragment activationDialogFragment = new ActivationDialogFragment();
        activationDialogFragment.setCancelable(false);
        if (!DeviceAdminInteractor.getInstance().isAdminActive()) {
            LogUtils.info("Admin is not active, showing activation dialog");
            if (activationDialogFragment != null && !activationDialogFragment.isVisible()) {
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
            if (activationDialogFragment != null && !activationDialogFragment.isVisible()) {
                activationDialogFragment.show(fragmentManager, ActivationDialogFragment.DIALOG_TAG);
            }
            return false;
        } else {
            activationDialogFragment.setCancelable(true);
        }

        if (activationDialogFragment != null && activationDialogFragment.isVisible()) {
            activationDialogFragment.dismiss();
        }
        return true;
    }

    private void requestStoragePermission() {
        if (AppPreferences.getInstance().getStorageTreePath().equals("") || this.getContentResolver().getPersistedUriPermissions().size() <= 0) {
            DialogQuestionBinding dialogQuestionBinding = DialogQuestionBinding.inflate(getLayoutInflater());
            dialogQuestionBinding.titleTextView.setText(R.string.dialog_storage_permission_title);
            dialogQuestionBinding.questionTextView.setText(R.string.dialog_storage_permission_summary);

            if (permissionDialog == null || !permissionDialog.isShowing()) {
                permissionDialog = new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setView(dialogQuestionBinding.getRoot())
                    .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                            setSelectFileActivityLaunched(true);

                            openDocumentTreeLauncher.launch(Uri.fromFile(Environment.getExternalStorageDirectory()));
                        }
                    )
                    .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> finish())
                    .setCancelable(false)
                    .create();

                permissionDialog.show();
            }
        }
    }

    private void onTabSelected(int tabId) {
        LogUtils.info("Tab '" + tabId + "' is selected");
        if (previousSelectedTabId != tabId || fragmentManager.getBackStackEntryCount() > 1 || themeChanged) {
            fragmentManager.popBackStack(BACK_STACK_TAB_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            Fragment replacing;
            if (tabId == R.id.homeTab) {
                replacing = new HomeTabFragment();
            } else if (tabId == R.id.appsManagementTab) {
                replacing = new AppTabFragment();
            } else if (tabId == R.id.domainsTab) {
                replacing = new DomainTabFragment();
            } else if (tabId == R.id.othersTab) {
                replacing = new OtherTabFragment();
                if (themeChanged) {
                    Bundle bundle = new Bundle();
                    bundle.putString("viewpager_position", "Settings");
                    replacing.setArguments(bundle);
                }
            } else {
                replacing = new Fragment();
            }
            // Hide Snackbar if is shown
            if (snackbar != null && snackbar.isShown()) {
                snackbar.dismiss();
            }
            // Reset double back press to exit
            doubleBackToExitPressedOnce = false;

            // Set fragment
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction
                    .replace(R.id.fragmentContainer, replacing)
                    .addToBackStack(BACK_STACK_TAB_TAG)
                    .commit();
        }
        previousSelectedTabId = tabId;
        themeChanged = false;
    }

    private void setTheme() {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        Window window = getWindow();
        View decor = window.getDecorView();

        String extraStringSetting = getIntent().getStringExtra("settingsFragment");
        if (extraStringSetting != null) {
            themeChanged = extraStringSetting.contains(SET_NIGHT_MODE_PREFERENCE);
        } else {
            themeChanged = false;
        }

        // Change status bar icon and navigation bar tint based on theme
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!mPrefs.getBoolean(SET_NIGHT_MODE_PREFERENCE, false)) {
                window.setNavigationBarColor(getResources().getColor(R.color.colorPrimary, getTheme()));
                decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                decor.setSystemUiVisibility(0);
            }
        } else {
            if (!mPrefs.getBoolean(SET_NIGHT_MODE_PREFERENCE, false)) {
                decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                decor.setSystemUiVisibility(0);
            }
        }
    }

    private static class ReloadAppCacheIfNeeded extends AsyncTask<Void, Void, Boolean> {
        private final WeakReference<Context> applicationContextReference;
        private FragmentManager fragmentManager;
        private Fragment visibleFragment;
        private Set<String> appCachePackageNames;
        private List<ApplicationInfo> installedPackages;

        ReloadAppCacheIfNeeded(Context applicationContext, FragmentManager fragmentManager) {
            this.applicationContextReference = new WeakReference<>(applicationContext);
            this.fragmentManager = fragmentManager;
        }

        @Override
        protected void onPreExecute() {
            this.appCachePackageNames = AppCache.getInstance(applicationContextReference.get(), null).getNames().keySet();
            this.installedPackages = AdhellFactory.getInstance().getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            // Get visible fragment
            if (fragmentManager != null) {
                for (Fragment fragment: fragmentManager.getFragments()) {
                    if (fragment != null && fragment.isVisible()) {
                        visibleFragment = fragment;
                        break;
                    }
                }
            }

            // make a copy of the list so the original list is not changed, and remove() is supported
            ArrayList<String> cp = new ArrayList<>( appCachePackageNames );
            cp.add(applicationContextReference.get().getPackageName());
            for ( ApplicationInfo ai : installedPackages ) {
                if ( !cp.remove( ai.packageName ) ) {
                    return true;
                }
            }
            return !cp.isEmpty();
        }

        @Override
        protected void onPostExecute(Boolean needReload) {
            if (needReload) {
                AppCache.reload(applicationContextReference.get(), null);
            }
            if (appCacheReady.compareAndSet(false, true)) {
                if (visibleFragment != null) {
                    visibleFragment.onResume();
                }
            }

            // Clean resources to prevent memory leak
            this.visibleFragment = null;
        }
    }
}
