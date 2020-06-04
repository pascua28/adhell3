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

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppPagerAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.Objects;

public class AppTabFragment extends Fragment {

    private final int[] imageResId = {
            R.drawable.ic_visibility_off_black_24dp,
            R.drawable.ic_signal_cellular_off_black_24dp,
            R.drawable.ic_signal_wifi_off_black_24dp,
            R.drawable.ic_beenhere_black_24dp
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        requireActivity().setTitle("Apps Management");
        AppCompatActivity parentActivity = (AppCompatActivity) getActivity();
        MainActivity mainActivity = (MainActivity) parentActivity;
        if (parentActivity != null && parentActivity.getSupportActionBar() != null) {
            parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            parentActivity.getSupportActionBar().setHomeButtonEnabled(false);
            parentActivity.getSupportActionBar().setDisplayShowCustomEnabled(false);
        }
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_apps, container, false);

        TabLayout tabLayout = view.findViewById(R.id.apps_sliding_tabs);
        ViewPager viewPager = view.findViewById(R.id.apps_viewpager);
        viewPager.setAdapter(new AppPagerAdapter(getChildFragmentManager(), requireContext()));
        viewPager.setOffscreenPageLimit(4);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.addOnTabSelectedListener(
                new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {

                    @Override
                    public void onTabSelected(@NonNull TabLayout.Tab tab) {
                        super.onTabSelected(tab);
                        int tabIconColor = ContextCompat.getColor(requireContext(), R.color.colorAccent);
                        Objects.requireNonNull(tab.getIcon()).setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                        if (mainActivity != null) {
                            mainActivity.setSelectedAppTab(tab.getPosition());
                        }
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                        super.onTabUnselected(tab);
                        int tabIconColor = ContextCompat.getColor(requireContext(), R.color.colorText);
                        Objects.requireNonNull(tab.getIcon()).setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                        super.onTabReselected(tab);
                        int tabIconColor = ContextCompat.getColor(requireContext(), R.color.colorAccent);
                        if (tab.getIcon() != null) {
                            tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                        }
                        if (mainActivity != null) {
                            mainActivity.setSelectedAppTab(tab.getPosition());
                        }
                    }
                }
        );

        int imageIndex = BuildConfig.DISABLE_APPS ? 0 : 1;
        int tabCount = Objects.requireNonNull(viewPager.getAdapter()).getCount();
        for (int i = 0; i < tabCount; i++, imageIndex++) {
            Objects.requireNonNull(tabLayout.getTabAt(i)).setIcon(imageResId[imageIndex]);
            int tabIconColor = ContextCompat.getColor(requireContext(), R.color.colorBottomNavUnselected);
            Objects.requireNonNull(Objects.requireNonNull(tabLayout.getTabAt(i)).getIcon()).setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
        }

        TabLayout.Tab tab = null;
        if (mainActivity != null) {
            tab = tabLayout.getTabAt(mainActivity.getSelectedAppTab());
        }
        if (tab != null) {
            tab.select();
        }
        return view;
    }
}
