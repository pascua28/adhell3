package com.fusionjack.adhell3.fragments;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.ComponentDisabledPagerAdapter;
import com.google.android.material.tabs.TabLayout;

public class ComponentDisabledTabFragment extends Fragment {
    private final int[] imageResId = {
            R.drawable.ic_permission,
            R.drawable.ic_service,
            R.drawable.ic_receiver,
            R.drawable.ic_activity
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() != null) {
            getActivity().setTitle("Disabled app component");

            AppCompatActivity parentActivity = (AppCompatActivity) getActivity();
            if (parentActivity.getSupportActionBar() != null) {
                parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                parentActivity.getSupportActionBar().setHomeButtonEnabled(true);
                parentActivity.getSupportActionBar().setDisplayShowCustomEnabled(false);
            }
        }

        View view = inflater.inflate(R.layout.fragment_app_component_tabs, container, false);

        TabLayout tabLayout = view.findViewById(R.id.apps_sliding_tabs);
        ViewPager viewPager = view.findViewById(R.id.apps_viewpager);
        viewPager.setAdapter(new ComponentDisabledPagerAdapter(getChildFragmentManager(), requireContext()));
        viewPager.setOffscreenPageLimit(3);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.addOnTabSelectedListener(
                new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {

                    @Override
                    public void onTabSelected(@NonNull TabLayout.Tab tab) {
                        super.onTabSelected(tab);
                        int tabIconColor = ContextCompat.getColor(requireContext(), R.color.colorAccent);
                        if (tab.getIcon() != null) {
                            tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                        }
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                        super.onTabUnselected(tab);
                        int tabIconColor = ContextCompat.getColor(requireContext(), R.color.colorText);
                        if (tab.getIcon() != null) {
                            tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                        }
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                        super.onTabReselected(tab);
                        int tabIconColor = ContextCompat.getColor(requireContext(), R.color.colorAccent);
                        if (tab.getIcon() != null) {
                            tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                        }
                    }
                }
        );

        if (viewPager.getAdapter() != null) {
            int tabCount = viewPager.getAdapter().getCount();
            for (int i = 0; i < tabCount; i++) {
                TabLayout.Tab tab = tabLayout.getTabAt(i);
                if (tab != null) {
                    tab.setIcon(imageResId[i]);
                    int tabIconColor = ContextCompat.getColor(requireContext(), R.color.colorBottomNavUnselected);
                    if (tab.getIcon() != null) {
                        tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                    }
                }
            }
        }

        TabLayout.Tab firstTab = tabLayout.getTabAt(0);
        if (firstTab != null) {
            firstTab.select();
        }

        return view;
    }
}
