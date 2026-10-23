# Al-Hikam Digital — Android V1.1.1

Full Android Studio Gradle project (not just index.html).

## Build
1. Install Android Studio with Android SDK Platform 35 and JDK 17.
2. Open this directory as a project; allow Android Studio to sync Gradle 8.7.3 plugin.
3. Build > Generate Signed Bundle / APK > Android App Bundle (AAB).
4. Configure a **permanent private signing key** and Play Console listing before publishing.

## What is included
- Full offline bilingual Al-Hikam HTML app in `app/src/main/assets/index.html`.
- 100 approved notification messages in `app/src/main/assets/notifications.json`.
- Android WebView native activity, notification runtime permission bridge, shuffled non-repeating daily reminders at 05:00 and 17:00, and rescheduling after reboot/timezone changes.
- 50 morning and 50 evening messages shuffled independently, reshuffled after exhaustion.
- User may decline notification permission.

## Important limitations
- No compiled APK/AAB included. Android SDK/Gradle build environment is not installed in this workspace.
- Android may delay alarms (Doze/battery restrictions). Uses inexact alarms, without requesting special exact-alarm permission.
- Launcher icons and notification icon are included in Android resource density folders, adapted from the supplied CorelDRAW SVG. The original SVG is preserved in app assets. Review icon legibility on actual devices before publishing.
- WebView loads offline file URL; JavaScript bridge is exposed only to bundled trusted content.
- AdMob is NOT integrated. The existing ad elements are placeholders. Add real AdMob SDK, consent/privacy compliance and ad IDs before monetization.
- Premium purchase is NOT implemented. A visible premium price is not a Google Play Billing integration.
- App does not access external servers or require INTERNET permission in this build.
- Android 15 edge-to-edge layout, back-navigation, accessibility and store policy compliance should be tested on real devices.
- Package name `id.sejutasantri.alhikam` is a proposed identifier; confirm before Play Store's first upload.


## GitHub Actions — automatic APK build

The repository now includes `.github/workflows/android.yml`. Upload **all files and hidden directories** to the repository root, not into a nested folder. On pushes to `main` or `master`, GitHub Actions builds a **debug APK**. You can also use Actions > Build Al-Hikam Android APK > Run workflow. After the run succeeds, download the `AlHikamDigital-V1.1.1-debug-apk` artifact.

This debug APK is for device testing, **not** a Play Store release AAB. Publishing requires signing and a separate release workflow. GitHub Actions must be enabled in repository settings.
