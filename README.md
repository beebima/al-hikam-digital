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

## V1.0.3 UI revision (version unchanged)
- Settings: System Fonts card now precedes Content Fonts card.
- Android status bar follows the active theme's top navigation background color.
- While the splash is displayed, status bar uses splash cream (#FAF6EE) with dark status icons.
- Native Android bridge validates six-digit hex colors; Android 15 inset backing follows the same color.
- Requires rebuilding APK through GitHub Actions to see native changes.

## System bar hotfix (same version 1.0.3)
- Separate top and bottom inset backing views for Android 15 edge-to-edge.
- Top backing color follows JavaScript-selected menu theme, splash uses cream.
- Bottom inset and Android navigation bar use light mint instead of blank white.
- Contrast enforcement disabled where supported to prevent forced translucent scrims.
- APK/device validation still required.

## Lightweight layout revision (V1.0.3 retained)
- Removed sky clouds and falling leaves and all home decorative motion.
- Ad-free offer is static.
- Removed four home shortcut buttons and full learning statistics block.
- Compact 75/25 learning progress and tutorial cards; progress label and action simplified.
- Fixed Forest Sage palette, retaining light/dark toggle.
- Separated System Fonts and Content Fonts cards in settings.
- Disabled tab transition animations.
- Android GitHub Actions project included; validate in emulator before publishing.

## Bottom navigation layout correction (V1.0.3 unchanged)
The WebView now reaches the Android bottom edge instead of stopping above the
system navigation inset. The existing app menu and banner move down together;
the ad content remains 64px, with extra bottom padding only for the Android
gesture/button region. This removes the detached blank strip below the banner.
Requires emulator testing across gesture and three-button navigation modes.

## Last opened chapter (V1.0.3)
Compact Last Opened card between Progress/Tutorial and Ad-Free. Shows saved lastRead chapter number and localized title, and Open button returns to that chapter. Button disabled until a chapter has been opened. No change to app version.
