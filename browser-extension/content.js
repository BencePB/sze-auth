// Finds the Neptun 2FA code field and fills it with a freshly generated TOTP
// code -- but only while the extension is unlocked (secret present in the
// session-only, in-memory storage). If it's locked, this does nothing.

const CANDIDATE_SELECTORS = [
  "input[name*='otp' i]",
  "input[name*='token' i]",
  "input[name*='code' i]",
  "input[id*='otp' i]",
  "input[id*='token' i]",
  "input[id*='code' i]",
  "input[maxlength='6']"
];

function findOtpField() {
  for (const sel of CANDIDATE_SELECTORS) {
    const el = document.querySelector(sel);
    if (el) return el;
  }
  return null;
}

async function fillCode() {
  const { totpSecretPlain } = await chrome.storage.session.get("totpSecretPlain");
  if (!totpSecretPlain) return; // locked -- do nothing

  const field = findOtpField();
  if (!field) return;

  const code = await generateTOTP(totpSecretPlain);
  field.focus();
  field.value = code;
  field.dispatchEvent(new Event("input", { bubbles: true }));
  field.dispatchEvent(new Event("change", { bubbles: true }));
}

let attempts = 0;
const interval = setInterval(async () => {
  attempts++;
  const field = findOtpField();
  if (field && !field.value) {
    await fillCode();
  }
  if (attempts > 20) clearInterval(interval);
}, 500);

setInterval(async () => {
  const field = findOtpField();
  if (field && !field.value) await fillCode();
}, 5000);
