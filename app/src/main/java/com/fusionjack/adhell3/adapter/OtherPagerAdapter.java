package com.fusionjack.adhell3.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.fragments.OtherTabPageFragment;

public class OtherPagerAdapter extends FragmentStateAdapter {
    private static final int PAGE_COUNT = 3;

    public OtherPagerAdapter(Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return OtherTabPageFragment.newInstance(BuildConfig.APP_COMPONENT ? position : position + 1);
    }

    @Override
    public int getItemCount() {
        return BuildConfig.APP_COMPONENT ? PAGE_COUNT : PAGE_COUNT - 1;
    }
}
