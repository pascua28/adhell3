package com.fusionjack.adhell3.adapter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.fragments.ComponentTabPageFragment;

public class ComponentPagerAdapter extends FragmentPagerAdapter {
    private static final int PAGE_COUNT = 5;

    private final String[] tabTitles;
    private final String packageName;
    private final boolean isDisabledComponentMode;
    private final int[] pages;

    public ComponentPagerAdapter(FragmentManager fm, Context context, String packageName, boolean isDisabledComponentMode, int[] pages) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        tabTitles = new String[] {
                context.getString(R.string.permission_fragment_title),
                context.getString(R.string.activity_fragment_title),
                context.getString(R.string.service_fragment_title),
                context.getString(R.string.receiver_fragment_title),
                context.getString(R.string.content_provider_fragment_title)
        };

        this.packageName = packageName;
        this.isDisabledComponentMode = isDisabledComponentMode;
        this.pages = pages;
    }

    private int getPage(int position) {
        if (isDisabledComponentMode && pages.length > 0) {
            return pages[position];
        }
        return position;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return ComponentTabPageFragment.newInstance(getPage(position), packageName, isDisabledComponentMode);
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return POSITION_NONE;
    }

    @Override
    public int getCount() {
        return isDisabledComponentMode ? pages.length : PAGE_COUNT;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[getPage(position)];
    }
}
