package com.example.authtotpverify

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UtilitiesTest {

    @Test
    fun formatOtpSuccess(){
        val otp = "123456"
        val result = otp.formatOtp()
        assertThat(result).isEqualTo("123 456")
    }

    @Test
    fun formatOtpFailure(){
        val otp = "123456"
        val result = otp.formatOtp()
        assertThat(result).isNotEqualTo("123456")
    }

    @Test
    fun formatOtpFailureLongerInput(){
        val otp = "1234567"
        val result = otp.formatOtp()
        assertThat(result).isEqualTo("1234567")
    }

    @Test
    fun formatOtpFailureEmptyInput(){
        val otp = ""
        val result = otp.formatOtp()
        assertThat(result).isEqualTo("")
    }

    @Test
    fun parseOtpAuthUriSuccess(){
        val stringUri = "otpauth://totp/userIssuerNameHere:user?secret=ABC123&issuer=userIssuerName"
        val secret = "ABC123"
        val issuer = "userIssuerName"

        val parsed = parseOtpAuthUri(stringUri)
        assertThat(parsed.secret).isEqualTo(secret)
        assertThat(parsed.issuer).isEqualTo(issuer)
    }

    @Test
    fun parseOtpAuthUriSuccessNoIssuer(){
        val stringUri = "otpauth://totp/userIssuerNameHere:user?secret=ABC123&issuer="
        val secret = "ABC123"
        val issuer = "Unknown"

        val parsed = parseOtpAuthUri(stringUri)
        assertThat(parsed.secret).isEqualTo(secret)
        assertThat(parsed.issuer).isEqualTo(issuer)
    }

    @Test
    fun parseOtpAuthUriSuccessIssuerNull(){
        val stringUri = "otpauth://totp/?secret=ABC123&issuer="
        val secret = "ABC123"

        val parsed = parseOtpAuthUri(stringUri)
        assertThat(parsed.secret).isEqualTo(secret)
        assertThat(parsed.issuer).isEqualTo("Unknown")
    }

    @Test
    fun parseOtpAuthUriFailure(){
        val stringUri = "otpauthss://totp/?secret=ABC123&issuer=someIssuer"

        val parsed = parseOtpAuthUri(stringUri)
        assertThat(parsed.secret).isEqualTo(null)
        assertThat(parsed.issuer).isEqualTo(null)
    }

    @Test
    fun parseOtpAuthUriFailureEmptyUri(){
        val stringUri = ""

        val parsed = parseOtpAuthUri(stringUri)
        assertThat(parsed.secret).isEqualTo(null)
        assertThat(parsed.issuer).isEqualTo(null)
    }

}