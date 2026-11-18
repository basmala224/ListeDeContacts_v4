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

## Captures d'écran

Captures réelles sur émulateur (Android 37), application `com.example.contactsv4` v4.0.
Toutes les captures sont dans [`docs/captures/`](docs/captures) ; le détail des tests est dans
[`docs/COMPTE_RENDU.md`](docs/COMPTE_RENDU.md).

| Liste (SQLite) | Détail | Nouveau contact | Validation |
|---|---|---|---|
| <img src="docs/captures/01_liste_contacts.png" width="200"> | <img src="docs/captures/03_detail_contact.png" width="200"> | <img src="docs/captures/06_nouveau_contact.png" width="200"> | <img src="docs/captures/07_validation_champs_obligatoires.png" width="200"> |

| Filtre « o'n » | Ajout « L'Ariana » | Confirmation de suppression | Après redémarrage |
|---|---|---|---|
| <img src="docs/captures/02_recherche_apostrophe.png" width="200"> | <img src="docs/captures/10_ajout_contact_apostrophe.png" width="200"> | <img src="docs/captures/11_dialogue_suppression.png" width="200"> | <img src="docs/captures/12_persistance_apres_redemarrage.png" width="200"> |

| Modification (favori) | Modification enregistrée | Liste après suppression |
|---|---|---|
| <img src="docs/captures/04_detail_favori_boutons.png" width="200"> | <img src="docs/captures/05_modification_enregistree.png" width="200"> | <img src="docs/captures/13_apres_suppression.png" width="200"> |

| Paramètres par défaut | Choix de la taille | Paramètres modifiés | Détail : Grande + Bleu |
|---|---|---|---|
| <img src="docs/captures/14_parametres_defaut.png" width="200"> | <img src="docs/captures/15_choix_taille_police.png" width="200"> | <img src="docs/captures/16_parametres_modifies.png" width="200"> | <img src="docs/captures/19_detail_grande_bleu.png" width="200"> |

| Grande + Bleu + Appeler | Petite + Rouge + favoris | Composeur depuis la liste |
|---|---|---|
| <img src="docs/captures/17_liste_grande_bleu_appel.png" width="200"> | <img src="docs/captures/21_liste_petite_rouge_favoris.png" width="200"> | <img src="docs/captures/18_composeur_depuis_liste.png" width="200"> |

| Photo depuis la Galerie | Photo dans la liste |
|---|---|
| <img src="docs/captures/24_photo_choisie_galerie.png" width="200"> | <img src="docs/captures/25_photo_enregistree_liste.png" width="200"> |

| GPS invalide | Carte : coordonnées invalides | Carte avec marqueur | Rotation |
|---|---|---|---|
| <img src="docs/captures/08_validation_gps.png" width="200"> | <img src="docs/captures/09_carte_coordonnees_invalides.png" width="200"> | <img src="docs/captures/20_carte_marqueur.png" width="200"> | <img src="docs/captures/22_rotation_paysage.png" width="300"> |

Table `contacts` réelle (extraite de l'émulateur) :

<img src="docs/captures/23_table_contacts_sqlite.png" width="800">

## Bonus réalisés
- Base en **version 2** : colonne `favori INTEGER NOT NULL DEFAULT 0` ajoutée par `onUpgrade`
  sans perte de données ; case « Contact favori » dans le détail, étoile dans la liste.
- Préférence **Ordre de tri** (nom, prénom, favoris d'abord) appliquée par la clause `ORDER BY`.
