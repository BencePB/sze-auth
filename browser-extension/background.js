// Service worker: allows content scripts to read the session-only decrypted
// secret, and auto-locks (clears it) after inactivity via chrome.alarms.

importScripts("totp.js");

const AUTO_LOCK_ALARM = "autoLock";

function allowContentScriptAccess() {
  if (chrome.storage.session && chrome.storage.session.setAccessLevel) {
    chrome.storage.session.setAccessLevel({
      accessLevel: "TRUSTED_AND_UNTRUSTED_CONTEXTS"
    });
  }
}

chrome.runtime.onInstalled.addListener(allowContentScriptAccess);
chrome.runtime.onStartup.addListener(allowContentScriptAccess);
allowContentScriptAccess();

chrome.alarms.onAlarm.addListener((alarm) => {
  if (alarm.name === AUTO_LOCK_ALARM) {
    chrome.storage.session.remove("totpSecretPlain");
  }
});
