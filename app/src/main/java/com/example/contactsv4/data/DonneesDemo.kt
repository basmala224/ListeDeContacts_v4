package com.example.contactsv4.data

import com.example.contactsv4.model.Contact

/** Contacts insérés une seule fois, dans ContactDbHelper.onCreate (création de contacts.db). */
object DonneesDemo {
    val contacts: List<Contact> = listOf(
        Contact(nom = "Ben Ali", prenom = "Amine", telephone = "+216 20 123 456",
            email = "amine.benali@example.com", latitude = 36.8065, longitude = 10.1815),   // Tunis
        Contact(nom = "Trabelsi", prenom = "Sarra", telephone = "+216 22 987 654",
            email = "sarra.trabelsi@example.com", latitude = 34.7406, longitude = 10.7603), // Sfax
        Contact(nom = "Gharbi", prenom = "Youssef", telephone = "98 555 111",
            email = "youssef.gharbi@example.com", latitude = 35.8256, longitude = 10.6360),  // Sousse
        Contact(nom = "Mansour", prenom = "Leila", telephone = "55 444 333",
            email = "leila.mansour@example.com"),                                            // sans GPS
        Contact(nom = "O'Neil", prenom = "Marie", telephone = "+33 6 12 34 56 78",
            email = "marie.oneil@example.com", latitude = 48.8584, longitude = 2.2945),      // Paris
        Contact(nom = "Jlassi", prenom = "Nour", telephone = "21 333 222")                  // sans GPS ni e-mail
    )
}
