package com.fusionjack.adhell3.fragments;

import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.fusionjack.adhell3.R;
import com.google.android.material.tabs.TabLayout;

public class TabFragment extends Fragment implements TabLayout.OnTabSelectedListener {

    private TabLayout tabLayout;

    protected View inflateFragment(int fragmentViewId, LayoutInflater inflater, ViewGroup container,
                                   FragmentPagerAdapter adapter, int screenLimit, int[] imageResId, int offsetImageId) {
        View view = inflater.inflate(fragmentViewId, container, false);

        this.tabLayout = view.findViewById(R.id.sliding_tabs);
        ViewPager viewPager = view.findViewById(R.id.viewpager);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(screenLimit);
        tabLayout.setupWithViewPager(viewPager);

        int tabCount = viewPager.getAdapter().getCount();
        for (int i = 0; i < tabCount; i++, offsetImageId++) {
            tabLayout.getTabAt(i).setIcon(imageResId[offsetImageId]);
            int tabIconColor = ContextCompat.getColor(getContext(), R.color.colorBottomNavUnselected);
            tabLayout.getTabAt(i).getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        tabLayout.addOnTabSelectedListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        tabLayout.removeOnTabSelectedListener(this);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        int tabIconColor = ContextCompat.getColor(getContext(), R.color.colorAccent);
        tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        int tabIconColor = ContextCompat.getColor(getContext(), R.color.colorText);
        tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        int tabIconColor = ContextCompat.getColor(getContext(), R.color.colorAccent);
        tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
    }

}
