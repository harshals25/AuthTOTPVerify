package com.example.authtotpverify

import com.example.authtotpverify.data.ParsedOtp
import androidx.core.net.toUri


// helper to parse the URI when QR code is scanned
// checks if the URI belongs to otpauth or totp
// extracts secret and issuer
fun parseOtpAuthUri(text: String): ParsedOtp {
    return try {
        val uri = text.toUri()
        if (uri.scheme != "otpauth" || uri.host != "totp") {
            return ParsedOtp(secret = null, issuer = null)
        }

        val secret = uri.getQueryParameter("secret")?.replace(" ", "")?.uppercase()
        var issuer = uri.getQueryParameter("issuer")
        if(issuer.isNullOrBlank()) issuer = "Unknown"

        ParsedOtp(secret = secret, issuer = issuer)
    } catch (_: Throwable) {
        ParsedOtp(secret = null, issuer = null)
    }
}

// extension function to show the OTP as depicted in mock UI
fun String.formatOtp(): String =
    if (length == 6) "${substring(0, 3)} ${substring(3)}" else this

// extension function to calculate remaining seconds
// can divide this into another helper function and pass it nowSec to make it more testable
fun Int.secondsRemainingNow(): Int {
    val nowSec = System.currentTimeMillis() / 1000
    val elapsedIntoInterval = (nowSec % this).toInt()
    val remaining = this - elapsedIntoInterval
    return if (remaining == this) this else remaining
}