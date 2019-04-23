package com.fusionjack.adhell3.blocker;

import android.os.Handler;

import com.fusionjack.adhell3.fragments.HomeTabFragment;

public interface ContentBlocker {
    void enableDomainRules(boolean updateProviders);

    void updateAllRules(boolean updateProviders, HomeTabFragment parentFragment);

    void disableDomainRules();

    void enableFirewallRules();

    void disableFirewallRules();

    boolean isEnabled();

    boolean isDomainRuleEmpty();

    boolean isFirewallRuleEmpty();

    void setHandler(Handler handler);
}
