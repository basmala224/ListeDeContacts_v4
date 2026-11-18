package com.example.contactsv4.util

/** Enveloppe d'un événement LiveData consommable une seule fois (pas de toast rejoué après rotation). */
class Evenement<out T>(private val contenu: T) {
    private var consomme = false

    fun consommer(): T? = if (consomme) null else { consomme = true; contenu }

    /** Consomme l'événement uniquement s'il satisfait la condition. */
    fun consommerSi(condition: (T) -> Boolean): T? =
        if (!consomme && condition(contenu)) { consomme = true; contenu } else null
}
