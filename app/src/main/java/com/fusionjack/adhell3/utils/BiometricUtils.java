package com.fusionjack.adhell3.utils;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.widget.Toast;

import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;

import static android.content.Context.KEYGUARD_SERVICE;


public class BiometricUtils {

    public static Boolean checkBiometricSupport(Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(KEYGUARD_SERVICE);
        PackageManager packageManager = context.getPackageManager();
        if (!keyguardManager.isKeyguardSecure()) {
            Toast.makeText(context, context.getString(R.string.biometric_check_lockscreen_security), Toast.LENGTH_LONG).show();
            return false;
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_BIOMETRIC) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, context.getString(R.string.biometric_check_permission), Toast.LENGTH_LONG).show();
            return false;
        }
        if (!FingerprintManagerCompat.from(context).isHardwareDetected()) {
            Toast.makeText(context, context.getString(R.string.biometric_check_hardware), Toast.LENGTH_LONG).show();
            return false;
        }
        if (!FingerprintManagerCompat.from(context).hasEnrolledFingerprints()) {
            Toast.makeText(context, context.getString(R.string.biometric_check_enrolled_fingerprint), Toast.LENGTH_LONG).show();
            return false;
        }
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            return true;
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private static BiometricPrompt.AuthenticationCallback getAuthenticationCallback(Context context) {

        return new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                Toast.makeText(context, context.getString(R.string.biometric_auth_error) + errString, Toast.LENGTH_LONG).show();
                super.onAuthenticationError(errorCode, errString);
            }

            @Override
            public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                super.onAuthenticationHelp(helpCode, helpString);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }

            @Override
            public void onAuthenticationSucceeded(
                BiometricPrompt.AuthenticationResult result) {
                    Toast.makeText(context, context.getString(R.string.biometric_auth_succeeded), Toast.LENGTH_SHORT).show();
                    ((MainActivity) context).passwordDialog.dismiss();
                    ((MainActivity) context).isKnoxValid();
                    super.onAuthenticationSucceeded(result);

            }
        };
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private static CancellationSignal getCancellationSignal(Context context) {
        CancellationSignal cancellationSignal = new CancellationSignal();
        cancellationSignal.setOnCancelListener(() -> Toast.makeText(context, context.getString(R.string.biometric_auth_cancel), Toast.LENGTH_LONG).show());
        return cancellationSignal;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public static void authenticateUser(Context context) {
        BiometricPrompt biometricPrompt = new BiometricPrompt.Builder(context)
                .setTitle(context.getString(R.string.biometric_dialog_title))
                .setSubtitle(context.getString(R.string.biometric_dialog_subtitle))
                .setDescription(context.getString(R.string.biometric_dialog_description))
                .setNegativeButton(context.getString(R.string.biometric_dialog_cancel_button_text), context.getMainExecutor(), (dialogInterface, i) -> Toast.makeText(context, context.getString(R.string.biometric_auth_cancel), Toast.LENGTH_LONG).show())
                .build();

        biometricPrompt.authenticate(getCancellationSignal(context), context.getMainExecutor(), getAuthenticationCallback(context));
    }
}