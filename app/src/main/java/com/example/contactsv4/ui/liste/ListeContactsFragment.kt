package com.example.contactsv4.ui.liste

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactsv4.MainActivity
import com.example.contactsv4.R
import com.example.contactsv4.data.ContactViewModel
import com.example.contactsv4.databinding.FragmentListeContactsBinding
import com.example.contactsv4.ui.detail.DetailContactFragment
import com.example.contactsv4.util.Intents
import com.example.contactsv4.util.PreferencesManager

/** Écran 1 : liste chargée depuis SQLite, filtrage, bouton « + », appel par ligne. */
class ListeContactsFragment : Fragment() {

    private var _binding: FragmentListeContactsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactViewModel by activityViewModels()
    private lateinit var adapter: ContactAdapter
    private lateinit var prefs: PreferencesManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentListeContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferencesManager(requireContext())

        adapter = ContactAdapter(
            onClick = { contact -> ouvrirDetail(contact.id) },
            onAppeler = { contact -> Intents.composer(requireContext(), contact.telephone) }
        )
        binding.recyclerContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerContacts.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )
        binding.recyclerContacts.adapter = adapter

        // Création : DetailContactFragment avec id = 0 (champs vides).
        binding.fabAjouter.setOnClickListener { ouvrirDetail(0L) }

        val requeteCourante = viewModel.requete.value.orEmpty()
        if (binding.etRecherche.text?.toString() != requeteCourante) binding.etRecherche.setText(requeteCourante)
        binding.etRecherche.doAfterTextChanged { viewModel.setRequete(it?.toString().orEmpty()) }

        viewModel.contactsFiltres.observe(viewLifecycleOwner) { liste ->
            adapter.submitList(liste)
            binding.tvCompteur.text = resources.getQuantityString(R.plurals.nb_contacts, liste.size, liste.size)
            majMessageVide()
        }
        viewModel.chargement.observe(viewLifecycleOwner) {
            binding.progress.isVisible = it
            majMessageVide()
        }

        // Erreurs de lecture éventuelles (les résultats d'enregistrement sont traités par le détail).
        viewModel.resultat.observe(viewLifecycleOwner) { ev ->
            val r = ev.consommerSi { it is ContactViewModel.Resultat.Erreur } as? ContactViewModel.Resultat.Erreur
            r?.let { Toast.makeText(requireContext(), it.messageRes, Toast.LENGTH_LONG).show() }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = getString(R.string.titre_liste)
        // Préférences relues à chaque retour (Paramètres -> Liste) : application immédiate.
        adapter.affichage = prefs.affichage()
        // Lecture depuis la base à chaque affichage, avec l'ordre de tri choisi.
        viewModel.charger(prefs.ordre)
    }

    /** Message si la base est vide ou si le filtre ne donne aucun résultat. */
    private fun majMessageVide() {
        val b = _binding ?: return
        val vide = viewModel.contactsFiltres.value.isNullOrEmpty() && viewModel.chargement.value != true
        b.tvAucunResultat.isVisible = vide
        if (vide) {
            val q = viewModel.requete.value.orEmpty()
            b.tvAucunResultat.text =
                if (q.isBlank()) getString(R.string.liste_vide) else getString(R.string.aucun_resultat, q)
        }
    }

    private fun ouvrirDetail(id: Long) {
        (requireActivity() as MainActivity).naviguerVers(
            DetailContactFragment.newInstance(id), DetailContactFragment.TAG
        )
    }

    override fun onDestroyView() {
        binding.recyclerContacts.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "liste"
    }
}
