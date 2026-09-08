# sze-auth

**Personal browser extension that autofills the Neptun (SZE) two-factor authentication code.**

## ⚠️ DISCLAIMER — READ BEFORE USING

This project is **not affiliated with, endorsed by, or supported by** Széchenyi István University (SZE), Neptun.Net Zrt., or Google. It is a personal convenience tool built by a student for their own use.

- **Use at your own risk.** This extension exists solely to reduce login friction for the author's own Neptun account. Using it on any account you do not own, or distributing your own TOTP secret to others, is your responsibility and may violate your institution's IT security policy or terms of service.
- **No warranty.** This code is provided "as is," with no guarantee it will keep working if Neptun changes its login page, its 2FA implementation, or its terms of service. It may break at any time without notice.
- **You are responsible for your own secret.** This extension does not ship with, store remotely, or transmit any TOTP secret. You must supply your own secret locally via the extension popup; it is kept only in your browser's local extension storage (`chrome.storage.local`) and is never sent to any server, including by the author.
- **This does not replace responsible security practices.** Storing a TOTP secret in a browser extension means both factors of your login (password + code) can end up on the same device. That is a legitimate trade-off for convenience on a low-stakes account, but it is **not recommended for high-value accounts** (banking, email, etc.). Do not reuse this pattern there.
- **Do not commit or share your actual secret.** Never paste your real base32 secret into a public repository, issue, commit message, or screenshot. If you believe your secret has been exposed, immediately reset two-factor authentication in Neptun to invalidate it.
- **No liability.** The author accepts no liability for account lockouts, security incidents, academic consequences, or any other damages arising from use of this code.

## What it does

Neptun's web 2FA (`neptun-hweb.sze.hu`) uses standard TOTP (RFC 6238), the same protocol as Google Authenticator. This extension:

1. Stores a TOTP secret you provide, locally in your browser.
2. Computes the current 6-digit code using the WebCrypto API (`crypto.subtle`), with no external libraries or network calls.
3. Autofills that code into the Neptun login page's one-time code field.

## Setup

1. Load this folder as an unpacked extension (`chrome://extensions` → Developer mode → Load unpacked).
2. Click the extension icon and paste your own base32 TOTP secret (obtained from your own Google Authenticator export/QR code — see "Getting your secret" below).
3. Log into Neptun as usual; the code field should autofill automatically.

If the field isn't detected automatically, inspect the actual `<input>` element on the Neptun 2FA page and add its selector to `CANDIDATE_SELECTORS` in `content.js`.

## Getting your secret

Your TOTP secret is the same value encoded in the QR code you scanned into Google Authenticator when you first set up Neptun 2FA. You can retrieve it via Google Authenticator's built-in "Export accounts" / transfer feature, or from Neptun's 2FA setup screen if it offers a manual-entry text code instead of a QR image. **Never share this value publicly.**

## Files

- `manifest.json` — Chrome/Edge Manifest V3 extension definition.
- `totp.js` — RFC 6238 TOTP implementation using WebCrypto.
- `content.js` — Finds the Neptun 2FA field and autofills it.
- `popup.html` / `popup.js` — UI for entering and previewing your TOTP secret/code.

## License

Personal-use project, provided without warranty. Fork and adapt for your own accounts at your own risk.
