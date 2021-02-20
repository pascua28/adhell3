package com.fusionjack.adhell3.utils;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.ImageView;

import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;

import com.fusionjack.adhell3.R;

public final class UiUtils {

    private UiUtils() {
    }

    public static void setSearchIconColor(SearchView searchView, Context context) {
        ImageView searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_button);
        ImageView searchCloseIcon = (ImageView) searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        tintSearchIcon(searchIcon, context);
        tintSearchIcon(searchCloseIcon, context);
    }

    private static void tintSearchIcon(ImageView icon, Context context) {
        icon.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent), android.graphics.PorterDuff.Mode.SRC_IN);
    }

    public static void setMenuIconColor(Menu menu, Context context) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            tintMenuIcon(item, context);
            SubMenu subMenu = item.getSubMenu();
            if (subMenu != null) {
                for (int j = 0; j < subMenu.size(); j++) {
                    tintMenuIcon(subMenu.getItem(j), context);
                }
            }
        }
    }

    public static void tintMenuIcon(MenuItem item, Context context) {
        Drawable drawable = item.getIcon();
        if (drawable != null) {
            drawable.mutate();
            drawable.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
        }
    }

}
