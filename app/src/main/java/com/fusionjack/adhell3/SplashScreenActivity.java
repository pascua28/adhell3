package com.fusionjack.adhell3;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.BiometricUtils;
import com.fusionjack.adhell3.utils.CrashHandler;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.PasswordStorage;

import static com.fusionjack.adhell3.fragments.SettingsFragment.SET_NIGHT_MODE_PREFERENCE;

public class SplashScreenActivity extends AppCompatActivity {
    private LinearLayout passwordLayout;
    private TextView passwordInfoTextView;
    private EditText passwordEditText;
    private ImageButton passwordButton;
    private ImageView biometricButton;
    private static boolean biometricSupport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent().getBooleanExtra("EXIT", false)) {
            setTheme(android.R.style.Theme_NoDisplay);
        }
        super.onCreate(savedInstanceState);

        // Exit if intent extra EXIT exist
        if (getIntent().getBooleanExtra("EXIT", false)) {
            finish();
            //finishAffinity();
            return;
        }

        // Launch main activity if no password set
        if (AppPreferences.getInstance().getPasswordHash().isEmpty()) {
            launchMainActivity();
            return;
        }

        // Early exit if the device doesn't support Knox
        if (!DeviceAdminInteractor.getInstance().isSupported()) {
            LogUtils.info("Device not supported");
            AdhellFactory.getInstance().createNotSupportedDialog(this);
            return;
        }

        // Set the crash handler to log crash's stack trace into a file
        if (!(Thread.getDefaultUncaughtExceptionHandler() instanceof CrashHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler.getInstance());
        }

        // Remove ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        biometricSupport = BiometricUtils.checkBiometricSupport(this);

        setContentView(R.layout.activity_splash_screen);

        passwordLayout = this.findViewById(R.id.passwordLayout);
        passwordInfoTextView = this.findViewById(R.id.passwordInfoTextView);
        passwordEditText = this.findViewById(R.id.passwordEditText);
        passwordButton = this.findViewById(R.id.passwordButton);
        biometricButton = this.findViewById(R.id.biometricButton);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        Window window = getWindow();
        View decor = window.getDecorView();

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

        if (!isPasswordShowing()) {
            launchMainActivity();
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, SplashScreenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("EXIT", true);
        startActivity(intent);
        overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_out);
        finish();
    }

    public void successAuthentication() {
        passwordLayout.setVisibility(View.GONE);
        launchMainActivity();
    }

    private void launchMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("START", true);
        startActivity(intent);
        overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_out);
        finish();
    }

    private boolean isPasswordShowing() {
        String passwordHash = AppPreferences.getInstance().getPasswordHash();
        if (!passwordHash.isEmpty()) {
            if (passwordLayout.getVisibility() != View.VISIBLE) {
                LogUtils.info("Showing password layout");
                showPasswordPrompt();
            }
            return true;
        }
        return false;
    }

    private void showPasswordPrompt() {
        passwordLayout.setVisibility(View.VISIBLE);

        if (biometricSupport) {
            biometricButton.setVisibility(View.VISIBLE);
            biometricButton.setOnClickListener(v -> BiometricUtils.authenticateUser(this));
            BiometricUtils.authenticateUser(this);
        } else {
            biometricButton.setVisibility(View.INVISIBLE);
        }

        passwordButton.setOnClickListener(view -> validatePassword());

        passwordEditText.setOnEditorActionListener((textView, id, keyEvent) -> {
            if ((keyEvent != null && (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (id == EditorInfo.IME_ACTION_DONE)) {
                validatePassword();
            }
            return false;
        });
    }

    private void validatePassword() {
        String password = passwordEditText.getText().toString();
        try {
            String passwordHash = AppPreferences.getInstance().getPasswordHash();
            if (PasswordStorage.verifyPassword(password, passwordHash)) {
                passwordInfoTextView.setText(R.string.dialog_enter_password_summary);
                passwordEditText.setText("");
                successAuthentication();
            } else {
                passwordInfoTextView.setText(R.string.dialog_wrong_password);
            }
        } catch (PasswordStorage.CannotPerformOperationException | PasswordStorage.InvalidHashException e) {
            e.printStackTrace();
        }
    }
}