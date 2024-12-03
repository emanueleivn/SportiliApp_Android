package com.matthew.sportiliapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.matthew.sportiliapp.R
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme

@Composable
fun LoginScreen(navController: NavHostController) {
    var code by remember { mutableStateOf("") }
    var showAlert by remember { mutableStateOf(false) }
    var alertMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.icon),
                    contentDescription = null,
                    modifier = Modifier.size(200.dp)
                )
                Text(
                    text = "SportiliApp",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Codice", fontWeight = FontWeight.Medium) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface,
                    )
                )

                Spacer(modifier = Modifier.height(70.dp))

                Button(
                    onClick = {
                        if (code.isEmpty()) {
                            alertMessage = "Inserisci il codice!"
                            showAlert = true
                        } else {
                            coroutineScope.launch {
                                try {
                                    register(
                                        codice = code,
                                        navController = navController,
                                        context = context,
                                        onError = { message ->
                                            alertMessage = message
                                            showAlert = true
                                        }
                                    )
                                } catch (e: Exception) {
                                    alertMessage = "Errore inaspettato: ${e.message}"
                                    showAlert = true
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Entra",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = {
                        alertMessage =
                            "Per accedere è necessario avere un codice fornito dal personal trainer. Ti preghiamo di contattarlo per assistenza."
                        showAlert = true
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = "Non hai il codice?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showAlert) {
        AlertDialog(
            onDismissRequest = { showAlert = false },
            title = { Text("Attenzione!", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineMedium) },
            text = { Text(alertMessage, color = MaterialTheme.colorScheme.onBackground) },
            confirmButton = {
                Button(
                    onClick = { showAlert = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("OK")
                }
            }
        )
    }
}

private suspend fun register(
    codice: String,
    context: Context, // Aggiunto il parametro del contesto
    navController: NavHostController,
    onError: (String) -> Unit
) {
    try {
        val db = FirebaseDatabase.getInstance().getReference("users")
        val snapshot = db.get().await()
        val authUsers = snapshot.value as? Map<String, Map<String, Any>>

        val isAdmin = codice == FirebaseDatabase.getInstance().getReference("fausto").get().await().value
        salvaIsAdminInSharedPreferences(context, isAdmin)  // Salva lo stato di admin nelle SharedPreferences

        if (isAdmin) {
            FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    navController.navigate("admin")
                } else {
                    // Errore durante l'accesso
                    onError("Errore durante l'accesso. Riprova più tardi.")
                }
            }.await() // Aggiunto await per attendere il completamento dell'operazione
        } else {
            if (authUsers != null && authUsers[codice] != null) {
                FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        val profileUpdates = userProfileChangeRequest {
                            displayName = authUsers[codice]?.get("nome") as? String
                        }
                        user?.updateProfile(profileUpdates)
                        salvaCodeInSharedPreferences(context, code = codice)
                        navController.navigate("content")
                    } else {
                        // Errore durante l'accesso
                        onError("Errore durante l'accesso. Riprova più tardi.")
                    }
                }.await() // Aggiunto await per attendere il completamento dell'operazione
            } else {
                // Codice non autorizzato
                onError("Codice non autorizzato.")
            }
        }
    } catch (e: Exception) {
        onError("Errore durante il processo di registrazione: ${e.message}")
    }
}

fun salvaIsAdminInSharedPreferences(context: Context, isAdmin: Boolean) {
    val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putBoolean("isAdmin", isAdmin)
    editor.apply()
}

fun salvaCodeInSharedPreferences(context: Context, code: String) {
    val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putString("code", code)
    editor.apply()
}