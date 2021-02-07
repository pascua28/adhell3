package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import com.fusionjack.adhell3.model.IComponentInfo;

import java.util.List;

public class ComponentAdapter extends ArrayAdapter<IComponentInfo> {

    private static final String UNKNOWN = "Unknown";

    ComponentAdapter(@NonNull Context context, @NonNull List<IComponentInfo> componentInfos) {
        super(context, 0, componentInfos);
    }

    public String getNamePart(String componentName) {
        int lastIndex = componentName.lastIndexOf('.');
        if (lastIndex != -1) {
            return componentName.substring(lastIndex + 1);
        }
        return UNKNOWN;
    }

    String getPackagePart(String componentName) {
        int lastIndex = componentName.lastIndexOf('.');
        if (lastIndex != -1) {
            return componentName.substring(0, lastIndex);
        }
        return componentName;
    }

}
