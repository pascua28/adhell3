package com.fusionjack.adhell3.fragments;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.DomainPagerAdapter;

import static com.fusionjack.adhell3.fragments.DomainTabPageFragment.PROVIDER_LIST_PAGE;

public class DomainTabFragment extends Fragment {

    private final int[] imageResId = {
            R.drawable.ic_event_busy_black_24dp,
            R.drawable.ic_event_available_black_24dp,
            R.drawable.ic_event_note_black_24dp,
            R.drawable.ic_http_black_24dp
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle("Domains Management");
        AppCompatActivity parentActivity = (AppCompatActivity) getActivity();
        MainActivity mainActivity = (MainActivity) parentActivity;
        if (parentActivity.getSupportActionBar() != null) {
            parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            parentActivity.getSupportActionBar().setHomeButtonEnabled(false);
            parentActivity.getSupportActionBar().setDisplayShowCustomEnabled(false);
        }
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_domains, container, false);

        TabLayout tabLayout = view.findViewById(R.id.domains_sliding_tabs);
        ViewPager viewPager = view.findViewById(R.id.domains_viewpager);
        viewPager.setAdapter(new DomainPagerAdapter(getChildFragmentManager(), getContext()));
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setOnTabSelectedListener(
                new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {

                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        super.onTabSelected(tab);
                        int tabIconColor = ContextCompat.getColor(getContext(), R.color.colorAccent);
                        tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                        mainActivity.setSelectedDomainTab(tab.getPosition());
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                        super.onTabUnselected(tab);
                        int tabIconColor = ContextCompat.getColor(getContext(), R.color.colorText);
                        tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                        super.onTabReselected(tab);
                        int tabIconColor = ContextCompat.getColor(getContext(), R.color.colorAccent);
                        tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                        mainActivity.setSelectedDomainTab(tab.getPosition());
                    }
                }
        );
        for (int i = 0; i < imageResId.length; i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null) {
                tab.setIcon(imageResId[i]);
                int tabIconColor = ContextCompat.getColor(getContext(), R.color.colorBottomNavUnselected);
                tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
            }
        }

        // Select provider list tab as default
        TabLayout.Tab tab = tabLayout.getTabAt(mainActivity.getSelectedDomainTab());
        if (tab != null) {
            tab.select();
        }

        return view;
    }


}
