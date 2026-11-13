package com.growbig.browser.settings;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Thin wrapper around SharedPreferences. Phase 1 stores only the few values it needs;
 * later phases add theme, JavaScript, cookie and download settings here.
 */
public class BrowserSettings {

    public static final String DEFAULT_HOMEPAGE = "https://www.google.com";
    /** %s is replaced with the URL-encoded search query. */
    public static final String DEFAULT_SEARCH_URL = "https://www.google.com/search?q=%s";

    private static final String PREFS_NAME = "growbig_settings";
    private static final String KEY_HOMEPAGE = "homepage";
    private static final String KEY_SEARCH_URL = "search_url";
    private static final String KEY_DESKTOP_DEFAULT = "desktop_mode_default";

    private final SharedPreferences prefs;

    public BrowserSettings(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getHomepage() {
        String value = prefs.getString(KEY_HOMEPAGE, DEFAULT_HOMEPAGE);
        return (value == null || value.trim().length() == 0) ? DEFAULT_HOMEPAGE : value;
    }

    public void setHomepage(String homepage) {
        prefs.edit().putString(KEY_HOMEPAGE, homepage).apply();
    }

    public String getSearchUrlTemplate() {
        String value = prefs.getString(KEY_SEARCH_URL, DEFAULT_SEARCH_URL);
        if (value == null || !value.contains("%s")) {
            return DEFAULT_SEARCH_URL;
        }
        return value;
    }

    public void setSearchUrlTemplate(String template) {
        if (template != null && template.contains("%s")) {
            prefs.edit().putString(KEY_SEARCH_URL, template).apply();
        }
    }

    public boolean isDesktopModeDefault() {
        return prefs.getBoolean(KEY_DESKTOP_DEFAULT, false);
    }

    public void setDesktopModeDefault(boolean desktop) {
        prefs.edit().putBoolean(KEY_DESKTOP_DEFAULT, desktop).apply();
    }
}
