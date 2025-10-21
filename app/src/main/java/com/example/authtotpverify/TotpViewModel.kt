package com.example.authtotpverify

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authtotpverify.data.TOTPEntry
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.commons.codec.binary.Base32
import java.util.concurrent.TimeUnit

class TotpViewModel(periodSecondsInterval: Int, codeDigits: Int) : ViewModel() {

    private val _userEntries = mutableStateListOf<TOTPEntry>()
    val userEntries: List<TOTPEntry> = _userEntries

    val periodSeconds = periodSecondsInterval
    val digits = codeDigits

    // starting the clock as soon as we initiate the viewModel
    init {
        viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val msUntilNextSecond = 1000 - (now % 1000)
                delay(msUntilNextSecond)
                val sr = periodSeconds.secondsRemainingNow()
                Log.d("TOTPS", "Seconds remaining: $sr")
                for (i in _userEntries.indices) {
                    val e = _userEntries[i]
                    // If a new 30s window just started, regenerate this entry’s code
                    val newCode = if (sr == periodSeconds && e.keyBytes.isNotEmpty()) {
                        e.generator.generate()          // only at :00/:30
                    } else {
                        e.code
                    }
                    Log.d("TOTPS", "New code generated: $newCode")
                    _userEntries[i] = e.copy(code = newCode, secondsRemaining = sr)
                }
            }
        }

    }

    //creating config to be used to TOTP
    val config = TimeBasedOneTimePasswordConfig(
        codeDigits = digits,
        hmacAlgorithm = HmacAlgorithm.SHA1,
        timeStep = periodSeconds.toLong(),
        timeStepUnit = TimeUnit.SECONDS
    )

    // Adds a new entry in our list of secrets
    // has logic to check if the secret is already present as its our id for list in TotpSecretVMFlow.kt
    fun addNewEntry(
        newSecret: String,
        newIssuer: String,
    ): Boolean {
        val formattedSecret = newSecret.trim().replace(" ", "").uppercase()
        if(_userEntries.find { it.secret == formattedSecret } != null){
            return false
        }
        // if the conversion fails, we default it to an empty ByteArray
        // without the conversion it was not generating the correct code
        val currentKeyByte = runCatching { Base32().decode(formattedSecret) }.getOrElse { ByteArray(0) }
        if (currentKeyByte.isNotEmpty()) {
            val gen = TimeBasedOneTimePasswordGenerator(currentKeyByte, config)
            val initialCode = gen.generate()
            val sr = periodSeconds.secondsRemainingNow()
            _userEntries.add(
                TOTPEntry(
                    secret = formattedSecret,
                    issuer = newIssuer.ifBlank { "Unknown" },
                    keyBytes = currentKeyByte,
                    generator = gen,
                    code = initialCode,
                    secondsRemaining = sr
                )
            )
            return true
        }
        return false
    }
}




