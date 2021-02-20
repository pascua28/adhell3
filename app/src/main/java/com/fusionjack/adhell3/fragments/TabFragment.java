package com.fusionjack.adhell3.fragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.fusionjack.adhell3.R;
import com.google.android.material.tabs.TabLayout;

public class TabFragment extends Fragment {

    protected View inflateFragment(int fragmentViewId, LayoutInflater inflater, ViewGroup container,
                                   FragmentPagerAdapter adapter, int screenLimit, int[] imageResId, int offsetImageId) {
        View view = inflater.inflate(fragmentViewId, container, false);

        TabLayout tabLayout = view.findViewById(R.id.sliding_tabs);
        ViewPager viewPager = view.findViewById(R.id.viewpager);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(screenLimit);
        tabLayout.setupWithViewPager(viewPager);

        int tabCount = viewPager.getAdapter().getCount();
        for (int i = 0; i < tabCount; i++, offsetImageId++) {
            tabLayout.getTabAt(i).setIcon(imageResId[offsetImageId]);
        }

        return view;
    }

}
