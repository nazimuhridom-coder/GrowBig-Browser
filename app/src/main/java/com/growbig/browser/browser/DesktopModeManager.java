package com.growbig.browser.browser;

import android.webkit.WebSettings;

/**
 * Desktop / mobile site switching.
 *
 * This only changes the User-Agent string and the viewport behaviour. It does NOT
 * guarantee that a site will show its desktop layout, and it does not bypass any
 * site security or login checks.
 */
public final class DesktopModeManager {

    /** A generic desktop Chrome-style user agent. Change the version here when needed. */
    public static final String DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/124.0.0.0 Safari/537.36";

    private DesktopModeManager() {
    }

    /**
     * @param defaultUserAgent the WebView's own user agent, captured before any change
     */
    public static void apply(WebSettings settings, String defaultUserAgent, boolean desktop) {
        settings.setUserAgentString(desktop ? DESKTOP_USER_AGENT : defaultUserAgent);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
    }
}
