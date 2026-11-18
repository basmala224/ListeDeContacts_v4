package com.example.contactsv4.util

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Les photos sont COPIÉES dans le stockage interne (filesDir/photos) : on enregistre en base
 * un chemin de fichier permanent, et non une URI de galerie qui peut devenir illisible.
 */
object PhotoStorage {

    private const val DOSSIER = "photos"

    fun dossier(ctx: Context) = File(ctx.filesDir, DOSSIER).apply { mkdirs() }

    fun nouveauFichier(ctx: Context): File =
        File(dossier(ctx), "photo_${System.currentTimeMillis()}.jpg")

    /** Copie l'image choisie dans la galerie. Bloquant : appeler depuis Dispatchers.IO. */
    fun copierDepuisUri(ctx: Context, source: Uri): String? {
        return try {
            val dest = nouveauFichier(ctx)
            ctx.contentResolver.openInputStream(source)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            if (dest.length() > 0) dest.absolutePath else { dest.delete(); null }
        } catch (e: Exception) {
            null
        }
    }

    /** Supprime un fichier photo s'il appartient au dossier interne de l'application. */
    fun supprimer(chemin: String?) {
        if (chemin.isNullOrBlank()) return
        val f = File(chemin)
        if (f.parentFile?.name == DOSSIER && f.exists()) f.delete()
    }
}
