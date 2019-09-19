package com.fusionjack.adhell3.adapter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.fragments.OtherTabPageFragment;

public class OtherPagerAdapter extends FragmentPagerAdapter {
    private static final int PAGE_COUNT = 3;
    private final String[] tabTitles;

    public OtherPagerAdapter(FragmentManager fm, Context context) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        tabTitles = new String[]{
                context.getString(R.string.app_component_fragment_title),
                context.getString(R.string.dns_fragment_title),
                context.getString(R.string.settings_fragment_title)
        };
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return OtherTabPageFragment.newInstance(BuildConfig.APP_COMPONENT ? position : position + 1);
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return POSITION_NONE;
    }

    @Override
    public int getCount() {
        return BuildConfig.APP_COMPONENT ? PAGE_COUNT : PAGE_COUNT - 1;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[BuildConfig.APP_COMPONENT ? position : position + 1];
    }
}
