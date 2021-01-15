package com.fusionjack.adhell3.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.OtherPagerAdapter;
import com.google.android.material.tabs.TabLayout;

import io.reactivex.annotations.NonNull;

import static com.fusionjack.adhell3.fragments.OtherTabPageFragment.APP_COMPONENT_PAGE;
import static com.fusionjack.adhell3.fragments.OtherTabPageFragment.SETTINGS_PAGE;

public class OtherTabFragment extends TabFragment {

    private final int[] imageResId = {
            R.drawable.ic_appcomponent,
            R.drawable.ic_restrictions,
            R.drawable.ic_dns,
            R.drawable.ic_settings
    };
    private String viewpagerPosition;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        if (bundle != null){
            viewpagerPosition = getArguments().getString("viewpager_position");
        }
        getActivity().setTitle("Others");
        AppCompatActivity parentActivity = (AppCompatActivity) getActivity();
        if (parentActivity.getSupportActionBar() != null) {
            parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            parentActivity.getSupportActionBar().setHomeButtonEnabled(false);
            parentActivity.getSupportActionBar().setDisplayShowCustomEnabled(false);
        }
        setHasOptionsMenu(true);

        OtherPagerAdapter adapter = new OtherPagerAdapter(getChildFragmentManager(), getContext());
        int offset = BuildConfig.APP_COMPONENT ? 0 : 1;
        View view = inflateFragment(R.layout.fragment_others, inflater, container, adapter, 4, imageResId, offset);

        TabLayout tabLayout = view.findViewById(R.id.sliding_tabs);
        if (viewpagerPosition != null && viewpagerPosition.equalsIgnoreCase("Settings")) {
            TabLayout.Tab tab = tabLayout.getTabAt(SETTINGS_PAGE);
            if (tab != null) {
                tab.select();
            }
        } else {
            TabLayout.Tab tab = tabLayout.getTabAt(APP_COMPONENT_PAGE);
            if (tab != null) {
                tab.select();
            }
        }

        return view;
    }
}
