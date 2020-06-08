package com.fusionjack.adhell3.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.fragments.AppTabPageFragment;

public class AppPagerAdapter extends FragmentStateAdapter {
    private static final int PAGE_COUNT = 4;

    public AppPagerAdapter(Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return AppTabPageFragment.newInstance(BuildConfig.DISABLE_APPS ? position : position + 1);
    }

    @Override
    public int getItemCount() {
        return BuildConfig.DISABLE_APPS ? PAGE_COUNT : PAGE_COUNT - 1;
    }
}
