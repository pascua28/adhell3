package com.fusionjack.adhell3.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.fusionjack.adhell3.fragments.DomainTabPageFragment;

public class DomainPagerAdapter extends FragmentStateAdapter {
    private static final int PAGE_COUNT = 4;

    public DomainPagerAdapter(Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return DomainTabPageFragment.newInstance(position);
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }
}
