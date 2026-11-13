@SuppressWarnings("deprecation")
@Override
public void onReceivedError(WebView view, int errorCode, String description,
                            String failingUrl) {

    // Android 4.4: ignore errors from small page resources such as favicon.
    if (!TextUtils.equals(failingUrl, view.getUrl())) {
        return;
    }

    String message = context.getString(
            isOnline() ? R.string.error_generic : R.string.error_offline);

    showErrorPage(view, failingUrl, message, description, true);
}

@TargetApi(23)
@Override
public void onReceivedError(WebView view, WebResourceRequest request,
                            WebResourceError error) {

    // Only replace the page when the main document itself fails.
    if (!request.isForMainFrame()) {
        return;
    }

    String failingUrl = request.getUrl().toString();

    String message = context.getString(
            isOnline() ? R.string.error_generic : R.string.error_offline);

    showErrorPage(view, failingUrl, message,
            String.valueOf(error.getDescription()), true);
}

@Override
public void onReceivedSslError(WebView view, SslErrorHandler handler,
                               SslError error) {

    // Never bypass SSL certificate errors.
    handler.cancel();

    // A favicon or image certificate error must not replace the whole page.
    if (!TextUtils.equals(error.getUrl(), view.getUrl())) {
        return;
    }

    String message = String.format(
            context.getString(R.string.ssl_blocked_note),
            context.getString(R.string.app_name));

    showErrorPage(view, error.getUrl(), message,
            describeSslError(error), false);
}
