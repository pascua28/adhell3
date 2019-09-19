package com.fusionjack.adhell3.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.ComponentDisabledPagerAdapter;
import com.google.android.material.tabs.TabLayout;

public class ComponentDisabledTabFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle("Disabled app component");
        AppCompatActivity parentActivity = (AppCompatActivity) getActivity();
        if (parentActivity.getSupportActionBar() != null) {
            parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            parentActivity.getSupportActionBar().setHomeButtonEnabled(true);
            parentActivity.getSupportActionBar().setDisplayShowCustomEnabled(false);
        }

        View view = inflater.inflate(R.layout.fragment_app_component_tabs, container, false);

        TabLayout tabLayout = view.findViewById(R.id.apps_sliding_tabs);
        ViewPager viewPager = view.findViewById(R.id.apps_viewpager);
        viewPager.setAdapter(new ComponentDisabledPagerAdapter(getChildFragmentManager(), getContext()));
        viewPager.setOffscreenPageLimit(3);
        tabLayout.setupWithViewPager(viewPager);

        return view;
    }
}
