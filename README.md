# TP Android — Gestion des contacts V4

Évolution de la V3 : les contacts sont stockés dans une **base SQLite** (`contacts.db`, API native
`SQLiteOpenHelper`, sans Room) avec ajout / modification / suppression, et un écran **Paramètres**
(AndroidX Preference) règle la taille de police, la couleur du texte et un bouton Appeler par ligne.

## Lancer le projet

1. Android Studio → *File › Open* → dossier `ContactsV4` (celui qui contient `ContactsV4/settings.gradle.kts`).
2. *Settings › Build Tools › Gradle › Gradle JDK* : **JDK 17 ou 21** (Gradle 8.7 ne supporte pas Java 25).
3. Copier `local.properties.example` en `local.properties` et y mettre `MAPS_API_KEY=...`.
4. *Sync Project with Gradle Files*, puis ▶ sur un émulateur avec Google Play.

Terminal : `./gradlew installDebug` puis `adb shell am start -n com.example.contactsv4/.MainActivity`.
Tests unitaires : `./gradlew test`.

> Le package est `com.example.contactsv4` : la V3 et la V4 peuvent être installées côte à côte.
> Si vous modifiez le schéma pendant le développement, désinstallez l'application ou incrémentez
> `DATABASE_VERSION` (onCreate n'est appelé que si `contacts.db` n'existe pas).

## Clé Google Maps
Lue dans `local.properties` (ignoré par Git) et injectée dans le manifeste via `manifestPlaceholders`.
Aucune clé ni secret dans le dépôt. Voir le README de la V3 pour obtenir et restreindre la clé.

## Architecture
```
MainActivity                 FragmentContainerView + menu « Paramètres »
 ├─ ListeContactsFragment    liste SQLite, filtre, FAB « + », bouton Appeler par ligne (préférence)
 ├─ DetailContactFragment    création (id = 0) / consultation / modification / suppression
 │   └─ ConfirmationSuppressionDialog
 ├─ CarteContactFragment     Google Maps (inchangé)
 └─ ParametresFragment       PreferenceFragmentCompat (res/xml/preferences.xml)
data/   ContactContract · ContactDbHelper (singleton) · ContactDao (seul SQL) · ContactViewModel · DonneesDemo
util/   PreferencesManager · PhotoStorage · ContactFilter · CoordinateValidator · ImageUtils · Intents
```

## Bonus réalisés
- Base en **version 2** : colonne `favori INTEGER NOT NULL DEFAULT 0` ajoutée par `onUpgrade`
  sans perte de données ; case « Contact favori » dans le détail, étoile dans la liste.
- Préférence **Ordre de tri** (nom, prénom, favoris d'abord) appliquée par la clause `ORDER BY`.
