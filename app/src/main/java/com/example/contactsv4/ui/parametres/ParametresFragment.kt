package com.example.contactsv4.ui.parametres

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.contactsv4.R

/**
 * Écran Paramètres décrit dans res/xml/preferences.xml.
 * Les valeurs sont enregistrées automatiquement dans les SharedPreferences par défaut ;
 * le résumé de chaque préférence affiche la valeur choisie (useSimpleSummaryProvider).
 */
class ParametresFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = getString(R.string.titre_parametres)
    }

    companion object {
        const val TAG = "parametres"
    }
}
