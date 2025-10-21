import android.util.Log
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.authtotpverify.data.TOTPEntry
import com.example.authtotpverify.formatOtp
import com.example.authtotpverify.parseOtpAuthUri
import com.example.authtotpverify.secondsRemainingNow
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator
import kotlinx.coroutines.delay
import org.apache.commons.codec.binary.Base32
import java.util.concurrent.TimeUnit


// This was just to get the project working
// leaving it in the project just to show the flow when view model flow is not used
// Will not be triggered unless MainActivity.kt code is changed to execute this instead

@Composable
fun TotpQuickDemo(
    secretBase32: String, periodSeconds: Int = 30, digits: Int = 6
) {
    var currentSecretShown by rememberSaveable { mutableStateOf(secretBase32) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    val userList = rememberSaveable { mutableStateListOf<TOTPEntry>() }
    var isFloatingButtonVisible by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current

    val config by remember(periodSeconds, digits) {
        mutableStateOf(
            TimeBasedOneTimePasswordConfig(
                codeDigits = digits,
                hmacAlgorithm = HmacAlgorithm.SHA1,
                timeStep = periodSeconds.toLong(),
                timeStepUnit = TimeUnit.SECONDS
            )
        )
    }

    //starting our timer, similar to init in viewmodel
    //passing keys so that its triggered again in case these value changes

    LaunchedEffect(key2 = userList.size, key1 = periodSeconds) {
        while (true) {
            val now = System.currentTimeMillis()
            val msUntilNextSecond = 1000 - (now % 1000)
            delay(msUntilNextSecond)
            val sr = periodSeconds.secondsRemainingNow()
            Log.d("TOTPS", "Seconds remaining: $sr")
            for (i in userList.indices) {
                val e = userList[i]
                // If a new 30s window just started, regenerate this entry’s code
                val newCode = if (sr == periodSeconds && e.keyBytes.isNotEmpty()) {
                    e.generator.generate()          // only at :00/:30
                } else {
                    e.code
                }
                Log.d("TOTPS", "New code generated: $newCode")
                userList[i] = e.copy(code = newCode, secondsRemaining = sr)
            }
        }
    }

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

        // showing user a message when no entries present
        if (userList.isEmpty()) {
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

                if (!isFloatingButtonVisible) {
                    Text("Add new secret")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            modifier = Modifier.weight(1f),
                            value = currentSecretShown,
                            onValueChange = {
                                currentSecretShown = it
                            },
                            label = {
                                Text("Input your secret")
                            })
                        Button(
                            onClick = {
                                val norm = currentSecretShown.trim().replace(" ", "").uppercase()
                                if (userList.find { it.secret == norm } != null) {
                                    Toast.makeText(
                                        context,
                                        "Cannot complete operation. Key either already present or is bad",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    val kb =
                                        runCatching { Base32().decode(norm) }.getOrElse {
                                            ByteArray(
                                                0
                                            )
                                        }
                                    if (kb.isNotEmpty()) {
                                        val gen = TimeBasedOneTimePasswordGenerator(kb, config)
                                        val initial = gen.generate()
                                        val sr = periodSeconds.secondsRemainingNow()
                                        userList.add(
                                            TOTPEntry(
                                                secret = norm,
                                                issuer = "New Entry - TODO",
                                                keyBytes = kb,
                                                generator = gen,
                                                code = initial,
                                                secondsRemaining = sr
                                            )
                                        )
                                        currentSecretShown = ""
                                    }
                                }
                            }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add new secret"
                            )
                        }
                    }
                }

                Text("Current list of secrets")
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(userList, key = { it.secret }) { currentUserSecret ->
                        UserSecretItem(
                            currentUserSecret.issuer,
                            currentUserSecret.secondsRemaining,
                            periodSeconds,
                            currentUserSecret.code
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSecretDialog(onSave = { secret, issuer ->
            if (addNewEntry(secret, issuer, periodSeconds, userList, config))
                showAddDialog = false
            else {
                Toast.makeText(
                    context, "Cannot complete operation. Key either already present or is bad",
                    Toast.LENGTH_LONG
                ).show()
            }
        }, onScanQr = { qrSecret, qrIssuer ->
            if (addNewEntry(qrSecret, qrIssuer, periodSeconds, userList, config))
                showAddDialog = false
            else {
                Toast.makeText(
                    context, "Cannot complete operation. Key either already present or is bad",
                    Toast.LENGTH_LONG
                ).show()
            }
        }, onDismiss = { showAddDialog = false })
    }
}

@Composable
fun UserSecretItem(
    secretInputToShowInList: String,
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
                val initial = secretInputToShowInList.firstOrNull()?.uppercase() ?: "?"
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
                        secretInputToShowInList, maxLines = 1, overflow = TextOverflow.Ellipsis
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
                    .height(4.dp), progress = {
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
    var currentUserSecret by remember { mutableStateOf("") }
    var currentUserIssuer by remember { mutableStateOf("") }
    val activity = LocalContext.current

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

fun addNewEntry(
    newSecret: String,
    newIssuer: String,
    periodSeconds: Int,
    userList: SnapshotStateList<TOTPEntry>,
    config: TimeBasedOneTimePasswordConfig
): Boolean {
    val formattedSecret = newSecret.trim().replace(" ", "").uppercase()
    if (userList.find { it.secret == formattedSecret } != null)
        return false
    val kb = runCatching { Base32().decode(formattedSecret) }.getOrElse { ByteArray(0) }
    if (kb.isNotEmpty()) {
        val gen = TimeBasedOneTimePasswordGenerator(kb, config)
        val initialCode = gen.generate()
        val sr = periodSeconds.secondsRemainingNow()
        userList.add(
            TOTPEntry(
                secret = formattedSecret,
                issuer = newIssuer.ifBlank { "Unknown" },
                keyBytes = kb,
                generator = gen,
                code = initialCode,
                secondsRemaining = sr
            )
        )
        return true
    }
    return false
}


