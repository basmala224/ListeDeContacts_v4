package com.example.contactsv4

import com.example.contactsv4.data.ContactContract
import com.example.contactsv4.util.PreferencesManager
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferencesManagerTest {

    @Test fun tailles() {
        assertEquals(14f, PreferencesManager.convertirTaille("14"))
        assertEquals(16f, PreferencesManager.convertirTaille("16"))
        assertEquals(20f, PreferencesManager.convertirTaille("20"))
    }

    @Test fun tailleInvalide_valeurParDefaut() {
        assertEquals(16f, PreferencesManager.convertirTaille(null))
        assertEquals(16f, PreferencesManager.convertirTaille("abc"))
        assertEquals(16f, PreferencesManager.convertirTaille("99"))
    }

    @Test fun couleurThemeClair() =
        assertEquals("#1565C0", PreferencesManager.hexAppliquee("#1565C0", sombre = false))

    @Test fun noirDevientClairEnThemeSombre() =
        assertEquals("#EEEEEE", PreferencesManager.hexAppliquee("#000000", sombre = true))

    @Test fun couleurInvalide_noirParDefaut() =
        assertEquals("#000000", PreferencesManager.hexAppliquee("rose", sombre = false))

    @Test fun ordreDeTri() {
        assertEquals(ContactContract.Ordre.PRENOM, ContactContract.Ordre.depuisCle("prenom"))
        assertEquals(ContactContract.Ordre.NOM, ContactContract.Ordre.depuisCle("inconnu"))
        assertEquals(ContactContract.Ordre.NOM, ContactContract.Ordre.depuisCle(null))
    }
}
