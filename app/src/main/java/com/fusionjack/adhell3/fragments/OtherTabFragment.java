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

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.OtherPagerAdapter;
import com.fusionjack.adhell3.databinding.FragmentOthersBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import static com.fusionjack.adhell3.fragments.OtherTabPageFragment.SETTINGS_PAGE;

public class OtherTabFragment extends Fragment {

    private TabLayoutMediator tabLayoutMediator;
    private FragmentOthersBinding binding;

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
        if (getActivity() != null) {
            getActivity().setTitle("Others");
        }
        setHasOptionsMenu(true);

        binding = FragmentOthersBinding.inflate(inflater);

        binding.othersViewpager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        binding.othersViewpager.setOffscreenPageLimit(BuildConfig.APP_COMPONENT ? 3 : 2);
        binding.othersViewpager.setAdapter(new OtherPagerAdapter(this));

        tabLayoutMediator = new TabLayoutMediator(binding.othersSlidingTabs, binding.othersViewpager,
                (tab, position) -> tab.setText(getTabTitle(position))
        );
        tabLayoutMediator.attach();

        binding.othersViewpager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                TabLayout.Tab currentTab = binding.othersSlidingTabs.getTabAt(position);
                if (currentTab != null) {
                    int tabIconColor = ContextCompat.getColor(requireContext(), R.color.colorAccent);
                    if (currentTab.getIcon() != null) {
                        currentTab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                    }
                }
                for (int i = 0; i < binding.othersSlidingTabs.getTabCount(); i++) {
                    if (i != position) {
                        int tabIconColor = ContextCompat.getColor(requireContext(), R.color.colorText);
                        TabLayout.Tab otherTab = binding.othersSlidingTabs.getTabAt(i);
                        if (otherTab!= null && otherTab.getIcon() != null) {
                            otherTab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                        }
                    }
                }
                MainActivity.setSelectedOtherTab(position);
            }
        });

        int imageIndex = BuildConfig.APP_COMPONENT ? 0 : 1;
        if (binding.othersViewpager.getAdapter() != null) {
            int tabCount = binding.othersViewpager.getAdapter().getItemCount();
            for (int i = 0; i < tabCount; i++, imageIndex++) {
                TabLayout.Tab tab = binding.othersSlidingTabs.getTabAt(i);
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
            //MainActivity.themeChanged = false;
        }

        binding.othersViewpager.setCurrentItem(MainActivity.getSelectedOtherTab(), false);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        tabLayoutMediator.detach();
        tabLayoutMediator = null;
        binding.othersViewpager.setAdapter(null);
        binding = null;
        super.onDestroyView();
    }

    private String getTabTitle(int position) {
        String[] tabTitles = new String[]{
                requireContext().getString(R.string.app_component_fragment_title),
                requireContext().getString(R.string.dns_fragment_title),
                requireContext().getString(R.string.settings_fragment_title)};

        return tabTitles[position];
    }
}
