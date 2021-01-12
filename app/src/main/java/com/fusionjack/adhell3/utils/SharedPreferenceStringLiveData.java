package com.fusionjack.adhell3.utils;

import android.content.SharedPreferences;

public class SharedPreferenceStringLiveData extends SharedPreferenceLiveData<String> {

    public SharedPreferenceStringLiveData(SharedPreferences prefs, String key, String defValue) {
        super(prefs, key, defValue);
    }

    @Override
    String getValueFromPreferences(String key, String defValue) {
        return sharedPrefs.getString(key, defValue);
    }
}
