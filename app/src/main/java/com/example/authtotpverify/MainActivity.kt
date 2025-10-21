package com.example.authtotpverify

import TotpQuickDemo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.authtotpverify.ui.theme.AuthTOTPVerifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val secret = "UFDKGUE5FJ6VECPLAS4CHVMNMEOE4ZQY" // a dummy secret, put in during dev and testing phase for TotpQuickDemo flow
        setContent {
            AuthTOTPVerifyTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                    ) {
                        // Uses VM flow to complete the required task
                        // periodSecondsInterval and codeDigits can be changed from here
                        // passing 30 and 6 by default
                        TotpVMFlow(
                            periodSecondsInterval = 30,
                            codeDigits = 6
                        )


                        //uncomment the below Composable and comment  out TotpVmFlow for non vm implementation
//                        TotpQuickDemo(
//                            secretBase32 = secret
//                        )
                    }
                }
            }
        }
    }
}