package com.bencepb.szeautofill

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings Tile alternative to the Autofill Service.
 *
 * Android only allows one active system-wide Autofill Service at a time,
 * so enabling this app as the Autofill Service would disable Google's
 * (or any other) autofill for every other app and site. This tile avoids
 * that trade-off: tapping it uses the Accessibility Service (if enabled)
 * to type the current TOTP code directly into whatever field is focused
 * -- a one-tap fill that doesn't touch the Autofill Service setting at
 * all. It also always copies the code to the clipboard as a fallback, in
 * case the Accessibility Service isn't enabled or the injection fails.
 */
class CopyTileService : TileService() {

    override fun onClick() {
        super.onClick()

        val secret = SecretStore.load(applicationContext)
        if (secret.isNullOrBlank()) {
            qsTile?.let {
                it.label = "No secret saved"
                it.state = Tile.STATE_INACTIVE
                it.updateTile()
            }
            return
        }

        val code = Totp.generate(secret)

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Neptun code", code))

        val filled = NeptunAccessibilityService.instance?.fillFocusedField(code) ?: false

        qsTile?.let {
            it.label = if (filled) "Filled: $code" else "Copied: $code"
            it.state = Tile.STATE_ACTIVE
            it.updateTile()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let {
            it.label = "Neptun code"
            it.state = Tile.STATE_INACTIVE
            it.updateTile()
        }
    }
}
