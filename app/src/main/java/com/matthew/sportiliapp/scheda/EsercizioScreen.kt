package com.matthew.sportiliapp.scheda

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.matthew.sportiliapp.model.SchedaViewModel
import com.matthew.sportiliapp.model.SchedaViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EsercizioScreen(
    navController: NavHostController,
    giornoId: String,
    gruppoMuscolareId: String,
    esercizioId: String,
) {
    val context = LocalContext.current
    val viewModel: SchedaViewModel = viewModel(factory = SchedaViewModelFactory(context))
    // Osserviamo il LiveData 'scheda' dal ViewModel
    val scheda by viewModel.scheda.observeAsState()
    val esercizio = scheda?.giorni?.get(giornoId)
        ?.gruppiMuscolari?.get(gruppoMuscolareId)?.esercizi?.get(esercizioId)
    var notaState by remember { mutableStateOf(esercizio?.noteUtente ?: "") }
    val showingAlertState = remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(esercizio?.noteUtente) {
        notaState = esercizio?.noteUtente ?: ""
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        },
        content = { padding ->
            if (esercizio != null) {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = esercizio.name,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    // Immagine dell'esercizio
                    val painter = rememberAsyncImagePainter(
                        model = "https://firebasestorage.googleapis.com/v0/b/sportiliapp.appspot.com/o/${esercizio.name}.png?alt=media&token=cd00fa34-6a1f-4fa7-afa5-d80a1ef5cdaa"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        Image(
                            painter = painter,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                        )
                        when (painter.state) {
                            is AsyncImagePainter.State.Loading -> {
                                // Display a placeholder while the image loads
                            }
                            is AsyncImagePainter.State.Error -> {
                                // Display a placeholder or error icon if the image fails to load
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        text = "Immagine non disponibile",
                                        modifier = Modifier.align(Alignment.Center),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                            else -> {
                                // Do nothing, the image will be displayed
                            }
                        }
                    }
                    // Dati dell'esercizio e note utente
                    Column(
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                    ) {
                        Text(
                            text = esercizio.serie,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        esercizio.riposo?.let {
                            if (it.isNotEmpty()) {
                                Text(
                                    text = "$it recupero",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Note PT:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = esercizio.notePT?.takeIf { it.isNotEmpty() } ?: "Nessuna nota.",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        // Sezione per le note utente
                        Text(
                            text = "Note personali:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        // Visualizza la nota utente attuale o un messaggio se non ci sono note
                        Text(
                            text = notaState.ifEmpty { "Nessuna nota." },
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        // Pulsante per aprire l'AlertDialog per modificare le note
                        Button(
                            onClick = { showingAlertState.value = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Aggiungi/Modifica Note")
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    sheetState.show()  // Open the bottom sheet
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Avvia Timer di Recupero")
                        }
                        // AlertDialog per inserire/modificare le note personali
                        if (showingAlertState.value) {
                            var tempNote by remember { mutableStateOf(notaState) }
                            AlertDialog(
                                onDismissRequest = { showingAlertState.value = false },
                                title = { Text("Modifica le tue note") },
                                text = {
                                    Column {
                                        OutlinedTextField(
                                            value = tempNote,
                                            onValueChange = { tempNote = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            label = { Text("Inserisci le tue note") },
                                            maxLines = 5,
                                            singleLine = false
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        notaState = tempNote  // Aggiorna le note personali
                                        viewModel.updateEsercizioNotes(
                                            giornoId = giornoId,
                                            gruppoMuscolareId = gruppoMuscolareId,
                                            esercizioId = esercizioId,
                                            noteUtente = tempNote,
                                            onSuccess = {
                                                Toast.makeText(
                                                    context,
                                                    "Note aggiornate con successo",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                showingAlertState.value = false
                                            },
                                            onFailure = { errorMessage ->
                                                Toast.makeText(
                                                    context,
                                                    "Errore: $errorMessage",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }) {
                                        Text("Salva")
                                    }
                                },
                                dismissButton = {
                                    Button(onClick = { showingAlertState.value = false }) {
                                        Text("Annulla")
                                    }
                                }
                            )
                        }
                        if (sheetState.isVisible) {
                            ModalBottomSheet(
                                onDismissRequest = {
                                    scope.launch {
                                        sheetState.hide()  // Close the bottom sheet
                                    }
                                },
                                sheetState = sheetState
                            ) {
                                TimerSheet(riposo = esercizio.riposo ?: "")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    )
}

