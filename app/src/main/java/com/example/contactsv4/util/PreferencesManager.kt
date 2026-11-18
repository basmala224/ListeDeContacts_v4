package com.example.contactsv4.util

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.preference.PreferenceManager
import com.example.contactsv4.data.ContactContract

/** Réglages déjà convertis, transmis tels quels à l'adapter et au détail. */
data class Affichage(
    val tailleSp: Float,
    val couleur: Int,
    val appelDepuisListe: Boolean
)

/**
 * Lecture CENTRALISÉE des SharedPreferences (aucune clé lue ailleurs).
 * Les fonctions de conversion sont pures pour être testées en test unitaire.
 */
class PreferencesManager(context: Context) {

    private val ctx = context.applicationContext
    private val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)

    val tailleSp: Float get() = convertirTaille(prefs.getString(KEY_TAILLE, DEFAUT_TAILLE))
    val couleur: Int get() = convertirCouleur(prefs.getString(KEY_COULEUR, DEFAUT_COULEUR), modeSombre())
    val appelDepuisListe: Boolean get() = prefs.getBoolean(KEY_APPEL, false)
    val ordre: ContactContract.Ordre get() = ContactContract.Ordre.depuisCle(prefs.getString(KEY_ORDRE, "nom"))

    fun affichage() = Affichage(tailleSp, couleur, appelDepuisListe)

    private fun modeSombre(): Boolean =
        (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    companion object {
        const val KEY_TAILLE = "taille_police"
        const val KEY_COULEUR = "couleur_preferee"
        const val KEY_APPEL = "appel_depuis_liste"
        const val KEY_ORDRE = "ordre_tri"
        const val DEFAUT_TAILLE = "16"
        const val DEFAUT_COULEUR = "#000000"

        private val TAILLES_AUTORISEES = setOf(14f, 16f, 20f)

        /**
         * Couleur claire -> variante lisible sur fond sombre.
         * Les valeurs #RRGGBB de arrays.xml sont choisies pour le thème clair.
         */
        private val VARIANTES_SOMBRES = mapOf(
            "#000000" to "#EEEEEE", // Noir   -> texte clair
            "#1565C0" to "#90CAF9", // Bleu
            "#2E7D32" to "#A5D6A7", // Vert
            "#C62828" to "#EF9A9A", // Rouge
            "#6A1B9A" to "#CE93D8"  // Violet
        )

        fun convertirTaille(valeur: String?): Float {
            val t = valeur?.toFloatOrNull() ?: return DEFAUT_TAILLE.toFloat()
            return if (t in TAILLES_AUTORISEES) t else DEFAUT_TAILLE.toFloat()
        }

        /** Renvoie la couleur hexadécimale à appliquer (testable sans Android). */
        fun hexAppliquee(valeur: String?, sombre: Boolean): String {
            val v = valeur?.uppercase()?.takeIf { it.matches(Regex("^#[0-9A-F]{6}$")) } ?: DEFAUT_COULEUR
            return if (sombre) VARIANTES_SOMBRES[v] ?: v else v
        }

        fun convertirCouleur(valeur: String?, sombre: Boolean): Int =
            Color.parseColor(hexAppliquee(valeur, sombre))
    }
}
