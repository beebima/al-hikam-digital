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

## Corrected fixed banner and Android system navigation
- Banner height is strictly 64 CSS px, including its padding; never expands with navigation inset.
- App bottom menu is anchored immediately above the banner.
- WebView extends to device bottom edge; banner sits directly above system navigation inset.
- Native system navigation area uses the same computed background color as the banner, eliminating a white stripe.
- The native navigation area is not counted as advertising space.
- Device test required for Android 15 gesture and three-button navigation.

## V1.0.3 — Remove bottom banner
- Removed the bottom advertising banner entirely from HTML.
- Bottom sticky menu is anchored at viewport bottom with Android navigation inset as internal padding.
- Android navigation gesture area uses forest-green native backing, with transparent system navigation bar.
- No extra ad placeholder or white gap below the bottom menu.
- Header, Last Opened, progress/tutorial and other features preserved.
- Requires APK device testing; Android 15 inset handling may vary by navigation mode.

## Compact navigation color alignment
- Bottom ad banner remains removed.
- Fixed menu uses bottom offset equal to Android navigation inset; no double inset padding.
- Reduced bottom menu vertical padding to 3px, icon buttons 48px minimum.
- Android navigation backing color is synchronized to the exact menu palette color through native bridge.
- Navigation gesture area is a separate system area, not an oversized app menu.
- Device visual test still required.

## V1.0.3 navigation flush fix
- Corrected root cause: sticky menu was placed ABOVE Android bottom inset, leaving a white gap.
- Sticky menu now uses bottom:0 and includes bottom inset inside its own background, not outside it.
- Removed duplicate bottom spacing; retained accessible buttons.
- Android navigation system bar is transparent; native backing and menu use the same Forest Sage color.
- Insets are reapplied after WebView page finishes loading to avoid initialization timing issues.
- Bottom ad banner remains removed. Verify on emulator/device.

## Navigation inset rebuild
- Removed conflicting duplicate Android bottom navigation styles.
- Navigation backing is drawn behind the WebView, no longer on top of the menu.
- One controlled gesture-safe inset with reduced dead space; same #304634 color.
- No bottom ad banner. APK/device testing required.
