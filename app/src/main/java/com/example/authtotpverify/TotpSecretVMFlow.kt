package com.example.authtotpverify

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@Composable
fun TotpVMFlow(periodSecondsInterval: Int, codeDigits: Int) {

    // using viewModel factory to send params to view model
    val totpViewModel: TotpViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TotpViewModel(periodSecondsInterval, codeDigits) as T
        }
    })
    var currentSecretShown by rememberSaveable { mutableStateOf("") }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var isFloatingButtonVisible by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current

    // using scaffold to show floating button
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.padding(all = 20.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = {
                    showAddDialog = true
                }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        }) { innerPadding ->

        // if no entries, show user a message
        if (totpViewModel.userEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(10.dp)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Please select the plus icon to add a secret",
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Added this in the beginning to get the app working
                // just has a textField and a submit button which adds the secret to the list
                // generates code
                if (!isFloatingButtonVisible) {
                    ShowTextInputField(
                        value = currentSecretShown,
                        onValueChange = {
                            currentSecretShown = it
                        },
                        onSubmit = {
                            if (totpViewModel.addNewEntry(currentSecretShown, "")) {
                                currentSecretShown = ""
                            }
                            else {
                                Toast.makeText(context,"Cannot complete operation. Key either already present or is bad",
                                    Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }

                Text("Current list of codes")
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(totpViewModel.userEntries, key = { it.secret }) { currentUserSecret ->
                        UserSecretItem(
                            currentUserSecret.issuer,
                            currentUserSecret.secondsRemaining,
                            totpViewModel.periodSeconds,
                            currentUserSecret.code
                        )
                    }
                }
            }
        }
    }

    // logic to show the add dialog
    if (showAddDialog) {
        AddSecretDialog(onSave = { secret, issuer ->
            if (totpViewModel.addNewEntry(secret, issuer))
                showAddDialog = false
            else {
                Toast.makeText(context,"Cannot complete operation. Key either already present or is bad",
                    Toast.LENGTH_LONG).show()
            }
        }, onScanQr = { qrSecret, qrIssuer ->
            if (totpViewModel.addNewEntry(qrSecret, qrIssuer))
                showAddDialog = false
            else {
                Toast.makeText(context,"Cannot complete operation. Key either already present or is bad",
                    Toast.LENGTH_LONG).show()
            }
        }, onDismiss = { showAddDialog = false })
    }
}


@Composable
fun UserSecretItem(
    issuerInputToShowInList: String,
    secondsRemaining: Int,
    periodSeconds: Int,
    codeForCurrentSecret: String
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                // wont ever run since we always provide unknown, just putting the logic to be safe
                val initial = issuerInputToShowInList.firstOrNull()?.uppercase() ?: "?"
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp)
                ) {
                    Text(
                        issuerInputToShowInList, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        codeForCurrentSecret.formatOtp(),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                progress = {
                    (periodSeconds - secondsRemaining) / periodSeconds.toFloat()
                })
            Text("${secondsRemaining}s left", fontSize = 12.sp)
        }
    }
}

@Composable
fun AddSecretDialog(
    onSave: (userSecret: String, userIssuer: String) -> Unit,
    onScanQr: (userSecret: String, userIssuer: String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentUserSecret by rememberSaveable { mutableStateOf("") }
    var currentUserIssuer by rememberSaveable { mutableStateOf("") }
    val activity = LocalContext.current

    // QR code scanner
    val scanner by remember {
        mutableStateOf(
            GmsBarcodeScanning.getClient(
                activity,
                GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
            )
        )
    }

    val canSave = currentUserSecret.isNotEmpty()

    // Showing an alert dialog to either let user enter info or scan a QR code
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add a new secret") }, text = {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(value = currentUserSecret, onValueChange = {
                currentUserSecret = it
            }, label = {
                Text("Input Secret")
            })
            TextField(value = currentUserIssuer, onValueChange = {
                currentUserIssuer = it
            }, label = {
                Text("Input Issuer")
            })
            Button(
                onClick = {
                    scanner.startScan()
                        .addOnSuccessListener { barcode ->
                            val raw = barcode.rawValue ?: return@addOnSuccessListener
                            val parsed = parseOtpAuthUri(raw)
                            val secretFromQr = parsed.secret ?: ""
                            val issuerFromQr = parsed.issuer ?: ""
                            onScanQr(secretFromQr, issuerFromQr)
                        }
                }) {
                Text("Scan QR code")
                Icon(imageVector = Icons.Default.QrCode, contentDescription = "Add new secret")
            }
        }
    }, confirmButton = {
        Button(
            onClick = { onSave(currentUserSecret.trim(), currentUserIssuer.trim()) },
            enabled = canSave,
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = Color.LightGray,
                disabledContentColor = Color.DarkGray
            )
        ) { Text("Save") }
    }, dismissButton = {
        Button(
            onClick = onDismiss
        ) {
            Text("Cancel")
        }
    })

}

@Composable
fun ShowTextInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Text("Add new secret")
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            modifier = Modifier.weight(1f),
            value = value,
            onValueChange = {
                onValueChange(it)
            },
            label = {
                Text("Input your secret")
            })
        Button(
            onClick = onSubmit
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add new secret"
            )
        }
    }
}

