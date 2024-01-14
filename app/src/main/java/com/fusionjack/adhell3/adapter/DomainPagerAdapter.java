package com.fusionjack.adhell3.adapter;

import android.content.Context;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.fragments.DomainTabPageFragment;

public class DomainPagerAdapter extends FragmentPagerAdapter {
    private static final int PAGE_COUNT = 5;
    private String tabTitles[];

    public DomainPagerAdapter(FragmentManager fm, Context context) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        tabTitles = new String[] {
                context.getString(R.string.firewall_url_fragment_titile),
                context.getString(R.string.blacklist_url_fragment_title),
                context.getString(R.string.whitelist_url_fragment_title),
                context.getString(R.string.provider_url_fragment_title),
                context.getString(R.string.list_url_fragment_title)
        };
    }

    @Override
    public Fragment getItem(int position) {
        return DomainTabPageFragment.newInstance(position);
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
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
