package com.bencepb.szeautofill

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

/**
 * Hosts Neptun inside an in-app WebView.
 *
 * Google's (or any other) autofill for the username/password fields keeps
 * working exactly as it would in Chrome, since Android's WebView uses the
 * same Chromium engine and the same system Autofill Framework hooks --
 * nothing about that is disabled here.
 *
 * For the 2FA code field specifically, this activity owns the WebView
 * directly, so it can inject the current TOTP code via JavaScript once the
 * field appears -- no Accessibility Service, Quick Settings Tile, or
 * separate Autofill Service registration required.
 */
class MainActivity : AppCompatActivity() {

    private val neptunUrl = "https://neptun-hweb.sze.hu/"
    private val handler = Handler(Looper.getMainLooper())
    private var fillLoopRunning = false

    private lateinit var webView: WebView

    private val fillLoop = object : Runnable {
        override fun run() {
            injectCurrentCode()
            if (fillLoopRunning) {
                handler.postDelayed(this, 1000)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val setupPanel = findViewById<LinearLayout>(R.id.setupPanel)
        val browserBar = findViewById<LinearLayout>(R.id.browserBar)
        val secretInput = findViewById<EditText>(R.id.secretInput)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val changeSecretButton = findViewById<Button>(R.id.changeSecretButton)
        val reloadButton = findViewById<Button>(R.id.reloadButton)
        webView = findViewById(R.id.webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectCurrentCode()
            }
        }

        val hasSecret = !SecretStore.load(this).isNullOrBlank()
        if (hasSecret) {
            showBrowser(setupPanel, browserBar)
            webView.loadUrl(neptunUrl)
        }

        saveButton.setOnClickListener {
            val value = secretInput.text.toString().trim()
            if (value.isNotEmpty()) {
                SecretStore.save(this, value)
                secretInput.text.clear()
                showBrowser(setupPanel, browserBar)
                webView.loadUrl(neptunUrl)
            }
        }

        changeSecretButton.setOnClickListener {
            browserBar.visibility = View.GONE
            webView.visibility = View.GONE
            setupPanel.visibility = View.VISIBLE
        }

        reloadButton.setOnClickListener {
            webView.loadUrl(neptunUrl)
        }
    }

    private fun showBrowser(setupPanel: LinearLayout, browserBar: LinearLayout) {
        setupPanel.visibility = View.GONE
        browserBar.visibility = View.VISIBLE
        webView.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        fillLoopRunning = true
        handler.post(fillLoop)
    }

    override fun onPause() {
        super.onPause()
        fillLoopRunning = false
        handler.removeCallbacks(fillLoop)
    }

    private fun injectCurrentCode() {
        val secret = SecretStore.load(this) ?: return
        val code = Totp.generate(secret)
        val js = buildFillScript(code)
        webView.evaluateJavascript(js, null)
    }

    private fun buildFillScript(code: String): String {
        return """
            (function() {
                var code = "$code";
                var selectors = [
                    "input[name*='otp' i]", "input[name*='token' i]", "input[name*='code' i]",
                    "input[id*='otp' i]", "input[id*='token' i]", "input[id*='code' i]",
                    "input[name*='kod' i]", "input[id*='kod' i]",
                    "input[maxlength='6']"
                ];
                var field = null;
                for (var i = 0; i < selectors.length; i++) {
                    field = document.querySelector(selectors[i]);
                    if (field) break;
                }
                if (!field) {
                    var inputs = Array.prototype.slice.call(document.querySelectorAll('input'));
                    var visible = inputs.filter(function(el) {
                        return el.offsetParent !== null && !el.disabled && el.type !== 'hidden';
                    });
                    if (visible.length === 1) field = visible[0];
                }
                if (field && field.value === '') {
                    field.focus();
                    field.value = code;
                    field.dispatchEvent(new Event('input', { bubbles: true }));
                    field.dispatchEvent(new Event('change', { bubbles: true }));
                }
            })();
        """.trimIndent()
    }
}
