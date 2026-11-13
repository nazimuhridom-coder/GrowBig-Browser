# Growbig Browser — Phase 1 (foundation)

A standalone Android browser. Java, native XML UI, Android System WebView, minSdk 19 (Android 4.4), no third-party dependencies.

## Status — read this first

| Item | State |
|---|---|
| Source files written | Yes |
| XML well-formed, every `R.*` / `@...` reference resolves, Java braces balanced | Checked by script |
| Address-bar URL/search logic (`NavigationManager`) | Run on a desktop JVM with a stub; results as expected |
| **Compiled with Gradle / Android SDK** | **NO. Not done yet.** No Android SDK was available where this was written. |
| **Run on any device or emulator** | **NO.** |

Your first build is the first real compile. If it fails, send me the exact error text and I will diagnose it before changing anything.

## What Phase 1 does

- Tab bar: new tab, switch tab, close tab (max 8 tabs, max 4 on low-RAM devices)
- Address bar: URL or search (Google by default), lock icon (green https / red http), clear button, progress bar
- Back, Forward, Refresh/Stop, Home
- Menu: New tab, Desktop site (toggle, remembered), Zoom in/out, Share link, Copy link, Open in another browser, Exit
- Error page with offline hint; SSL errors are blocked, never bypassed
- Opens http/https links from other apps; rotation does not reload pages

Not in Phase 1 (planned next): downloads, bookmarks, history, incognito, find in page, settings screen, permissions (camera/mic/location), file upload, session restore, background-tab suspension, full-screen mode, dark/light switch.
Until the downloads phase, tapping a download link does nothing.

## Open and run (Android Studio)

1. Install **Android Studio** (current stable) from developer.android.com/studio.
2. **File > Open** and choose the `GrowbigBrowser` folder (the one containing `settings.gradle`).
3. Wait for "Gradle sync" to finish. Accept any prompt to install the **Android SDK Platform 34** or **Build-Tools**. Studio may offer to upgrade the Android Gradle Plugin: choose **Don't remind me / not now** for the first build.
4. Pick an emulator or plug in a phone (USB debugging on), then press the green **Run** button.

## Build the debug APK

- Android Studio: **Build > Build APK(s)**, then click **locate** in the popup.
  File: `app/build/outputs/apk/debug/app-debug.apk`
- Command line (needs JDK 17 and Gradle 8.4): `gradle assembleDebug`

### With GitHub Actions
Push the project to a GitHub repository. Open the **Actions** tab, then **Build Debug APK**. When it finishes, download **growbig-browser-debug-apk** from the run page. If the build fails, the log shows the error, and a **build-reports** artifact is attached.

## Release APK (later)

`gradle assembleRelease` produces an **unsigned** APK. Sign it on your own computer with your own keystore (never commit it):
```
zipalign -p 4 app-release-unsigned.apk app-release-aligned.apk
apksigner sign --ks my-release-key.jks --out growbig-browser.apk app-release-aligned.apk
```
`zipalign` and `apksigner` are in `Android/Sdk/build-tools/<version>/`. Keep the keystore and its password outside the repository.

## Phase 1 test checklist (all unchecked: nothing has been run yet)

Per device/emulator: Android 4.4, 5+, 8+, 10+, 13+, 14+, 15+.

- [ ] App installs and launches; interface appears
- [ ] Type `wikipedia.org` + Go: page loads, green lock
- [ ] Type `hello world`: Google search results
- [ ] Type `http://example.com`: loads with red open lock
- [ ] Back / Forward enable and disable correctly
- [ ] Refresh turns into Stop while loading
- [ ] Home loads the homepage
- [ ] New tab, switch tabs, close a tab, close the last tab (a fresh tab opens)
- [ ] 9th tab shows the "tab limit" message
- [ ] Menu: Desktop site changes the layout of a site and is remembered; Share; Copy link; Open in another browser
- [ ] Rotate the phone: tabs and page survive
- [ ] Airplane mode + load a page: error page with offline text
- [ ] `https://expired.badssl.com`: blocked, no "proceed" option
- [ ] Open a link from another app: opens in a new tab
- [ ] Android 4.4: record which sites fail (Facebook, YouTube, Gmail, WhatsApp Web are expected to be unreliable)

See `docs/ARCHITECTURE.md` for compatibility risks, design and the phase plan.
