package com.fusionjack.adhell3.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppPagerAdapter;
import com.google.android.material.tabs.TabLayout;

import static com.fusionjack.adhell3.fragments.AppTabPageFragment.PACKAGE_DISABLER_PAGE;

public class AppTabFragment extends TabFragment {

    private final int[] imageResId = {
            R.drawable.ic_disable_app,
            R.drawable.ic_disable_mobile,
            R.drawable.ic_disable_wifi,
            R.drawable.ic_whitelist_app
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle("Apps Management");
        AppCompatActivity parentActivity = (AppCompatActivity) getActivity();
        if (parentActivity.getSupportActionBar() != null) {
            parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            parentActivity.getSupportActionBar().setHomeButtonEnabled(false);
            parentActivity.getSupportActionBar().setDisplayShowCustomEnabled(false);
        }
        setHasOptionsMenu(true);

        AppPagerAdapter adapter = new AppPagerAdapter(getChildFragmentManager(), getContext());
        int offset = BuildConfig.DISABLE_APPS ? 0 : 1;
        View view = inflateFragment(R.layout.fragment_apps, inflater, container, adapter, 4, imageResId, offset);

        TabLayout tabLayout = view.findViewById(R.id.sliding_tabs);
        TabLayout.Tab tab = tabLayout.getTabAt(PACKAGE_DISABLER_PAGE);
        if (tab != null) {
            tab.select();
        }
        return view;
    }
}
