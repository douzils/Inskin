package com.inskin.app.implants

import com.inskin.app.ImplantInfo
import com.inskin.app.TagDetails

/**
 * Détecteur d'implants Dangerous Things
 * Analyse les caractéristiques d'un tag pour identifier s'il s'agit d'un implant DT
 */
object ImplantDetector {

    /**
     * Analyse un TagDetails et retourne les informations d'implant si détecté
     */
    fun detect(details: TagDetails): ImplantInfo? {
        val profile = DangerousThingsImplants.detectImplant(
            atqa = details.atqaHex,
            sak = details.sakHex,
            chipType = details.chipType,
            memorySize = details.totalMemoryBytes,
            techList = details.techList
        ) ?: return null

        // Calcule la confiance de la détection
        val confidence = calculateConfidence(details, profile)

        // Ne retourne un résultat que si la confiance est suffisante
        if (confidence < 0.5f) return null

        return ImplantInfo(
            type = profile.type.name,
            name = profile.name,
            frequency = profile.frequency,
            confidence = confidence,
            recommendations = profile.recommendations,
            warnings = profile.warnings,
            useCases = profile.usesCases
        )
    }

    /**
     * Calcule un score de confiance pour la détection
     * Retourne une valeur entre 0.0 (aucune confiance) et 1.0 (certitude absolue)
     */
    private fun calculateConfidence(details: TagDetails, profile: ImplantProfile): Float {
        var score = 0f
        var criteria = 0

        // ATQA correspond
        if (profile.detectionCriteria.atqa != null) {
            criteria++
            if (details.atqaHex != null &&
                details.atqaHex.uppercase().replace(" ", "") ==
                profile.detectionCriteria.atqa.replace(" ", "")
            ) {
                score += 0.25f
            }
        }

        // SAK correspond
        if (profile.detectionCriteria.sak != null) {
            criteria++
            if (details.sakHex != null &&
                details.sakHex.uppercase().replace(" ", "") ==
                profile.detectionCriteria.sak.replace(" ", "")
            ) {
                score += 0.25f
            }
        }

        // Type de puce correspond
        if (profile.detectionCriteria.chipTypePattern != null) {
            criteria++
            if (details.chipType != null &&
                details.chipType.contains(
                    Regex(profile.detectionCriteria.chipTypePattern, RegexOption.IGNORE_CASE)
                )
            ) {
                score += 0.3f
            }
        }

        // Taille mémoire correspond
        if (profile.detectionCriteria.memorySize != null) {
            criteria++
            if (details.totalMemoryBytes != null &&
                details.totalMemoryBytes == profile.detectionCriteria.memorySize
            ) {
                score += 0.2f
            }
        }

        // Normalise le score si moins de critères sont utilisés
        return if (criteria > 0) score.coerceIn(0f, 1f) else 0f
    }

    /**
     * Enrichit un TagDetails avec les informations d'implant détecté
     */
    fun enrichWithImplantInfo(details: TagDetails): TagDetails {
        val implantInfo = detect(details) ?: return details
        return details.copy(detectedImplant = implantInfo)
    }

    /**
     * Vérifie si un tag est probablement un implant Dangerous Things
     */
    fun isLikelyImplant(details: TagDetails): Boolean {
        return detect(details)?.confidence?.let { it >= 0.7f } ?: false
    }

    /**
     * Retourne des recommandations spécifiques pour la lecture d'un implant
     */
    fun getReadingRecommendations(details: TagDetails): List<String> {
        val implantInfo = detect(details) ?: return emptyList()

        val general = listOf(
            "Maintenez le téléphone stable pendant la lecture",
            "Position optimale: parallèle à l'implant",
            "Évitez les mouvements brusques"
        )

        return general + implantInfo.recommendations
    }

    /**
     * Analyse la santé d'un implant basé sur les compteurs d'écriture
     */
    fun analyzeImplantHealth(details: TagDetails): ImplantHealthStatus {
        val implantInfo = detect(details) ?: return ImplantHealthStatus.Unknown

        // Pour les tags NTAG, on peut lire les compteurs
        val writeCounter = extractWriteCounter(details)

        return when {
            writeCounter == null -> ImplantHealthStatus.Unknown
            writeCounter < 10_000 -> ImplantHealthStatus.Excellent
            writeCounter < 50_000 -> ImplantHealthStatus.Good
            writeCounter < 80_000 -> ImplantHealthStatus.Fair
            writeCounter < 100_000 -> ImplantHealthStatus.Warning
            else -> ImplantHealthStatus.Critical
        }
    }

    /**
     * Extrait le compteur d'écritures pour les NTAG
     */
    private fun extractWriteCounter(details: TagDetails): Int? {
        // Le compteur est généralement dans countersHex pour les NTAG
        val countersHex = details.countersHex ?: return null

        return try {
            // Les 3 octets du compteur (24 bits)
            if (countersHex.length >= 6) {
                countersHex.substring(0, 6).toInt(16)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * État de santé d'un implant
 */
enum class ImplantHealthStatus {
    Excellent,      // < 10k écritures
    Good,           // 10k-50k écritures
    Fair,           // 50k-80k écritures
    Warning,        // 80k-100k écritures
    Critical,       // > 100k écritures
    Unknown         // Impossible à déterminer
}
