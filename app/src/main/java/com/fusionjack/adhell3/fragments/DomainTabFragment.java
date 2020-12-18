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
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback;

import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.DomainPagerAdapter;
import com.fusionjack.adhell3.databinding.FragmentDomainsBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class DomainTabFragment extends Fragment {

    private TabLayoutMediator tabLayoutMediator;
    FragmentDomainsBinding binding;

    private final int[] imageResId = {
            R.drawable.ic_event_busy_black_24dp,
            R.drawable.ic_event_available_black_24dp,
            R.drawable.ic_event_note_black_24dp,
            R.drawable.ic_http_black_24dp
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() != null) {
            getActivity().setTitle("Domains Management");
        }
        setHasOptionsMenu(true);

        binding = FragmentDomainsBinding.inflate(inflater);

        binding.domainsViewpager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        binding.domainsViewpager.setOffscreenPageLimit(4);
        binding.domainsViewpager.setAdapter(new DomainPagerAdapter(this));

        tabLayoutMediator = new TabLayoutMediator(binding.domainsSlidingTabs, binding.domainsViewpager,
                (tab, position) -> tab.setText(getTabTitle(position))
        );
        tabLayoutMediator.attach();

        binding.domainsViewpager.registerOnPageChangeCallback(new OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                TabLayout.Tab currentTab = binding.domainsSlidingTabs.getTabAt(position);
                if (currentTab != null) {
                    int tabIconColor = ContextCompat.getColor(requireContext(), R.color.colorAccent);
                    if (currentTab.getIcon() != null) {
                        currentTab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                    }
                }
                for (int i = 0; i < binding.domainsSlidingTabs.getTabCount(); i++) {
                    if (i != position) {
                        int tabIconColor = ContextCompat.getColor(requireContext(), R.color.colorText);
                        TabLayout.Tab otherTab = binding.domainsSlidingTabs.getTabAt(i);
                        if (otherTab!= null && otherTab.getIcon() != null) {
                            otherTab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                        }
                    }
                }
                MainActivity.setSelectedDomainTab(position);
            }
        });

        for (int i = 0; i < imageResId.length; i++) {
            TabLayout.Tab tab = binding.domainsSlidingTabs.getTabAt(i);
            if (tab != null && this.getActivity() != null && this.getActivity().getTheme() != null) {
                tab.setIcon(imageResId[i]);
                int tabIconColor = getResources().getColor(R.color.colorBottomNavUnselected, this.getActivity().getTheme());
                if (tab.getIcon() != null) {
                    tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                }
            }
        }

        binding.domainsViewpager.setCurrentItem(MainActivity.getSelectedDomainTab(), false);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        tabLayoutMediator.detach();
        tabLayoutMediator = null;
        binding.domainsViewpager.setAdapter(null);
        binding = null;
        super.onDestroyView();
    }

    private String getTabTitle(int position) {
        String[] tabTitles = new String[]{
                requireContext().getString(R.string.blacklist_url_fragment_title),
                requireContext().getString(R.string.whitelist_url_fragment_title),
                requireContext().getString(R.string.provider_url_fragment_title),
                requireContext().getString(R.string.list_url_fragment_title)
        };
        return tabTitles[position];
    }
}
