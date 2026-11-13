# Growbig Browser — architecture and compatibility notes

## Android 4.4 (API 19) risks

| Risk | What this project does |
|---|---|
| Android 4.4 ships a frozen Chromium 30-based WebView; it is not updated through Google Play. Many modern sites (Facebook, YouTube, Gmail, WhatsApp Web, login flows) can fail or refuse it. | Not hidden or faked. "Open in another browser" is in the menu. Real results go in the test checklist. |
| Newer AndroidX/Material libraries need minSdk 21 or higher. | Phase 1 has **zero** dependencies. Any dependency added later must be checked for its minSdk first. |
| Vector drawables do not work on API 19 without AndroidX. | Icons are drawn in code (`ui/ToolbarIcon`). |
| TLS 1.2 is not enabled by default on some 4.4 devices, so some https sites fail. | Shown as a normal error page. Certificate checks are never weakened. |
| Method count / multidex. | App is tiny; not an issue yet. Re-check when libraries are added. |
| Modern WebView APIs do not exist on old versions. | All of them are behind `Build.VERSION.SDK_INT` checks (below). |
| Android 9+ blocks cleartext http by default. | `usesCleartextTraffic="true"` because a browser must open http pages; http pages show a red open lock. |

## Versions chosen

- Gradle 8.4, Android Gradle Plugin 8.1.4, JDK 17, Java 8 language level (desugared for API 19)
- compileSdk 34, targetSdk 34, minSdk 19
- Dependencies: none
- AGP 8.1.4 is not the newest; it was picked because it is a known-good pairing with minSdk 19. Newer AGP releases may need verification of their minSdk support before upgrading.

## Version-specific fallbacks

| Feature | API | Handling |
|---|---|---|
| Mixed-content mode | 21+ | set only on 21+; older WebView uses its own behaviour |
| Safe Browsing | 26+ | enabled only on 26+ |
| `shouldOverrideUrlLoading(WebResourceRequest)` | 24+ | both old and new overrides implemented |
| `onReceivedError(WebResourceRequest, ...)` | 23+ | both old and new overrides implemented |
| Material theme, coloured status bar | 21+ | `values-v21/styles.xml`; Holo theme below |
| Low-RAM detection | 19+ | `ActivityManager.isLowRamDevice()` lowers tab cap to 4 |
| Camera/mic/location/storage runtime permissions | 23+ | planned for later phases, with old-version fallbacks |
| Voice search | device-dependent | planned: shown only if a recogniser app exists |

## Structure (Phase 1)

```
app/src/main/java/com/growbig/browser/
  MainActivity.java            wires UI to the classes below; no browser logic
  browser/
    BrowserTab.java            one WebView + url/title/progress state, error page, SSL blocking
    TabManager.java            tab list, active tab, tab cap (RAM)
    NavigationManager.java     address-bar text -> URL or search URL
    DesktopModeManager.java    user agent / viewport switching
  settings/BrowserSettings.java   SharedPreferences (homepage, search engine, desktop default)
  ui/ToolbarIcon.java          canvas-drawn icons
res/  layout (activity_main, item_tab), menu, drawable, values, values-v21, mipmap
```

Naming is easy to rebrand: the visible name lives only in `strings.xml` (`app_name`); the package name is `com.growbig.browser`.

## Planned phases

2. Downloads (DownloadManager, storage permission by API level) and download history
3. SQLite: bookmarks + bookmark bar, history
4. Incognito (separate in-memory session, no history), cookie controller
5. Settings screen, dark/light theme, JavaScript/pop-up controls, full screen, find in page
6. Permissions (camera/mic/location), file upload
7. Background-tab suspension, session restore, low-memory handling (`onTrimMemory`)
8. Final testing report and compatibility documentation

Incognito limitation to be documented in the app: private tabs do not save history or cookies on the device, but the websites, your network and your employer/school can still see your activity. It is not anonymity.

## Security decisions in Phase 1

- SSL errors always call `handler.cancel()`; no "continue anyway" option exists
- Only http/https load inside tabs; `file:`, `content:`, `intent:`, `data:` navigations are blocked; `tel:`, `mailto:`, `sms:` go to the system app
- WebView file/content access disabled; no `addJavascriptInterface`
- No permissions are granted to websites (no handlers exist yet; camera/mic/location are denied by default)
- `allowBackup=false`; no analytics or tracking; no keys or signing secrets in the repository
