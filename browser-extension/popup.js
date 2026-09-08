document.getElementById("save").addEventListener("click", async () => {
  const secret = document.getElementById("secret").value.trim();
  if (secret) {
    await chrome.storage.local.set({ totpSecret: secret });
    document.getElementById("secret").value = "";
    refresh();
  }
});

async function refresh() {
  const { totpSecret } = await chrome.storage.local.get("totpSecret");
  if (!totpSecret) {
    document.getElementById("code").textContent = "(no secret saved)";
    document.getElementById("timer").textContent = "";
    return;
  }
  const code = await generateTOTP(totpSecret);
  document.getElementById("code").textContent = code;
  document.getElementById("timer").textContent = secondsRemaining() + "s left";
}

refresh();
setInterval(refresh, 1000);
