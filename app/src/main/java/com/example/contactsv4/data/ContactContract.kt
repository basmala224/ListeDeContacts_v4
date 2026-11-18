package com.example.contactsv4.data

/**
 * Noms de la table et des colonnes : aucun nom de colonne n'est écrit en dur ailleurs.
 */
object ContactContract {
    const val TABLE = "contacts"

    const val COL_ID = "_id"
    const val COL_NOM = "nom"
    const val COL_PRENOM = "prenom"
    const val COL_TELEPHONE = "telephone"
    const val COL_EMAIL = "email"
    const val COL_PHOTO_URI = "photo_uri"
    const val COL_LATITUDE = "latitude"
    const val COL_LONGITUDE = "longitude"
    /** Bonus (version 2 du schéma). */
    const val COL_FAVORI = "favori"

    /** Schéma de la version 1 (celui de l'énoncé). */
    const val SQL_CREATE_V1 = """
        CREATE TABLE $TABLE (
            $COL_ID        INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_NOM       TEXT NOT NULL,
            $COL_PRENOM    TEXT NOT NULL,
            $COL_TELEPHONE TEXT NOT NULL,
            $COL_EMAIL     TEXT,
            $COL_PHOTO_URI TEXT,
            $COL_LATITUDE  REAL,
            $COL_LONGITUDE REAL
        )"""

    /** Migration 1 -> 2 : ajout de la colonne favori sans perte de données. */
    const val SQL_ADD_FAVORI = "ALTER TABLE $TABLE ADD COLUMN $COL_FAVORI INTEGER NOT NULL DEFAULT 0"

    /** Ordres de tri autorisés : fragments SQL constants, jamais issus d'une saisie. */
    enum class Ordre(val cle: String, val orderBy: String) {
        NOM("nom", "$COL_NOM COLLATE NOCASE, $COL_PRENOM COLLATE NOCASE"),
        PRENOM("prenom", "$COL_PRENOM COLLATE NOCASE, $COL_NOM COLLATE NOCASE"),
        FAVORIS("favoris", "$COL_FAVORI DESC, $COL_NOM COLLATE NOCASE, $COL_PRENOM COLLATE NOCASE");

        companion object {
            fun depuisCle(cle: String?): Ordre = entries.firstOrNull { it.cle == cle } ?: NOM
        }
    }
}
