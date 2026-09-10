// RFC 6238 TOTP + AES-GCM encryption helpers, all via WebCrypto (no external libraries).
// Shared by background.js, content.js, and popup.js.

function base32ToBytes(base32) {
  const alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
  const clean = base32.replace(/=+$/, "").toUpperCase().replace(/\s/g, "");
  let bits = "";
  for (const c of clean) {
    const val = alphabet.indexOf(c);
    if (val === -1) continue;
    bits += val.toString(2).padStart(5, "0");
  }
  const bytes = [];
  for (let i = 0; i + 8 <= bits.length; i += 8) {
    bytes.push(parseInt(bits.substring(i, i + 8), 2));
  }
  return new Uint8Array(bytes);
}

async function hmacSha1(keyBytes, msgBytes) {
  const key = await crypto.subtle.importKey(
    "raw", keyBytes, { name: "HMAC", hash: "SHA-1" }, false, ["sign"]
  );
  const sig = await crypto.subtle.sign("HMAC", key, msgBytes);
  return new Uint8Array(sig);
}

function intToBytes(num) {
  const buf = new ArrayBuffer(8);
  const view = new DataView(buf);
  view.setUint32(4, num, false);
  return new Uint8Array(buf);
}

async function generateTOTP(base32Secret, period = 30, digits = 6) {
  const key = base32ToBytes(base32Secret);
  const counter = Math.floor(Date.now() / 1000 / period);
  const msg = intToBytes(counter);
  const hmac = await hmacSha1(key, msg);

  const offset = hmac[hmac.length - 1] & 0x0f;
  const binCode =
    ((hmac[offset] & 0x7f) << 24) |
    ((hmac[offset + 1] & 0xff) << 16) |
    ((hmac[offset + 2] & 0xff) << 8) |
    (hmac[offset + 3] & 0xff);

  const otp = (binCode % 10 ** digits).toString().padStart(digits, "0");
  return otp;
}

function secondsRemaining(period = 30) {
  return period - (Math.floor(Date.now() / 1000) % period);
}

// --- Encryption at rest (AES-GCM, key derived from passphrase via PBKDF2) ---

function bytesToBase64(bytes) {
  return btoa(String.fromCharCode(...bytes));
}

function base64ToBytes(b64) {
  return Uint8Array.from(atob(b64), (c) => c.charCodeAt(0));
}

async function deriveAesKey(passphrase, saltBytes, usage) {
  const enc = new TextEncoder();
  const baseKey = await crypto.subtle.importKey(
    "raw", enc.encode(passphrase), "PBKDF2", false, ["deriveKey"]
  );
  return crypto.subtle.deriveKey(
    { name: "PBKDF2", salt: saltBytes, iterations: 150000, hash: "SHA-256" },
    baseKey,
    { name: "AES-GCM", length: 256 },
    false,
    [usage]
  );
}

async function encryptSecret(plainSecret, passphrase) {
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const key = await deriveAesKey(passphrase, salt, "encrypt");
  const enc = new TextEncoder();
  const ciphertextBuf = await crypto.subtle.encrypt({ name: "AES-GCM", iv }, key, enc.encode(plainSecret));
  return {
    ciphertext: bytesToBase64(new Uint8Array(ciphertextBuf)),
    iv: bytesToBase64(iv),
    salt: bytesToBase64(salt)
  };
}

// Throws if the passphrase is wrong (AES-GCM authentication tag check fails).
async function decryptSecret(passphrase, stored) {
  const salt = base64ToBytes(stored.salt);
  const iv = base64ToBytes(stored.iv);
  const ciphertextBytes = base64ToBytes(stored.ciphertext);
  const key = await deriveAesKey(passphrase, salt, "decrypt");
  const plainBuf = await crypto.subtle.decrypt({ name: "AES-GCM", iv }, key, ciphertextBytes);
  return new TextDecoder().decode(plainBuf);
}
