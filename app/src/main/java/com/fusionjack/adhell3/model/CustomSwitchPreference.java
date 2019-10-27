package com.fusionjack.adhell3.model;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.SwitchPreference;

public class CustomSwitchPreference extends SwitchPreference {
    public CustomSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomSwitchPreference(Context context) {
        super(context);
    }

    @Override
    protected void onClick(){}
}
