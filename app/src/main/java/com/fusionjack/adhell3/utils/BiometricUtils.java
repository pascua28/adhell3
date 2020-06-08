package com.fusionjack.adhell3.utils;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.SplashScreenActivity;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class BiometricUtils {

    public static Boolean checkBiometricSupport(Context context) {
        return BiometricManager.from(context).canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static void authenticateUser(Context context) {
        FragmentActivity fragmentActivity = (FragmentActivity) context;
        Executor executor = Executors.newSingleThreadExecutor();

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
                ((SplashScreenActivity) context).runOnUiThread(() -> Toast.makeText(context, context.getString(R.string.biometric_auth_failed), Toast.LENGTH_SHORT).show());
                super.onAuthenticationFailed();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                ((SplashScreenActivity) context).runOnUiThread(() -> Toast.makeText(context, context.getString(R.string.biometric_auth_error) + errString, Toast.LENGTH_SHORT).show());
                super.onAuthenticationError(errorCode, errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                ((SplashScreenActivity) context).runOnUiThread(((SplashScreenActivity) context)::successAuthentication);
                super.onAuthenticationSucceeded(result);

            }
        };
    }
}