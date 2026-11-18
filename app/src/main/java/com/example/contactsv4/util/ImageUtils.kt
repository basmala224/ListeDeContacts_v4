package com.example.contactsv4.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import com.example.contactsv4.R
import java.io.File

/**
 * Affiche une photo enregistrée (chemin dans filesDir) en taille réduite (inSampleSize).
 * Fichier absent ou illisible -> image par défaut, sans plantage.
 */
object ImageUtils {

    fun afficher(view: ImageView, chemin: String?, taillePx: Int = 400) {
        val bmp = if (chemin.isNullOrBlank()) null else try { decoder(chemin, taillePx) } catch (e: Exception) { null }
        if (bmp != null) view.setImageBitmap(bmp) else view.setImageResource(R.drawable.ic_person)
    }

    private fun decoder(chemin: String, taille: Int): Bitmap? {
        if (!File(chemin).exists()) return null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(chemin, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
        var sample = 1
        while (opts.outWidth / (sample * 2) >= taille && opts.outHeight / (sample * 2) >= taille) sample *= 2
        return BitmapFactory.decodeFile(chemin, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
