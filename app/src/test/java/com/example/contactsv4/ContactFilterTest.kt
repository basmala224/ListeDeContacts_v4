package com.example.contactsv4

import com.example.contactsv4.model.Contact
import com.example.contactsv4.util.ContactFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactFilterTest {

    private val contacts = listOf(
        Contact(1, "Ben Ali", "Amine", "+216 20 123 456"),
        Contact(2, "Trabelsi", "Sarra", "+216 22 987 654"),
        Contact(3, "Gharbi", "Youssef", "98 555 111"),
        Contact(4, "Mansour", "Leïla", "55 444 333"),
        Contact(5, "O'Neil", "Marie", "+33 6 12 34 56 78"),
        Contact(6, "Jlassi", "Nour", "21 333 222")
    )

    @Test fun requeteVide_renvoieTout() {
        assertEquals(contacts.size, ContactFilter.filtrer(contacts, "").size)
        assertEquals(contacts.size, ContactFilter.filtrer(contacts, "   ").size)
        assertEquals(contacts.size, ContactFilter.filtrer(contacts, null).size)
    }

    @Test fun nom_insensibleALaCasse() =
        assertEquals(listOf(2L), ContactFilter.filtrer(contacts, "TRABELSI").map { it.id })

    @Test fun prenom_insensibleAuxAccents() =
        assertEquals(listOf(4L), ContactFilter.filtrer(contacts, "leila").map { it.id })

    @Test fun prenomEtNom() =
        assertEquals(listOf(5L), ContactFilter.filtrer(contacts, "marie o'n").map { it.id })

    @Test fun apostrophe_sansErreur() =
        assertEquals(listOf(5L), ContactFilter.filtrer(contacts, "o'neil").map { it.id })

    @Test fun telephone_ignoreLesEspaces() {
        assertEquals(listOf(3L), ContactFilter.filtrer(contacts, "98555").map { it.id })
        assertEquals(listOf(3L), ContactFilter.filtrer(contacts, "98 555 111").map { it.id })
    }

    @Test fun aucunResultat() = assertTrue(ContactFilter.filtrer(contacts, "zzzz").isEmpty())

    @Test fun lIdEstConserveApresFiltrage() =
        assertEquals(2L, ContactFilter.filtrer(contacts, "sarra")[0].id)
}
