package com.fusionjack.adhell3;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
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
    private boolean biometricSupport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);

        // Early exit if the device doesn't support Knox
        if (!DeviceAdminInteractor.getInstance().isSupported()) {
            LogUtils.info("Device not supported");
            AdhellFactory.getInstance().createNotSupportedDialog(this);
            return;
        }

        // Exit if intent extra EXIT exist
        if (getIntent().getBooleanExtra("EXIT", false)) {
            finish();
            return;
        }

        // Set the crash handler to log crash's stack trace into a file
        if (!(Thread.getDefaultUncaughtExceptionHandler() instanceof CrashHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler.getInstance());
        }

        // Change status bar icon tint based on theme
        View decor = getWindow().getDecorView();
        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SET_NIGHT_MODE_PREFERENCE, false)) {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            decor.setSystemUiVisibility(0);
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
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
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
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
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