package com.bencepb.szeautofill

import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.net.Uri
import android.content.Intent

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val secretInput = findViewById<EditText>(R.id.secretInput)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val enableButton = findViewById<Button>(R.id.enableButton)
        val statusText = findViewById<TextView>(R.id.statusText)

        SecretStore.load(this)?.let {
            statusText.text = "Secret saved. Codes will be offered on neptun-hweb.sze.hu."
        }

        saveButton.setOnClickListener {
            val value = secretInput.text.toString().trim()
            if (value.isNotEmpty()) {
                SecretStore.save(this, value)
                secretInput.text.clear()
                statusText.text = "Secret saved. Now set this app as your Autofill service below."
            }
        }

        enableButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
            intent.data = Uri.parse("package:$packageName")
            startActivityForResult(intent, 1)
        }
    }
}
