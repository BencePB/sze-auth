package com.bencepb.szeautofill

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object SecretStore {

    private const val PREFS_FILE = "neptun_secret_store"
    private const val KEY_SECRET = "totp_secret"

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        PREFS_FILE,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context.applicationContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(context: Context, secret: String) {
        prefs(context).edit().putString(KEY_SECRET, secret).apply()
    }

    fun load(context: Context): String? =
        prefs(context).getString(KEY_SECRET, null)

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_SECRET).apply()
    }
}
