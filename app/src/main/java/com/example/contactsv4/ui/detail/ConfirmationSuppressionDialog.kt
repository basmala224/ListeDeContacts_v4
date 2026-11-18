package com.example.contactsv4.ui.detail

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.example.contactsv4.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * AlertDialog de confirmation, enveloppé dans un DialogFragment pour survivre à la rotation.
 * La réponse est renvoyée au fragment parent par la Fragment Result API.
 */
class ConfirmationSuppressionDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val nom = requireArguments().getString(ARG_NOM).orEmpty()
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_suppression_titre)
            .setMessage(getString(R.string.dialog_suppression_message, nom))
            .setNegativeButton(R.string.btn_annuler, null)
            .setPositiveButton(R.string.btn_supprimer) { _, _ ->
                setFragmentResult(REQUEST_KEY, bundleOf(KEY_CONFIRME to true))
            }
            .create()
    }

    companion object {
        const val TAG = "confirmation_suppression"
        const val REQUEST_KEY = "suppression"
        const val KEY_CONFIRME = "confirme"
        private const val ARG_NOM = "nom"

        fun newInstance(nom: String) = ConfirmationSuppressionDialog().apply {
            arguments = bundleOf(ARG_NOM to nom)
        }
    }
}
