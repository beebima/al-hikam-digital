# Al-Hikam Digital Android v1.1.1 — Monetization UI preparation

Based on v1.0.3 SplashCrash Hotfix. No SDK / billing purchases integrated in this build.

## Prepared layouts
- Sticky simulated bottom banner above bottom navigation.
- Home simulated banner beneath quote/hero.
- Contents simulated inline ad every 10 chapters.
- Interstitial simulation only on tab transitions after a 180-second cooldown (not a timer that pops unexpectedly). Existing mock preview is labelled simulation.
- Native read-only `AlHikamAndroid.isAdsFree()` entitlement check centralizes hiding the above ad layouts and the `.no-ads-card` offer, including the purchase button contained therein.
- Chapter-reader inline ads removed to avoid interrupting Arabic/translation reading.

## Important before shipping
- The native `isAdsFree()` function deliberately returns false, until a REAL Google Play Billing one-time non-consumable entitlement, purchase acknowledgement and restoration is integrated. A click on the offer currently opens the existing *not yet available* explanation. Do NOT claim that paying already removes ads.
- Existing preview is NOT real monetization and MUST be replaced with Google Mobile Ads SDK native banner/interstitial integration with verified gating before production launch.
- Do not use production Ad Unit IDs for testing; use Google's test IDs.
- Real interstitial display/close is controlled by AdMob SDK, never by an artificial forced 5-second timer.
- Verify copyright/rights for modern translations and commentary before publishing.

## Revision — navigation transitions (still v1.1.1)
- One global minimum 180-second cooldown for **tab changes**, **Al-Hikam chapter-to-chapter navigation** (previous/next or any chapter switch while reading), and **Kultum/Tausiah article-to-article navigation**.
- Added explicit Previous / Next controls for Kultum and Tausiah article readers; both directions share the same cooldown.
- If fewer than 180 seconds have elapsed, navigation continues immediately; no preview pops up just because time elapses. Navigation also continues immediately for premium entitlement.
- Ad spaces and the purchase-offer block are hidden with CSS `display:none!important` when the native, verified ads-free entitlement is true; they do not reserve blank layout space. This version's bridge is still a placeholder returning false.
- The five-second preview close countdown is **simulation only**, not a proposed AdMob production timer. Actual interstitial must be driven by the Google Mobile Ads SDK and be shown only at compliant natural transitions.
- Not compiled to APK/AAB or device-tested in this revision. JavaScript syntax check passed.
