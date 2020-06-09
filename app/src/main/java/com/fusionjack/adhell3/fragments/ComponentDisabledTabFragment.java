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
import androidx.viewpager2.widget.ViewPager2;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.ComponentDisabledPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

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
        ViewPager2 viewPager = view.findViewById(R.id.apps_viewpager);
        viewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        viewPager.setOffscreenPageLimit(4);
        viewPager.setAdapter(new ComponentDisabledPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(getTabTitle(position))
        ).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                TabLayout.Tab currentTab = tabLayout.getTabAt(position);
                if (currentTab != null) {
                    int tabIconColor = ContextCompat.getColor(requireContext(), R.color.colorAccent);
                    if (currentTab.getIcon() != null) {
                        currentTab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                    }
                }
                for (int i = 0; i < tabLayout.getTabCount(); i++) {
                    if (i != position) {
                        int tabIconColor = ContextCompat.getColor(requireContext(), R.color.colorText);
                        TabLayout.Tab otherTab = tabLayout.getTabAt(i);
                        if (otherTab!= null && otherTab.getIcon() != null) {
                            otherTab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                        }
                    }
                }
            }
        });

        if (viewPager.getAdapter() != null) {
            int tabCount = viewPager.getAdapter().getItemCount();
            for (int i = 0; i < tabCount; i++) {
                TabLayout.Tab tab = tabLayout.getTabAt(i);
                if (tab != null) {
                    tab.setIcon(imageResId[i]);
                    int tabIconColor = getResources().getColor(R.color.colorBottomNavUnselected, this.getActivity().getTheme());
                    if (tab.getIcon() != null) {
                        tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                    }
                }
            }
        }

        viewPager.setCurrentItem(0, false);

        return view;
    }

    private String getTabTitle(int position) {
        String[] tabTitles = new String[]{
                requireContext().getString(R.string.permission_fragment_title),
                requireContext().getString(R.string.service_fragment_title),
                requireContext().getString(R.string.receiver_fragment_title),
                requireContext().getString(R.string.activity_fragment_title)};

        return tabTitles[position];
    }
}
