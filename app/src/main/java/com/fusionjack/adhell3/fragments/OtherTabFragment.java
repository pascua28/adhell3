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

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.OtherPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

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
        if (parentActivity != null && parentActivity.getSupportActionBar() != null) {
            parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            parentActivity.getSupportActionBar().setHomeButtonEnabled(false);
            parentActivity.getSupportActionBar().setDisplayShowCustomEnabled(false);
        }
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_others, container, false);

        TabLayout tabLayout = view.findViewById(R.id.others_sliding_tabs);
        ViewPager2 viewPager = view.findViewById(R.id.others_viewpager);
        viewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        viewPager.setOffscreenPageLimit(BuildConfig.APP_COMPONENT ? 3 : 2);
        viewPager.setAdapter(new OtherPagerAdapter(this));

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
                MainActivity.setSelectedOtherTab(position);
            }
        });

        int imageIndex = BuildConfig.APP_COMPONENT ? 0 : 1;
        if (viewPager.getAdapter() != null) {
            int tabCount = viewPager.getAdapter().getItemCount();
            for (int i = 0; i < tabCount; i++, imageIndex++) {
                TabLayout.Tab tab = tabLayout.getTabAt(i);
                if (tab != null && this.getActivity() != null && this.getActivity().getTheme() != null) {
                    tab.setIcon(imageResId[i]);
                    int tabIconColor = getResources().getColor(R.color.colorBottomNavUnselected, this.getActivity().getTheme());
                    if (tab.getIcon() != null) {
                        tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                    }
                }
            }
        }

        if (viewpagerPosition != null && viewpagerPosition.equals("Settings")) {
            MainActivity.setSelectedOtherTab(SETTINGS_PAGE);
            MainActivity.themeChanged = false;
        }

        viewPager.setCurrentItem(MainActivity.getSelectedOtherTab(), false);

        return view;
    }

    private String getTabTitle(int position) {
        String[] tabTitles = new String[]{
                requireContext().getString(R.string.app_component_fragment_title),
                requireContext().getString(R.string.dns_fragment_title),
                requireContext().getString(R.string.settings_fragment_title)};

        return tabTitles[position];
    }
}
