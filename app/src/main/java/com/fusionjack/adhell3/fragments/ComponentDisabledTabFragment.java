package com.fusionjack.adhell3.fragments;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.ComponentDisabledPagerAdapter;
import com.fusionjack.adhell3.databinding.FragmentAppComponentTabsBinding;
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
        }

        FragmentAppComponentTabsBinding binding = FragmentAppComponentTabsBinding.inflate(inflater);

        binding.appsViewpager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        binding.appsViewpager.setOffscreenPageLimit(4);
        binding.appsViewpager.setAdapter(new ComponentDisabledPagerAdapter(this));

        new TabLayoutMediator(binding.appsSlidingTabs, binding.appsViewpager,
                (tab, position) -> tab.setText(getTabTitle(position))
        ).attach();

        binding.appsViewpager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                TabLayout.Tab currentTab = binding.appsSlidingTabs.getTabAt(position);
                if (currentTab != null) {
                    int tabIconColor = ContextCompat.getColor(requireContext(), R.color.colorAccent);
                    if (currentTab.getIcon() != null) {
                        currentTab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                    }
                }
                for (int i = 0; i < binding.appsSlidingTabs.getTabCount(); i++) {
                    if (i != position) {
                        int tabIconColor = ContextCompat.getColor(requireContext(), R.color.colorText);
                        TabLayout.Tab otherTab = binding.appsSlidingTabs.getTabAt(i);
                        if (otherTab!= null && otherTab.getIcon() != null) {
                            otherTab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                        }
                    }
                }
            }
        });

        if (binding.appsViewpager.getAdapter() != null) {
            int tabCount = binding.appsViewpager.getAdapter().getItemCount();
            for (int i = 0; i < tabCount; i++) {
                TabLayout.Tab tab = binding.appsSlidingTabs.getTabAt(i);
                if (tab != null && this.getActivity() != null && this.getActivity().getTheme() != null) {
                    tab.setIcon(imageResId[i]);
                    int tabIconColor = getResources().getColor(R.color.colorBottomNavUnselected, this.getActivity().getTheme());
                    if (tab.getIcon() != null) {
                        tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                    }
                }
            }
        }

        binding.appsViewpager.setCurrentItem(0, false);

        return binding.getRoot();
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
