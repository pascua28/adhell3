package com.fusionjack.adhell3.adapter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.fragments.ComponentDisabledTabPageFragment;

public class ComponentDisabledPagerAdapter extends FragmentStatePagerAdapter {
    private static final int PAGE_COUNT = 3;
    private final String[] tabTitles;

    public ComponentDisabledPagerAdapter(FragmentManager fragmentManager, Context context) {
        super(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        tabTitles = new String[]{
                context.getString(R.string.permission_fragment_title),
                context.getString(R.string.service_fragment_title),
                context.getString(R.string.receiver_fragment_title)};
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return ComponentDisabledTabPageFragment.newInstance(position);
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[position];
    }
}
