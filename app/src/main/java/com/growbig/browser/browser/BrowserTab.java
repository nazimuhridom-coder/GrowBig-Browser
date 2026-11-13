package com.growbig.browser.browser;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.growbig.browser.R;

import java.util.Locale;

/**
 * One browser tab = one WebView plus the state the UI needs (url, title, progress).
 * All callbacks arrive on the UI thread.
 */
public class BrowserTab {

    /** Notified whenever url, title, progress, loading state or desktop mode changes. */
    public interface Listener {
        void onTabChanged(BrowserTab tab);
    }

    /** Title of our own error page; lets us recognise it. */
    private static final String ERROR_PAGE_MARKER = "growbig-error-page";

    private final Context context;
    private final Listener listener;
    private final WebView webView;
    private final String defaultUserAgent;

    private String url = "";
    private String title = "";
    private int progress = 0;
    private boolean loading = false;
    private boolean desktopMode;

    /** The address that failed, while our error page is shown. Otherwise null. */
    private String errorUrl = null;
    private String pendingErrorUrl = null;

    @SuppressLint("SetJavaScriptEnabled")
    public BrowserTab(Context context, Listener listener, boolean desktopMode) {
        this.context = context;
        this.listener = listener;
        this.desktopMode = desktopMode;

        webView = new WebView(context);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setBackgroundColor(Color.WHITE);

        WebSettings s = webView.getSettings();
        defaultUserAgent = s.getUserAgentString();

        // JavaScript is needed by most modern sites. A user switch comes in a later phase.
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);

        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(true);
        s.setSupportMultipleWindows(false); // pop-ups are not opened (pop-up control: later phase)

        // Web pages must not read local files or content providers.
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);

        if (Build.VERSION.SDK_INT >= 21) {
            // Same behaviour as Chrome: block active mixed content, allow passive (images).
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
        if (Build.VERSION.SDK_INT >= 26) {
            s.setSafeBrowsingEnabled(true);
        }

        DesktopModeManager.apply(s, defaultUserAgent, desktopMode);

        webView.setWebViewClient(new TabWebViewClient());
        webView.setWebChromeClient(new TabChromeClient());
    }

    // ---------------------------------------------------------------- public API

    public WebView getWebView() {
        return webView;
    }

    public String getUrl() {
        return url;
    }

    public String getDisplayTitle() {
        if (!TextUtils.isEmpty(title)) {
            return title;
        }
        if (!TextUtils.isEmpty(url)) {
            return url;
        }
        return context.getString(R.string.new_tab_title);
    }

    public int getProgress() {
        return progress;
    }

    public boolean isLoading() {
        return loading;
    }

    public boolean isDesktopMode() {
        return desktopMode;
    }

    public boolean canGoBack() {
        return webView.canGoBack();
    }

    public boolean canGoForward() {
        return webView.canGoForward();
    }

    public void load(String targetUrl) {
        errorUrl = null;
        url = targetUrl;
        webView.loadUrl(targetUrl);
        notifyChanged();
    }

    public void goBack() {
        if (webView.canGoBack()) {
            webView.goBack();
        }
    }

    public void goForward() {
        if (webView.canGoForward()) {
            webView.goForward();
        }
    }

    public void reload() {
        if (errorUrl != null) {
            // The visible page is our error page, so retry the real address.
            webView.loadUrl(errorUrl);
        } else {
            webView.reload();
        }
    }

    public void stop() {
        webView.stopLoading();
        loading = false;
        notifyChanged();
    }

    public void setDesktopMode(boolean desktop) {
        if (desktopMode == desktop) {
            return;
        }
        desktopMode = desktop;
        DesktopModeManager.apply(webView.getSettings(), defaultUserAgent, desktop);
        reload();
        notifyChanged();
    }

    public void zoomIn() {
        webView.zoomIn();
    }

    public void zoomOut() {
        webView.zoomOut();
    }

    public void onPause() {
        webView.onPause();
    }

    public void onResume() {
        webView.onResume();
    }

    /** Releases the WebView. The tab must not be used afterwards. */
    public void destroy() {
        ViewParent parent = webView.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(webView);
        }
        webView.stopLoading();
        webView.destroy();
    }

    // ---------------------------------------------------------------- internals

    private void notifyChanged() {
        if (listener != null) {
            listener.onTabChanged(this);
        }
    }

    @SuppressWarnings("deprecation")
    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return true;
        }
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    /** Only http(s) pages are loaded inside the tab. */
    private boolean handleUrl(String target) {
        if (target == null) {
            return true;
        }
        String lower = target.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")
                || lower.startsWith("javascript:") || lower.equals("about:blank")) {
            return false; // let the WebView load it
        }
        if (lower.startsWith("tel:") || lower.startsWith("mailto:") || lower.startsWith("sms:")) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(target));
                context.startActivity(intent);
            } catch (ActivityNotFoundException ignored) {
                // No app can handle it; nothing else to do.
            }
        }
        return true; // everything else (file:, intent:, data: ...) is blocked
    }

    private void showErrorPage(WebView view, String failingUrl, String message,
                               String detail, boolean offerHttp) {
        pendingErrorUrl = failingUrl;
        String safeUrl = TextUtils.htmlEncode(failingUrl == null ? "" : failingUrl);

        StringBuilder html = new StringBuilder();
        html.append("<html><head><title>").append(ERROR_PAGE_MARKER).append("</title>")
                .append("<meta name='viewport' content='width=device-width, initial-scale=1'>")
                .append("<style>body{font-family:sans-serif;background:#202124;color:#e8eaed;")
                .append("padding:24px;}h2{font-weight:normal;}a{color:#8ab4f8;}")
                .append(".s{color:#9aa0a6;font-size:13px;word-break:break-all;}</style>")
                .append("</head><body><h2>")
                .append(TextUtils.htmlEncode(context.getString(R.string.error_title)))
                .append("</h2><p>").append(TextUtils.htmlEncode(message)).append("</p>");
        if (detail != null && detail.length() > 0) {
            html.append("<p class='s'>").append(TextUtils.htmlEncode(detail)).append("</p>");
        }
        html.append("<p class='s'>").append(safeUrl).append("</p>");
        if (failingUrl != null && failingUrl.length() > 0) {
            html.append("<p><a href=\"").append(safeUrl).append("\">")
                    .append(TextUtils.htmlEncode(context.getString(R.string.error_retry)))
                    .append("</a></p>");
            if (offerHttp && failingUrl.toLowerCase(Locale.ROOT).startsWith("https://")) {
                String httpUrl = "http://" + failingUrl.substring("https://".length());
                html.append("<p><a href=\"").append(TextUtils.htmlEncode(httpUrl)).append("\">")
                        .append(TextUtils.htmlEncode(context.getString(R.string.error_try_http)))
                        .append("</a></p>");
            }
        }
        html.append("</body></html>");

        view.loadDataWithBaseURL(failingUrl, html.toString(), "text/html", "UTF-8", failingUrl);
    }

    private String describeSslError(SslError error) {
        int resId;
        switch (error.getPrimaryError()) {
            case SslError.SSL_UNTRUSTED:
                resId = R.string.ssl_untrusted;
                break;
            case SslError.SSL_EXPIRED:
                resId = R.string.ssl_expired;
                break;
            case SslError.SSL_IDMISMATCH:
                resId = R.string.ssl_idmismatch;
                break;
            case SslError.SSL_NOTYETVALID:
                resId = R.string.ssl_notyetvalid;
                break;
            default:
                resId = R.string.ssl_generic;
                break;
        }
        return context.getString(resId);
    }

    // ---------------------------------------------------------------- WebView clients

    private class TabWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String pageUrl, Bitmap favicon) {
            url = pageUrl == null ? "" : pageUrl;
            title = "";
            errorUrl = null;
            loading = true;
            progress = 0;
            notifyChanged();
        }

        @Override
        public void onPageFinished(WebView view, String pageUrl) {
            loading = false;
            progress = 100;
            String pageTitle = view.getTitle();
            if (ERROR_PAGE_MARKER.equals(pageTitle)) {
                errorUrl = pendingErrorUrl != null ? pendingErrorUrl : pageUrl;
                url = errorUrl == null ? "" : errorUrl;
                title = context.getString(R.string.error_title);
            } else {
                errorUrl = null;
                url = pageUrl == null ? "" : pageUrl;
                if (!TextUtils.isEmpty(pageTitle)) {
                    title = pageTitle;
                }
            }
            notifyChanged();
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String target) {
            return handleUrl(target);
        }

        @TargetApi(24)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return handleUrl(request.getUrl().toString());
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onReceivedError(WebView view, int errorCode, String description,
                                    String failingUrl) {
            String message = context.getString(
                    isOnline() ? R.string.error_generic : R.string.error_offline);
            showErrorPage(view, failingUrl, message, description, true);
        }

        @TargetApi(23)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request,
                                    WebResourceError error) {
            if (request.isForMainFrame()) {
                String message = context.getString(
                        isOnline() ? R.string.error_generic : R.string.error_offline);
                showErrorPage(view, request.getUrl().toString(), message,
                        String.valueOf(error.getDescription()), true);
            }
        }

        /**
         * SECURITY: certificate errors are never ignored. The connection is cancelled and
         * an error page is shown. There is deliberately no "proceed anyway" path.
         */
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.cancel();
            String message = String.format(
                    context.getString(R.string.ssl_blocked_note),
                    context.getString(R.string.app_name));
            showErrorPage(view, error.getUrl(), message, describeSslError(error), false);
        }
    }

    private class TabChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progress = newProgress;
            loading = newProgress < 100;
            notifyChanged();
        }

        @Override
        public void onReceivedTitle(WebView view, String receivedTitle) {
            if (!TextUtils.isEmpty(receivedTitle) && !ERROR_PAGE_MARKER.equals(receivedTitle)) {
                title = receivedTitle;
                notifyChanged();
            }
        }
    }
}
