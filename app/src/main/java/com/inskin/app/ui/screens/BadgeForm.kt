package com.inskin.app.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** Enum publique et unique (supprime toute autre déclaration de BadgeForm). */
enum class BadgeForm(
    val label: String,
    val tint: Color,
    val icon: ImageVector
) {
    Card("Carte", Color(0xFF4FC3F7), Icons.Filled.CreditCard),
    Keyfob("Badge immeuble", Color(0xFFFFB74D), Icons.Filled.VpnKey),
    Implant("Implant", Color(0xFF81C784), Icons.Filled.Healing),
    Sticker("Sticker/Ntag", Color(0xFFBA68C8), Icons.Filled.Memory),
    Watch("Montre/bracelet", Color(0xFFA1887F), Icons.Filled.Watch),
    AccessPoint("Contrôle accès", Color(0xFF64B5F6), Icons.Filled.Apartment),
}
