package com.bencepb.szeautofill

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** RFC 6238 TOTP, matching the desktop extension's totp.js implementation. */
object Totp {

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    private fun base32ToBytes(base32: String): ByteArray {
        val clean = base32.trim().uppercase().replace("=", "").replace(" ", "")
        val bits = StringBuilder()
        for (c in clean) {
            val idx = ALPHABET.indexOf(c)
            if (idx == -1) continue
            bits.append(idx.toString(2).padStart(5, '0'))
        }
        val bytes = ArrayList<Byte>()
        var i = 0
        while (i + 8 <= bits.length) {
            bytes.add(bits.substring(i, i + 8).toInt(2).toByte())
            i += 8
        }
        return bytes.toByteArray()
    }

    fun generate(base32Secret: String, period: Long = 30, digits: Int = 6): String {
        val key = base32ToBytes(base32Secret)
        val counter = System.currentTimeMillis() / 1000 / period

        val buffer = ByteBuffer.allocate(8)
        buffer.putLong(counter)
        val msg = buffer.array()

        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hmac = mac.doFinal(msg)

        val offset = hmac[hmac.size - 1].toInt() and 0x0f
        val binCode = ((hmac[offset].toInt() and 0x7f) shl 24) or
                ((hmac[offset + 1].toInt() and 0xff) shl 16) or
                ((hmac[offset + 2].toInt() and 0xff) shl 8) or
                (hmac[offset + 3].toInt() and 0xff)

        val otp = (binCode % Math.pow(10.0, digits.toDouble()).toInt())
        return otp.toString().padStart(digits, '0')
    }

    fun secondsRemaining(period: Long = 30): Long {
        val now = System.currentTimeMillis() / 1000
        return period - (now % period)
    }
}
