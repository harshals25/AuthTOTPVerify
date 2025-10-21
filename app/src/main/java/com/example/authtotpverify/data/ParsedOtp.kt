package com.example.authtotpverify.data

data class ParsedOtp(
    val secret: String?,
    val issuer: String?
)