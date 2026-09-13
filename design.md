# Design.md — Design System & UI/UX Complet de Ctrl (v2 — Daylit)

> Ce document est le **compagnon design** du cahier des charges natif (`cahier-des-charges-ctrl-native.md`). Il remplace intégralement la v1 (basée sur le thème "Textla" néobrutaliste, associée à l'ancienne approche AutoJs6). Cette v2 reprend le thème **Daylit** (burgundy/parchment, "ledger moderne") fourni par le porteur du projet, repersonnalisé pour Ctrl et pensé **mobile-first pour Jetpack Compose** (pas pour le web).

| | |
|---|---|
| **Projet** | Ctrl |
| **Document parent** | cahier-des-charges-ctrl-native.md |
| **Base du thème** | Daylit — burgundy ink sur parchemin crème |
| **Plateforme cible** | Android natif, Jetpack Compose, Material 3 re-thémé |
| **Version** | 2.0 |
| **Date** | Septembre 2026 |

---

## Sommaire

0. Contrainte Non-Négociable — Mobile-First
1. Concept Directeur
2. Design Tokens — Couleurs
3. Design Tokens — Typographie
4. Design Tokens — Espacement & Formes
5. Implémentation Compose des Tokens
6. Composants
7. Imagerie & Iconographie
8. Do's & Don'ts
9. Écrans Principaux (détaillés)
10. Parcours Utilisateurs
11. États & Micro-interactions
12. Accessibilité

---

## 0. Contrainte Non-Négociable — Mobile-First

**Ctrl est une application mobile Android. Point final.** Aucun écran, composant ou gabarit de ce document ne doit être pensé, dimensionné, ou testé pour un écran desktop/web.

Règles strictes :
- **Toutes les tailles d'écran ci-dessous sont pensées pour un viewport de 360-430dp de large** (la plage réelle des téléphones Android, du compact au grand format), jamais pour 1200px+.
- **Une seule colonne par défaut.** Aucune grille à 2-3 colonnes de cartes côte à côte (contrairement à un dashboard web) — les Cartes Macro s'empilent verticalement, scrollables.
- **Navigation par bottom bar ou drawer**, jamais de barre de navigation horizontale façon site web avec liens espacés.
- **Zones tactiles avant tout** : chaque élément interactif dimensionné pour le doigt (48dp minimum, cf. section 12), pas pour un curseur de souris.
- **Le "Bandeau de Statistiques Globales" et autres blocs larges du design d'origine (desktop) sont réinterprétés en carte compacte ou en ligne de chips scrollable horizontalement** — jamais en bandeau 4 colonnes.
- Si un composant du design system source (Daylit, pensé pour un site web) ne trouve pas d'équivalent mobile naturel, il est **adapté ou omis**, jamais copié tel quel.

---

## 1. Concept Directeur

**Style :** Wine Ledger — encre burgundy profonde sur parchemin chaleureux, typographie éditoriale confiante, composants plats à faible élévation.

**Concept :** *"Le carnet de contrôle"* — Ctrl est pensé comme un registre élégant et sobre plutôt qu'un tableau de bord technique froid. Le parchemin ivoire remplace le blanc froid habituel des apps Android, l'encre burgundy fait tout le travail structurel (texte, bordures, action principale), et le jaune citron n'apparaît qu'en accent rare — badges, highlights, éléments décoratifs. L'app doit donner l'impression d'un outil personnel soigné, pas d'un dashboard d'entreprise.

**Origine :** ce design system reprend intégralement la palette et les tokens typographiques du design system "Daylit" fourni, en l'adaptant de web/desktop vers mobile natif Compose, et en remappant chaque composant vers son usage dans Ctrl (ex : "Testimonial Card" devient "Carte Macro").

**Ce qui NE change PAS :** les valeurs de couleur, la police principale (ou son substitut), la philosophie "un seul accent chromatique fort + un accent rare".

**Ce qui change :** dimensions (réduites pour mobile — un titre de 76px desktop n'a pas sa place sur un écran de 390dp de large), layout (colonne unique au lieu de grille large), rôle des composants.

---

## 2. Design Tokens — Couleurs

| Nom Ctrl | Valeur | Token Compose | Rôle |
|---|---|---|---|
| Encre Bordeaux | `#4d1520` | `WineInk` | Texte principal, fond des boutons pleins, bordures de carte, couleur de marque dominante — l'ancre chromatique unique |
| Bordeaux Minuit | `#360912` | `MidnightWine` | Surface la plus sombre — fond de la bottom bar ou d'un panneau inversé |
| Citron Doux | `#faffa7` | `LemonWhisper` | Accent décoratif rare — badges "Actif", highlights, jamais en fond large ni en texte de corps |
| Rouille Adoucie | `#662f3d` | `BlushRust` | Remplissage de marque secondaire, ombrage d'illustrations |
| Or Vif | `#e6b800` | `CitrinePop` | Accent saturé rare, micro-détails uniquement |
| Cendre Mauve | `#825b63` | `MauveAsh` | Texte de corps atténué, bordures discrètes |
| Parchemin | `#fbf9f6` | `Parchment` | Fond de page principal — l'ivoire chaleureux qui remplace le blanc froid |
| Blanc Pur | `#ffffff` | `PureWhite` | Surfaces de carte, champs de saisie — le neutre le plus clair au-dessus du parchemin |
| Lin Beige | `#f2eee7` | `LinenBeige` | Bordures fines, séparateurs, contour de bouton fantôme |
| Grès | `#d3cac3` | `Sandstone` | Bordures atténuées secondaires |
| Bois Flotté | `#aaa49f` | `Driftwood` | Neutre le plus bas contraste — bordures décoratives, liens inactifs |
| Fumée Ardoise | `#717182` | `SlateSmoke` | Texte secondaire, icônes, texte d'aide — seul neutre à tendance froide |
| Crème Beurre | `#feffe1` | `ButterCream` | Remplissage doux pour zones de highlight |
| Alerte* | `#b3314a` | `AlertRed` | *Exception ajoutée, absente du thème d'origine* : badge d'erreur d'exécution, action destructive |

*Alerte est ajoutée hors palette d'origine, dérivée du dégradé "Wine deepening" du thème source (`rgb(179,49,74)`) pour rester dans la même famille chromatique tout en étant visuellement distincte de l'encre normale — strictement réservée aux erreurs et suppressions.

### Règles d'usage
- `WineInk` porte l'essentiel du poids visuel : texte, bordures, remplissage d'action principale.
- `LemonWhisper` est un accent rare — jamais en fond de section entière ni en texte de corps (contraste insuffisant).
- `AlertRed` n'apparaît jamais en dehors d'un contexte d'erreur ou de suppression.

---

## 3. Design Tokens — Typographie

### Police principale — Tt Commons Pro Variable
*(Substitut Android : si la police custom n'est pas embarquée, utiliser **Inter** ou **General Sans** comme fallback proche — géométrique, sans-serif, poids variable)*

- **Rôle dans Ctrl :** toute la typographie de l'app — titres, corps, boutons, cartes.
- **Poids disponibles :** 400 (Regular), 500 (Medium), 600 (SemiBold) — jamais de 700 ("le système plafonne à 600 pour l'emphase").
- **Échelle réduite pour mobile** (le desktop montait à 85px, sans objet sur un écran de 390dp) :

| Rôle | Taille (desktop d'origine) | **Taille Ctrl mobile** | Poids | Letter-spacing |
|---|---|---|---|---|
| caption | 14px | **12sp** | 400 | -0.01em |
| body-sm | 14px | **14sp** | 400 | -0.012em |
| body | 16px | **16sp** | 400 | -0.012em |
| subheading | 22px | **18sp** | 500 | -0.015em |
| heading-sm | 32px | **20sp** | 500 | -0.02em |
| heading | 44px | **24sp** | 500 | -0.021em |
| heading-lg | 56px | **28sp** | 400 | -0.021em |
| display | 76px | **32sp** | 400 | -0.03em |

*(Utiliser `sp` — unité Android qui respecte les réglages d'accessibilité de taille de police système — jamais `dp` pour du texte.)*

### Police mono — Geist Mono
*(Substitut : JetBrains Mono)*

- **Rôle dans Ctrl :** labels de badge en majuscules, type de trigger affiché en tag (ex: "SMS REÇU"), identifiants techniques (id de macro en debug).
- **Poids :** 400, 500. **Tailles Ctrl :** 11sp (badge), 13sp (tag).

---

## 4. Design Tokens — Espacement & Formes

- **Unité de base :** 4dp.
- **Densité :** confortable, mais resserrée par rapport au desktop d'origine — un padding de carte de 24px desktop devient 16dp mobile.

### Échelle d'espacement (mobile)

| Nom | Valeur | Token |
|---|---|---|
| 4 | 4dp | `Spacing4` |
| 8 | 8dp | `Spacing8` |
| 12 | 12dp | `Spacing12` |
| 16 | 16dp | `Spacing16` |
| 20 | 20dp | `Spacing20` |
| 24 | 24dp | `Spacing24` |
| 32 | 32dp | `Spacing32` |
| 40 | 40dp | `Spacing40` |

### Rayons de bordure (Shapes Compose)

| Élément | Valeur |
|---|---|
| Cartes | 6dp |
| Badges/pills | 999dp (pilule complète) |
| Champs de saisie | 6dp |
| Boutons standards | 6dp |
| Gros boutons (CTA hero, rare sur mobile) | 16dp |
| Panneaux d'illustration | 12dp |

### Élévation
Le thème d'origine réserve une ombre riche (`rgba(77,21,32,0.16) 19px 32px 73px`) aux éléments "hero-scale" — sur mobile, cette échelle est disproportionnée. Adaptation :
- **Ombre standard Ctrl** (Cartes Macro, cartes de dashboard) : `Material3 Elevation` niveau 1-2 (2-4dp), teintée légèrement `WineInk` plutôt que le gris par défaut Material.
- **Ombre "hero"** (rare — ex: carte de résultat IA fraîchement générée) : élévation niveau 4dp avec la teinte burgundy plus marquée, jamais l'ombre desktop brute.
- **Pas d'ombre du tout** sur boutons, champs de saisie, nav — cohérent avec la règle du thème d'origine ("shadows are reserved for hero-scale elevation").

---

## 5. Implémentation Compose des Tokens

```kotlin
// presentation/theme/CtrlColor.kt
object CtrlColor {
    val WineInk = Color(0xFF4D1520)
    val MidnightWine = Color(0xFF360912)
    val LemonWhisper = Color(0xFFFAFFA7)
    val BlushRust = Color(0xFF662F3D)
    val CitrinePop = Color(0xFFE6B800)
    val MauveAsh = Color(0xFF825B63)
    val Parchment = Color(0xFFFBF9F6)
    val PureWhite = Color(0xFFFFFFFF)
    val LinenBeige = Color(0xFFF2EEE7)
    val Sandstone = Color(0xFFD3CAC3)
    val Driftwood = Color(0xFFAAA49F)
    val SlateSmoke = Color(0xFF717182)
    val ButterCream = Color(0xFFFEFFE1)
    val AlertRed = Color(0xFFB3314A)
}

// presentation/theme/CtrlTheme.kt
private val CtrlColorScheme = lightColorScheme(
    primary = CtrlColor.WineInk,
    onPrimary = CtrlColor.Parchment,
    background = CtrlColor.Parchment,
    onBackground = CtrlColor.WineInk,
    surface = CtrlColor.PureWhite,
    onSurface = CtrlColor.WineInk,
    secondary = CtrlColor.LemonWhisper,
    onSecondary = CtrlColor.WineInk,
    error = CtrlColor.AlertRed,
    outline = CtrlColor.LinenBeige,
)

val CtrlShapes = Shapes(
    small = RoundedCornerShape(6.dp),      // champs, boutons
    medium = RoundedCornerShape(6.dp),     // cartes
    large = RoundedCornerShape(16.dp),     // gros CTA (rare)
    extraLarge = RoundedCornerShape(12.dp), // panneaux d'illustration
)

@Composable
fun CtrlTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CtrlColorScheme,
        typography = CtrlTypography, // cf. échelle section 3
        shapes = CtrlShapes,
        content = content,
    )
}
```

**Règle stricte pour l'agent IA** : aucun écran ne doit référencer une valeur hexadécimale ou un `sp`/`dp` brut en dehors de `presentation/theme/`. Toujours `MaterialTheme.colorScheme.primary`, jamais `Color(0xFF4D1520)` codé en dur dans un composable d'écran.

---

## 6. Composants

### 6.1 `CtrlPrimaryButton` (bouton pilule plein)
**Rôle :** action principale d'un écran — "Créer une macro", "Enregistrer", "Générer".
Fond `WineInk`, texte `Parchment` 16sp poids 500, rayon 6dp (pilule pour CTA hero uniquement — rayon 999dp), padding vertical 12dp / horizontal 24dp, hauteur minimale 48dp (zone tactile). Pas d'ombre.

### 6.2 `CtrlGhostButton` (bouton texte)
**Rôle :** action secondaire — "Annuler", "Voir plus", lien inline.
Pas de fond ni bordure, texte `WineInk` ou `SlateSmoke` 16sp poids 500, chevron optionnel en fin de libellé.

### 6.3 `CtrlOutlinedButton` (bouton contour)
**Rôle :** action de vérification non destructive — "Tester la macro".
Bordure 1dp `WineInk`, fond transparent, texte `WineInk`, rayon 6dp, hauteur minimale 48dp.

### 6.4 `CtrlMacroCard`
**Rôle :** élément central de la liste de macros — remplace la "Testimonial Card" du thème d'origine.
Fond `PureWhite`, bordure 1dp `LinenBeige`, rayon 6dp, padding 16dp, élévation niveau 1 teintée burgundy.
**Contenu (empilé verticalement, mobile-first) :**
- Ligne 1 : tag de trigger (mono, badge `LemonWhisper`/`WineInk`) + toggle actif/inactif aligné à droite
- Ligne 2 : nom de la macro (heading-sm, `WineInk`)
- Ligne 3 : description courte (body-sm, `SlateSmoke`), 2 lignes max avec ellipsis
- Ligne 4 (séparée par une fine ligne `LinenBeige`) : stats d'exécution (caption, `SlateSmoke`) + boutons "Tester" (Outlined) / "Détails" (Ghost) côte à côte

### 6.5 `CtrlStatChip`
**Rôle :** remplace le "Bandeau de Statistiques Globales" 4-colonnes du thème desktop — sur mobile, une **rangée de chips scrollable horizontalement** en haut du Dashboard.
Fond `ButterCream`, rayon 999dp, padding 8dp/16dp, contenu : nombre en heading-sm `WineInk` + label en caption `SlateSmoke` à sa droite, dans une seule pilule compacte.

### 6.6 `CtrlStatusBadge`
**Rôle :** indicateur d'état de macro.
Rayon 999dp, padding 4dp/10dp, texte mono 11sp.
**Variantes :**
- **Active** : fond `LemonWhisper`, texte `WineInk`.
- **Inactive** : fond `LinenBeige`, texte `SlateSmoke`.
- **Erreur** : fond `AlertRed`, texte `PureWhite`.

### 6.7 `CtrlAiRibbon`
**Rôle :** signale une macro générée par IA, non encore validée.
Petit ruban en coin de `CtrlMacroCard`, fond `CitrinePop`, texte `WineInk` mono 11sp "IA", disparaît après validation.

### 6.8 `CtrlLogBubble`
**Rôle :** entrée du Journal d'exécution, façon fil d'événements.
Fond `PureWhite` ou `ButterCream` selon statut, rayon 6dp avec un coin asymétrique côté émetteur, padding 12dp. Contenu : icône de statut + description courte + timestamp en caption `SlateSmoke`.

### 6.9 `CtrlBottomNavBar`
**Rôle :** navigation principale — remplace la barre horizontale desktop, inadaptée au mobile.
Fond `Parchment` ou `MidnightWine` (variante sombre à évaluer en phase polish), 4 destinations : Dashboard / Macros / Journal / Paramètres, icônes ligne fine + label caption, item actif en `WineInk` plein, items inactifs en `Driftwood`.

### 6.10 `CtrlEmptyState`
**Rôle :** état vide (aucune macro), premier lancement.
Illustration plate simple (cf. section 7) centrée, titre heading-sm, sous-texte body-sm `SlateSmoke`, `CtrlPrimaryButton` "Créer ma première macro".

### 6.11 `CtrlPermissionBanner`
**Rôle :** bandeau persistant quand le service d'accessibilité n'est pas actif.
Pleine largeur, fond `WineInk`, texte `Parchment`, icône d'alerte, `CtrlGhostButton` "Activer" (texte `LemonWhisper` pour ressortir sur fond sombre) menant aux réglages système.

---

## 7. Imagerie & Iconographie

Le thème d'origine utilise des illustrations plates géométriques (imprimantes à facture, calculatrices) sans photographie, en traits fins burgundy sur fonds citron/crème. Pour Ctrl, même esprit graphique, sujet adapté :

- **Nœuds et connecteurs** représentant le flux trigger → condition → action.
- **Symboles de contrôle simplifiés** : interrupteurs, curseurs, icônes de terminal stylisées.
- Style : formes géométriques plates, pas de dégradé sur les objets eux-mêmes (les dégradés du thème d'origine restent réservés à des halos décoratifs de fond, très rares sur mobile — à n'utiliser qu'en écran d'accueil/onboarding, jamais dans les cartes).
- Icônes fonctionnelles (nav, boutons) : set d'icônes ligne fine (1.5-2dp de trait), style Material Symbols Outlined re-teinté aux couleurs Ctrl plutôt que des icônes remplies.

---

## 8. Do's & Don'ts

### Do
- Utiliser `WineInk` pour toute la structure : texte, bordures, boutons pleins.
- Garder le fond de page en `Parchment` partout, `PureWhite` uniquement pour les surfaces de carte.
- Réserver `LemonWhisper` aux badges/accents rares — jamais en fond de section ni en texte de corps.
- Empiler les Cartes Macro verticalement, en une seule colonne scrollable.
- Utiliser la police à 400-500 pour les titres, jamais 700 (identité du thème : "whisper-weight" plutôt que "bold-shouty").
- Garder tous les boutons/champs à hauteur minimale 48dp pour le tactile.

### Don't
- Ne pas reproduire les tailles desktop telles quelles (76-85px) — toujours passer par l'échelle mobile réduite de la section 3.
- Ne pas mettre plusieurs Cartes Macro côte à côte en grille — une colonne, toujours.
- Ne pas utiliser de gris froids (#e5e7eb, #6b7280) — le système reste chaud de bout en bout.
- Ne pas mettre d'ombre sur les boutons, champs, ou items de nav — l'élévation est réservée aux cartes.
- Ne pas utiliser `AlertRed` en dehors d'un contexte d'erreur/suppression.
- Ne pas centrer des paragraphes de plus de deux lignes — aligner à gauche pour la lisibilité.

---

## 9. Écrans Principaux (détaillés)

### 9.1 Dashboard
**Structure verticale (mobile, une colonne) :**
1. Barre supérieure simple : logo/wordmark Ctrl à gauche, icône profil/paramètres à droite (pas de nav horizontale desktop).
2. Rangée de `CtrlStatChip` scrollable horizontalement (Macros actives / Exécutions aujourd'hui / Taux de succès).
3. Titre de section "Mes macros" (heading-sm) + champ de recherche compact.
4. Liste verticale de `CtrlMacroCard`, scroll infini si besoin.
5. `CtrlBottomNavBar` fixe en bas.
6. Bouton flottant (FAB Material, re-thémé `WineInk`/`LemonWhisper`) "+" pour créer une macro, au-dessus de la bottom bar.

### 9.2 Créateur de Macro (no-code)
Assistant en 3 étapes, une **par écran complet** (pas de scroll horizontal d'étapes sur mobile) avec indicateur de progression en haut (3 points) :
1. **Trigger** : liste verticale de catégories (icône + label), tap pour sélectionner, puis sous-liste des triggers de cette catégorie.
2. **Conditions** (optionnel, skippable) : liste des conditions ajoutées, bouton "+ Ajouter une condition" en `CtrlOutlinedButton`.
3. **Actions** : liste réordonnable (drag mobile natif), bouton "+ Ajouter une action".
Bouton "Enregistrer" en `CtrlPrimaryButton`, fixe en bas de l'écran (au-dessus du clavier si actif).

### 9.3 Détail d'une Macro
En-tête : nom (heading), `CtrlStatusBadge`, toggle actif/inactif. Sections empilées : résumé trigger/conditions/actions (lecture seule, lien "Modifier"), historique d'exécution (liste de `CtrlLogBubble` filtrée). Boutons en bas : "Tester maintenant" (Outlined), "Dupliquer" (Ghost), "Supprimer" (texte `AlertRed`, confirmation modale).

### 9.4 Générateur IA
Champ de texte multiligne en haut (placeholder "Décris l'automatisation que tu veux créer..."), `CtrlPrimaryButton` "Générer" en dessous. Résultat affiché en `CtrlMacroCard` avec `CtrlAiRibbon`, boutons "Régénérer" (Ghost) / "Valider et enregistrer" (Primary) en bas, fixes.

### 9.5 Journal d'Exécution
Liste verticale de `CtrlLogBubble`, groupée par jour avec séparateur de date (caption, fond `ButterCream`, pleine largeur). Filtre par macro accessible via une icône en haut à droite (ouvre une modale de sélection).

### 9.6 Variables Globales
Liste verticale simple (pas de tableau desktop) : chaque variable en mini-carte (nom + valeur + type), tap pour éditer, bouton "+" flottant pour ajouter.

### 9.7 Paramètres
Liste groupée par section (Permissions, Batterie, Sync, Import/Export, À propos), chaque item en ligne simple avec icône + label + valeur/chevron, fond `Parchment`, séparateurs fins `LinenBeige`.

---

## 10. Parcours Utilisateurs

### Parcours 1 — Créer une macro simple
Dashboard → FAB "+" → Étape Trigger → Étape Conditions (skip possible) → Étape Actions → Nommer → Enregistrer → retour Dashboard, nouvelle `CtrlMacroCard` visible.

### Parcours 2 — Générer une macro via IA
Dashboard → icône IA → saisie langage naturel → "Générer" → `CtrlMacroCard` avec `CtrlAiRibbon` → validation ou ajustement → "Valider et enregistrer".

### Parcours 3 — Debug d'une macro qui échoue
Dashboard → `CtrlMacroCard` avec badge "Erreur" → tap → Détail → section historique → `CtrlLogBubble` d'erreur → "Tester maintenant" (mode pas-à-pas) → correction via Créateur no-code → nouveau test.

---

## 11. États & Micro-interactions

- **État vide** : `CtrlEmptyState`, illustration nœuds connectés.
- **Chargement** : squelette de `CtrlMacroCard` (silhouette grise animée aux mêmes dimensions), jamais de spinner générique isolé.
- **Feedback d'exécution temps réel** : léger halo `LemonWhisper` autour de la `CtrlMacroCard` concernée pendant 1-2 secondes lors d'une exécution.
- **Permission manquante** : `CtrlPermissionBanner` persistant, disparaît uniquement quand la permission est effectivement accordée.
- **Confirmation destructive** : modale native Compose (`AlertDialog` re-thémé), fond `PureWhite`, titre heading-sm, bouton de confirmation `AlertRed`, annulation en `CtrlGhostButton`.
- **Toggle actif/inactif** : bascule immédiate, pas de confirmation (action réversible en un tap).

---

## 12. Accessibilité

- **Contraste AA minimum** vérifié sur toutes les combinaisons utilisées (`WineInk` sur `Parchment`, `PureWhite` sur `WineInk`, `WineInk` sur `LemonWhisper`).
- **Zones tactiles 48dp minimum** pour tout élément interactif (boutons, toggles, items de nav) — norme Android standard, non négociable en mobile-first.
- **`contentDescription` obligatoire** sur toute icône fonctionnelle sans label texte visible (compatibilité TalkBack).
- **`LemonWhisper` et `AlertRed` jamais seuls vecteurs d'information** — toujours doublés d'un label texte (ex : badge "Active" = fond + mot écrit).
- **Taille de texte respectant les réglages système** : usage systématique de `sp` (jamais `dp`) pour tout texte, afin de respecter l'échelle d'accessibilité choisie par l'utilisateur Android.
- **Taille de texte minimale fonctionnelle** : 12sp (caption), jamais en dessous, y compris dans les badges.

---

*Ce fichier fait autorité sur toute question de design pour la version native de Ctrl. Il remplace la v1 (thème Textla/AutoJs6). Toute évolution du design system doit être répercutée ici en priorité.*