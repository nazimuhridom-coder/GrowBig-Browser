package com.growbig.browser.browser;

import android.app.ActivityManager;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the list of tabs and which one is active.
 *
 * Low-RAM policy (Phase 1): the number of open tabs, and therefore live WebViews, is capped.
 * Later phases will also suspend background tabs and restore them from saved state.
 */
public class TabManager {

    private static final int MAX_TABS_NORMAL = 8;
    private static final int MAX_TABS_LOW_RAM = 4;

    private final Context context;
    private final BrowserTab.Listener listener;
    private final List<BrowserTab> tabs = new ArrayList<BrowserTab>();
    private final int maxTabs;
    private int activeIndex = -1;

    public TabManager(Context context, BrowserTab.Listener listener) {
        this.context = context;
        this.listener = listener;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        boolean lowRam = am != null && am.isLowRamDevice();
        this.maxTabs = lowRam ? MAX_TABS_LOW_RAM : MAX_TABS_NORMAL;
    }

    public boolean isFull() {
        return tabs.size() >= maxTabs;
    }

    /** Creates a tab and adds it at the end. Returns null when the tab limit is reached. */
    public BrowserTab openTab(boolean desktopMode) {
        if (isFull()) {
            return null;
        }
        BrowserTab tab = new BrowserTab(context, listener, desktopMode);
        tabs.add(tab);
        return tab;
    }

    /** Destroys the tab and fixes the active index. The caller must show the new active tab. */
    public void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) {
            return;
        }
        BrowserTab removed = tabs.remove(index);
        removed.destroy();

        if (tabs.isEmpty()) {
            activeIndex = -1;
        } else if (index < activeIndex) {
            activeIndex--;
        } else if (index == activeIndex) {
            activeIndex = Math.min(index, tabs.size() - 1);
        }
    }

    public void destroyAll() {
        for (BrowserTab tab : tabs) {
            tab.destroy();
        }
        tabs.clear();
        activeIndex = -1;
    }

    public int size() {
        return tabs.size();
    }

    public BrowserTab getTab(int index) {
        return tabs.get(index);
    }

    public int indexOf(BrowserTab tab) {
        return tabs.indexOf(tab);
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(int index) {
        if (index >= 0 && index < tabs.size()) {
            activeIndex = index;
        }
    }

    public BrowserTab getActive() {
        if (activeIndex >= 0 && activeIndex < tabs.size()) {
            return tabs.get(activeIndex);
        }
        return null;
    }
}
