// RFC 6238 TOTP implementation using WebCrypto (no external libraries)

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
  // JS numbers are safe up to 2^53, time counters never get that large
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

// Seconds remaining until the current 30s window expires
function secondsRemaining(period = 30) {
  return period - (Math.floor(Date.now() / 1000) % period);
}
