package com.example.contactsv4.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.contactsv4.R

object Intents {
    /**
     * Ouvre le composeur avec le numéro (ACTION_DIAL : aucune permission requise,
     * l'utilisateur confirme l'appel lui-même). Gère un numéro vide et l'absence d'appli téléphone.
     */
    fun composer(ctx: Context, numero: String?): Boolean {
        val tel = numero?.trim().orEmpty()
        if (tel.isEmpty()) {
            Toast.makeText(ctx, R.string.err_numero_vide, Toast.LENGTH_SHORT).show()
            return false
        }
        return try {
            ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(tel))))
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(ctx, R.string.err_pas_de_composeur, Toast.LENGTH_SHORT).show()
            false
        }
    }
}
