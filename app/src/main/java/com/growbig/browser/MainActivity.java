package com.growbig.browser;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.growbig.browser.browser.BrowserTab;
import com.growbig.browser.browser.NavigationManager;
import com.growbig.browser.browser.TabManager;
import com.growbig.browser.settings.BrowserSettings;
import com.growbig.browser.ui.ToolbarIcon;

/**
 * The single screen of the browser. It only wires the UI to the tab and navigation classes;
 * browser logic lives in the browser/ package.
 */
public class MainActivity extends Activity {

    private static final int COLOR_SECURE = 0xFF81C995;
    private static final int COLOR_INSECURE = 0xFFF28B82;
    private static final int COLOR_NEUTRAL = 0xFF9AA0A6;

    private BrowserSettings settings;
    private TabManager tabManager;

    private HorizontalScrollView tabScroll;
    private LinearLayout tabContainer;
    private ToolbarIcon btnNewTab;
    private ToolbarIcon btnBack;
    private ToolbarIcon btnForward;
    private ToolbarIcon btnRefresh;
    private ToolbarIcon btnHome;
    private ToolbarIcon btnMenu;
    private ToolbarIcon btnClear;
    private ToolbarIcon iconSecure;
    private EditText addressInput;
    private ProgressBar progressBar;
    private FrameLayout webContainer;

    private final BrowserTab.Listener tabListener = new BrowserTab.Listener() {
        @Override
        public void onTabChanged(BrowserTab tab) {
            updateTabTitle(tab);
            if (tab == tabManager.getActive()) {
                updateToolbar(tab);
            }
        }
    };

    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = new BrowserSettings(this);
        tabManager = new TabManager(this, tabListener);

        bindViews();
        setupToolbar();
        setupAddressBar();

        String initial = urlFromIntent(getIntent());
        openNewTab(initial != null ? initial : settings.getHomepage());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String url = urlFromIntent(intent);
        if (url != null) {
            openNewTab(url);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        BrowserTab tab = tabManager.getActive();
        if (tab != null) {
            tab.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        BrowserTab tab = tabManager.getActive();
        if (tab != null) {
            tab.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        tabManager.destroyAll();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        BrowserTab tab = tabManager.getActive();
        if (tab != null && tab.canGoBack()) {
            tab.goBack();
        } else if (tabManager.size() > 1) {
            closeTab(tabManager.getActiveIndex());
        } else {
            super.onBackPressed();
        }
    }

    // ---------------------------------------------------------------- setup

    private void bindViews() {
        tabScroll = (HorizontalScrollView) findViewById(R.id.tab_scroll);
        tabContainer = (LinearLayout) findViewById(R.id.tab_container);
        btnNewTab = (ToolbarIcon) findViewById(R.id.btn_new_tab);
        btnBack = (ToolbarIcon) findViewById(R.id.btn_back);
        btnForward = (ToolbarIcon) findViewById(R.id.btn_forward);
        btnRefresh = (ToolbarIcon) findViewById(R.id.btn_refresh);
        btnHome = (ToolbarIcon) findViewById(R.id.btn_home);
        btnMenu = (ToolbarIcon) findViewById(R.id.btn_menu);
        btnClear = (ToolbarIcon) findViewById(R.id.btn_clear);
        iconSecure = (ToolbarIcon) findViewById(R.id.icon_secure);
        addressInput = (EditText) findViewById(R.id.address_input);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        webContainer = (FrameLayout) findViewById(R.id.web_container);
    }

    private void setupToolbar() {
        btnBack.setOnClickListener(v -> {
            BrowserTab tab = tabManager.getActive();
            if (tab != null) {
                tab.goBack();
            }
        });
        btnForward.setOnClickListener(v -> {
            BrowserTab tab = tabManager.getActive();
            if (tab != null) {
                tab.goForward();
            }
        });
        btnRefresh.setOnClickListener(v -> {
            BrowserTab tab = tabManager.getActive();
            if (tab == null) {
                return;
            }
            if (tab.isLoading()) {
                tab.stop();
            } else {
                tab.reload();
            }
        });
        btnHome.setOnClickListener(v -> {
            BrowserTab tab = tabManager.getActive();
            if (tab != null) {
                tab.load(homeUrl());
            }
        });
        btnNewTab.setOnClickListener(v -> openNewTab(settings.getHomepage()));
        btnMenu.setOnClickListener(v -> showMenu());
    }

    private void setupAddressBar() {
        addressInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean enterKey = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_GO || enterKey) {
                submitAddress();
                return true;
            }
            return false;
        });

        addressInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateClearButton();
            }
        });

        addressInput.setOnFocusChangeListener((v, hasFocus) -> {
            updateClearButton();
            if (!hasFocus) {
                // Show the real address again if the user left without submitting.
                BrowserTab tab = tabManager.getActive();
                if (tab != null) {
                    setAddressText(tab.getUrl());
                }
            }
        });

        btnClear.setOnClickListener(v -> {
            addressInput.setText("");
            addressInput.requestFocus();
        });
    }

    // ---------------------------------------------------------------- address bar

    private void submitAddress() {
        String target = NavigationManager.resolve(
                addressInput.getText().toString(), settings.getSearchUrlTemplate());
        if (target == null) {
            return;
        }
        hideKeyboard();
        addressInput.clearFocus();
        webContainer.requestFocus();

        BrowserTab tab = tabManager.getActive();
        if (tab != null) {
            tab.load(target);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(addressInput.getWindowToken(), 0);
        }
    }

    private void updateClearButton() {
        boolean show = addressInput.hasFocus() && addressInput.length() > 0;
        btnClear.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void setAddressText(String text) {
        if (!text.equals(addressInput.getText().toString())) {
            addressInput.setText(text);
        }
    }

    // ---------------------------------------------------------------- tabs

    private String homeUrl() {
        String resolved = NavigationManager.resolve(
                settings.getHomepage(), settings.getSearchUrlTemplate());
        return resolved != null ? resolved : BrowserSettings.DEFAULT_HOMEPAGE;
    }

    private void openNewTab(String raw) {
        String target = NavigationManager.resolve(raw, settings.getSearchUrlTemplate());
        if (target == null) {
            target = homeUrl();
        }
        BrowserTab tab = tabManager.openTab(settings.isDesktopModeDefault());
        if (tab == null) {
            Toast.makeText(this, R.string.tab_limit_reached, Toast.LENGTH_SHORT).show();
            return;
        }
        activate(tabManager.size() - 1);
        tab.load(target);
    }

    /** Switches to a tab that is still alive. */
    private void activate(int index) {
        BrowserTab old = tabManager.getActive();
        if (old != null) {
            old.onPause();
        }
        tabManager.setActiveIndex(index);
        showActive();
    }

    private void closeTab(int index) {
        boolean wasActive = index == tabManager.getActiveIndex();
        tabManager.closeTab(index);

        if (tabManager.size() == 0) {
            openNewTab(settings.getHomepage());
        } else if (wasActive) {
            showActive();
        } else {
            rebuildTabStrip();
        }
    }

    /** Puts the active tab's WebView on screen and refreshes the toolbar. */
    private void showActive() {
        BrowserTab tab = tabManager.getActive();
        if (tab == null) {
            return;
        }
        webContainer.removeAllViews();
        webContainer.addView(tab.getWebView());
        tab.onResume();
        rebuildTabStrip();
        updateToolbar(tab);
    }

    private void rebuildTabStrip() {
        tabContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int active = tabManager.getActiveIndex();

        for (int i = 0; i < tabManager.size(); i++) {
            final int index = i;
            BrowserTab tab = tabManager.getTab(i);

            View item = inflater.inflate(R.layout.item_tab, tabContainer, false);
            TextView title = (TextView) item.findViewById(R.id.tab_title);
            title.setText(tab.getDisplayTitle());
            item.setBackgroundResource(i == active ? R.drawable.tab_active : R.drawable.tab_inactive);

            item.setOnClickListener(v -> {
                if (index != tabManager.getActiveIndex()) {
                    activate(index);
                }
            });
            item.findViewById(R.id.tab_close).setOnClickListener(v -> closeTab(index));
            tabContainer.addView(item);
        }

        tabScroll.post(() -> {
            View current = tabContainer.getChildAt(tabManager.getActiveIndex());
            if (current != null) {
                tabScroll.smoothScrollTo(current.getLeft(), 0);
            }
        });
    }

    private void updateTabTitle(BrowserTab tab) {
        int index = tabManager.indexOf(tab);
        if (index < 0 || index >= tabContainer.getChildCount()) {
            return;
        }
        TextView title = (TextView) tabContainer.getChildAt(index).findViewById(R.id.tab_title);
        String text = tab.getDisplayTitle();
        if (!text.contentEquals(title.getText())) {
            title.setText(text);
        }
    }

    private void updateToolbar(BrowserTab tab) {
        btnBack.setEnabled(tab.canGoBack());
        btnForward.setEnabled(tab.canGoForward());
        btnRefresh.setIconType(tab.isLoading() ? ToolbarIcon.STOP : ToolbarIcon.REFRESH);

        progressBar.setProgress(tab.getProgress());
        progressBar.setVisibility(tab.isLoading() ? View.VISIBLE : View.GONE);

        String url = tab.getUrl();
        if (NavigationManager.isSecure(url)) {
            iconSecure.setIconType(ToolbarIcon.LOCK);
            iconSecure.setIconColor(COLOR_SECURE);
        } else if (NavigationManager.isInsecureWeb(url)) {
            iconSecure.setIconType(ToolbarIcon.LOCK_OPEN);
            iconSecure.setIconColor(COLOR_INSECURE);
        } else {
            iconSecure.setIconType(ToolbarIcon.SEARCH);
            iconSecure.setIconColor(COLOR_NEUTRAL);
        }

        if (!addressInput.hasFocus()) {
            setAddressText(url);
        }
    }

    // ---------------------------------------------------------------- menu

    private void showMenu() {
        BrowserTab tab = tabManager.getActive();
        PopupMenu popup = new PopupMenu(this, btnMenu);
        popup.getMenuInflater().inflate(R.menu.browser_menu, popup.getMenu());
        MenuItem desktop = popup.getMenu().findItem(R.id.menu_desktop);
        if (tab != null) {
            desktop.setChecked(tab.isDesktopMode());
        }
        popup.setOnMenuItemClickListener(item -> handleMenuAction(item.getItemId()));
        popup.show();
    }

    // if/else instead of switch: resource ids are not compile-time constants in new AGP versions.
    private boolean handleMenuAction(int id) {
        BrowserTab tab = tabManager.getActive();

        if (id == R.id.menu_new_tab) {
            openNewTab(settings.getHomepage());
        } else if (id == R.id.menu_desktop) {
            if (tab != null) {
                boolean desktop = !tab.isDesktopMode();
                tab.setDesktopMode(desktop);
                settings.setDesktopModeDefault(desktop); // remembered for new tabs
            }
        } else if (id == R.id.menu_zoom_in) {
            if (tab != null) {
                tab.zoomIn();
            }
        } else if (id == R.id.menu_zoom_out) {
            if (tab != null) {
                tab.zoomOut();
            }
        } else if (id == R.id.menu_share) {
            shareCurrentUrl(tab);
        } else if (id == R.id.menu_copy_link) {
            copyCurrentUrl(tab);
        } else if (id == R.id.menu_open_external) {
            openExternally(tab);
        } else if (id == R.id.menu_exit) {
            finish();
        } else {
            return false;
        }
        return true;
    }

    private String currentUrlOrToast(BrowserTab tab) {
        String url = tab == null ? "" : tab.getUrl();
        if (url.length() == 0) {
            Toast.makeText(this, R.string.no_page_to_share, Toast.LENGTH_SHORT).show();
            return null;
        }
        return url;
    }

    private void shareCurrentUrl(BrowserTab tab) {
        String url = currentUrlOrToast(tab);
        if (url == null) {
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, url);
        startActivity(Intent.createChooser(send, getString(R.string.share_via)));
    }

    private void copyCurrentUrl(BrowserTab tab) {
        String url = currentUrlOrToast(tab);
        if (url == null) {
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("URL", url));
            Toast.makeText(this, R.string.link_copied, Toast.LENGTH_SHORT).show();
        }
    }

    /** Fallback for pages the built-in WebView cannot handle. The chooser may list this app too. */
    private void openExternally(BrowserTab tab) {
        String url = currentUrlOrToast(tab);
        if (url == null) {
            return;
        }
        try {
            Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(Intent.createChooser(view, getString(R.string.open_with)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_app_found, Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------------------------------------------------------- incoming links

    private static String urlFromIntent(Intent intent) {
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())) {
            return null;
        }
        Uri data = intent.getData();
        if (data == null) {
            return null;
        }
        String scheme = data.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            return data.toString();
        }
        return null;
    }
}
