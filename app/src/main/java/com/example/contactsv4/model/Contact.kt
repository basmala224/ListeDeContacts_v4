package com.example.contactsv4.model

/**
 * Modèle d'un contact.
 * [id] vaut 0 tant que le contact n'est pas enregistré, puis prend la valeur _id
 * attribuée par SQLite. C'est cet id (jamais la position) qui circule entre écrans.
 */
data class Contact(
    val id: Long = 0L,
    val nom: String,
    val prenom: String,
    val telephone: String,
    val email: String? = null,
    val photoUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val favori: Boolean = false            // Bonus : colonne ajoutée en version 2
) {
    val nomComplet: String get() = "$prenom $nom".trim()
    val aUnePosition: Boolean get() = latitude != null && longitude != null
    val estNouveau: Boolean get() = id == 0L
}
