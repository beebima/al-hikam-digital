# Al-Hikam Digital Android V1.0.2

Complete Android Studio project and GitHub Actions APK workflow.

## Performance changes
- The two large offline learning dictionaries (`fullHikam.js` and `englishContent.js`) are loaded **on demand** only when opening a wisdom detail or selected sermon. All 127 entries in both languages remain available offline.
- Android-specific CSS disables continuous decorative animations and expensive blur/backdrop-filter effects. The standard HTML design remains intact.
- The compact horizontal learning-progress layout remains enforced on Android.
- Home, chapter listing, statistics and settings can render before the large sermon dictionaries are parsed.

## Build
Push all files (including hidden `.github/workflows/android.yml`) to GitHub. Actions builds a debug APK. This source archive is not a compiled APK.

## Still pending
Real-device profiling, signed AAB, AdMob integration and Google Play Billing. Android can delay inexact reminder alarms.

## V1.0.3 Android fixes
- Continuous ornament motion disabled, falling leaf elements hidden; background illustration remains static.
- Dark emerald status-bar background with light icons; Android 15+ safe-area padding uses emerald backing.
- First-run tutorial launches automatically after splash (~3.9 s), only until user skips/completes.
- Existing user localStorage tutorial completion is respected. To test first-run behavior, clear app data or reinstall.
- This is a source project; GitHub Actions build and device performance still need validation.
