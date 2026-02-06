package com.inskin.app.badges

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

/**
 * Badge NFC sauvegardé
 */
data class SavedBadge(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val uid: String,
    val data: ByteArray,
    val type: String, // NTAG216, Mifare, etc.
    val color: Color = Color(0xFF00D9FF),
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long? = null,
    val category: BadgeCategory = BadgeCategory.OTHER
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SavedBadge

        if (id != other.id) return false
        if (uid != other.uid) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + uid.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * Catégories de badges
 */
enum class BadgeCategory(val displayName: String) {
    WORK("Travail"),
    HOME("Maison"),
    TRANSPORT("Transport"),
    ACCESS("Accès"),
    IMPLANT("Implant"),
    OTHER("Autre")
}

/**
 * Gestionnaire de sauvegarde/chargement de badges
 */
class BadgeManager(private val context: Context) {

    private val badgesDir = File(context.filesDir, "badges")

    init {
        if (!badgesDir.exists()) {
            badgesDir.mkdirs()
        }
    }

    /**
     * Sauvegarde un badge
     */
    suspend fun saveBadge(badge: SavedBadge): Result<String> = withContext(Dispatchers.IO) {
        try {
            val badgeFile = File(badgesDir, "${badge.id}.badge")
            val metadataFile = File(badgesDir, "${badge.id}.json")

            // Sauvegarder les données binaires
            FileOutputStream(badgeFile).use { output ->
                output.write(badge.data)
            }

            // Sauvegarder les métadonnées
            val metadata = """
                {
                    "id": "${badge.id}",
                    "name": "${badge.name}",
                    "uid": "${badge.uid}",
                    "type": "${badge.type}",
                    "notes": "${badge.notes}",
                    "createdAt": ${badge.createdAt},
                    "category": "${badge.category.name}"
                }
            """.trimIndent()

            FileOutputStream(metadataFile).use { output ->
                output.write(metadata.toByteArray())
            }

            Result.success(badge.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Charge un badge
     */
    suspend fun loadBadge(id: String): Result<SavedBadge> = withContext(Dispatchers.IO) {
        try {
            val badgeFile = File(badgesDir, "$id.badge")
            val metadataFile = File(badgesDir, "$id.json")

            if (!badgeFile.exists() || !metadataFile.exists()) {
                return@withContext Result.failure(Exception("Badge non trouvé"))
            }

            // Charger les données
            val data = FileInputStream(badgeFile).use { input ->
                input.readBytes()
            }

            // Charger les métadonnées (parsing simple)
            val metadata = FileInputStream(metadataFile).use { input ->
                String(input.readBytes())
            }

            // Parser basique (en production, utiliser Gson/Kotlinx.serialization)
            val name = metadata.substringAfter("\"name\": \"").substringBefore("\"")
            val uid = metadata.substringAfter("\"uid\": \"").substringBefore("\"")
            val type = metadata.substringAfter("\"type\": \"").substringBefore("\"")

            val badge = SavedBadge(
                id = id,
                name = name,
                uid = uid,
                data = data,
                type = type
            )

            Result.success(badge)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Liste tous les badges sauvegardés
     */
    suspend fun listBadges(): List<String> = withContext(Dispatchers.IO) {
        badgesDir.listFiles { file ->
            file.extension == "badge"
        }?.map { file ->
            file.nameWithoutExtension
        } ?: emptyList()
    }

    /**
     * Supprime un badge
     */
    suspend fun deleteBadge(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val badgeFile = File(badgesDir, "$id.badge")
            val metadataFile = File(badgesDir, "$id.json")

            badgeFile.delete()
            metadataFile.delete()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exporte un badge vers un fichier externe
     */
    suspend fun exportBadge(badge: SavedBadge, destinationPath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val destFile = File(destinationPath)
                FileOutputStream(destFile).use { output ->
                    output.write(badge.data)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Importe un badge depuis un fichier externe
     */
    suspend fun importBadge(
        sourcePath: String,
        name: String,
        uid: String,
        type: String
    ): Result<SavedBadge> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(sourcePath)
            val data = FileInputStream(sourceFile).use { input ->
                input.readBytes()
            }

            val badge = SavedBadge(
                name = name,
                uid = uid,
                data = data,
                type = type
            )

            saveBadge(badge)

            Result.success(badge)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clone un badge existant
     */
    suspend fun cloneBadge(badge: SavedBadge, newName: String): Result<SavedBadge> {
        val cloned = badge.copy(
            id = UUID.randomUUID().toString(),
            name = newName,
            createdAt = System.currentTimeMillis()
        )
        return saveBadge(cloned).map { cloned }
    }
}
