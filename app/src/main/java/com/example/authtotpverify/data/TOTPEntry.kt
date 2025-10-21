package com.example.authtotpverify.data

import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator

data class TOTPEntry(
    val secret: String,
    val issuer: String,
    val keyBytes: ByteArray,
    val generator: TimeBasedOneTimePasswordGenerator,
    val code: String,
    val secondsRemaining: Int
)
