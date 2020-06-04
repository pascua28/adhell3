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
import com.fusionjack.adhell3.adapter.OtherPagerAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.Objects;

import static com.fusionjack.adhell3.fragments.OtherTabPageFragment.SETTINGS_PAGE;

public class OtherTabFragment extends Fragment {

    private final int[] imageResId = {
            R.drawable.ic_security_black_24dp,
            R.drawable.ic_dns_black_24dp,
            R.drawable.ic_settings_black_24dp
    };
    private String viewpagerPosition;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        if (bundle != null) {
            viewpagerPosition = getArguments().getString("viewpager_position");
        }
        requireActivity().setTitle("Others");
        AppCompatActivity parentActivity = (AppCompatActivity) getActivity();
        MainActivity mainActivity = (MainActivity) parentActivity;
        if (parentActivity != null && parentActivity.getSupportActionBar() != null) {
            parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            parentActivity.getSupportActionBar().setHomeButtonEnabled(false);
            parentActivity.getSupportActionBar().setDisplayShowCustomEnabled(false);
        }
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_others, container, false);

        TabLayout tabLayout = view.findViewById(R.id.others_sliding_tabs);
        ViewPager viewPager = view.findViewById(R.id.others_viewpager);
        viewPager.setAdapter(new OtherPagerAdapter(getChildFragmentManager(), requireContext()));
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.addOnTabSelectedListener(
                new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {

                    @Override
                    public void onTabSelected(@NonNull TabLayout.Tab tab) {
                        super.onTabSelected(tab);
                        int tabIconColor = ContextCompat.getColor(requireContext(), R.color.colorAccent);
                        Objects.requireNonNull(tab.getIcon()).setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                        if (mainActivity != null) {
                            mainActivity.setSelectedOtherTab(tab.getPosition());
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
                        Objects.requireNonNull(tab.getIcon()).setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                        if (mainActivity != null) {
                            mainActivity.setSelectedOtherTab(tab.getPosition());
                        }
                    }
                }
        );

        int imageIndex = BuildConfig.APP_COMPONENT ? 0 : 1;
        int tabCount = Objects.requireNonNull(viewPager.getAdapter()).getCount();
        for (int i = 0; i < tabCount; i++, imageIndex++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null) {
                tab.setIcon(imageResId[i]);
                int tabIconColor = ContextCompat.getColor(requireContext(), R.color.colorBottomNavUnselected);
                Objects.requireNonNull(tab.getIcon()).setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
            }
        }

        if (viewpagerPosition != null && viewpagerPosition.equals("Settings") && mainActivity != null) {

            mainActivity.setSelectedOtherTab(SETTINGS_PAGE);
            mainActivity.themeChange = null;
        }
        TabLayout.Tab tab = null;
        if (mainActivity != null) {
            tab = tabLayout.getTabAt(mainActivity.getSelectedOtherTab());
        }
        if (tab != null) {
            tab.select();
        }

/*        if (viewpagerPosition == null) {
            TabLayout.Tab tab = tabLayout.getTabAt(APP_COMPONENT_PAGE);
            if (tab != null) {
                tab.select();
            }
        } else {
            if (viewpagerPosition.equals("Settings")) {
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
        }*/
        return view;
    }
}
