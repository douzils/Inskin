package com.inskin.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.ui.graphics.vector.ImageVector

data class TagIconOption(val key: String, val label: String, val icon: ImageVector)

object TagIcons {
    val all: List<TagIconOption> = listOf(
        TagIconOption("implant", "Implant", Icons.Outlined.Science),
        TagIconOption("card", "Carte", Icons.Outlined.CreditCard),
        TagIconOption("building_badge", "Badge d’immeuble", Icons.Outlined.Badge),
        TagIconOption("sticker", "Autocollant", Icons.Outlined.Waves),
        TagIconOption("pcb", "PCB", Icons.Outlined.Memory),
        TagIconOption("keyfob", "Porte-clé", Icons.Outlined.VpnKey),
        TagIconOption("ring", "Anneau", Icons.Outlined.Radio),
        TagIconOption("generic", "Générique", Icons.Outlined.Nfc),
    )
    fun byKey(key: String?): TagIconOption = all.firstOrNull { it.key == key } ?: all.last()
}
