package com.example.presentation.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Liste centralisée des permissions Android "dangereuses" utilisées par les
 * triggers/actions de Ctrl. Sans demande explicite à l'exécution, ces permissions
 * déclarées dans le manifeste restent inertes (Android 6.0+) et les fonctionnalités
 * correspondantes échouent silencieusement.
 */
object PermissionsHelper {

    data class PermissionInfo(val permission: String, val label: String, val raison: String)

    fun permissionsRequises(): List<PermissionInfo> {
        val liste = mutableListOf(
            PermissionInfo(Manifest.permission.SEND_SMS, "Envoyer des SMS", "Action \"Envoyer un SMS\""),
            PermissionInfo(Manifest.permission.RECEIVE_SMS, "Recevoir des SMS", "Déclencheur \"SMS Reçu\""),
            PermissionInfo(Manifest.permission.READ_SMS, "Lire les SMS", "Déclencheur \"SMS Envoyé\""),
            PermissionInfo(Manifest.permission.READ_PHONE_STATE, "État du téléphone", "Déclencheurs d'appel"),
            PermissionInfo(Manifest.permission.CALL_PHONE, "Passer des appels", "Action \"Passer un appel\""),
            PermissionInfo(Manifest.permission.CAMERA, "Caméra (torche)", "Déclencheur/action Torche")
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            liste.add(PermissionInfo(Manifest.permission.POST_NOTIFICATIONS, "Notifications", "Notification persistante + action Notification"))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            liste.add(PermissionInfo(Manifest.permission.BLUETOOTH_CONNECT, "Bluetooth", "Déclencheurs/actions Bluetooth"))
        }
        return liste
    }

    fun estAccordee(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun toutesAccordees(context: Context): Boolean {
        return permissionsRequises().all { estAccordee(context, it.permission) }
    }

    fun manquantes(context: Context): List<PermissionInfo> {
        return permissionsRequises().filterNot { estAccordee(context, it.permission) }
    }
}
