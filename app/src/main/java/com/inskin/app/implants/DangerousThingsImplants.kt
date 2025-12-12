package com.inskin.app.implants

import androidx.compose.ui.graphics.Color

/**
 * Énumération des types d'implants Dangerous Things
 */
enum class ImplantType {
    XNT,           // NTAG216
    XM1,           // Mifare Classic 1K
    XEM,           // EM4102 (125kHz)
    XAC,           // NTAG I2C Plus
    NEXT,          // Dual (HF + LF)
    FLEX_NT,       // NTAG flexible
    FLEX_M1,       // Mifare flexible
    FLEX_EM,       // EM flexible
    SPARK,         // LED implant
    VIVOKEY,       // VivoKey Apex/Spark2
    UNKNOWN        // Non identifié
}

/**
 * Profil détaillé d'un implant Dangerous Things
 */
data class ImplantProfile(
    val type: ImplantType,
    val name: String,
    val description: String,
    val frequency: String,
    val chipType: String,
    val memorySize: Int,
    val readRange: String,
    val writeEndurance: Int?,
    val dataRetention: String,
    val usesCases: List<String>,
    val color: Color,
    val detectionCriteria: DetectionCriteria,
    val recommendations: List<String>,
    val warnings: List<String> = emptyList()
)

/**
 * Critères de détection pour identifier un implant
 */
data class DetectionCriteria(
    val atqa: String? = null,
    val sak: String? = null,
    val chipTypePattern: String? = null,
    val memorySize: Int? = null,
    val versionPattern: String? = null,
    val techList: List<String>? = null
)

/**
 * Catalogue complet des implants Dangerous Things
 */
object DangerousThingsImplants {

    val profiles = listOf(
        // xNT - NTAG216
        ImplantProfile(
            type = ImplantType.XNT,
            name = "xNT",
            description = "Implant NFC haute fréquence basé sur NTAG216",
            frequency = "13.56 MHz (HF)",
            chipType = "NTAG216",
            memorySize = 888,
            readRange = "1-4 cm",
            writeEndurance = 100_000,
            dataRetention = "10 ans",
            usesCases = listOf(
                "Cartes de visite numériques (vCard)",
                "Partage de profils sociaux",
                "URLs et liens web",
                "Authentification 2FA",
                "Déverrouillage de smartphone",
                "Partage WiFi"
            ),
            color = Color(0xFF4CAF50),
            detectionCriteria = DetectionCriteria(
                atqa = "0044",
                sak = "00",
                chipTypePattern = "NTAG216",
                memorySize = 888
            ),
            recommendations = listOf(
                "Positionnez votre téléphone parallèlement à l'implant",
                "Distance optimale: 0-2 cm",
                "Évitez les surfaces métalliques lors de la lecture",
                "Utilisez des URLs courtes pour optimiser l'espace"
            ),
            warnings = listOf(
                "Maximum 100,000 cycles d'écriture",
                "Évitez les écritures trop fréquentes"
            )
        ),

        // xM1 - Mifare Classic 1K
        ImplantProfile(
            type = ImplantType.XM1,
            name = "xM1",
            description = "Implant Mifare Classic 1K compatible",
            frequency = "13.56 MHz (HF)",
            chipType = "Mifare Classic 1K compatible",
            memorySize = 1024,
            readRange = "1-4 cm",
            writeEndurance = 100_000,
            dataRetention = "10 ans",
            usesCases = listOf(
                "Clonage de badges d'accès",
                "Contrôle d'accès bâtiments",
                "Systèmes de transport",
                "Remplacement de cartes Mifare"
            ),
            color = Color(0xFF2196F3),
            detectionCriteria = DetectionCriteria(
                atqa = "0004",
                sak = "08",
                chipTypePattern = "(Mifare|MFC|MIFARE Classic)",
                memorySize = 1024,
                techList = listOf("android.nfc.tech.MifareClassic")
            ),
            recommendations = listOf(
                "Utilisez un Proxmark3 pour cloner des badges",
                "Testez la compatibilité avant implantation",
                "Certains lecteurs peuvent ne pas fonctionner",
                "Vérifiez les clés d'authentification avant écriture"
            ),
            warnings = listOf(
                "Crypto-1 est considéré comme non sécurisé",
                "Certains systèmes modernes peuvent rejeter ce type de puce",
                "Assurez-vous d'avoir les clés d'accès correctes"
            )
        ),

        // xEM - EM4102 125kHz
        ImplantProfile(
            type = ImplantType.XEM,
            name = "xEM",
            description = "Implant RFID basse fréquence EM4102",
            frequency = "125 kHz (LF)",
            chipType = "EM4102",
            memorySize = 64,
            readRange = "2-10 cm",
            writeEndurance = null, // Read-only après programmation
            dataRetention = "Permanent",
            usesCases = listOf(
                "Contrôle d'accès bâtiments anciens",
                "Badges employés",
                "Identification animaux",
                "Systèmes de parking"
            ),
            color = Color(0xFFFF9800),
            detectionCriteria = DetectionCriteria(
                chipTypePattern = "EM41(02|50)",
                techList = null // Nécessite Proxmark3 pour LF
            ),
            recommendations = listOf(
                "Nécessite un lecteur basse fréquence (125 kHz)",
                "Les smartphones ne peuvent PAS lire ce type d'implant",
                "Utilisez un Proxmark3 pour la lecture/écriture",
                "Distance de lecture plus grande que les implants HF"
            ),
            warnings = listOf(
                "Non lisible avec les smartphones standards",
                "Programmable une seule fois (OTP - One Time Programmable)",
                "Assurez-vous de l'ID correct avant programmation"
            )
        ),

        // xAC - NTAG I2C Plus
        ImplantProfile(
            type = ImplantType.XAC,
            name = "xAC",
            description = "Implant NFC avancé avec I2C et détection de champ",
            frequency = "13.56 MHz (HF)",
            chipType = "NTAG I2C Plus",
            memorySize = 1904,
            readRange = "1-4 cm",
            writeEndurance = 100_000,
            dataRetention = "10 ans",
            usesCases = listOf(
                "Contrôle d'accès avancé",
                "Authentification sécurisée",
                "Détection de présence",
                "Applications IoT"
            ),
            color = Color(0xFF9C27B0),
            detectionCriteria = DetectionCriteria(
                atqa = "0044",
                sak = "00",
                chipTypePattern = "NTAG I2C",
                memorySize = 1904
            ),
            recommendations = listOf(
                "Supporte les fonctionnalités avancées I2C",
                "Détection de champ RF pour économie d'énergie",
                "Compatible avec la plupart des lecteurs NFC",
                "Mémoire plus grande que le xNT"
            )
        ),

        // NExT - Dual frequency
        ImplantProfile(
            type = ImplantType.NEXT,
            name = "NExT",
            description = "Implant double fréquence (HF + LF)",
            frequency = "13.56 MHz + 125 kHz",
            chipType = "NTAG216 + EM4102",
            memorySize = 888,
            readRange = "1-4 cm (HF), 2-10 cm (LF)",
            writeEndurance = 100_000,
            dataRetention = "10 ans",
            usesCases = listOf(
                "Systèmes d'accès hybrides",
                "Remplacement de multiples badges",
                "Compatibilité maximale",
                "Anciens et nouveaux systèmes"
            ),
            color = Color(0xFF00BCD4),
            detectionCriteria = DetectionCriteria(
                atqa = "0044",
                sak = "00",
                chipTypePattern = "NTAG216"
            ),
            recommendations = listOf(
                "Contient à la fois xNT (HF) et xEM (LF)",
                "Deux puces distinctes dans un seul implant",
                "Le smartphone lit uniquement la partie HF",
                "Utilisez Proxmark3 pour la partie LF"
            ),
            warnings = listOf(
                "Implant plus volumineux que les versions simples",
                "Nécessite plus d'espace sous la peau"
            )
        ),

        // FlexNT
        ImplantProfile(
            type = ImplantType.FLEX_NT,
            name = "FlexNT",
            description = "Implant NFC flexible de grande taille",
            frequency = "13.56 MHz (HF)",
            chipType = "NTAG216",
            memorySize = 888,
            readRange = "1-8 cm",
            writeEndurance = 100_000,
            dataRetention = "10 ans",
            usesCases = listOf(
                "Applications nécessitant longue portée",
                "Zones corporelles plates (avant-bras)",
                "Projets artistiques/body modification",
                "Performance maximale"
            ),
            color = Color(0xFF4CAF50),
            detectionCriteria = DetectionCriteria(
                atqa = "0044",
                sak = "00",
                chipTypePattern = "NTAG216",
                memorySize = 888
            ),
            recommendations = listOf(
                "Antenne plus grande = meilleure portée",
                "Installation par professionnel body modification",
                "Nécessite plus d'espace sous la peau",
                "Guérison plus longue que les implants standards"
            ),
            warnings = listOf(
                "Installation plus invasive",
                "Consultez un professionnel expérimenté",
                "Temps de guérison: 2-4 semaines"
            )
        ),

        // FlexM1
        ImplantProfile(
            type = ImplantType.FLEX_M1,
            name = "FlexM1",
            description = "Implant Mifare flexible de grande taille",
            frequency = "13.56 MHz (HF)",
            chipType = "Mifare Classic 1K compatible",
            memorySize = 1024,
            readRange = "1-8 cm",
            writeEndurance = 100_000,
            dataRetention = "10 ans",
            usesCases = listOf(
                "Clonage badges avec meilleure portée",
                "Contrôle d'accès longue distance",
                "Remplacement de cartes Mifare"
            ),
            color = Color(0xFF2196F3),
            detectionCriteria = DetectionCriteria(
                atqa = "0004",
                sak = "08",
                chipTypePattern = "(Mifare|MFC|MIFARE Classic)",
                memorySize = 1024
            ),
            recommendations = listOf(
                "Version flexible du xM1",
                "Meilleure portée de lecture",
                "Installation professionnelle recommandée"
            )
        ),

        // VivoKey Apex
        ImplantProfile(
            type = ImplantType.VIVOKEY,
            name = "VivoKey Apex",
            description = "Implant NFC sécurisé avec JavaCard",
            frequency = "13.56 MHz (HF)",
            chipType = "JavaCard secure element",
            memorySize = 80_000,
            readRange = "1-4 cm",
            writeEndurance = 500_000,
            dataRetention = "25 ans",
            usesCases = listOf(
                "Authentification hautement sécurisée",
                "Stockage de clés cryptographiques",
                "Portefeuille de crypto-monnaies",
                "Authentification PKI",
                "Applications personnalisées JavaCard"
            ),
            color = Color(0xFFE91E63),
            detectionCriteria = DetectionCriteria(
                chipTypePattern = "(VivoKey|JavaCard)",
                techList = listOf("android.nfc.tech.IsoDep")
            ),
            recommendations = listOf(
                "Puce sécurisée de niveau bancaire",
                "Supporte les applications JavaCard",
                "Mémoire beaucoup plus grande",
                "Durabilité supérieure",
                "Cryptographie matérielle"
            ),
            warnings = listOf(
                "Plus coûteux que les implants standards",
                "Nécessite des connaissances en développement JavaCard",
                "Configuration initiale plus complexe"
            )
        )
    )

    /**
     * Trouve un profil d'implant basé sur les critères de détection
     */
    fun detectImplant(
        atqa: String?,
        sak: String?,
        chipType: String?,
        memorySize: Int?,
        techList: List<String>?
    ): ImplantProfile? {
        return profiles.firstOrNull { profile ->
            val criteria = profile.detectionCriteria

            // Vérifie ATQA
            val atqaMatch = criteria.atqa == null ||
                (atqa != null && atqa.uppercase().replace(" ", "") == criteria.atqa.replace(" ", ""))

            // Vérifie SAK
            val sakMatch = criteria.sak == null ||
                (sak != null && sak.uppercase().replace(" ", "") == criteria.sak.replace(" ", ""))

            // Vérifie le type de puce
            val chipMatch = criteria.chipTypePattern == null ||
                (chipType != null && chipType.contains(Regex(criteria.chipTypePattern, RegexOption.IGNORE_CASE)))

            // Vérifie la taille mémoire
            val memMatch = criteria.memorySize == null ||
                (memorySize != null && memorySize == criteria.memorySize)

            // Vérifie la liste des technologies
            val techMatch = criteria.techList == null ||
                (techList != null && criteria.techList.any { tech -> techList.contains(tech) })

            atqaMatch && sakMatch && chipMatch && memMatch && techMatch
        }
    }

    /**
     * Retourne tous les implants d'un type de fréquence donné
     */
    fun getImplantsByFrequency(isHighFrequency: Boolean): List<ImplantProfile> {
        return if (isHighFrequency) {
            profiles.filter { it.frequency.contains("13.56") }
        } else {
            profiles.filter { it.frequency.contains("125") }
        }
    }

    /**
     * Retourne un profil par type
     */
    fun getProfileByType(type: ImplantType): ImplantProfile? {
        return profiles.firstOrNull { it.type == type }
    }
}
