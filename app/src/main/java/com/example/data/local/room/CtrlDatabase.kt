package com.example.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.domain.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [MacroEntity::class, VariableEntity::class, LogEntryEntity::class],
    version = 3,
    exportSchema = false
)
abstract class CtrlDatabase : RoomDatabase() {
    abstract fun ctrlDao(): CtrlDao

    companion object {
        @Volatile
        private var INSTANCE: CtrlDatabase? = null

        fun getInstance(context: Context): CtrlDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CtrlDatabase::class.java,
                    "ctrl_automation.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Initialisation avec des modèles prêts à l'emploi (0 exécution fictive)
                            CoroutineScope(Dispatchers.IO).launch {
                                preloadInitialData(getInstance(context).ctrlDao())
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun preloadInitialData(dao: CtrlDao) {
            val now = System.currentTimeMillis()

            val macro1 = Macro(
                id = "macro_nuit_eco",
                nom = "Mode Nuit Automatique",
                description = "Coupe le Bluetooth, baisse le volume et envoie une alerte de sommeil à 22h30",
                active = true,
                trigger = Trigger.HeureFixe(heure = 22, minute = 30),
                conditions = listOf(Condition.PlageHoraire(22, 0, 7, 0)),
                actions = listOf(
                    ActionMacro.BasculerBluetooth(activer = false),
                    ActionMacro.ChangerVolume(niveauPourcentage = 20, canal = "Média"),
                    ActionMacro.EnvoyerNotification(
                        titre = "Ctrl - Nuit paisible",
                        message = "Profil nuit appliqué : Bluetooth coupé, volume réduit."
                    )
                ),
                dateCreation = now,
                derniereExecution = null,
                nbExecutions = 0,
                nbEchecs = 0
            )

            val macro2 = Macro(
                id = "macro_batterie_critique",
                nom = "Alerte Batterie Critique",
                description = "Vibre et émet un bip sonore dès que la batterie passe sous les 15%",
                active = true,
                trigger = Trigger.NiveauBatterie(seuil = 15, inferieur = true),
                conditions = listOf(Condition.EnCharge(doitEtreEnCharge = false)),
                actions = listOf(
                    ActionMacro.Vibrer(dureeMs = 600),
                    ActionMacro.JouerSon(typeSon = "Alerte batterie"),
                    ActionMacro.EnvoyerNotification(
                        titre = "Batterie Critique",
                        message = "Batterie sous 15%. Branchez votre appareil pour éviter la coupure."
                    )
                ),
                dateCreation = now,
                derniereExecution = null,
                nbExecutions = 0,
                nbEchecs = 0
            )

            val macro3 = Macro(
                id = "macro_casque_audio",
                nom = "Connexion Casque Audio",
                description = "Ajuste automatiquement le volume média à 70% et confirme par toast",
                active = true,
                trigger = Trigger.CasqueAudio(branche = true),
                conditions = emptyList(),
                actions = listOf(
                    ActionMacro.ChangerVolume(niveauPourcentage = 70, canal = "Média"),
                    ActionMacro.AfficherToast(message = "Casque détecté : volume réglé à 70%")
                ),
                dateCreation = now,
                derniereExecution = null,
                nbExecutions = 0,
                nbEchecs = 0
            )

            val macro4 = Macro(
                id = "macro_clic_ui_auto",
                nom = "Validation Formulaire Écran",
                description = "Exécute un clic automatisé sur le bouton 'Valider' via le service d'accessibilité",
                active = false,
                trigger = Trigger.Manuel(),
                conditions = listOf(Condition.ServiceAccessibiliteActif(requis = true)),
                actions = listOf(
                    ActionMacro.ActionUI(
                        typeGeste = TypeGesteUI.CLIC,
                        selecteurType = TypeSelecteur.TEXTE,
                        valeurCible = "Valider"
                    ),
                    ActionMacro.Attendre(secondes = 1),
                    ActionMacro.EnvoyerNotification(
                        titre = "Action UI Ctrl",
                        message = "Bouton ciblé et actionné avec succès."
                    )
                ),
                dateCreation = now,
                derniereExecution = null,
                nbExecutions = 0,
                nbEchecs = 0
            )

            val macro5 = Macro(
                id = "macro_ia_resume",
                nom = "Synthèse Journalière IA",
                description = "Interroge le moteur IA pour résumer les alertes et tâches de la journée",
                active = true,
                trigger = Trigger.HeureFixe(heure = 8, minute = 0),
                conditions = listOf(Condition.WifiConnecte(doitEtreConnecte = true)),
                actions = listOf(
                    ActionMacro.AppelIA(
                        promptUtilisateur = "Générer la météo et les priorités de la journée",
                        variableSortie = "resume_matin"
                    ),
                    ActionMacro.EnvoyerNotification(
                        titre = "Synthèse matinale",
                        message = "Votre synthèse matinale Ctrl est prête."
                    )
                ),
                dateCreation = now,
                derniereExecution = null,
                nbExecutions = 0,
                nbEchecs = 0,
                estGenereeParIA = true
            )

            dao.insertOrUpdateMacro(MacroEntity.fromDomain(macro1))
            dao.insertOrUpdateMacro(MacroEntity.fromDomain(macro2))
            dao.insertOrUpdateMacro(MacroEntity.fromDomain(macro3))
            dao.insertOrUpdateMacro(MacroEntity.fromDomain(macro4))
            dao.insertOrUpdateMacro(MacroEntity.fromDomain(macro5))

            // Variables système réelles par défaut
            dao.insertOrUpdateVariable(VariableEntity("mode_actuel", "Normal", "TEXTE", now))
            dao.insertOrUpdateVariable(VariableEntity("seuil_eco", "20", "NOMBRE", now))
            dao.insertOrUpdateVariable(VariableEntity("auto_sync", "true", "BOOLEEN", now))
            // Aucun faux log inséré : le journal reflète uniquement les exécutions réelles
        }
    }
}
