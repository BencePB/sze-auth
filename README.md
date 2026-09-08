# sze-auth

**A lightweight, self-hosted autofill helper for Neptun's mandatory two-factor authentication.**

Since Neptun's web login now requires a 6-digit TOTP code from an authenticator app on every login, this project holds your own decoded TOTP secret locally (never on any server) and offers a few different ways to get that code into the login form faster than typing it in from your phone each time.

## ⚠️ Disclaimer

This project is **not affiliated with, endorsed by, or supported by** Széchenyi István University (SZE), Neptun.Net Zrt., or Google. It is a personal convenience tool built by a student for their own use.

- **Use at your own risk.** No warranty; may break any time Neptun changes its login page or 2FA implementation.
- **Your secret stays local.** Every component here stores your TOTP secret only on your own device (browser extension storage or Android `EncryptedSharedPreferences`) and never transmits it anywhere.
- **Do not commit your real secret** into this repo, an issue, or a commit message. If it's ever exposed, reset 2FA in Neptun immediately to invalidate it.
- Storing a TOTP secret alongside your password on the same device reduces some of the security benefit 2FA is meant to add. That's an acceptable trade-off for a personal, low-stakes school portal login — don't reuse this pattern for higher-value accounts (banking, email, etc.).

## What's in this repo

| Folder | What it is | Status |
|---|---|---|
| [`browser-extension/`](browser-extension) | Manifest V3 Chrome/Edge/Brave extension. Computes the current TOTP code with WebCrypto and autofills it into the Neptun login page on desktop. | Working |
| [`android/`](android) | Native Android app (Kotlin). Started as a system-level Autofill Service + Quick Settings Tile + Accessibility Service; currently an in-app WebView browser that loads Neptun directly and injects the code via JavaScript. | In progress — see Android status below |

## Desktop (browser extension) — how it works

Neptun's 2FA is standard TOTP (RFC 6238), the same protocol as Google Authenticator. The extension holds a copy of your own TOTP secret (extracted once from a Google Authenticator export or Neptun's manual-entry setup code), computes the current 6-digit code locally, and fills it into the login page's code field automatically. See [`browser-extension/README` section below] and inline code comments for setup steps: load the folder as an unpacked extension, paste your secret into the popup, and it autofills on `neptun-hweb.sze.hu`.

## Android — how it works, and current status

Mobile turned out to be a much harder platform than desktop, because Android doesn't let a browser extension exist at all in stock Chrome. Several approaches were tried, in order:

1. **Android Autofill Service** (`NeptunAutofillService.kt`) — a real system-level autofill provider scoped to `neptun-hweb.sze.hu`. Works in principle, but Android only allows one active Autofill Service system-wide, so enabling it disables Google's autofill for everything else — not an acceptable trade-off.
2. **Quick Settings Tile + Accessibility Service** (`CopyTileService.kt`, `NeptunAccessibilityService.kt`) — tap a tile to type the code into the focused field via Accessibility APIs, without touching the Autofill Service setting. Coexists with Google autofill, but ran into Xiaomi/MIUI's aggressive restrictions on third-party Quick Settings Tiles, and Android 13+ "Restricted settings" blocking Accessibility permission for sideloaded APKs (workaround: App info → three-dot menu → "Allow restricted settings").
3. **In-app WebView browser** (current `MainActivity.kt`) — the app itself loads Neptun in an embedded WebView and injects the TOTP code via JavaScript once the code field appears, while leaving the system Autofill Service untouched. Hit two hurdles: Neptun's own browser-detection blocks WebView's default user-agent as "unsupported browser" (worked around by overriding the user-agent string to mimic Chrome — see the note in `MainActivity.kt` about this trade-off), and Google's autofill support inside WebView is less consistent than in real Chrome across different Android/OEM versions.
4. **Fallback that always works regardless of the above**: opening the app shows a live, auto-refreshing 6-digit code with a "Copy to clipboard" button, so you can always get the current code with two taps and a manual paste in Chrome, no special permissions required.

As of the latest commit, the WebView approach with the spoofed user-agent is still being debugged — next steps are documented in the commit history and will continue from there.

## Building the Android app

A GitHub Actions workflow (`.github/workflows/build-apk.yml`) automatically builds a debug APK on every push to `android/` and uploads it as a downloadable artifact from the [Actions tab](../../actions) — no local Android Studio setup required to just try the latest build.

## Getting your TOTP secret

Your secret is the same value encoded in the QR code you scanned into your authenticator app when you first set up Neptun 2FA. Retrieve it via your authenticator app's export/transfer feature (decode the resulting `otpauth-migration://` link), or from Neptun's 2FA setup screen if it offers a manual-entry text code instead of a QR image. **Never share this value publicly or commit it to this repo.**
