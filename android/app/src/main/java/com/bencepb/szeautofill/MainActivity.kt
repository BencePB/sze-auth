package com.bencepb.szeautofill

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var tickerRunning = false

    private lateinit var liveCode: TextView
    private lateinit var liveTimer: TextView

    private val ticker = object : Runnable {
        override fun run() {
            updateLiveCode()
            if (tickerRunning) {
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val secretInput = findViewById<EditText>(R.id.secretInput)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val enableButton = findViewById<Button>(R.id.enableButton)
        val copyButton = findViewById<Button>(R.id.copyButton)
        val statusText = findViewById<TextView>(R.id.statusText)
        liveCode = findViewById(R.id.liveCode)
        liveTimer = findViewById(R.id.liveTimer)

        SecretStore.load(this)?.let {
            statusText.text = "Secret saved. Use the button below any time to copy the current code."
        }

        saveButton.setOnClickListener {
            val value = secretInput.text.toString().trim()
            if (value.isNotEmpty()) {
                SecretStore.save(this, value)
                secretInput.text.clear()
                statusText.text = "Secret saved. Use the button below any time to copy the current code."
                updateLiveCode()
            }
        }

        copyButton.setOnClickListener {
            val secret = SecretStore.load(this)
            if (secret.isNullOrBlank()) {
                Toast.makeText(this, "Save your secret first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val code = Totp.generate(secret)
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Neptun code", code))
            Toast.makeText(this, "Copied: $code", Toast.LENGTH_SHORT).show()
        }

        enableButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
            intent.data = Uri.parse("package:$packageName")
            startActivityForResult(intent, 1)
        }
    }

    override fun onResume() {
        super.onResume()
        tickerRunning = true
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        tickerRunning = false
        handler.removeCallbacks(ticker)
    }

    private fun updateLiveCode() {
        val secret = SecretStore.load(this)
        if (secret.isNullOrBlank()) {
            liveCode.text = "------"
            liveTimer.text = ""
            return
        }
        liveCode.text = Totp.generate(secret)
        liveTimer.text = "${Totp.secondsRemaining()}s left"
    }
}
