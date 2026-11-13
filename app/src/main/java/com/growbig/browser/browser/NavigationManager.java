package com.growbig.browser.browser;

import android.net.Uri;

import java.util.Locale;

/**
 * Turns whatever the user typed into a loadable http(s) URL or a search URL.
 * Pure logic, no Android UI, so it is easy to test later.
 */
public final class NavigationManager {

    private NavigationManager() {
    }

    /**
     * @param input          text from the address bar
     * @param searchTemplate search URL containing %s
     * @return a URL to load, or null when the input is empty
     */
    public static String resolve(String input, String searchTemplate) {
        if (input == null) {
            return null;
        }
        String text = input.trim();
        if (text.length() == 0) {
            return null;
        }

        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return text;
        }
        if (looksLikeHost(text)) {
            return "https://" + text;
        }
        return searchUrl(text, searchTemplate);
    }

    public static String searchUrl(String query, String searchTemplate) {
        return searchTemplate.replace("%s", Uri.encode(query));
    }

    public static boolean isSecure(String url) {
        return url != null && url.toLowerCase(Locale.ROOT).startsWith("https://");
    }

    public static boolean isInsecureWeb(String url) {
        return url != null && url.toLowerCase(Locale.ROOT).startsWith("http://");
    }

    /**
     * True for things like "example.com", "sub.example.org/path?x=1", "localhost:8080",
     * "192.168.0.1". False for anything with spaces and for scheme-like input such as
     * "javascript:alert(1)" (the text after the colon is not a port number).
     */
    private static boolean looksLikeHost(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return false;
            }
        }

        int end = text.length();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                end = i;
                break;
            }
        }
        String hostPort = text.substring(0, end);
        String host = hostPort;

        int colon = hostPort.lastIndexOf(':');
        if (colon >= 0) {
            String port = hostPort.substring(colon + 1);
            if (port.length() == 0 || !isDigits(port)) {
                return false;
            }
            host = hostPort.substring(0, colon);
        }

        if (host.length() == 0) {
            return false;
        }
        if (host.equalsIgnoreCase("localhost")) {
            return true;
        }
        return host.indexOf('.') > 0
                && !host.startsWith(".")
                && !host.endsWith(".")
                && host.matches("[\\p{L}\\p{N}.-]+");
    }

    private static boolean isDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
