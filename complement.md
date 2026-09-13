# Reference.md — Catalogue Complet MacroDroid (extrait des captures)

> Ce document est un **complément** au cahier des charges principal (`cahier-des-charges-ctrl.md`), section 8 (Spécifications Fonctionnelles). Il liste de façon exhaustive tout le catalogue Trigger/Condition/Action observé dans l'app MacroDroid, pour servir de **checklist de référence** lors de l'implémentation de la parité fonctionnelle de Ctrl. Rien n'est retiré du cahier des charges principal — ce fichier l'enrichit avec le détail complet du catalogue réel de MacroDroid.

| | |
|---|---|
| **Source** | Captures d'écran de l'app MacroDroid (menus Déclencheur / Condition / Action) |
| **Usage** | Checklist de couverture fonctionnelle pour Ctrl — section 8 du cahier des charges |
| **Légende** | 🔒 = nécessite Root/Hack ADB/Shizuku · 💎 = fonctionnalité Pro uniquement · 🧪 = fonctionnalité Béta |

---

## Légende de priorisation pour Ctrl

- **P1** — faisable nativement avec AutoJs6 sans root, à couvrir en priorité (Phase 2 du cahier des charges).
- **P2** — faisable mais secondaire ou plus complexe (OCR, capteurs avancés), Phase 4.
- **P3** — nécessite root/Shizuku/ADB, à considérer optionnel/exclu en phase 1 (Ctrl vise le non-root par défaut, cf. section 5 du cahier des charges).

*(La priorisation P1/P2/P3 ci-dessous est une proposition à valider — elle n'engage pas le cahier des charges principal, elle sert de point de départ pour trier ce catalogue.)*

---

## 1. Déclencheurs (Triggers)

### 1.1 Appel / SMS — P1
- Appel Entrant
- Appel Manqué
- Appel Sortant
- Appel Terminé
- Appel en Cours
- Numéro de Téléphone (filtre associé à un déclencheur d'appel)
- SMS Envoyé
- SMS Reçu

### 1.2 Applications — P1/P2
- Activité de l'application lancée/fermée
- Application Installée/Désinstallée/Mise à jour
- Application Lancée/Fermée
- Application activée/désactivée
- Clic sur l'interface utilisateur *(P2 — nécessite lecture UI avancée, cœur de la valeur ajoutée de Ctrl)*
- Contenu de l'écran *(P2 — OCR/lecture structurée)*
- Contenu de la Capture d'écran *(P2)*
- Fichier partagé vers MacroDroid
- Icône de raccourci pour l'appui
- Message pop-up
- Modification de la piste média
- Plugin Tasker/Locale *(hors scope — spécifique écosystème Tasker)*
- Shizuku s'est arrêtée 🔒 *(P3)*
- Spotify *(intégration tierce spécifique — à évaluer)*
- Texte partagé avec MacroDroid
- État du service d'accessibilité

### 1.3 Batterie / Alimentation — P1
- Alimentation Connectée/Déconnectée
- Bouton Mise sous Tension par Bascule
- Niveau de la Batterie
- Température de la batterie
- État de l'économiseur de batterie

### 1.4 Capteurs — P1/P2
- Appareil Retourné
- Appareil secoué
- Capteur de Luminosité
- Capteur de Proximité
- Orientation de l'écran
- Reconnaissance d'Activités *(P2 — nécessite API activité Google)*
- Sommeil *(P2 — dépend d'API santé/sommeil)*
- Valeur du capteur (générique)

### 1.5 Connectivité — P1
- Android Wear *(hors scope initial — pas de wearable)*
- Casque Audio Branché/Débranché
- Changement d'état du VPN
- Changement d'état du Wi-Fi
- Changement de Connectivité des Données
- Clic Complication Wear OS *(hors scope)*
- Connexion USB
- Courriel reçu *(P2 — nécessite accès IMAP/Gmail API)*
- Données utilisées
- Hook Web *(P2 — webhook entrant, nécessite un petit serveur)*
- Mode Itinérance démarré/arrêté
- Modification de l'adresse IP
- Modification du transport réseau
- Point d'Accès Activé/Désactivé
- Périphérique USB Connecté/Déconnecté
- Requête du serveur HTTP *(P2 — même besoin que Hook Web)*
- SSID Wi-Fi (déclenchement sur réseau spécifique)
- Webhook (avec réponse) 💎 *(P2/P3 selon complexité)*
- État du Service Mobile
- Événement Bluetooth (connexion à un appareil précis)

### 1.6 Date / Heure — P1
- Chronomètre
- Coucher du soleil / Lever du soleil
- Heure système
- Jour/Heure
- Journée de la Semaine/Mois
- À intervalles réguliers
- Événement du Calendrier *(P2 — nécessite accès Calendar API)*

### 1.7 Événements de l'appareil — P1/P2
- Appareil Posé/Retiré d'une Station d'Accueil
- Assistant Activé
- Autosync modifié
- Caméra en cours d'utilisation
- Changement de carte SIM
- Changement du Presse-Papier
- Démarrage de l'Appareil
- Fichier modifié
- GPS Activé/Désactivé
- Intention reçue (Android Intent)
- Lecture de musique
- Message Logcat 🔒 *(P3)*
- Mode Avion
- Mode Prioritaire/Ne pas déranger
- Mode Silencieux Activé/Désactivé
- Modification des paramètres du système
- Modification du thème sombre
- Notification *(P1 — déjà couvert en section 8.1 du cahier des charges principal)*
- Photo prise
- Profil de travail *(entreprise — hors scope initial)*
- Rotation automatique modifiée
- Échec à la Tentative de Connexion
- Écran Allumé/Éteint
- Écran Déverrouillé
- Écran de Veille Marche/Arrêt
- État NFC

### 1.8 Localisation — P1/P2
- Changement d'Antenne-Relais *(P2)*
- Emplacement
- Géorepérage (geofence)
- Météo *(P2 — nécessite API météo externe)*

### 1.9 Saisie utilisateur — P1
- Appui long du Bouton d'alimentation
- Appui long sur le bouton de volume 🔒 *(P3)*
- Appui long sur le bouton home
- Assistant Google
- Bouton Media Appuyé
- Bouton Widget
- Bouton de Volume Appuyé
- Bouton de lecteur V2 🧪
- Bouton flottant (créé par l'app elle-même)
- Clavier ouvert/fermé
- Raccourci Lancé
- Écran Balayé (geste spécifique)

### 1.10 Spécifique app (équivalent "Ctrl") — P1
*(Ces déclencheurs sont propres à l'auto-référence de l'app — leur équivalent Ctrl est listé entre parenthèses)*
- Barre de boutons de notification
- Changement d'une Variable *(→ trigger "changement de variable globale")*
- Déclencheur Vide *(utile en debug)*
- Entrée du Journal Système
- Macro activée / Macro terminée *(→ chaînage de macros, déjà prévu section 9.3 du cahier des charges)*
- App Initialisée / App activée
- Mode (changement) *(concept de "profils" à évaluer pour Ctrl)*
- Scène personnalisée montrée/dissimulée
- Texte Flottant Affiché/Masqué
- Tiroir Ouvert/Fermé
- Tuile de réglages rapides

---

## 2. Conditions

### 2.1 Connectivité — P1
- Adresse IP
- Connexion d'un appareil USB
- Données utilisées
- Est en Itinérance
- Mode de localisation
- État du Bluetooth
- État du GPS
- État du Mode Données Mobiles
- État du Point d'Accès Wi-Fi
- État du Service Mobile
- État du Wi-Fi

### 2.2 Date/Heure — P1
*(Déjà couvert dans le cahier des charges section 8.2 : plage horaire, jour de la semaine — à enrichir avec :)*
- Chronomètre (condition sur sa valeur)
- Lever/coucher du soleil (avant/après)
- Jour du Mois / Mois de l'année
- Événement calendrier *(P2)*

### 2.3 Écran — P1
- Clavier Affiché
- Luminosité (valeur actuelle)
- Thème sombre (actif ou non)
- Écran Allumé/Éteint
- État de l'affichage ambiant

### 2.4 État de l'appareil — P1/P3
- ADB Hack installé 🔒
- Appareil Verrouillé/Déverrouillé
- Appareil rooté 🔒
- Application activée/désactivée
- Application en Cours d'Exécution *(P1 — déjà prévu section 8.2 : "app au premier plan", à étendre à "app en cours d'exécution en arrière-plan")*
- Application installée
- Auto rotation (état)
- Caméra en cours d'utilisation
- Contenu du presse-papier
- Enregistrement au microphone (en cours)
- Lampe Allumée/Éteinte
- Lanceur au premier plan
- Mode Itinérance Activé
- Paramètre système (générique)
- Profil de travail
- Synchronisation Automatique (état)
- Temps Depuis le Démarrage
- Texte Parlé en cours (TTS actif)
- État NFC
- État de Shizuku 🔒
- État de la connexion USB
- État du Mode Avion
- État du VPN
- État du service d'accessibilité *(important pour Ctrl : condition de auto-vérification que le service tourne)*

### 2.5 Localisation — P2
- Antennes-Relais
- Géofence (localisation)

### 2.6 Média — P1
- Connexion Casque Audio
- Musique active

### 2.7 Notification — P1
- Mode de Priorité / Ne Pas Déranger
- Notification présente (condition sur existence)
- Volume des Notifications

### 2.8 Spécifique app (logique de composition) — P1, critique
- **AND / OR / XOR / NOT** — déjà prévu section 8.2 du cahier des charges ("condition composée"), XOR à ajouter comme variante
- Affichage du cercle de notification de l'app (spécifique constructeur)
- Affichage du voyant de notification Edge (spécifique Samsung)
- Bouton flottant (état affiché/masqué)
- Catégorie Activée/Désactivée (regroupement de macros)
- **Comparer des valeurs** — condition générique sur variables, à formaliser dans le schéma JSON (section 7.3 du cahier des charges)
- Déclencheur activé (condition sur l'état d'un trigger)
- Exécution en cours de la macro (empêcher double exécution — important pour la fiabilité)
- Macro(s) Activée(s)/Désactivée(s)
- Macro(s) Invoquée(s)/Pas Invoquée(s) Récemment (anti-spam d'exécution)
- Mode (changement de profil)
- Méthode d'appel de macro (manuel vs automatique)
- Scène personnalisée (état)
- Texte flottant (état)
- Variable (test générique sur une variable globale)
- État de la tuile rapide
- État du tiroir

### 2.9 Téléphone — P1
- Téléphone Sonne (état)
- État d'un Appel

### 2.10 Volume — P1
- Activer/Désactiver le Haut-Parleur (état)
- Niveau de volume
- Volume de la Sonnerie

---

## 3. Actions

### 3.1 IA — P1, différenciant pour Ctrl
- **AI LLM Query** — MacroDroid a déjà introduit une action d'appel direct à un LLM ; à formaliser dans Ctrl comme l'action native `appel_ia` en plus de la génération de macro par IA (section 9.2 du cahier des charges)

### 3.2 Applications — P1/P3
- Activer/désactiver une application 🔒
- Arrêter l'Application 🔒
- Arrêter une application en arrière-plan
- Code Java 🧪 *(hors scope — Ctrl reste en JS)*
- Code JavaScript 🧪 *(P1 — c'est exactement l'équivalent de l'action "Exécuter un bloc de code JS" déjà prévue section 8.3)*
- Effacer les données de l'application 🔒
- Lancer l'activité de l'application (deep-link vers un écran précis)
- Lancer un Raccourci
- Lancer une Application *(déjà prévu)*
- Obtenir la liste des applications installées
- Ouvrir un Site Web
- Script Shell *(déjà prévu, root uniquement)*
- Tasker/Plugin Locale *(hors scope)*

### 3.3 Caméra/Photo — P2
- Activer/Désactiver la caméra
- Faire une Capture d'Écran
- Générer un code QR
- Ouvrir la dernière photo
- Partager la Dernière Photo
- Prendre une Photo

### 3.4 Conditions/Boucles — P1, critique pour le moteur
- Clause Si
- Confirmer la Prochaine (dialogue de confirmation avant de continuer)
- Goto
- Parcourir un dictionnaire/tableau (boucle for-each)
- Recommencer la boucle
- Répéter les actions (boucle for/while)
- Si Confirmé Alors
- Sortir de la boucle (break)

*(Ce bloc entier correspond à la logique de flux de contrôle — à intégrer dans le module `core/engine` comme des types d'actions spéciaux qui modifient le pointeur d'exécution, plutôt que des actions simples)*

### 3.5 Connectivité — P1/P3
- Activer/Désactiver Données Mobiles 🔒
- Activer/Désactiver Point d'Accès
- Activer/Désactiver Synchronisation Auto
- Activer/Désactiver le Mode Avion
- Analyse SSID Wifi
- Configurer le Bluetooth
- Configurer le Wi-Fi
- Connexion USB 🔒
- Contrôle de la connectivité (générique)
- Obtenir les données utilisées
- Synchroniser un compte

### 3.6 Date/Heure — P1
- Chronomètre (démarrer/arrêter/lire)
- Dire l'Heure Actuelle (TTS)
- Réveil (créer une alarme système)

### 3.7 Écran — P1/P2, cœur différenciant de Ctrl
- Allumer/Éteindre l'Écran
- Assombrir l'écran
- Bloquer les touchés d'écran
- Forcer la Rotation de l'Écran
- Garder l'Appareil Éveillé (wake lock)
- Lire le code QR
- Lire le contenu de l'écran *(P1 — équivalent direct des sélecteurs AutoJs6)*
- Lire le contenu de la capture d'écran *(P2 — OCR)*
- Luminosité
- Obtenir le texte de la vue Id *(P1 — sélecteur par id, cœur de la fonctionnalité 9.1 du cahier des charges)*
- Régler la Mise en Veille de l'Écran
- **Texte à partir d'une image (OCR)** *(P2 — correspond exactement à la fonctionnalité avancée 9.1 "lecture d'écran intelligente")*
- Vérifier l'image à l'écran (template matching)
- Vérifier la couleur des pixels
- Vérifier le texte dans la capture d'écran
- Vérifier le texte à l'écran

### 3.8 Fichiers — P1
- Créer un graphique
- Lire depuis un fichier
- Opérations sur des Fichiers (copier/déplacer/renommer)
- Opérations sur les fichiers (accès étendu)
- Ouvrir un Fichier
- Écrire dans un fichier

### 3.9 Interactions avec le Web — P1/P2
- Analyse JSON (parsing)
- Commande UDP
- Envoyer la réponse du Webhook 💎
- Requête HTTP *(P1 — essentiel pour l'intégration IA et sync cloud)*
- Réponse du serveur HTTP *(P2)*
- Résultat JSON

### 3.10 Journalisation — P1
- Calendrier - Ajout d'un événement *(P2)*
- Consulter le Journal *(→ équivalent Journal d'Exécution, section 9.5 cahier des charges)*
- Effacer les Journaux
- Exporter le journal
- Journal d'Événement (écrire une entrée custom)
- Obtenir le calendrier des événements *(P2)*

### 3.11 Localisation — P2/P3
- Activer/Désactiver le GPS 🔒
- Définir Fréquence d'Actualisation d'Emplacement
- Forcer la Mise à Jour de l'Emplacement 🔒
- Mode de localisation 🔒
- Partager l'Emplacement

### 3.12 Macros (méta-actions) — P1, essentiel pour le chaînage
- Activer/Désactiver une Macro
- Annuler les Actions de la Macro (rollback si possible)
- Attendre avant la Prochaine Action *(déjà prévu section 8.3 : "attendre X secondes")*
- Bloc d'action (regroupement réutilisable — à considérer comme "sous-macro")
- **Exécuter une Macro** *(déjà prévu — chaînage, section 9.3)*
- Supprimer la Macro
- Terminer l'instance en cours d'exécution (kill switch — lié au bouton d'arrêt d'urgence, section 13 du cahier des charges)

### 3.13 Média — P1
- Contrôler le Média (play/pause/next/prev)
- Enregistrer une vidéo
- Enregistrer à Partir du Microphone
- Jouer/Arrêter un Son *(déjà prévu)*

### 3.14 Messagerie — P1
- Envoyer par WhatsApp 🧪 *(P2 — nécessite accès à l'UI WhatsApp via sélecteurs, pas d'API officielle)*
- Envoyer un Courriel *(P2 — nécessite compte SMTP ou intent mail)*
- Envoyer un SMS *(déjà prévu)*

### 3.15 Notification — P1
- Activer/Désactiver LED de Notification
- Activer/désactiver tête haute 🔒
- Afficher Boîte de Dialogue *(déjà prévu — "popup/dialog personnalisé")*
- Afficher une Notification *(déjà prévu)*
- Afficher une Notification Bulle
- Bordure de notification lumineuse (spécifique constructeur)
- Cercle de notification de l'appareil photo (spécifique constructeur)
- Configurer les Notifications des Apps 🔒
- Définir le son de la notification
- Effacer les Notifications
- Interaction sur notification (répondre depuis une notif)
- Message Flottant
- Restaurer les notifications cachées
- Réponse à la notification
- Superposition d'animation

### 3.16 Paramètres de l'appareil — P1/P3
- Activer/Désactiver Rotation Automatique
- Affichage ambiant 🔒
- Clavier - Paramètres par défaut 🔒
- Configuration système (générique)
- Configurer les tuiles de réglage rapide
- Définir l'assistant numérique 🔒
- Densité d'affichage 🔒
- Définir le Fond d'Écran
- Economiseur de batterie (activer/désactiver)
- Invite du clavier
- Mode Voiture
- Mode démo 🔒
- Mode immersif 🔒
- Régler le Verrouillage de l'Écran 🔒
- Secure Settings (accès paramètres sécurisés)
- Service d'accessibilité (activer/vérifier) 🔒
- Taille de la police
- Thème sombre 🔒

### 3.17 Spécifique app (méta-actions Ctrl) — P1
- Action vide (utile en placeholder/debug)
- Activer/Désactiver une catégorie
- Activer/désactiver le déclencheur (sans désactiver toute la macro)
- Actualiser le Widget
- Affecter une image à la barre de bouton
- Afficher une scène personnalisée
- Attendre le déclencheur (bloquant jusqu'à événement)
- Barre de superposition
- Boîte de dialogue à options (choix multiple)
- Configurer le bouton flottant
- Dialogue de sélection
- Définir l'Icône de l'app
- Définir le Mode (profil)
- Définir le texte de notification persistante
- Désactiver l'app entièrement (kill switch global)
- Effacer la boîte de dialogue
- Exporter les Macros *(déjà prévu — section 9.4 sync/export)*
- Groupe d'action (sous-ensemble réutilisable d'actions)
- Masquer la scène personnalisée
- Modifier le bouton Widget
- Paramètres de l'app
- Résumer les macros/blocs d'action (vue de synthèse)
- Texte flottant (afficher un widget flottant custom)
- Tiroir (menu latéral)
- Traduire un texte *(P2 — via API traduction)*

### 3.18 Téléphone — P1
- Configurer une Sonnerie
- Contact par l'application (sélectionner un contact)
- Effacer le Journal d'Appels
- Ouvrir le Journal d'Appels
- Passer un Appel *(déjà prévu)*
- Rejeter un Appel
- Récupérer les contacts
- Réponse à un appel

### 3.19 Variables — P1, critique
- Chiffrer/déchiffrer un texte
- **Définir une variable** *(déjà prévu section 7.5)*
- Effacer les variables
- Effacer une entrée de dictionnaire/de tableau
- Manipulation de texte (regex, concat, split, etc.)
- Manipulation des tableaux
- Supprimer une variable

### 3.20 Volume — P1
- Activer/Désactiver le Haut-Parleur
- Activer/Désactiver le Vibreur
- Afficher la fenêtre contextuelle du volume
- Augmenter/Diminuer le Volume (relatif)
- Changer le Volume (absolu, déjà prévu)
- Couper le Microphone
- Lire le Niveau Sonore
- Mode Prioritaire / Ne pas déranger (activer)
- Silencieux - Vibreur désactivé

### 3.21 Actions génériques de l'appareil — P1/P3
- Actualisation du presse-papiers
- Allumer/Éteindre la Lampe Torche
- Appuyer sur le Bouton Retour *(P1 — équivalent direct `back()` d'AutoJs6)*
- Authentifier l'utilisateur (biométrie)
- Capture Prochain clic X,Y (calibration de coordonnées — outil de dev utile pour Ctrl lui-même)
- Déployer/Réduire la barre d'état
- Déverrouillage avec code PIN 🔒
- Envoyer un «Intent» *(P1 — utile pour interop avec d'autres apps)*
- Interaction avec l'Interface *(P1 — équivalent direct des actions clic/swipe sur sélecteur)*
- Lancer l'Écran d'Accueil
- Lecture de Capteur
- Obtenir le niveau de lumière
- Partager du texte (intent de partage standard)
- Raccourci Android (créer un raccourci sur l'écran d'accueil)
- Recherche Vocale
- Redémarrage/Mise Hors Tension 🔒
- Remplir le Presse-Papier
- Routines Samsung (intégration spécifique constructeur — hors scope)
- Saisie vocale (dicter du texte)
- Texte par Synthèse Vocale *(déjà prévu — TTS)*
- Vibration *(déjà prévu)*
- Événement touche (simuler une touche clavier physique)

---

## 4. Fonctionnalités "app" annexes (hors Trigger/Condition/Action)

Observées dans le menu d'accueil MacroDroid — à considérer pour le Dashboard Ctrl (section 9.1 du `design.md`) :

- Journal Système / Journal Utilisateur *(→ Journal d'Exécution de Ctrl, déjà prévu)*
- Vidéos tutoriels intégrées *(à envisager en Phase 6, polish)*
- Extensions (marketplace de plugins tiers) *(→ lien avec la Marketplace optionnelle, section 9.6)*
- Dernière macro ouverte / Dernier bloc d'action ouvert (raccourcis de navigation récente)
- Macros favorites (épinglage)
- Exécuter rapidement une macro (accès direct sans passer par le détail)
- Sauvegarde automatique *(→ lié à la sync cloud, section 9.4)*
- Tuiles de réglages rapides Android (accès à une macro depuis le panneau de notifications système)
- Catégories (regroupement visuel des macros)
- Télécommande (contrôle à distance depuis un autre appareil — hors scope initial)
- Widgets (accès à une macro depuis l'écran d'accueil Android)
- **Constructeur de macros AI** — confirme que la génération de macro par IA (section 9.2 du cahier des charges) est une fonctionnalité déjà validée sur le marché, pas juste une idée théorique
- Variables globales (déjà prévu, section 7.5)
- Chronomètres (liste dédiée, au-delà d'un simple trigger/condition)
- Zones de géorepérage (gestion centralisée des geofences définies)

---

## 5. Synthèse — Ce que ce catalogue change pour la roadmap Ctrl

1. **Le module `core/engine` doit gérer la logique de flux de contrôle** (section 3.4 ci-dessus : boucles, goto, conditions imbriquées) comme des primitives du moteur, pas seulement des actions simples — à refléter dans le schéma JSON (section 7 du cahier des charges principal), qui gagnerait un type d'action `controle_flux` distinct des actions classiques.
2. **Les "méta-actions sur macros"** (activer/désactiver/exécuter/terminer une macro, section 3.12) confirment que le chaînage de macros (déjà prévu en 9.3) doit être une catégorie d'action à part entière, pas une fonctionnalité annexe.
3. **La manipulation de texte/tableaux/variables** (section 3.19) est plus riche que ce qui était détaillé en section 7.5 du cahier des charges — à enrichir avec des opérations regex, concat, et structures de données (listes/dictionnaires), pas seulement des valeurs scalaires.
4. **Tout ce qui est marqué 🔒 (Root/Shizuku/ADB)** représente une portion significative du catalogue MacroDroid — confirme que viser le non-root en priorité (comme prévu) laisse volontairement de côté certaines fonctionnalités avancées de MacroDroid, à assumer explicitement plutôt qu'à essayer de tout couvrir dès la Phase 2.
5. **L'action "AI LLM Query"** existe déjà chez MacroDroid — Ctrl a donc intérêt à ne pas se contenter de la génération de macro par IA (one-shot), mais aussi proposer un **appel IA en tant qu'action dans une macro** (ex: "résume ce texte reçu et envoie le résumé par SMS").

---

*Ce fichier sert de checklist de couverture pour la Phase 2 (parité MacroDroid) et la Phase 4 (fonctionnalités avancées) du cahier des charges principal. À cocher/annoter au fur et à mesure de l'implémentation.*