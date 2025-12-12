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
    ImplantXNT("xNT (NTAG216)", Color(0xFF4CAF50), Icons.Filled.Healing),
    ImplantXM1("xM1 (Mifare)", Color(0xFF2196F3), Icons.Filled.Healing),
    ImplantXEM("xEM (125kHz)", Color(0xFFFF9800), Icons.Filled.Healing),
    ImplantXAC("xAC (NTAG I2C)", Color(0xFF9C27B0), Icons.Filled.Healing),
    ImplantNExT("NExT (Dual)", Color(0xFF00BCD4), Icons.Filled.Healing),
    ImplantFlexNT("FlexNT", Color(0xFF8BC34A), Icons.Filled.Healing),
    ImplantVivoKey("VivoKey Apex", Color(0xFFE91E63), Icons.Filled.Healing),
    Sticker("Sticker/Ntag", Color(0xFFBA68C8), Icons.Filled.Memory),
    Watch("Montre/bracelet", Color(0xFFA1887F), Icons.Filled.Watch),
    AccessPoint("Contrôle accès", Color(0xFF64B5F6), Icons.Filled.Apartment),
    Ring("Anneau NFC", Color(0xFFFFD700), Icons.Filled.Circle),
    Phone("Téléphone", Color(0xFF607D8B), Icons.Filled.Smartphone),
}
