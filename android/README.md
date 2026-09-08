# sze-auth Android Autofill Service

A minimal native Android app implementing the platform **Autofill Framework**,
scoped to a single purpose: recognising the Neptun 2FA field on
`neptun-hweb.sze.hu` inside stock Chrome (or any browser using the native
Android Autofill API) and offering the current TOTP code as a tap-to-fill
suggestion.

## ⚠️ Disclaimer

Same terms as the root [README.md](../README.md): unaffiliated personal
project, no warranty, your secret never leaves the device (stored via
`EncryptedSharedPreferences`), use at your own risk, do not reuse this
pattern for high-value accounts.

## How it works

- `Totp.kt` — same RFC 6238 algorithm as the browser extension's `totp.js`, implemented with `javax.crypto.Mac`.
- `SecretStore.kt` — stores your base32 secret in `EncryptedSharedPreferences` (AES-256), never in plain text, never transmitted anywhere.
- `NeptunAutofillService.kt` — an `AutofillService` that:
  1. Only activates when the assist structure's `webDomain` matches `neptun-hweb.sze.hu`.
  2. Searches the page's fields for one that looks like a one-time-code input (by id/hint/autofill-hint heuristics).
  3. Returns a single `Dataset` containing the freshly computed 6-digit code.
  4. Never saves anything you type (`onSaveRequest` is a no-op).
- `MainActivity.kt` — lets you paste your secret and jump straight to Android's "Set as Autofill service" system dialog.

## Building it

1. Open the `android/` folder in Android Studio (or run `./gradlew assembleDebug` from the command line with the Android SDK installed).
2. Install the debug APK on your device.
3. Open the app, paste your base32 secret, tap **Save secret**.
4. Tap **Set as Autofill service**, and choose this app in the system dialog.
5. Open Chrome, go to `neptun-hweb.sze.hu`, and tap the 2FA field — Chrome should show an autofill suggestion with the current code.

## Limitations

- Because this service scopes itself strictly to `neptun-hweb.sze.hu`, it will not offer suggestions on any other site or app — by design.
- Chrome must have the system-level Autofill setting pointed at "another service" (Chrome Settings → Autofill Services → Autofill using another service) for this to activate; some Chrome builds/regions gate this setting slightly differently.
- If Neptun changes its login page markup and the OTP field's `id`/`hint` no longer contains one of the matched keywords (`otp`, `token`, `code`, `totp`, `2fa`), update the `codeFieldHints` list in `NeptunAutofillService.kt`.
