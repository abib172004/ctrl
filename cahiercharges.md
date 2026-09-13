# Cahier des Charges — Ctrl (Version Native Android)

**Outil d'automatisation Android avancé — application native Kotlin**

| | |
|---|---|
| **Nom du projet** | Ctrl |
| **Baseline** | *"Take control."* |
| **Type** | Application Android native (Kotlin, Jetpack Compose) |
| **Auteur / Porteur** | Abib |
| **Usage initial** | Personnel (extensible à un produit public plus tard) |
| **Version du document** | 3.0 — Pivot natif, document de référence unique et définitif |
| **Date** | Septembre 2026 |
| **Document compagnon** | `design.md` (v2) — design system Daylit adapté à Ctrl, et `reference-macrodroid-catalogue-complet.md` — catalogue exhaustif des fonctionnalités de référence |
| **Statut des anciens documents** | Les versions précédentes (`cahier-des-charges-ctrl.md`, `design.md` v1, basées sur AutoJs6) sont **abandonnées et remplacées** par celui-ci. Elles restent disponibles comme historique mais ne doivent plus être utilisées pour le développement. |

---

## 0. Pourquoi ce pivot — historique de la décision

Le projet a d'abord été conçu autour d'**AutoJs6** (fork d'Auto.js), pour bénéficier gratuitement d'un moteur d'automatisation Android déjà construit (service d'accessibilité, sélecteurs UI, OCR) sans avoir à l'écrire soi-même.

Cette approche a été abandonnée après des blocages techniques récurrents et non anticipés :
- Le moteur JavaScript d'AutoJs6 (basé sur **Rhino**, pas V8/Node) ne supporte pas la syntaxe `async`/`await`, un pilier du code généré par l'IA pour toute la logique moteur — crash au démarrage.
- Au-delà de ce cas précis, plusieurs autres constructions JavaScript modernes (optional chaining, nullish coalescing, certaines méthodes récentes d'Array/Object) ont un support incertain ou partiel selon la version exacte de Rhino embarquée, créant un risque de "jeu de la taupe" : corriger un crash en révèle un autre, sans garantie de fin.
- Le packaging en APK via l'outil tiers `AutoJs6-ApkBuilder` a lui-même révélé des limitations non documentées (gestion des librairies natives OpenCV/OCR peu fiable).

**Décision** : passer à une application **100% native Kotlin**, qui élimine cette classe entière de problèmes (Kotlin/Coroutines sont un langage et un runtime complets, pas un sous-ensemble JavaScript partiellement compatible), au prix d'un travail plus important pour réimplémenter ce qu'AutoJs6 offrait nativement (service d'accessibilité, sélecteurs, OCR).

Ce cahier des charges est volontairement **exhaustif et directement actionnable par un agent IA** : chaque section est écrite pour qu'un agent de type Antigravity/Claude Code puisse l'implémenter sans avoir à deviner une intention, un flux, ou un détail de design.

---

## Sommaire

1. Vision & Objectifs
2. Utilisateurs Cibles
3. Analyse Concurrentielle
4. Choix Technique & Stack
5. Architecture Générale (Clean Architecture Android)
6. Modèle de Données (Room)
7. Le Service d'Accessibilité — Cœur Technique du Projet
8. Spécifications Fonctionnelles Complètes (Triggers / Conditions / Actions)
9. Fonctionnalités Avancées
10. Design System — voir `design.md`
11. Spécifications UI/UX Détaillées (Jetpack Compose)
12. Normes de Code Kotlin & Instructions pour l'IA
13. Contraintes Techniques Android
14. Permissions & Déclarations Manifest
15. Roadmap de Développement
16. Risques & Mitigation
17. Critères de Succès
18. Glossaire

---

## 1. Vision & Objectifs

### 1.1 Vision
Ctrl est un outil d'automatisation Android personnel qui reproduit intégralement les capacités de MacroDroid, avec des ajouts (OCR intelligent, génération de macro par IA, chaînage avancé), livré comme une application native, stable, publiable, sans dépendance à un moteur tiers.

### 1.2 Objectifs

| # | Objectif | Priorité |
|---|---|---|
| O1 | Reproduire l'intégralité du modèle Trigger → Condition → Action de MacroDroid (cf. `reference-macrodroid-catalogue-complet.md`) | Critique |
| O2 | Implémenter un vrai service d'accessibilité natif Kotlin (lecture/manipulation UI d'autres apps) | Critique |
| O3 | Lecture d'écran intelligente : OCR on-device (ML Kit Text Recognition) | Haute |
| O4 | Génération de macro à partir d'une description en langage naturel (appel API IA) | Haute |
| O5 | Chaînage de macros, logique de flux (boucles, conditions imbriquées, goto) | Haute |
| O6 | Sauvegarde/export/import JSON des macros + sync cloud optionnelle | Moyenne |
| O7 | UI 100% Jetpack Compose, mobile-first, suivant le design system `design.md` | Critique |
| O8 | Fiabilité en arrière-plan malgré les restrictions Android modernes (Doze, restrictions OEM) | Critique |
| O9 | Base de code Kotlin idiomatique, testée, maintenue selon les normes de la section 12 | Critique |
| O10 | (Optionnel, phase ultérieure) Publication Play Store avec justification de l'usage du service d'accessibilité | Basse |

---

## 2. Utilisateurs Cibles

**Phase 1 (actuelle)** : usage strictement personnel — Abib, unique utilisateur, sur son propre téléphone Android.

**Phase 2 (hypothétique)** : power users / développeurs francophones ou ouest-africains cherchant une alternative à MacroDroid plus transparente et personnalisable.

---

## 3. Analyse Concurrentielle

| Outil | Points forts | Limites |
|---|---|---|
| **MacroDroid** | UI simple, stable, faible conso batterie, catalogue trigger/condition/action très riche (cf. section 8) | Pas de scripting réel, lecture UI limitée en gratuit, pas d'IA native |
| **Tasker** | Très puissant, plugins tiers nombreux | Courbe d'apprentissage élevée, UI datée |
| **IFTTT** | Simplicité extrême, intégrations cloud | Très limité en local/UI Android |
| **Ctrl (natif, cible)** | Catalogue complet MacroDroid + IA + OCR + code natif stable, design system distinctif | À construire entièrement (pas de moteur tiers à exploiter) |

---

## 4. Choix Technique & Stack

### 4.1 Langage et framework UI
- **Kotlin** (100% — pas de Java, sauf éventuel code d'interopérabilité imposé par une librairie tierce).
- **Jetpack Compose** pour toute l'interface — pas de XML layouts, sauf si un composant système l'impose (rare en 2026).
- **Material 3** comme fondation de composants, entièrement re-thémée via les tokens du design system Daylit (`design.md`) — voir section 10.

### 4.2 Architecture applicative
- **Clean Architecture** en 3 couches : `presentation` (Compose UI + ViewModels), `domain` (cas d'usage, logique métier pure, aucune dépendance Android), `data` (Room, DataStore, Retrofit, AccessibilityService bridge).
- **MVVM** au niveau présentation : chaque écran Compose a un `ViewModel` exposant un `StateFlow<UiState>`.
- **Injection de dépendances : Hilt** (standard Android moderne, intégration Compose native).

### 4.3 Persistance
- **Room** (SQLite) pour les macros, variables globales, journal d'exécution.
- **DataStore (Preferences)** pour les réglages simples (thème, sync activée, etc.) — remplace `SharedPreferences`.

### 4.4 Exécution en arrière-plan
- **AccessibilityService** natif — cœur du moteur (détail section 7).
- **WorkManager** pour les triggers planifiés (intervalles, heures précises) qui doivent survivre au redémarrage et respecter Doze/contraintes système.
- **Foreground Service** avec notification persistante pour le "service de contrôle" toujours actif (équivalent MacroDroid).
- **BroadcastReceiver** pour les événements système (batterie, connectivité, écran, boot).

### 4.5 Réseau & IA
- **Retrofit + OkHttp** pour les appels API (génération de macro par IA, requêtes HTTP en tant qu'action, sync cloud).
- **Kotlinx.serialization** pour le JSON (macros, réponses API) — pas Gson, plus idiomatique Kotlin et sans réflexion.

### 4.6 OCR et vision
- **ML Kit Text Recognition (on-device)** — Google, gratuit, fonctionne hors-ligne, pas de dépendance native fragile comme dans l'approche AutoJs6.
- **ML Kit Barcode Scanning** pour les triggers/actions QR code (cf. catalogue MacroDroid section 8).

### 4.7 Concurrence
- **Coroutines Kotlin + Flow** pour tout le code asynchrone — c'est précisément ce que Rhino ne supportait pas correctement ; en natif, c'est un citoyen de première classe, standard, documenté, sans surprise.

### 4.8 Tests
- **JUnit5 + MockK** pour les tests unitaires de la couche `domain` (logique pure, testable sans Android).
- **Compose UI Testing** pour les tests d'interface critiques (créateur de macro, dashboard).

---

## 5. Architecture Générale (Clean Architecture Android)

```
app/
├── presentation/
│   ├── dashboard/          (écran d'accueil)
│   ├── macrobuilder/       (créateur de macro no-code)
│   ├── macrodetail/        (détail d'une macro)
│   ├── journal/            (journal d'exécution)
│   ├── aigenerator/        (générateur de macro par IA)
│   ├── variables/          (variables globales)
│   ├── settings/           (paramètres)
│   ├── navigation/         (graphe de navigation Compose)
│   └── theme/              (application du design system — voir design.md)
│
├── domain/
│   ├── model/              (Macro, Trigger, Condition, Action, Variable — classes pures Kotlin)
│   ├── usecase/            (CreerMacro, ExecuterMacro, EvaluerConditions, GenererMacroParIA, ...)
│   └── repository/         (interfaces — MacroRepository, VariableRepository, LogRepository)
│
├── data/
│   ├── local/
│   │   ├── room/           (entities, DAO, database)
│   │   └── datastore/      (préférences)
│   ├── remote/
│   │   └── api/            (client IA, webhooks, sync cloud)
│   ├── accessibility/
│   │   ├── CtrlAccessibilityService.kt   (service principal)
│   │   ├── SelectorEngine.kt             (résolution d'éléments UI : id/texte/description/position)
│   │   └── GestureExecutor.kt            (clics, swipes, saisie de texte)
│   ├── triggers/
│   │   ├── receivers/      (BroadcastReceivers par catégorie : SMS, batterie, connectivité...)
│   │   └── workers/        (WorkManager Workers pour triggers planifiés)
│   ├── ocr/
│   │   └── OcrEngine.kt    (wrapper ML Kit)
│   └── repositoryimpl/     (implémentations concrètes des interfaces domain)
│
├── service/
│   └── CtrlForegroundService.kt   (service au premier plan, notification persistante, orchestrateur global)
│
├── di/                     (modules Hilt)
└── CtrlApplication.kt      (point d'entrée, initialisation Hilt/WorkManager)
```

**Règle d'architecture stricte** : `domain` ne dépend **jamais** d'Android (`android.*`, `androidx.*` interdits dans ce module, sauf `kotlinx.coroutines`). C'est ce qui rend la logique métier testable en JVM pur, sans émulateur.

---

## 6. Modèle de Données (Room)

### 6.1 Entité Macro

```kotlin
@Entity(tableName = "macros")
data class MacroEntity(
    @PrimaryKey val id: String,
    val nom: String,
    val active: Boolean,
    val triggerJson: String,       // Trigger sérialisé (kotlinx.serialization)
    val conditionsJson: String,    // Liste de conditions sérialisée
    val actionsJson: String,       // Liste d'actions sérialisée
    val dateCreation: Long,
    val derniereExecution: Long?,
    val nbExecutions: Int = 0,
    val nbEchecs: Int = 0,
)
```

*Choix de conception* : trigger/conditions/actions sont stockés en JSON sérialisé plutôt qu'en tables relationnelles séparées, pour garder la flexibilité du schéma (types de trigger/action variés avec paramètres hétérogènes) sans multiplier les tables — au moment de l'exécution, ils sont désérialisés en objets `domain.model` typés.

### 6.2 Modèles de domaine (Kotlin purs, sealed classes)

```kotlin
sealed class Trigger {
    abstract val id: String

    data class SmsRecu(override val id: String, val expediteurFiltre: String?) : Trigger()
    data class NiveauBatterie(override val id: String, val seuil: Int, val sens: Comparaison) : Trigger()
    data class Intervalle(override val id: String, val periodeMs: Long) : Trigger()
    data class AppInstalleeOuLancee(override val id: String, val packageName: String, val evenement: EvenementApp) : Trigger()
    data class NotificationRecue(override val id: String, val appSource: String?, val motCle: String?) : Trigger()
    // ... un cas par famille de trigger listée en section 8
}

sealed class Condition {
    data class PlageHoraire(val debut: LocalTime, val fin: LocalTime) : Condition()
    data class Compose(val operateur: OperateurLogique, val sousConditions: List<Condition>) : Condition()
    // ... etc.
}

sealed class ActionMacro {
    data class EnvoyerSms(val destinataire: String, val message: String) : ActionMacro()
    data class ClicSurSelecteur(val selecteur: Selecteur, val timeoutMs: Long = 5000) : ActionMacro()
    data class ControleFlux(val type: TypeControleFlux, val params: Map<String, String>) : ActionMacro()
    // ... etc.
}
```

*Note pour l'agent IA* : cette utilisation de `sealed class` est le remplacement natif Kotlin du système JSON générique prévu dans l'ancienne version AutoJs6 — le compilateur garantit l'exhaustivité du traitement (`when` sans `else` requis), ce qui élimine une classe entière de bugs runtime déjà rencontrés côté JS.

### 6.3 Variables globales

```kotlin
@Entity(tableName = "variables_globales")
data class VariableEntity(
    @PrimaryKey val nom: String,
    val valeur: String,        // stocké en texte, typage géré à la lecture
    val type: TypeVariable,    // TEXTE, NOMBRE, BOOLEEN, LISTE
)
```

### 6.4 Journal d'exécution

```kotlin
@Entity(tableName = "journal_execution")
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val macroId: String,
    val timestamp: Long,
    val statut: StatutExecution,   // SUCCES, ECHEC, SUCCES_PARTIEL, IGNOREE
    val dureeMs: Long,
    val erreur: String?,
    val contexteJson: String,      // variables au moment T, pour debug
)
```

---

## 7. Le Service d'Accessibilité — Cœur Technique du Projet

C'est la partie la plus critique et la plus délicate du projet natif : c'est elle qui remplace tout ce qu'AutoJs6 offrait gratuitement.

### 7.1 `CtrlAccessibilityService`

```kotlin
class CtrlAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Dispatch vers les triggers de type "contenu d'écran", "app au premier plan", etc.
    }

    override fun onInterrupt() { /* nettoyage */ }

    override fun onServiceConnected() {
        // Configuration des flags : FLAG_RETRIEVE_INTERACTIVE_WINDOWS, etc.
        // Enregistrement auprès de l'orchestrateur (CtrlForegroundService)
    }
}
```

Déclaration XML requise (`res/xml/accessibility_service_config.xml`) :
```xml
<accessibility-service
    android:accessibilityEventTypes="typeAllMask"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:canRetrieveWindowContent="true"
    android:canPerformGestures="true"
    android:notificationTimeout="100" />
```

### 7.2 `SelectorEngine` — résolution d'éléments UI

Reproduit la stratégie de fallback définie dans l'ancien cahier des charges (section normes de code) : id → description → texte → position, avec chaque stratégie implémentée via `AccessibilityNodeInfo` :

```kotlin
class SelectorEngine {
    fun resoudre(selecteur: Selecteur, racine: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return resoudreParId(selecteur, racine)
            ?: resoudreParDescription(selecteur, racine)
            ?: resoudreParTexte(selecteur, racine)
            ?: resoudreParPosition(selecteur, racine)
    }
}
```

### 7.3 `GestureExecutor` — clics, swipes, saisie

Utilise `dispatchGesture()` (API `AccessibilityService`, disponible depuis Android 7+) pour simuler clics/swipes sans root, et `AccessibilityNodeInfo.performAction(ACTION_SET_TEXT, ...)` pour la saisie de texte dans des champs identifiés.

### 7.4 Activation du service (manip utilisateur obligatoire)

Comme pour toute app utilisant l'accessibilité, l'activation se fait manuellement dans **Réglages > Accessibilité > Apps téléchargées > Ctrl**. L'app doit détecter si le service est actif (`AccessibilityManager.isEnabled` + vérification du service dans la liste des services activés) et afficher un bandeau persistant sinon (cf. `design.md`, section états).

---

## 8. Spécifications Fonctionnelles Complètes

> **Référence exhaustive** : le catalogue complet des Triggers/Conditions/Actions à couvrir est déjà documenté en détail dans `reference-macrodroid-catalogue-complet.md` (issu de l'analyse des captures d'écran MacroDroid), avec priorisation P1/P2/P3. Ce fichier reste la checklist de référence — ne pas le dupliquer ici.

### 8.1 Mapping technique Android par catégorie

Pour guider l'implémentation native, voici la correspondance entre les catégories du catalogue et l'API Android à utiliser :

| Catégorie (cf. catalogue) | Mécanisme Android natif |
|---|---|
| Appel/SMS | `BroadcastReceiver` sur `SMS_RECEIVED`, `PhoneStateListener`/`TelephonyCallback` |
| Applications (lancée/fermée, premier plan) | `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED` via le service d'accessibilité |
| Batterie/Alimentation | `BroadcastReceiver` sur `ACTION_BATTERY_CHANGED`, `ACTION_POWER_CONNECTED` |
| Capteurs | `SensorManager` + `SensorEventListener` |
| Connectivité | `ConnectivityManager.NetworkCallback`, `BroadcastReceiver` sur `WIFI_STATE_CHANGED` |
| Date/Heure | `WorkManager` (périodique) ou `AlarmManager.setExactAndAllowWhileIdle` (heure précise) |
| Événements de l'appareil (écran, verrouillage) | `BroadcastReceiver` sur `ACTION_SCREEN_ON/OFF`, `ACTION_USER_PRESENT` |
| Localisation | `FusedLocationProviderClient` (Google Play Services) + `Geofencing API` |
| Saisie utilisateur (boutons physiques) | Accessibilité (limité) ou service dédié selon le bouton |
| Notification (trigger et lecture) | `NotificationListenerService` |
| Lecture d'écran / OCR | Service d'accessibilité (capture) + ML Kit Text Recognition |
| Actions UI (clic, swipe, saisie) | `AccessibilityService.dispatchGesture()` + `AccessibilityNodeInfo` |

### 8.2 Logique de flux de contrôle (boucles, goto, conditions imbriquées)

Contrairement à l'ancienne architecture JSON générique, en Kotlin cette logique est représentée nativement :

```kotlin
sealed class TypeControleFlux {
    data class ClauseSi(val condition: Condition, val actionsSiVrai: List<ActionMacro>, val actionsSinon: List<ActionMacro>) : TypeControleFlux()
    data class Repeter(val nombreFois: Int, val actions: List<ActionMacro>) : TypeControleFlux()
    data class RepeterTantQue(val condition: Condition, val actions: List<ActionMacro>) : TypeControleFlux()
    object SortirDeLaBoucle : TypeControleFlux()
}
```

L'exécuteur de macro (`ExecuterMacroUseCase`) interprète cette structure récursivement — c'est une fonction `suspend` Kotlin standard, sans aucune des limitations rencontrées avec Rhino.

---

## 9. Fonctionnalités Avancées

### 9.1 Lecture d'écran intelligente (OCR)
- `OcrEngine` encapsule ML Kit Text Recognition : capture d'une zone d'écran (via le service d'accessibilité + `MediaProjection` si capture plein écran nécessaire), passage à ML Kit, retour d'un texte structuré (blocs, lignes, position).
- Triggers/actions basés sur le contenu OCR détecté, avec extraction regex configurable par l'utilisateur.

### 9.2 Génération de macro par IA
- Écran dédié (`aigenerator`) : champ de saisie en langage naturel → appel Retrofit vers une API IA (Claude/GPT) avec un prompt système qui décrit le schéma `domain.model` (Trigger/Condition/ActionMacro) → parsing de la réponse JSON → validation stricte avant proposition à l'utilisateur.
- Le prompt système doit être versionné dans le code (`AiPromptTemplates.kt`), pas codé en dur dans le ViewModel.

### 9.3 Chaînage de macros
- `ActionMacro.DeclencherMacro(macroId: String)` — le `ExecuterMacroUseCase` peut s'invoquer récursivement, avec une limite de profondeur (garde-fou anti-boucle infinie, cf. section 13).

### 9.4 Sauvegarde / Sync cloud
- Export : sérialisation de toutes les `MacroEntity` en un fichier JSON via `kotlinx.serialization`, partagé via `Intent.ACTION_SEND` ou écrit dans le stockage partagé (Storage Access Framework, pas d'accès fichier direct non scopicé).
- Sync cloud (optionnelle, phase ultérieure) : upload/download vers Google Drive (API officielle) ou un petit backend FastAPI personnel.

### 9.5 Mode debug / test
- Exécution "pas à pas" d'une macro : chaque action produit un événement observable (`Flow<EtapeExecution>`) collecté par l'UI du Journal en temps réel pendant un test manuel.

---

## 10. Design System — voir `design.md`

Le design system complet (v2, basé sur le thème "Daylit" — burgundy/parchment, adapté à Ctrl) est détaillé intégralement dans le fichier compagnon `design.md`. Il couvre : tokens Compose (couleurs, typographie, formes), composants réutilisables, écrans détaillés, parcours utilisateurs, accessibilité. **Ce fichier fait autorité sur tout ce qui est visuel** — ne pas dupliquer son contenu ici.

---

## 11. Spécifications UI/UX Détaillées (Jetpack Compose)

> Le détail écran par écran est dans `design.md` section 9. Ici, uniquement les principes de structure technique Compose.

### 11.1 Structure Compose générale

- **Navigation** : `NavHost` unique (`presentation/navigation/CtrlNavGraph.kt`), routes typées via `sealed class Screen`.
- **State hoisting strict** : chaque écran a un seul `ViewModel` exposant un `StateFlow<UiState>` unique par écran (pas de multiples `StateFlow` épars). Le composable racine de l'écran collecte cet état via `collectAsStateWithLifecycle()`.
- **Composants du design system** dans un module dédié `presentation/theme/components/` (ex: `CtrlPillButton.kt`, `CtrlMacroCard.kt`) — jamais de style inline dupliqué dans les écrans, toujours un appel à un composant du design system.
- **Thème** : `CtrlTheme.kt` applique les tokens Daylit (section design.md) via `MaterialTheme` personnalisé (ColorScheme, Typography, Shapes fournis par le design system, pas les valeurs par défaut Material3).

### 11.2 Écrans (liste, détail dans `design.md`)

Dashboard · Liste des macros · Créateur de macro (no-code) · Détail d'une macro · Générateur IA · Journal d'exécution · Variables globales · Paramètres.

---

## 12. Normes de Code Kotlin & Instructions pour l'IA

> **Cette section s'adresse directement à l'agent IA qui implémentera Ctrl.** Elle est prescriptive.

### 12.1 Principes généraux

1. **Kotlin idiomatique** — utiliser `data class`, `sealed class`, `when` exhaustif, `scope functions` (`let`, `apply`, `run`) à bon escient, pas de style "Java traduit en Kotlin".
2. **Coroutines partout où c'est asynchrone** — `suspend fun` pour toute opération I/O (Room, Retrofit, accessibilité). Jamais de callback imbriqué à la main si une coroutine peut l'exprimer proprement.
3. **Immutabilité par défaut** — `val` partout où possible, `data class` avec `copy()` plutôt que mutation d'objets partagés.
4. **Null-safety stricte** — pas de `!!` sauf cas strictement justifié et commenté ; préférer `?.`, `?:`, `requireNotNull()` avec message explicite.
5. **Séparation stricte des couches** (cf. section 5) — `domain` ne connaît jamais Android ; `presentation` ne parle jamais directement à Room ou au service d'accessibilité, uniquement via `domain.usecase`.
6. **Un fichier = une responsabilité claire** — pas de fichier Kotlin de plus de ~300 lignes sauf justification (ex : un gros `sealed class` avec beaucoup de variantes peut légitimement dépasser).

### 12.2 Règle de commentaires (stricte, identique à l'esprit de l'ancien cahier des charges)

> **Les commentaires expliquent le "pourquoi" ou un "quoi" non évident — jamais une narration du code ligne par ligne.**

**À faire :**
```kotlin
// Fallback sélecteur : si l'id échoue (fréquent après une mise à jour de l'app cible),
// on retente par description puis par texte affiché.
val noeud = selectorEngine.resoudre(selecteur, racine)
```

**À ne pas faire :**
```kotlin
// On résout le sélecteur
val noeud = selectorEngine.resoudre(selecteur, racine)
```

Règles précises :
- Un commentaire = une phrase courte, en français.
- Commenter uniquement : décisions non évidentes, contournements de limitations Android (ex : comportement différent par version d'API), formats de données complexes, points d'extension prévus.
- KDoc (`/** ... */`) obligatoire sur toute fonction publique d'un `usecase` ou d'un `repository` — 1 à 3 lignes maximum, décrivant le rôle, pas une paraphrase du code.

```kotlin
/**
 * Évalue et exécute une macro si trigger + conditions sont validés.
 * @return résultat d'exécution (statut, durée, erreur éventuelle).
 */
suspend fun executer(macro: Macro, contexte: ContexteSysteme): ResultatExecution { ... }
```

### 12.3 Bonnes pratiques spécifiques au projet

- **Timeouts systématiques** sur toute résolution de sélecteur ou action UI (`withTimeoutOrNull`), jamais d'attente bloquante indéfinie.
- **Aucune macro générée par IA n'est exécutée automatiquement sans validation utilisateur explicite au moins une fois.**
- **Tests unitaires obligatoires sur `domain`** — chaque `UseCase` a son test (JUnit5 + MockK), exécutable en JVM pur sans émulateur/appareil.
- **Logs structurés** via une interface `Logger` injectée (pas de `Log.d` dispersés) — permet de rediriger vers le Journal d'exécution ET Logcat simultanément.
- **Gestion d'erreurs explicite** — `Result<T>` (Kotlin standard) ou une sealed class `Either<Erreur, T>` pour les opérations pouvant échouer, jamais d'exception non documentée remontant jusqu'à l'UI sans traitement.

### 12.4 Conventions de nommage

- Classes/objets : `PascalCase` (ex : `MacroRepository`).
- Fonctions/variables : `camelCase`, en **français** pour tout ce qui touche au domaine métier (`executerMacro`, `evaluerConditions`), en anglais pour les termes techniques standard Android/Kotlin (`onCreate`, `viewModelScope`).
- Fichiers Compose : un composable principal par fichier, nommé comme le composable (`CtrlMacroCard.kt` contient `fun CtrlMacroCard(...)`).

---

## 13. Contraintes Techniques Android

| Contrainte | Détail | Mitigation |
|---|---|---|
| **Service d'accessibilité** | Nécessite activation manuelle utilisateur ; Google surveille ces permissions de près pour publication Play Store | Distribution hors Play Store (APK direct) en phase perso, réévaluer si publication future |
| **Gestion batterie / Doze mode** | Android restreint fortement les apps en arrière-plan, surtout Xiaomi/Huawei/Samsung | Foreground Service avec notification persistante, demande d'exclusion d'optimisation batterie (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) |
| **Permissions runtime** | SMS, appels, localisation nécessitent des permissions demandées à l'usage | Demander uniquement les permissions liées aux macros effectivement créées (pas tout au premier lancement) |
| **Restriction d'exécution en arrière-plan (Android 12+)** | `AlarmManager` exact nécessite la permission `SCHEDULE_EXACT_ALARM` | Utiliser `WorkManager` par défaut, `AlarmManager` seulement si précision à la seconde requise |
| **Sécurité des scripts personnalisés** | Une macro mal configurée (boucle) peut bloquer l'exécution | Timeout global par macro (garde-fou, section 12.3), bouton d'arrêt d'urgence accessible depuis la notification persistante |
| **Fiabilité des sélecteurs UI** | Les apps tierces changent leur UI entre versions | Stratégie de fallback multi-niveaux (section 7.2), jamais un sélecteur unique fragile |

---

## 14. Permissions & Déclarations Manifest

Liste des permissions à déclarer, avec justification (nécessaire pour toute review Play Store future et pour que l'agent sache lesquelles demander) :

| Permission | Usage |
|---|---|
| `BIND_ACCESSIBILITY_SERVICE` | Service d'accessibilité — cœur du moteur |
| `RECEIVE_SMS`, `SEND_SMS`, `READ_SMS` | Triggers/actions SMS |
| `READ_PHONE_STATE`, `CALL_PHONE`, `READ_CALL_LOG` | Triggers/actions téléphone |
| `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` | Geofencing |
| `POST_NOTIFICATIONS` | Actions de notification (Android 13+) |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Service de contrôle permanent |
| `RECEIVE_BOOT_COMPLETED` | Réactivation des triggers après redémarrage |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Lecture des notifications (trigger + condition) |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Appels API IA, sync cloud, webhooks |
| `RECORD_AUDIO` | Actions liées au micro |
| `SCHEDULE_EXACT_ALARM` | Triggers "heure précise" |

---

## 15. Roadmap de Développement

| Phase | Contenu | Durée estimée |
|---|---|---|
| **Phase 0 — Setup projet** | Init projet Android Studio, Hilt, Room, navigation Compose de base, thème `CtrlTheme` appliquant les tokens `design.md` | 1 semaine |
| **Phase 1 — Service d'accessibilité minimal** | `CtrlAccessibilityService` fonctionnel, `SelectorEngine` basique (id + texte), `GestureExecutor` (clic simple) | 1-2 semaines |
| **Phase 2 — Moteur de macros (domain)** | `sealed class` Trigger/Condition/ActionMacro, `ExecuterMacroUseCase`, logique de flux (section 8.2), tests unitaires | 2 semaines |
| **Phase 3 — Parité MacroDroid P1** | Implémenter tous les triggers/conditions/actions priorité P1 du catalogue (`reference-macrodroid-catalogue-complet.md`) | 3-4 semaines |
| **Phase 4 — UI no-code (Compose)** | Dashboard, créateur de macro, détail macro, journal — selon `design.md` | 3 semaines |
| **Phase 5 — Fonctionnalités avancées** | OCR (ML Kit), chaînage de macros, priorité P2 du catalogue | 2-3 semaines |
| **Phase 6 — IA** | Génération de macro par langage naturel, intégration API | 1-2 semaines |
| **Phase 7 — Sync/export & polish** | Export/import JSON, sync cloud optionnelle, tests de fiabilité longue durée, peaufinage UI | 2 semaines |

---

## 16. Risques & Mitigation

| Risque | Impact | Mitigation |
|---|---|---|
| Complexité de réimplémentation du service d'accessibilité (vs AutoJs6 tout fait) | Élevé | Découper en sous-étapes testables (Phase 1), ne pas viser l'exhaustivité des sélecteurs dès le départ |
| Restrictions Play Store sur le service d'accessibilité si publication future | Moyen | Rester en distribution APK directe tant que l'usage reste personnel |
| Scope creep (trop de features avant un MVP stable) | Élevé | Respecter strictement les phases, Phase 3 (parité P1) avant toute fonctionnalité avancée |
| Restrictions batterie agressives des constructeurs (Xiaomi, etc.) empêchant l'exécution fiable | Moyen | Documenter clairement pour l'utilisateur la procédure de whitelist par constructeur |
| Dérive du design system pendant l'implémentation | Moyen | Centraliser tous les tokens dans `presentation/theme`, aucune valeur codée en dur ailleurs |

---

## 17. Critères de Succès

- [ ] Le service d'accessibilité Ctrl fonctionne de façon stable sur le téléphone de test pendant au moins 7 jours sans redémarrage manuel.
- [ ] Toutes les automatisations couvertes par MacroDroid en usage courant sont reproduites (priorité P1 du catalogue).
- [ ] Au moins une automatisation basée sur l'OCR fonctionne de bout en bout.
- [ ] La génération de macro par IA fonctionne pour au moins 3 cas d'usage réels, avec validation utilisateur systématique.
- [ ] La couverture de tests unitaires du module `domain` est satisfaisante (logique critique testée).
- [ ] L'interface respecte intégralement `design.md` (aucune couleur/taille codée en dur hors du module `theme`).

---

## 18. Glossaire

- **AccessibilityService** : API Android permettant à une app de lire/interagir avec l'UI d'autres apps, avec autorisation explicite de l'utilisateur.
- **AccessibilityNodeInfo** : représentation d'un élément d'interface (bouton, texte, etc.) exposée par le service d'accessibilité.
- **Sélecteur** : mécanisme de Ctrl pour cibler un élément d'UI (id, description, texte, position).
- **Coroutine** : mécanisme natif Kotlin pour du code asynchrone séquentiel, équivalent propre d'`async`/`await`, sans les limitations rencontrées avec Rhino.
- **Clean Architecture** : découpage en couches `presentation`/`domain`/`data` où la logique métier ne dépend jamais du framework (ici Android).
- **Design token** : valeur nommée et centralisée (couleur, taille, espacement) garantissant la cohérence visuelle.

---

*Ce document remplace intégralement l'ancienne version basée sur AutoJs6. Il est la référence unique pour l'implémentation native de Ctrl, y compris par un agent IA. À faire évoluer au fil de l'avancement.*