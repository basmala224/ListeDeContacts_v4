package com.example.contactsv4.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Gestion du fichier contacts.db.
 * - onCreate : appelé UNE SEULE FOIS, quand le fichier n'existe pas encore ;
 * - onUpgrade : appelé quand DATABASE_VERSION est supérieur à la version du fichier.
 * Singleton construit avec applicationContext (pas de fuite d'activité, une seule connexion).
 */
class ContactDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(ContactContract.SQL_CREATE_V1)
        db.execSQL(ContactContract.SQL_ADD_FAVORI)
        // Insertion des données de démonstration dans une seule transaction.
        db.beginTransaction()
        try {
            DonneesDemo.contacts.forEach { c ->
                db.insertOrThrow(ContactContract.TABLE, null, ContactDao.versContentValues(c))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migrations successives : les contacts existants sont conservés.
        if (oldVersion < 2) db.execSQL(ContactContract.SQL_ADD_FAVORI)
    }

    companion object {
        const val DATABASE_NAME = "contacts.db"
        /** v1 = schéma de l'énoncé ; v2 = ajout de la colonne favori (bonus). */
        const val DATABASE_VERSION = 2

        @Volatile private var instance: ContactDbHelper? = null

        fun getInstance(context: Context): ContactDbHelper =
            instance ?: synchronized(this) {
                instance ?: ContactDbHelper(context.applicationContext).also { instance = it }
            }
    }
}
