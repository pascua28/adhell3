package com.fusionjack.adhell3.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.fusionjack.adhell3.fragments.ComponentDisabledTabPageFragment;

public class ComponentDisabledPagerAdapter extends FragmentStateAdapter {
    private static final int PAGE_COUNT = 5;

    public ComponentDisabledPagerAdapter(Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return ComponentDisabledTabPageFragment.newInstance(position);
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }
}
