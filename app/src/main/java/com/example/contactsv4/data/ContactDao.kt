package com.example.contactsv4.data

import android.content.ContentValues
import android.database.Cursor
import com.example.contactsv4.data.ContactContract.COL_EMAIL
import com.example.contactsv4.data.ContactContract.COL_FAVORI
import com.example.contactsv4.data.ContactContract.COL_ID
import com.example.contactsv4.data.ContactContract.COL_LATITUDE
import com.example.contactsv4.data.ContactContract.COL_LONGITUDE
import com.example.contactsv4.data.ContactContract.COL_NOM
import com.example.contactsv4.data.ContactContract.COL_PHOTO_URI
import com.example.contactsv4.data.ContactContract.COL_PRENOM
import com.example.contactsv4.data.ContactContract.COL_TELEPHONE
import com.example.contactsv4.data.ContactContract.TABLE
import com.example.contactsv4.model.Contact

/**
 * SEULE classe contenant du SQL.
 * - toutes les valeurs variables passent par des arguments « ? » ;
 * - chaque Cursor est fermé par use { } ;
 * - les méthodes sont BLOQUANTES : elles sont appelées depuis Dispatchers.IO.
 */
class ContactDao(private val helper: ContactDbHelper) {

    /** @return le nouvel _id, ou -1 en cas d'échec. */
    fun insert(contact: Contact): Long =
        helper.writableDatabase.insert(TABLE, null, versContentValues(contact))

    /** @return le nombre de lignes modifiées (1 attendu, 0 = contact introuvable). */
    fun update(contact: Contact): Int =
        helper.writableDatabase.update(
            TABLE, versContentValues(contact), "$COL_ID = ?", arrayOf(contact.id.toString())
        )

    /** @return le nombre de lignes supprimées (1 attendu, 0 = contact introuvable). */
    fun delete(id: Long): Int =
        helper.writableDatabase.delete(TABLE, "$COL_ID = ?", arrayOf(id.toString()))

    fun getAll(ordre: ContactContract.Ordre = ContactContract.Ordre.NOM): List<Contact> =
        helper.readableDatabase.query(TABLE, null, null, null, null, null, ordre.orderBy)
            .use { lireListe(it) }

    fun getById(id: Long): Contact? =
        helper.readableDatabase.query(
            TABLE, null, "$COL_ID = ?", arrayOf(id.toString()), null, null, null
        ).use { c -> if (c.moveToFirst()) lireContact(c) else null }

    /**
     * Recherche SQL (LIKE paramétré) sur nom, prénom, « prénom nom » et téléphone.
     * Les caractères % et _ saisis sont échappés ; une apostrophe ne pose aucun problème.
     */
    fun search(texte: String, ordre: ContactContract.Ordre = ContactContract.Ordre.NOM): List<Contact> {
        val q = texte.trim()
        if (q.isEmpty()) return getAll(ordre)
        val motif = "%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
        val motifTel = "%" + q.filter { it.isDigit() || it == '+' } + "%"
        val selection = """
            $COL_NOM LIKE ? ESCAPE '\' OR $COL_PRENOM LIKE ? ESCAPE '\'
            OR ($COL_PRENOM || ' ' || $COL_NOM) LIKE ? ESCAPE '\'
            OR $COL_TELEPHONE LIKE ? ESCAPE '\'
            OR (? <> '%%' AND REPLACE($COL_TELEPHONE, ' ', '') LIKE ?)"""
        return helper.readableDatabase.query(
            TABLE, null, selection,
            arrayOf(motif, motif, motif, motif, motifTel, motifTel),
            null, null, ordre.orderBy
        ).use { lireListe(it) }
    }

    companion object {
        fun versContentValues(c: Contact) = ContentValues().apply {
            put(COL_NOM, c.nom)
            put(COL_PRENOM, c.prenom)
            put(COL_TELEPHONE, c.telephone)
            put(COL_EMAIL, c.email)
            put(COL_PHOTO_URI, c.photoUri)
            if (c.latitude != null) put(COL_LATITUDE, c.latitude) else putNull(COL_LATITUDE)
            if (c.longitude != null) put(COL_LONGITUDE, c.longitude) else putNull(COL_LONGITUDE)
            put(COL_FAVORI, if (c.favori) 1 else 0)
        }

        private fun lireListe(c: Cursor): List<Contact> {
            val liste = ArrayList<Contact>(c.count)
            while (c.moveToNext()) liste += lireContact(c)
            return liste
        }

        private fun lireContact(c: Cursor): Contact {
            val iLat = c.getColumnIndexOrThrow(COL_LATITUDE)
            val iLng = c.getColumnIndexOrThrow(COL_LONGITUDE)
            val iEmail = c.getColumnIndexOrThrow(COL_EMAIL)
            val iPhoto = c.getColumnIndexOrThrow(COL_PHOTO_URI)
            return Contact(
                id = c.getLong(c.getColumnIndexOrThrow(COL_ID)),
                nom = c.getString(c.getColumnIndexOrThrow(COL_NOM)),
                prenom = c.getString(c.getColumnIndexOrThrow(COL_PRENOM)),
                telephone = c.getString(c.getColumnIndexOrThrow(COL_TELEPHONE)),
                email = if (c.isNull(iEmail)) null else c.getString(iEmail),
                photoUri = if (c.isNull(iPhoto)) null else c.getString(iPhoto),
                latitude = if (c.isNull(iLat)) null else c.getDouble(iLat),
                longitude = if (c.isNull(iLng)) null else c.getDouble(iLng),
                favori = c.getInt(c.getColumnIndexOrThrow(COL_FAVORI)) == 1
            )
        }
    }
}
