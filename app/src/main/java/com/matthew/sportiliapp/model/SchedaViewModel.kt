package com.matthew.sportiliapp.model

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.security.MessageDigest

class SchedaViewModel(private val context: Context) : ViewModel() {
    private val _scheda = MutableLiveData<Scheda?>()
    private val _name = MutableLiveData<String?>()
    val scheda: LiveData<Scheda?> = _scheda
    val name: LiveData<String?> = _name
    val isLoading = MutableLiveData(true) // Stato di caricamento

    init {
        loadScheda()
    }

    private fun loadScheda() {
        viewModelScope.launch {
            // Carica la scheda locale
            val localScheda = loadSchedaFromLocal()
            val localSchedaHash = getLocalSchedaHash()

            // Se la scheda locale esiste, aggiorna il LiveData per mostrare i dati subito
            if (localScheda != null) {
                _scheda.postValue(localScheda)
            }

            // Imposta subito isLoading a false per consentire la visualizzazione dei dati locali
            isLoading.postValue(false)

            // Recupera il codice utente salvato
            val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
            val savedCode = sharedPreferences.getString("code", "") ?: ""

            // Se il codice dell'utente è vuoto, termina qui
            if (savedCode.isEmpty()) {
                return@launch
            }

            // Controlla in background il database per eventuali aggiornamenti
            val database = FirebaseDatabase.getInstance()
            val schedaRef = database.reference.child("users").child(savedCode).child("scheda")

            schedaRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val remoteScheda = snapshot.getValue(Scheda::class.java)

                    if (remoteScheda != null) {
                        val remoteHash = hashString(remoteScheda.toString())

                        // Se l'hash è diverso, aggiorna la scheda locale e la UI
                        if (localSchedaHash != remoteHash) {
                            saveSchedaToLocal(remoteScheda, remoteHash)
                            _scheda.postValue(remoteScheda)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Qui puoi eventualmente loggare l'errore o mostrare un messaggio
                }
            })
        }
    }



    // Salva la scheda in locale
    fun saveSchedaToLocal(scheda: Scheda, hash: String) {
        val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        // Inverti i gruppi muscolari e gli esercizi prima di salvare
        val schedaModificata = scheda.apply {
            giorni.forEach { (_, giorno) ->
                // Inverti i gruppi muscolari
                val gruppiMuscolariInvertiti = giorno.gruppiMuscolari.toList().reversed().toMap()
                giorno.gruppiMuscolari = gruppiMuscolariInvertiti
                // Inverti gli esercizi per ogni gruppo muscolare
                giorno.gruppiMuscolari.forEach { (_, gruppoMuscolare) ->
                    val eserciziInvertiti = gruppoMuscolare.esercizi.toList().reversed().toMap()
                    gruppoMuscolare.esercizi = eserciziInvertiti
                }
            }
        }
        // Salva l'hash della scheda
        editor.putString("scheda_hash", hash)
        // Salva i dati della scheda come JSON
        editor.putString("scheda_data", Gson().toJson(schedaModificata))
        editor.apply()
    }

    // Carica la scheda dalla memoria locale
    private fun loadSchedaFromLocal(): Scheda? {
        val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("scheda_data", null)
        // Deserializza il JSON in un oggetto Scheda
        return gson.fromJson(json, Scheda::class.java)
    }
    /*
    private fun logJson(json: String) {
        // Stampa il JSON nel log come tale
        Log.d("JSON_LOG", json)

        // Converti di nuovo il JSON in oggetto Kotlin per modifiche se necessarie
        val jsonObject = JSONObject(json)

        // Inverti solo l'ordine delle chiavi delle "giorni"
        val giorniInverted = invertMapOrder(jsonObject.getJSONObject("giorni"))

        // Sostituisci la parte del JSON con la mappa invertita
        jsonObject.put("giorni", giorniInverted)

        // Logga il nuovo JSON
        Log.d("JSON_LOG_INVERTED", jsonObject.toString())
    }
    // Funzione per invertire l'ordine delle chiavi
    private fun invertMapOrder(json: JSONObject): JSONObject {
        val invertedJson = JSONObject()
        val keys = json.keys().asSequence().toList().reversed() // Inverte le chiavi
        for (key in keys) {
            invertedJson.put(key, json.get(key))
        }
        return invertedJson
    }
*/
    // Recupera l'hash locale della scheda
    private fun getLocalSchedaHash(): String? {
        val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
        return sharedPreferences.getString("scheda_hash", null)
    }

    // Calcola l'hash di una stringa
    private fun hashString(input: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }

    // Funzione per aggiornare le note utente di un esercizio
    fun updateEsercizioNotes(
        giornoId: String,
        gruppoMuscolareId: String,
        esercizioId: String,
        noteUtente: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
            val savedCode = sharedPreferences.getString("code", "") ?: ""
            val database = FirebaseDatabase.getInstance()
            val esercizioRef = database.reference
                .child("users")
                .child(savedCode)
                .child("scheda")
                .child("giorni")
                .child(giornoId)
                .child("gruppiMuscolari")
                .child(gruppoMuscolareId)
                .child("esercizi")
                .child(esercizioId)
            val updates = hashMapOf<String, Any?>( "noteUtente" to noteUtente )
            esercizioRef.updateChildren(updates)
                .addOnSuccessListener {
                    onSuccess()  // Callback di successo
                }
                .addOnFailureListener { error ->
                    onFailure(error.message ?: "Errore sconosciuto")  // Callback di fallimento
                }
        }
    }
}


class SchedaViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SchedaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SchedaViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}