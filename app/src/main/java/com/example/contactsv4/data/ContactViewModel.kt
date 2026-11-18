package com.example.contactsv4.data

import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.contactsv4.R
import com.example.contactsv4.model.Contact
import com.example.contactsv4.util.ContactFilter
import com.example.contactsv4.util.Evenement
import com.example.contactsv4.util.PhotoStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel PARTAGÉ (portée MainActivity).
 * Lance tous les appels au DAO dans viewModelScope + Dispatchers.IO (jamais sur le thread principal)
 * et republie la liste après chaque modification. viewModelScope survit à la rotation :
 * un enregistrement en cours n'est pas interrompu.
 */
class ContactViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = ContactDao(ContactDbHelper.getInstance(app))

    sealed class Resultat {
        data class Enregistre(val id: Long, val creation: Boolean) : Resultat()
        data object Supprime : Resultat()
        data class Erreur(val messageRes: Int) : Resultat()
    }

    private val _contacts = MutableLiveData<List<Contact>>(emptyList())
    val contacts: LiveData<List<Contact>> = _contacts

    private val _requete = MutableLiveData("")
    val requete: LiveData<String> = _requete

    private val _chargement = MutableLiveData(false)
    val chargement: LiveData<Boolean> = _chargement

    /** Événement à consommer une seule fois (toast + retour à la liste). */
    private val _resultat = MutableLiveData<Evenement<Resultat>>()
    val resultat: LiveData<Evenement<Resultat>> = _resultat

    private var ordre = ContactContract.Ordre.NOM

    /** Filtrage en mémoire sur la liste chargée depuis la base (choix justifié dans le compte rendu). */
    val contactsFiltres: LiveData<List<Contact>> = MediatorLiveData<List<Contact>>().apply {
        fun maj() { value = ContactFilter.filtrer(_contacts.value.orEmpty(), _requete.value) }
        addSource(_contacts) { maj() }
        addSource(_requete) { maj() }
    }

    fun setRequete(texte: String) {
        if (texte != _requete.value) _requete.value = texte
    }

    /** Recharge la liste depuis SQLite (appelé à chaque affichage de la liste). */
    fun charger(nouvelOrdre: ContactContract.Ordre = ordre) {
        ordre = nouvelOrdre
        viewModelScope.launch {
            _chargement.value = true
            try {
                _contacts.value = withContext(Dispatchers.IO) { dao.getAll(ordre) }
            } catch (e: SQLiteException) {
                _resultat.value = Evenement(Resultat.Erreur(R.string.err_lecture_base))
            } finally {
                _chargement.value = false
            }
        }
    }

    /** Lecture d'un contact (appelée depuis une coroutine du fragment). */
    suspend fun getContact(id: Long): Contact? = withContext(Dispatchers.IO) {
        try { dao.getById(id) } catch (e: SQLiteException) { null }
    }

    /** Insert si id == 0, sinon update. L'ancienne photo remplacée est supprimée du stockage interne. */
    fun enregistrer(contact: Contact, anciennePhoto: String?) {
        viewModelScope.launch {
            val resultat = withContext(Dispatchers.IO) {
                try {
                    if (contact.estNouveau) {
                        val id = dao.insert(contact)
                        if (id == -1L) Resultat.Erreur(R.string.err_insertion)
                        else Resultat.Enregistre(id, creation = true)
                    } else {
                        val n = dao.update(contact)
                        if (n == 0) Resultat.Erreur(R.string.msg_contact_introuvable)
                        else {
                            if (anciennePhoto != contact.photoUri) PhotoStorage.supprimer(anciennePhoto)
                            Resultat.Enregistre(contact.id, creation = false)
                        }
                    }
                } catch (e: SQLiteException) {
                    Resultat.Erreur(R.string.err_contrainte)
                }
            }
            _resultat.value = Evenement(resultat)
            if (resultat is Resultat.Enregistre) charger()
        }
    }

    fun supprimer(id: Long) {
        viewModelScope.launch {
            val resultat = withContext(Dispatchers.IO) {
                try {
                    val photo = dao.getById(id)?.photoUri
                    if (dao.delete(id) == 0) Resultat.Erreur(R.string.msg_contact_introuvable)
                    else { PhotoStorage.supprimer(photo); Resultat.Supprime }
                } catch (e: SQLiteException) {
                    Resultat.Erreur(R.string.err_suppression)
                }
            }
            _resultat.value = Evenement(resultat)
            if (resultat is Resultat.Supprime) charger()
        }
    }
}
