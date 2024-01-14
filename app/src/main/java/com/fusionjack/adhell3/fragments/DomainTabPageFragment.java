package com.fusionjack.adhell3.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

public class DomainTabPageFragment extends Fragment {
    private static final String ARG_PAGE = "page";

    public static final  int FIREWALL_PAGE = 0;
    public static final int BLACKLIST_PAGE = 1;
    public static final int WHITELIST_PAGE = 2;
    public static final int PROVIDER_LIST_PAGE = 3;
    public static final int PROVIDER_CONTENT_PAGE = 4;

    public static Fragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);

        Fragment fragment;
        switch (page) {
            case FIREWALL_PAGE:
                fragment = new FirewallFragment();
                break;
            case BLACKLIST_PAGE:
                fragment = new BlacklistFragment();
                break;
            case WHITELIST_PAGE:
                fragment = new WhitelistFragment();
                break;
            case PROVIDER_LIST_PAGE:
                fragment = new ProviderListFragment();
                break;
            case PROVIDER_CONTENT_PAGE:
                default:
                fragment = new ProviderContentFragment();
                break;
        }
        fragment.setArguments(args);

        return fragment;
    }
}
