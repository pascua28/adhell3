package com.fusionjack.adhell3.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.SplashScreenActivity;

import java.util.concurrent.Executor;


public class BiometricUtils {

    public static Boolean checkBiometricSupport(Context context) {
        return BiometricManager.from(context).canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static void authenticateUser(Context context) {
        FragmentActivity fragmentActivity = (FragmentActivity) context;
        Executor executor = ContextCompat.getMainExecutor(context.getApplicationContext());

        final BiometricPrompt biometricPrompt = new BiometricPrompt(fragmentActivity, executor, getAuthenticationCallback(context));

        final BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(context.getString(R.string.biometric_dialog_title))
                .setSubtitle(context.getString(R.string.biometric_dialog_subtitle))
                .setDescription(context.getString(R.string.biometric_dialog_description))
                .setNegativeButtonText(context.getString(R.string.biometric_dialog_cancel_button_text))
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private static BiometricPrompt.AuthenticationCallback getAuthenticationCallback(Context context) {

        return new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationFailed() {
                LogUtils.error(context.getString(R.string.biometric_auth_failed));
                super.onAuthenticationFailed();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                LogUtils.error(context.getString(R.string.biometric_auth_error) + errString);
                super.onAuthenticationError(errorCode, errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                LogUtils.info(context.getString(R.string.biometric_auth_success));
                ((SplashScreenActivity) context).runOnUiThread(((SplashScreenActivity) context)::successAuthentication);
                super.onAuthenticationSucceeded(result);

            }
        };
    }
}