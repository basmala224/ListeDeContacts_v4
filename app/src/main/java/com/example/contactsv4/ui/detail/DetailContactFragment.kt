package com.example.contactsv4.ui.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.contactsv4.MainActivity
import com.example.contactsv4.R
import com.example.contactsv4.data.ContactViewModel
import com.example.contactsv4.databinding.FragmentDetailContactBinding
import com.example.contactsv4.model.Contact
import com.example.contactsv4.ui.carte.CarteContactFragment
import com.example.contactsv4.util.CoordResult
import com.example.contactsv4.util.CoordinateValidator
import com.example.contactsv4.util.ImageUtils
import com.example.contactsv4.util.Intents
import com.example.contactsv4.util.PhotoStorage
import com.example.contactsv4.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Écran 2 : création (id = 0), consultation, modification et suppression d'un contact.
 * Le contact est relu depuis la base à l'ouverture ; la saisie reste un brouillon
 * jusqu'à « Enregistrer ».
 */
class DetailContactFragment : Fragment() {

    private var _binding: FragmentDetailContactBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactViewModel by activityViewModels()
    private val contactId: Long by lazy { requireArguments().getLong(ARG_ID, 0L) }
    private val modeCreation get() = contactId == 0L

    /** Photo enregistrée en base au chargement (pour supprimer l'ancien fichier si remplacée). */
    private var photoOriginale: String? = null
    /** Photo choisie mais pas encore enregistrée. */
    private var photoBrouillon: String? = null
    /** Fichier en cours d'écriture par l'application Caméra. */
    private var photoCameraEnCours: String? = null
    private var champsRemplis = false
    private var enregistrementEnCours = false

    // ---- Galerie : l'image choisie est COPIÉE dans filesDir (chemin permanent)
    private val choisirImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) { toast(R.string.msg_galerie_annulee); return@registerForActivityResult }
        val ctx = requireContext().applicationContext
        lifecycleScope.launch {
            val chemin = withContext(Dispatchers.IO) { PhotoStorage.copierDepuisUri(ctx, uri) }
            if (chemin == null) toast(R.string.err_copie_photo) else remplacerBrouillon(chemin)
        }
    }

    // ---- Caméra : écrit directement dans filesDir/photos via FileProvider
    private val prendrePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val chemin = photoCameraEnCours
        photoCameraEnCours = null
        if (ok && chemin != null && File(chemin).length() > 0) {
            remplacerBrouillon(chemin)
        } else {
            PhotoStorage.supprimer(chemin)
            toast(R.string.msg_camera_annulee)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            photoOriginale = it.getString(STATE_PHOTO_ORIG)
            photoBrouillon = it.getString(STATE_PHOTO)
            photoCameraEnCours = it.getString(STATE_CAMERA)
            champsRemplis = it.getBoolean(STATE_REMPLIS)   // les EditText restaurent leur texte
        }
        // Réponse du dialogue de confirmation de suppression.
        childFragmentManager.setFragmentResultListener(
            ConfirmationSuppressionDialog.REQUEST_KEY, this
        ) { _, b ->
            if (b.getBoolean(ConfirmationSuppressionDialog.KEY_CONFIRME)) viewModel.supprimer(contactId)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetailContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appliquerPreferences()

        binding.btnSupprimer.isVisible = !modeCreation
        requireActivity().title = getString(if (modeCreation) R.string.titre_nouveau else R.string.titre_detail)

        if (modeCreation) {
            champsRemplis = true
            afficherPhoto()
            afficherResumePosition(null)
        } else {
            chargerDepuisLaBase()
        }

        binding.btnAppeler.setOnClickListener { Intents.composer(requireContext(), texte(binding.etTelephone)) }
        binding.btnEmail.setOnClickListener { envoyerEmail() }
        binding.btnGalerie.setOnClickListener { ouvrirGalerie() }
        binding.btnCamera.setOnClickListener { ouvrirCamera() }
        binding.btnCarte.setOnClickListener { voirSurLaCarte() }
        binding.btnEnregistrer.setOnClickListener { enregistrer() }
        binding.btnSupprimer.setOnClickListener {
            ConfirmationSuppressionDialog.newInstance(nomCompletSaisi())
                .show(childFragmentManager, ConfirmationSuppressionDialog.TAG)
        }
        binding.btnAnnuler.setOnClickListener { annuler() }

        viewModel.resultat.observe(viewLifecycleOwner) { ev ->
            when (val r = ev.consommer()) {
                is ContactViewModel.Resultat.Enregistre -> {
                    toast(if (r.creation) R.string.msg_cree else R.string.msg_enregistre)
                    parentFragmentManager.popBackStack()
                }
                ContactViewModel.Resultat.Supprime -> {
                    toast(R.string.msg_supprime)
                    parentFragmentManager.popBackStack()
                }
                is ContactViewModel.Resultat.Erreur -> {
                    enregistrementEnCours = false
                    binding.btnEnregistrer.isEnabled = true
                    Toast.makeText(requireContext(), r.messageRes, Toast.LENGTH_LONG).show()
                }
                null -> Unit
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_PHOTO_ORIG, photoOriginale)
        outState.putString(STATE_PHOTO, photoBrouillon)
        outState.putString(STATE_CAMERA, photoCameraEnCours)
        outState.putBoolean(STATE_REMPLIS, champsRemplis)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    // ------------------------------------------------------------------ chargement / affichage

    private fun chargerDepuisLaBase() {
        viewLifecycleOwner.lifecycleScope.launch {
            val contact = viewModel.getContact(contactId)
            if (_binding == null) return@launch
            if (contact == null) {
                toast(R.string.msg_contact_introuvable)
                parentFragmentManager.popBackStack()
                return@launch
            }
            requireActivity().title = contact.nomComplet
            if (!champsRemplis) {          // ne pas écraser une saisie restaurée après rotation
                remplirChamps(contact)
                photoOriginale = contact.photoUri
                photoBrouillon = contact.photoUri
                champsRemplis = true
            }
            afficherPhoto()
            afficherResumePosition(contact)
        }
    }

    private fun remplirChamps(c: Contact) = with(binding) {
        etNom.setText(c.nom)
        etPrenom.setText(c.prenom)
        etTelephone.setText(c.telephone)
        etEmail.setText(c.email.orEmpty())
        etLatitude.setText(c.latitude?.let { formater(it) }.orEmpty())
        etLongitude.setText(c.longitude?.let { formater(it) }.orEmpty())
        cbFavori.isChecked = c.favori
    }

    /** Taille et couleur choisies dans les Paramètres. */
    private fun appliquerPreferences() {
        val a = PreferencesManager(requireContext()).affichage()
        listOf(binding.etNom, binding.etPrenom, binding.etTelephone)
            .forEach { it.setTextSize(TypedValue.COMPLEX_UNIT_SP, a.tailleSp) }
        binding.etNom.setTextColor(a.couleur)
        binding.etPrenom.setTextColor(a.couleur)
    }

    private fun afficherResumePosition(c: Contact?) {
        binding.tvPosition.text = if (c != null && c.aUnePosition) {
            getString(R.string.position_enregistree, formater(c.latitude!!), formater(c.longitude!!))
        } else getString(R.string.position_absente)
    }

    private fun afficherPhoto() {
        _binding?.let { ImageUtils.afficher(it.ivPhoto, photoBrouillon, 480) }
    }

    /** Remplace le brouillon et supprime l'ancien brouillon s'il n'a jamais été enregistré. */
    private fun remplacerBrouillon(chemin: String) {
        if (photoBrouillon != photoOriginale) PhotoStorage.supprimer(photoBrouillon)
        photoBrouillon = chemin
        afficherPhoto()
    }

    private fun formater(d: Double) = String.format(Locale.US, "%.6f", d).trimEnd('0').trimEnd('.')
    private fun texte(e: android.widget.EditText) = e.text?.toString()?.trim().orEmpty()

    // ------------------------------------------------------------------ actions

    private fun envoyerEmail() {
        val email = texte(binding.etEmail)
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.err_email)
            return
        }
        binding.tilEmail.error = null
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        }
        try { startActivity(intent) } catch (_: ActivityNotFoundException) { toast(R.string.err_pas_de_messagerie) }
    }

    private fun ouvrirGalerie() {
        try {
            choisirImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (_: ActivityNotFoundException) {
            toast(R.string.err_pas_de_galerie)
        }
    }

    private fun ouvrirCamera() {
        try {
            val fichier = PhotoStorage.nouveauFichier(requireContext())
            val uri = FileProvider.getUriForFile(
                requireContext(), "${requireContext().packageName}.fileprovider", fichier
            )
            photoCameraEnCours = fichier.absolutePath
            prendrePhoto.launch(uri)
        } catch (_: ActivityNotFoundException) {
            photoCameraEnCours = null
            toast(R.string.err_pas_de_camera)
        } catch (_: Exception) {
            photoCameraEnCours = null
            toast(R.string.err_camera)
        }
    }

    private fun voirSurLaCarte() {
        val args = bundleOf(CarteContactFragment.ARG_ID to contactId)
        when (val r = validerCoordonnees()) {
            is CoordResult.Valide -> {
                args.putString(CarteContactFragment.ARG_ETAT, CarteContactFragment.ETAT_VALIDE)
                args.putDouble(CarteContactFragment.ARG_LAT, r.latitude)
                args.putDouble(CarteContactFragment.ARG_LNG, r.longitude)
            }
            CoordResult.Absente -> args.putString(CarteContactFragment.ARG_ETAT, CarteContactFragment.ETAT_ABSENTE)
            is CoordResult.Erreur -> args.putString(CarteContactFragment.ARG_ETAT, CarteContactFragment.ETAT_INVALIDE)
        }
        args.putString(CarteContactFragment.ARG_TITRE, nomCompletSaisi().ifBlank { getString(R.string.contact) })
        (requireActivity() as MainActivity).naviguerVers(
            CarteContactFragment().apply { arguments = args }, CarteContactFragment.TAG
        )
    }

    private fun nomCompletSaisi() = "${texte(binding.etPrenom)} ${texte(binding.etNom)}".trim()

    private fun annuler() {
        // La photo choisie mais non enregistrée est supprimée du stockage interne.
        if (photoBrouillon != photoOriginale) PhotoStorage.supprimer(photoBrouillon)
        toast(R.string.msg_modifs_annulees)
        parentFragmentManager.popBackStack()
    }

    // ------------------------------------------------------------------ validation / enregistrement

    private fun validerCoordonnees(): CoordResult {
        val r = CoordinateValidator.valider(binding.etLatitude.text?.toString(), binding.etLongitude.text?.toString())
        binding.tilLatitude.error = (r as? CoordResult.Erreur)?.champLatitude
        binding.tilLongitude.error = (r as? CoordResult.Erreur)?.champLongitude
        return r
    }

    private fun enregistrer() {
        if (enregistrementEnCours) return
        val nom = texte(binding.etNom)
        val prenom = texte(binding.etPrenom)
        val tel = texte(binding.etTelephone)
        val email = texte(binding.etEmail)

        var ok = true
        binding.tilNom.error = if (nom.isEmpty()) getString(R.string.err_obligatoire).also { ok = false } else null
        binding.tilPrenom.error = if (prenom.isEmpty()) getString(R.string.err_obligatoire).also { ok = false } else null
        binding.tilTelephone.error = when {
            tel.isEmpty() -> getString(R.string.err_obligatoire).also { ok = false }
            !tel.matches(Regex("^[+]?[0-9 .()-]{6,20}$")) -> getString(R.string.err_telephone).also { ok = false }
            else -> null
        }
        binding.tilEmail.error =
            if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches())
                getString(R.string.err_email).also { ok = false } else null
        val coord = validerCoordonnees()
        if (coord is CoordResult.Erreur) ok = false

        if (!ok) { toast(R.string.msg_corriger_erreurs); return }   // rien n'est écrit en base

        val (lat, lng) = if (coord is CoordResult.Valide) coord.latitude to coord.longitude else null to null
        enregistrementEnCours = true
        binding.btnEnregistrer.isEnabled = false
        viewModel.enregistrer(
            Contact(
                id = contactId, nom = nom, prenom = prenom, telephone = tel,
                email = email.ifEmpty { null }, photoUri = photoBrouillon,
                latitude = lat, longitude = lng, favori = binding.cbFavori.isChecked
            ),
            anciennePhoto = photoOriginale
        )
    }

    private fun toast(res: Int) = Toast.makeText(requireContext(), res, Toast.LENGTH_SHORT).show()

    companion object {
        const val TAG = "detail"
        private const val ARG_ID = "contact_id"
        private const val STATE_PHOTO_ORIG = "photo_originale"
        private const val STATE_PHOTO = "photo_brouillon"
        private const val STATE_CAMERA = "photo_camera"
        private const val STATE_REMPLIS = "champs_remplis"

        /** id = 0 -> mode création. */
        fun newInstance(id: Long) = DetailContactFragment().apply { arguments = bundleOf(ARG_ID to id) }
    }
}
