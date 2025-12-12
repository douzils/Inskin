package com.inskin.app.implants

import com.inskin.app.ImplantInfo
import com.inskin.app.ui.screens.BadgeForm

/**
 * Mapper pour associer automatiquement un BadgeForm à un implant détecté
 */
object ImplantBadgeMapper {

    /**
     * Suggère le BadgeForm approprié pour un implant détecté
     */
    fun suggestBadgeForm(implant: ImplantInfo): BadgeForm {
        return when (implant.type.uppercase()) {
            "XNT" -> BadgeForm.ImplantXNT
            "XM1" -> BadgeForm.ImplantXM1
            "XEM" -> BadgeForm.ImplantXEM
            "XAC" -> BadgeForm.ImplantXAC
            "NEXT" -> BadgeForm.ImplantNExT
            "FLEX_NT" -> BadgeForm.ImplantFlexNT
            "FLEX_M1" -> BadgeForm.ImplantFlexNT
            "FLEX_EM" -> BadgeForm.ImplantFlexNT
            "VIVOKEY" -> BadgeForm.ImplantVivoKey
            "SPARK" -> BadgeForm.ImplantVivoKey
            else -> BadgeForm.Implant
        }
    }

    /**
     * Vérifie si un BadgeForm correspond à un type d'implant
     */
    fun matchesImplant(form: BadgeForm, implantType: String): Boolean {
        val type = implantType.uppercase()
        return when (form) {
            BadgeForm.ImplantXNT -> type == "XNT"
            BadgeForm.ImplantXM1 -> type == "XM1"
            BadgeForm.ImplantXEM -> type == "XEM"
            BadgeForm.ImplantXAC -> type == "XAC"
            BadgeForm.ImplantNExT -> type == "NEXT"
            BadgeForm.ImplantFlexNT -> type.startsWith("FLEX")
            BadgeForm.ImplantVivoKey -> type.contains("VIVOKEY") || type == "SPARK"
            BadgeForm.Implant -> true // Générique, correspond à tout
            else -> false
        }
    }

    /**
     * Retourne tous les BadgeForm compatibles avec un implant
     */
    fun getCompatibleForms(implant: ImplantInfo): List<BadgeForm> {
        val specific = suggestBadgeForm(implant)
        return listOf(
            specific,
            BadgeForm.Implant  // Toujours proposer le générique en option
        )
    }

    /**
     * Détermine si un BadgeForm représente un implant DT
     */
    fun isImplantForm(form: BadgeForm): Boolean {
        return form in listOf(
            BadgeForm.Implant,
            BadgeForm.ImplantXNT,
            BadgeForm.ImplantXM1,
            BadgeForm.ImplantXEM,
            BadgeForm.ImplantXAC,
            BadgeForm.ImplantNExT,
            BadgeForm.ImplantFlexNT,
            BadgeForm.ImplantVivoKey
        )
    }
}
