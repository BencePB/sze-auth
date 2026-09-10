const AUTO_LOCK_ALARM = "autoLock";
const AUTO_LOCK_MINUTES = 60;

const setupView = document.getElementById("setupView");
const lockedView = document.getElementById("lockedView");
const unlockedView = document.getElementById("unlockedView");

const setupSecret = document.getElementById("setupSecret");
const setupPass = document.getElementById("setupPass");
const setupSaveBtn = document.getElementById("setupSaveBtn");
const setupError = document.getElementById("setupError");

const unlockPass = document.getElementById("unlockPass");
const unlockBtn = document.getElementById("unlockBtn");
const unlockError = document.getElementById("unlockError");
const resetBtn = document.getElementById("resetBtn");
const resetBtn2 = document.getElementById("resetBtn2");

const liveCode = document.getElementById("liveCode");
const progressBar = document.getElementById("progressBar");
const copyBtn = document.getElementById("copyBtn");
const lockBtn = document.getElementById("lockBtn");

let tickInterval = null;

function showView(view) {
  [setupView, lockedView, unlockedView].forEach((v) => v.classList.add("hidden"));
  view.classList.remove("hidden");
}

document.querySelectorAll(".toggle").forEach((btn) => {
  btn.addEventListener("click", () => {
    const target = document.getElementById(btn.dataset.target);
    target.type = target.type === "password" ? "text" : "password";
  });
});

async function refreshRootView() {
  const { encryptedSecret } = await chrome.storage.local.get("encryptedSecret");
  if (!encryptedSecret) {
    showView(setupView);
    return;
  }
  const { totpSecretPlain } = await chrome.storage.session.get("totpSecretPlain");
  if (totpSecretPlain) {
    showView(unlockedView);
    startTicker();
  } else {
    showView(lockedView);
  }
}

setupSaveBtn.addEventListener("click", async () => {
  const secret = setupSecret.value.trim();
  const pass = setupPass.value;
  setupError.textContent = "";
  if (!secret || !pass) {
    setupError.textContent = "Enter both a secret and a passphrase.";
    return;
  }
  const encrypted = await encryptSecret(secret, pass);
  await chrome.storage.local.set({ encryptedSecret: encrypted });
  await chrome.storage.session.set({ totpSecretPlain: secret });
  chrome.alarms.create(AUTO_LOCK_ALARM, { delayInMinutes: AUTO_LOCK_MINUTES });
  setupSecret.value = "";
  setupPass.value = "";
  showView(unlockedView);
  startTicker();
});

unlockBtn.addEventListener("click", async () => {
  unlockError.textContent = "";
  const pass = unlockPass.value;
  if (!pass) return;
  const { encryptedSecret } = await chrome.storage.local.get("encryptedSecret");
  try {
    const secret = await decryptSecret(pass, encryptedSecret);
    await chrome.storage.session.set({ totpSecretPlain: secret });
    chrome.alarms.create(AUTO_LOCK_ALARM, { delayInMinutes: AUTO_LOCK_MINUTES });
    unlockPass.value = "";
    showView(unlockedView);
    startTicker();
  } catch (e) {
    unlockError.textContent = "Incorrect passphrase.";
  }
});

lockBtn.addEventListener("click", async () => {
  await chrome.storage.session.remove("totpSecretPlain");
  chrome.alarms.clear(AUTO_LOCK_ALARM);
  stopTicker();
  showView(lockedView);
});

async function resetAll() {
  if (!confirm("This deletes the encrypted secret from this browser. You'll need to set it up again. Continue?")) return;
  await chrome.storage.local.remove("encryptedSecret");
  await chrome.storage.session.remove("totpSecretPlain");
  chrome.alarms.clear(AUTO_LOCK_ALARM);
  stopTicker();
  showView(setupView);
}
resetBtn.addEventListener("click", resetAll);
resetBtn2.addEventListener("click", resetAll);

copyBtn.addEventListener("click", async () => {
  const { totpSecretPlain } = await chrome.storage.session.get("totpSecretPlain");
  if (!totpSecretPlain) return;
  const code = await generateTOTP(totpSecretPlain);
  await navigator.clipboard.writeText(code);
  copyBtn.textContent = "Copied!";
  setTimeout(() => (copyBtn.textContent = "Copy code"), 1200);
});

async function tick() {
  const { totpSecretPlain } = await chrome.storage.session.get("totpSecretPlain");
  if (!totpSecretPlain) {
    stopTicker();
    showView(lockedView);
    return;
  }
  const code = await generateTOTP(totpSecretPlain);
  liveCode.textContent = code;
  const remaining = secondsRemaining();
  progressBar.style.width = `${(remaining / 30) * 100}%`;
}

function startTicker() {
  stopTicker();
  tick();
  tickInterval = setInterval(tick, 1000);
}

function stopTicker() {
  if (tickInterval) clearInterval(tickInterval);
  tickInterval = null;
}

refreshRootView();
