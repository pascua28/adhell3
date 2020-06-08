package com.fusionjack.adhell3.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.fusionjack.adhell3.fragments.ComponentTabPageFragment;

public class ComponentPagerAdapter extends FragmentStateAdapter {
    private static final int PAGE_COUNT = 4;
    private final String packageName;

    public ComponentPagerAdapter(Fragment fragment, String packageName) {
        super(fragment);

        this.packageName = packageName;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return ComponentTabPageFragment.newInstance(position, packageName);
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }
}
