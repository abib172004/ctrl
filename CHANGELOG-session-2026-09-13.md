# Changelog — session du 13/09/2026

Deux chantiers traités : (1) complétion du catalogue P1 de Triggers/Conditions/Actions,
(2) fiabilisation de l'arrière-plan (remplacement du polling par AlarmManager/WorkManager).

## Bugs corrigés

- **Le déclencheur "À intervalles réguliers" (`Trigger.Intervalle`) n'était vérifié nulle
  part dans le moteur** : une macro configurée dessus ne se déclenchait jamais. Corrigé
  via `TriggerScheduler`.
- **Le déclencheur "Heure Fixe" s'exécutait deux fois** à chaque occurrence : la boucle de
  polling vérifiait toutes les 30s si l'heure courante correspondait, donc une correspondance
  était détectée sur les deux vérifications de la même minute (60s / 30s = 2). Corrigé en
  remplaçant le polling par une alarme exacte unique par occurrence.

## Chantier 1 — Catalogue P1 (Triggers / Conditions / Actions)

### Nouveaux déclencheurs (`domain/model/Trigger.kt`)
Appel Sortant, Appel Manqué, Appel Terminé, SMS Envoyé, Température Batterie, Économiseur de
Batterie, VPN, Connexion USB, Point d'Accès (Hotspot), Capteur de Luminosité, Capteur de
Proximité, Orientation d'Écran, Écran Déverrouillé, État de la Torche, Mode Silencieux,
Modification du Presse-Papier (16 au total).

### Nouvelles conditions (`domain/model/Condition.kt`)
VPN actif, Économiseur de batterie actif, Appareil verrouillé, Jour du mois, Comparaison de
variables (opérateurs =, ≠, >, <, contient), Macro en cours d'exécution (anti-doublon).

### Nouvelles actions (`domain/model/ActionMacro.kt`)
Passer un appel, Partager du texte, Remplir le presse-papier, Envoyer un Intent générique,
Requête HTTP (GET/POST/PUT/DELETE), Analyse JSON (extraction par chemin de clé), Effacer les
notifications, Ouvrir le journal d'appels, Manipulation de listes (ajouter/supprimer/obtenir/
longueur/joindre).

### Câblage complet
- `EvaluerConditionsUseCase.kt` : toutes les nouvelles conditions évaluées.
- `ExecuterMacroUseCase.kt` : toutes les nouvelles actions exécutées ; ajout d'un registre
  process-wide (`companion object`) des macros en cours d'exécution pour la condition
  anti-doublon.
- `JsonConverters.kt` : sérialisation/désérialisation JSON pour tous les nouveaux types.
- `MacroBuilderScreen.kt` : **remplacement de tous les placeholders `Trigger.Manuel()`** qui
  occupaient déjà une place dans le catalogue UI (ex. "Appel Sortant" pointait en réalité vers
  `Trigger.AppelEntrant()`, "VPN" vers `Condition.WifiConnecte()`, etc.) par les vrais types
  nouvellement créés. Ajout de nouvelles entrées pour les actions Web/HTTP/téléphonie/listes.
- `AutomationMacroList.kt` : icônes dédiées pour les nouveaux types (au lieu de l'icône
  générique de fallback).
- `AndroidManifest.xml` : permissions ajoutées — `CALL_PHONE`, `READ_SMS`,
  `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`.

### Volontairement non couverts cette passe (restent en `Trigger.Manuel()` dans l'UI)
- **Événement du Calendrier** — nécessite l'API Calendar Provider + permission dédiée.
- **Chronomètre** — nécessite des actions Démarrer/Arrêter/Réinitialiser dédiées, non
  implémentées.
- **Clic sur l'interface utilisateur** — nécessite le routage des événements
  `TYPE_VIEW_CLICKED` dans `CtrlAccessibilityService` avec correspondance de sélecteur ;
  risque de complexité/performance à traiter séparément (P2 assumé).

## Chantier 2 — Fiabilité de l'arrière-plan

### Nouveau : `data/triggers/scheduler/TriggerScheduler.kt`
Point central de planification :
- **Heure Fixe** → `AlarmManager.setExactAndAllowWhileIdle` (dégrade gracieusement vers une
  alarme non-exacte si la permission "Alarmes et rappels" n'est pas accordée sur Android 12+),
  se replanifie lui-même au jour suivant après chaque exécution.
- **Intervalle ≥ 15 min** → `WorkManager PeriodicWorkRequest` (limite système), se répète
  nativement, survit au redémarrage sans code additionnel.
- **Intervalle < 15 min** → chaîne d'alarmes exactes auto-replanifiées (MacroDroid autorise
  des intervalles plus courts que la limite WorkManager).

### Nouveaux fichiers
- `data/triggers/receivers/AlarmTriggerReceiver.kt` — reçoit les alarmes, exécute la macro,
  se replanifie.
- `data/triggers/workers/MacroIntervalWorker.kt` — `CoroutineWorker` pour le chemin
  WorkManager.

### Câblage
- `MacroRepositoryImpl` accepte désormais un `Context?` optionnel (rétrocompatible, défaut
  `null`) et déclenche automatiquement `TriggerScheduler.schedule()` / `.cancel()` à chaque
  `insertOrUpdate` / `setMacroActive` / `delete`.
- 5 ViewModels mis à jour pour passer le contexte : `MacroBuilderViewModel`,
  `DashboardViewModel`, `MacroDetailViewModel`, `AiGeneratorViewModel`, `SettingsViewModel`.
- `SettingsViewModel.importerConfiguration()` : le flux d'import contournant
  `insertOrUpdate`, une replanification explicite est faite après import.
- `SystemEventReceiver` : `BOOT_COMPLETED` appelle désormais
  `TriggerScheduler.reschedulerTout()` (les alarmes/WorkManager ne survivent pas nativement
  à un redémarrage complet de l'appareil, contrairement à un simple relancement de process).
- `SystemEventReceiver` étendu pour distinguer Appel Sortant / Manqué / Terminé à partir des
  transitions d'état téléphonique (IDLE → OFFHOOK = sortant, RINGING → IDLE = manqué,
  OFFHOOK → IDLE = terminé), en plus de l'appel entrant déjà géré.

### `CtrlForegroundService.kt` — réécrit
Retrait de la boucle de polling buguée sur Heure Fixe (déplacée vers `TriggerScheduler`).
Ajout des écouteurs temps réel nécessaires aux nouveaux triggers (ceux-ci nécessitent un
process vivant, contrairement aux triggers temporels) :
- Capteurs luminosité (`TYPE_LIGHT`) et proximité (`TYPE_PROXIMITY`), avec déclenchement sur
  franchissement de seuil (pas de spam).
- `CameraManager.TorchCallback` (API publique) pour l'état de la torche.
- `ConnectivityManager.NetworkCallback` pour la détection VPN (`TRANSPORT_VPN`).
- `ClipboardManager.OnPrimaryClipChangedListener` pour le presse-papier (sans lecture du
  contenu, restreinte par Android 10+ pour les apps en arrière-plan).
- `ContentObserver` sur `Telephony.Sms.Sent` pour le SMS envoyé (nécessite `READ_SMS`).
- Broadcasts dynamiques supplémentaires : économiseur de batterie, mode silencieux, USB,
  écran déverrouillé, température batterie (avec anti-spam par seuil franchi/macro).
- `onConfigurationChanged` overridé pour l'orientation d'écran (plus fiable qu'un broadcast).
- Boucle basse fréquence (20s) conservée uniquement pour l'état du point d'accès Wi-Fi
  (aucune API d'écoute publique documentée ; utilise la réflexion sur `isWifiApEnabled`,
  dégradation silencieuse si indisponible sur l'OEM/version ciblée).

### Dépendance ajoutée
`androidx.work:work-runtime-ktx:2.10.1` (`libs.versions.toml` + `app/build.gradle.kts`).

## Passe complémentaire (suite à relance) — nettoyage + OCR + dernier item P1

Deux points identifiés dans l'audit initial mais jamais traités dans la première passe :

- **Dossier mort `ui/theme/` supprimé** (`Color.kt`, `Type.kt`, `Theme.kt` — reliquat du
  template par défaut d'Android Studio, non référencé nulle part).
- **`OcrEngine.kt` était un moteur complet (ML Kit + lecture d'arbre d'accessibilité) mais
  totalement orphelin — jamais appelé.** Maintenant câblé :
  - Nouveau déclencheur `Trigger.ContenuEcran(motifRegex, appSource?)` : dispatché depuis
    `CtrlAccessibilityService` sur `TYPE_WINDOW_CONTENT_CHANGED`, avec throttle (800ms) et
    déclenchement sur front (motif absent → présent) pour éviter le spam.
  - Nouvelle action `ActionMacro.VerifierTexteEcran(motifRegex, variableTrouve,
    variableContenu)` : lit l'écran courant à la demande dans une macro et stocke le
    résultat dans des variables, réutilisables ensuite dans une "Clause Si".
  - Les deux ont maintenant un écran de configuration dédié dans le Créateur de Macro
    (avant : `OcrEngine` n'était référencé dans aucun écran).
- **Dernier item P1 restant, "Clic sur l'interface utilisateur"**, également câblé :
  nouveau `Trigger.ClicUI(texteCible)` dispatché sur `AccessibilityEvent.TYPE_VIEW_CLICKED`.

### Audit de recherche de mocks/stubs restants

Recherche systématique (`TODO`, `FIXME`, `mock`, `fake`, `stub`, `placeholder`, `dummy`,
`simulat`, `hardcod`) sur l'ensemble du code source : **aucun marqueur de code factice
restant**. Vérification manuelle en plus des fichiers les plus susceptibles d'être creux :
- `GeminiApiClient.kt` : vrai appel HTTP à l'API Gemini (pas de réponse simulée).
- `SelectorEngine.kt` / `GestureExecutor.kt` : résolution de sélecteurs et
  `dispatchGesture()` réels sur l'arbre d'accessibilité, pas de stub.
- `DashboardViewModel` (stats globales) et `MacroRepositoryImpl.recordExecution()` :
  calculs réels à partir des compteurs persistés en base (Room), pas de données figées.

### Ce qui reste *vraiment* (2 items, tous deux nécessitant un sous-système entièrement neuf)

1. **Événement du Calendrier** — nécessite l'intégration du Calendar Provider Android
   (permission `READ_CALENDAR`, requête sur `CalendarContract`), non commencée.
2. **Chronomètre** — nécessite des actions dédiées Démarrer/Arrêter/Réinitialiser avec un
   état persistant par chronomètre nommé, non commencées.

Ces deux items restent affichés comme `Trigger.Manuel()` dans le catalogue UI (inoffensif :
ils ne se déclenchent simplement jamais tant qu'ils ne sont pas implémentés). Tout le reste
du catalogue P1/P2 du fichier `reference.md` que j'ai jugé raisonnablement faisable sans
root est maintenant réellement fonctionnel de bout en bout (modèle → moteur → UI).

## ⚠️ Non vérifié — pas de compilation possible dans cet environnement

Je n'ai pas accès au SDK Android / Gradle dans ce bac à sable, donc **ce code n'a pas été
compilé**. J'ai fait une relecture manuelle systématique (vérification de toutes les branches
`when` exhaustives sur les sealed classes `Trigger`/`Condition`/`ActionMacro` à travers tout
le projet, vérification des signatures de constructeurs, comptage d'équilibrage
accolades/parenthèses sur l'ensemble des fichiers), mais **il faut lancer `./gradlew build`
dans Android Studio pour confirmer que tout compile**, en particulier :
- Les noms d'icônes Material (`Icons.Default.Http`, `.ClearAll`, `.CallEnd`, `.Rule`,
  `.Block`, etc.) — je suis confiant qu'ils existent dans `material-icons-extended` mais je
  ne peux pas le garantir à 100% sans compiler.
- Le comportement runtime du `WifiManager.isWifiApEnabled` par réflexion, qui varie selon les
  OEM (Samsung/Xiaomi notamment) et les versions d'Android.

## Passe 3 — les 3 urgences (IA réelle, permissions, batterie)

**1. Générateur IA connecté à Gemini pour de vrai.** Nouveau
`GenererMacroAvecGeminiUseCase` : appelle `GeminiApiClient` avec un prompt système versionné
(`AiPromptTemplates.kt`, schéma JSON complet trigger/condition/action), parse la réponse via
`JsonConverters`. Si échec (pas de clé API / réseau / JSON invalide) : repli explicite vers
l'ancien moteur de mots-clés (`GenererMacroParIAUseCase`, conservé mais recadré comme
fallback offline) — **et l'UI l'affiche clairement** ("⚠ Mode hors-ligne..." vs "✓ Généré par
Gemini"), plus de faux-semblant.

**2. Permissions runtime demandées.** Nouveau `PermissionsHelper.kt` + section "Permissions
Android" dans Réglages : liste les permissions dangereuses réellement utilisées (SMS,
téléphone, caméra, Bluetooth 12+, notifications 13+), statut par permission, bouton unique
qui déclenche le vrai dialogue système (`ActivityResultContracts.RequestMultiplePermissions`).

**3. Batterie + alarmes exactes.** Section "Optimisation de batterie" dans Réglages :
détecte si Ctrl est soumis à Doze (`PowerManager.isIgnoringBatteryOptimizations`) et propose
`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. Ajout d'un contrôle pour
`canScheduleExactAlarms()` (Android 12+) avec bouton vers `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`
si refusé — jusqu'ici l'app dégradait silencieusement sans le dire.

### Reste des points de l'audit précédent (non traités cette passe)
Contrôle de flux sans UI, pas de Hilt/CtrlApplication, OCR MediaProjection absent,
export/import "copier-coller" au lieu de SAF, tests quasi inexistants, catégories entières
(géoloc, météo, calendrier, chronomètre, email, WhatsApp, fichiers, QR, capture écran,
chiffrement, traduction, widgets).
