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
