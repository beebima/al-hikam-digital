# Al-Hikam Code 13 (v.1.1.13) — Firebase Auth staged

**Status: integration prepared, NOT production-ready yet.** This ZIP intentionally does not contain a real Firebase API key or an account secret. The Android app preserves locally reported purchases until backend validation can work. Cloud Run must remain IAM-private until Firebase Authentication is enabled and Android configured.

## Setup required
1. Firebase Console: add Firebase to existing Google Cloud project `sejutasantri-play-billing`, register Android package `id.sejutasantri.alhikam`, enable Authentication -> Sign-in method -> Anonymous.
2. Populate `app/src/main/assets/firebase-billing-config.json` using Firebase project's **public** apiKey, mobilesdk_app_id/applicationId, projectId (do not use service-account JSON or private credentials).
3. Deploy **both** `backend-cloud-run/index.js` and `package.json` to the current Cloud Run service (Node 22, entrypoint helloHttp, existing service-account).
4. Confirm backend responds `401 AUTH_REQUIRED` when invoked without Firebase ID token **after** safely making service HTTPS-reachable. Firebase ID tokens do NOT satisfy Cloud Run IAM authentication. For this architecture public Cloud Run ingress + application-layer verifyIdToken is required; verify endpoint enforces Firebase auth before purchase handling. Ensure cost/abuse limits; strongly recommend Play Integrity and rate limiting and monitoring before public rollout.
5. Build signed AAB in GitHub Actions only after Firebase configuration is provided, then upload versionCode 13 (v.1.1.13).

## Limits
- No Firebase tenant/account setup, Cloud Run deployment, or production tests have been performed by this ZIP.
- Anonymous Firebase Authentication validates the Firebase identity of caller but does not prove app binary integrity nor bind purchase ownership to this user. Implement Play Integrity, token ownership tracking / replay prevention and RTDN before final public launch.
- Refund and revoke are not necessarily equivalent. `refundableQuantity` is checked if available. Absence of data is not proof of refund.
- Code 13 fixes static diagnostic version text `Code 9` and `Play code 7` to Code 13.
- Existing ad/navigation behavior was not purposefully modified.
