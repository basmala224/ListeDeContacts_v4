# Compte rendu — TP Android Contacts V4

**Étudiante :** Aouida Basmala  
**Dépôt GitHub :** _à compléter_

> Insérer les captures réelles dans `docs/captures/`, puis exporter ce fichier en PDF.

## 1. Schéma de la base et choix des types

Base `contacts.db`, table `contacts` :

| Colonne | Type | Contrainte | Kotlin |
|---|---|---|---|
| `_id` | INTEGER | PRIMARY KEY AUTOINCREMENT | `id: Long` |
| `nom` | TEXT | NOT NULL | `nom: String` |
| `prenom` | TEXT | NOT NULL | `prenom: String` |
| `telephone` | TEXT | NOT NULL | `telephone: String` |
| `email` | TEXT | — | `email: String?` |
| `photo_uri` | TEXT | — | `photoUri: String?` |
| `latitude` | REAL | NULL autorisé | `latitude: Double?` |
| `longitude` | REAL | NULL autorisé | `longitude: Double?` |
| `favori` (bonus, v2) | INTEGER | NOT NULL DEFAULT 0 | `favori: Boolean` |

- Téléphone en **TEXT** : un numéro peut commencer par 0 ou +216 et contenir des espaces ; en INTEGER
  le 0 initial et le + seraient perdus.
- Coordonnées en **REAL** (nombre à virgule) ; **NULL** dans les deux colonnes = position absente.
- `favori` en INTEGER 0/1 : SQLite n'a pas de type booléen.

## 2. Rôle du helper et du DAO

- **`ContactContract`** : nom de la table, des colonnes, requêtes de création/migration et ordres de
  tri sous forme de constantes. Aucun nom de colonne n'est écrit en dur ailleurs.
- **`ContactDbHelper`** (hérite de `SQLiteOpenHelper`) : crée la table dans `onCreate` et y insère
  6 contacts de démonstration (dont 2 sans GPS et un nom avec apostrophe, « O'Neil ») dans une seule
  transaction. `DATABASE_VERSION = 2` ; `onUpgrade` ajoute la colonne `favori` quand la base est en
  version 1. Singleton construit avec `applicationContext`.
- **`ContactDao`** : seule classe contenant du SQL. `insert`, `update`, `delete`, `getAll`, `getById`,
  `search`. Toutes les valeurs variables passent par des arguments `?`, chaque `Cursor` est fermé par
  `use { }` et les colonnes sont lues avec `getColumnIndexOrThrow()`. Tri `COLLATE NOCASE`
  (nom puis prénom par défaut). L'`ORDER BY` provient d'une énumération de chaînes constantes, jamais
  d'une saisie.

## 3. Stratégie de threads

Aucun accès à la base sur le thread principal :
- écritures et rechargements dans **`viewModelScope`** avec **`withContext(Dispatchers.IO)`**
  (`ContactViewModel.charger / enregistrer / supprimer`). `viewModelScope` survit à la rotation :
  un enregistrement en cours n'est pas interrompu ;
- lecture d'un contact dans le Détail via `viewLifecycleOwner.lifecycleScope` + fonction `suspend`
  exécutée sur `Dispatchers.IO` ;
- copie de la photo de galerie également sur `Dispatchers.IO`.
Le résultat (enregistré, supprimé, erreur) est publié dans une `LiveData` d'événement consommable une
seule fois, pour qu'un toast ne soit pas rejoué après une rotation.

## 4. Choix du filtrage : en mémoire

La liste complète est chargée depuis SQLite à chaque affichage de l'écran, puis filtrée **en mémoire**
par `ContactFilter` au fil de la saisie. Raisons :
- un carnet compte quelques dizaines ou centaines de contacts : le filtrage en mémoire est instantané
  et évite une requête disque à chaque caractère tapé ;
- il ignore les **accents** (« leila » trouve « Leïla ») et les **espaces du téléphone**, alors que
  `LIKE` de SQLite n'ignore la casse que pour les lettres ASCII ;
- il est couvert par des tests unitaires.
La méthode `ContactDao.search()` (LIKE paramétré, `%` et `_` échappés) est tout de même fournie ;
elle deviendrait préférable pour un très grand nombre de contacts.

## 5. Gestion des photos

- **Galerie** (`PickVisualMedia`) : l'image est **copiée** dans `filesDir/photos/` ; c'est ce chemin
  permanent qui est enregistré en base, car l'URI fournie par la galerie peut devenir illisible après
  un redémarrage.
- **Caméra** (`TakePicture` + `FileProvider`) : la photo est écrite directement dans `filesDir/photos/`.
- Une photo remplacée, ou celle d'un contact supprimé, est effacée du stockage. Une photo choisie puis
  annulée aussi.
- Affichage en taille réduite (`inSampleSize`) ; un fichier absent affiche l'image par défaut.

## 6. Préférences utilisateur

- **Stockage** : `SharedPreferences` par défaut (clé/valeur), gérées automatiquement par
  `ParametresFragment` (`PreferenceFragmentCompat`, `res/xml/preferences.xml`, `res/values/arrays.xml`).
  Le résumé affiche la valeur choisie (`useSimpleSummaryProvider`).
- **Lecture centralisée** : `PreferencesManager` convertit les valeurs (taille → `Float` en sp,
  couleur `#RRGGBB` → `Int`, appel → `Boolean`, ordre de tri → enum) ; l'adapter reçoit un objet
  `Affichage` déjà converti.
- **Application immédiate** : dans `ListeContactsFragment.onResume()`, les préférences sont relues et
  passées à l'adapter, qui rafraîchit ses lignes (`notifyItemRangeChanged`) ; la liste est rechargée avec
  l'ordre de tri choisi. Le Détail applique la taille et la couleur à son ouverture.
- **Lisibilité** : les couleurs sont des teintes foncées pour le thème clair ; en thème sombre,
  `PreferencesManager` applique une variante claire (le noir devient gris très clair).
- **Bouton Appeler par ligne** : `ImageButton` `GONE` si désactivé ; `ACTION_DIAL` ; il consomme son
  clic et n'ouvre donc pas la fiche ; numéro vide et absence d'application téléphone gérés.

## 7. Tests réalisés

| # | Test | Résultat attendu | OK |
|---|---|---|---|
| 1 | Premier lancement / deuxième lancement | 6 contacts de démo, pas de doublon | ☐ |
| 2 | Ajouter (« + »), modifier, supprimer puis fermer l'app (récents) | chaque opération persiste | ☐ |
| 3 | Nom « L'Ariana », filtre contenant ' | aucune erreur SQL | ☐ |
| 4 | Enregistrer sans nom / prénom / téléphone | message, rien en base | ☐ |
| 5 | Changer la photo puis redémarrer l'appareil | photo toujours affichée | ☐ |
| 6 | GPS absent / « abc » / 95 / 200 | rien d'invalide enregistré, message sur la carte | ☐ |
| 7 | Chaque taille et couleur | appliquées dans la liste et le détail | ☐ |
| 8 | Activer / désactiver l'appel depuis la liste | bouton visible / masqué, ouvre le composeur sans ouvrir le détail | ☐ |
| 9 | Relancer l'application | les préférences sont conservées | ☐ |
| 10 | Rotation : liste, détail en saisie, paramètres, dialogue de suppression | pas de doublon, saisie conservée | ☐ |
| 11 | Retour depuis chaque écran | écran précédent | ☐ |
| 12 | App Inspection › Database Inspector | table identique à l'affichage | ☐ |
| 13 | Bonus : base v1 existante → v2 | contacts conservés, colonne favori ajoutée | ☐ |

Tests unitaires automatisés (`./gradlew test`, 22 tests) : filtrage (casse, accents, apostrophe,
téléphone, id conservé), validation GPS, conversion des préférences et de l'ordre de tri.

## 8. Difficultés rencontrées

_À compléter (exemples : JDK de Gradle incompatible avec Java 25, base non recréée après modification de
`onCreate`, URI de galerie illisible après redémarrage, conservation de la saisie à la rotation)._

## 9. Questions de compréhension

**1. onCreate / onUpgrade.** `onCreate` est appelé au premier accès à la base (`getWritableDatabase` ou
`getReadableDatabase`) quand le fichier `contacts.db` n'existe pas. `onUpgrade` est appelé quand
`DATABASE_VERSION` est supérieur à la version enregistrée dans le fichier. Si l'on modifie `onCreate`
sans changer la version, la modification est ignorée sur les appareils où la base existe déjà : il
faut désinstaller l'application (ou effacer ses données) ou incrémenter la version et écrire
`onUpgrade`.

**2. Arguments `?`.** Les valeurs sont transmises séparément de la requête : elles ne peuvent jamais être
interprétées comme du SQL, ce qui protège contre l'injection SQL et évite d'échapper les caractères
spéciaux. Exemple : avec `"... WHERE nom = '" + saisie + "'"`, la saisie `O'Neil` produit
`WHERE nom = 'O'Neil'` → erreur de syntaxe ; la saisie `' OR '1'='1` renverrait tous les contacts.

**3. Valeurs de retour.** `insert()` renvoie le nouvel `_id` (ou −1 en cas d'échec) ; `update()` et
`delete()` renvoient le nombre de lignes affectées. Dans l'application : −1 → « L'insertion a échoué » ;
0 ligne → « Contact introuvable » ; sinon message de succès, retour à la liste et rechargement.

**4. Thread principal.** Il dessine l'interface et traite les événements ; un accès disque peut le
bloquer, ce qui donne une interface figée et, au-delà de quelques secondes, une erreur « L'application
ne répond pas » (ANR). Les appels au DAO sont exécutés dans des coroutines (`viewModelScope` /
`lifecycleScope`) avec `withContext(Dispatchers.IO)`.

**5. Helper unique avec applicationContext.** Une seule instance partage une seule connexion : pas
d'accès concurrents entre plusieurs connexions, pas d'ouvertures répétées coûteuses. `applicationContext`
vit aussi longtemps que l'application : garder une Activity dans un singleton provoquerait une fuite
mémoire après sa destruction (rotation, fermeture).

**6. SharedPreferences ou SQLite.** SharedPreferences : quelques valeurs simples clé/valeur, lues en
entier, sans recherche ni relation (réglages). SQLite : données structurées, nombreuses, à rechercher,
trier, modifier ligne par ligne, avec contraintes. Les contacts sont des enregistrements multiples à
trier et filtrer → SQLite. Les trois réglages sont des valeurs isolées → SharedPreferences.

**7. Application sans redémarrage.** Au retour des Paramètres, `ListeContactsFragment.onResume()` relit
les préférences via `PreferencesManager`, les donne à l'adapter (qui rafraîchit les lignes visibles)
et recharge la liste avec l'ordre de tri choisi.

**8. ACTION_DIAL vs ACTION_CALL.** `ACTION_DIAL` ouvre le composeur avec le numéro prérempli :
l'utilisateur décide d'appeler, **aucune permission** n'est nécessaire. `ACTION_CALL` lance l'appel
immédiatement et exige la permission dangereuse `CALL_PHONE`, accordée à l'exécution.

**9. Désinstallation / effacement des données.** La base (`databases/contacts.db`), les préférences
(`shared_prefs/`) et les photos (`files/`) sont dans le stockage privé de l'application : elles sont
supprimées à la désinstallation et lorsqu'on efface les données (Paramètres › Applications › Stockage).
Au lancement suivant, `onCreate` recrée la base avec les contacts de démonstration et les préférences
reprennent leurs valeurs par défaut. (Si la sauvegarde automatique Android est active, ces fichiers
peuvent être restaurés lors d'une réinstallation.)
