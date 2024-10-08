package com.fusionjack.adhell3.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

public class OtherTabPageFragment extends Fragment {
    private static final String ARG_PAGE = "page";

    public static final int APP_COMPONENT_PAGE = 0;
    public static final int RESTRICTION_PAGE = 1;
    public static final int DNS_PAGE = 2;
    public static final int PROXY_PAGE = 3;
    public static final int SETTINGS_PAGE = 4;

    public static Fragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);

        Fragment fragment;
        switch (page) {
            case APP_COMPONENT_PAGE:
                fragment = new AppComponentFragment();
                break;
            case RESTRICTION_PAGE:
                fragment = new RestrictionFragment();
                break;
            case DNS_PAGE:
                fragment = new DnsFragment();
                break;
            case PROXY_PAGE:
                fragment = new ProxyFragment();
                break;
            case SETTINGS_PAGE:
                default:
                fragment = new SettingsFragment();
                break;
        }
        fragment.setArguments(args);

        return fragment;
    }
}
