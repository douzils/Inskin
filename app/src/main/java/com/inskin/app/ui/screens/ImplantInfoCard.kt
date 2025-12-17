package com.inskin.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inskin.app.ImplantInfo
import com.inskin.app.implants.ImplantHealthStatus

/**
 * Carte d'information pour les implants Dangerous Things détectés
 */
@Composable
fun ImplantInfoCard(
    implant: ImplantInfo,
    healthStatus: ImplantHealthStatus = ImplantHealthStatus.Unknown,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        0.0f to getImplantColor(implant.type).copy(alpha = 0.15f),
                        0.5f to getImplantColor(implant.type).copy(alpha = 0.08f),
                        1.0f to Color.Transparent
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
            // En-tête avec icône et nom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Healing,
                        contentDescription = null,
                        tint = getImplantColor(implant.type),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = implant.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = getImplantColor(implant.type)
                        )
                        Text(
                            text = "Implant Dangerous Things",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Badge de confiance
                ConfidenceBadge(confidence = implant.confidence)
            }

            Spacer(Modifier.height(12.dp))

            // Informations principales
            InfoRow(
                icon = Icons.Filled.Sensors,
                label = "Fréquence",
                value = implant.frequency
            )

            // État de santé
            if (healthStatus != ImplantHealthStatus.Unknown) {
                Spacer(Modifier.height(8.dp))
                HealthStatusRow(status = healthStatus)
            }

            // Bouton pour étendre/réduire
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(if (expanded) "Voir moins" else "Voir plus de détails")
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
            }

            // Contenu étendu
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Cas d'usage
                    if (implant.useCases.isNotEmpty()) {
                        SectionHeader("Utilisations")
                        implant.useCases.forEach { useCase ->
                            BulletPoint(text = useCase)
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // Recommandations
                    if (implant.recommendations.isNotEmpty()) {
                        SectionHeader("Recommandations", Icons.Filled.Lightbulb, Color(0xFFFFC107))
                        implant.recommendations.forEach { recommendation ->
                            RecommendationChip(text = recommendation)
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // Avertissements
                    if (implant.warnings.isNotEmpty()) {
                        SectionHeader("Avertissements", Icons.Filled.Warning, Color(0xFFFF5722))
                        implant.warnings.forEach { warning ->
                            WarningChip(text = warning)
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun ConfidenceBadge(confidence: Float) {
    val (color, text) = when {
        confidence >= 0.9f -> Color(0xFF4CAF50) to "Certain"
        confidence >= 0.7f -> Color(0xFF8BC34A) to "Probable"
        confidence >= 0.5f -> Color(0xFFFFC107) to "Possible"
        else -> Color(0xFFFF5722) to "Incertain"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.25f),
                        color.copy(alpha = 0.15f)
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$label: ",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class HealthStatusData(
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val text: String,
    val description: String
)

@Composable
private fun HealthStatusRow(status: ImplantHealthStatus) {
    val statusData = when (status) {
        ImplantHealthStatus.Excellent -> HealthStatusData(
            Color(0xFF4CAF50),
            Icons.Filled.CheckCircle,
            "Excellent",
            "< 10k écritures"
        )
        ImplantHealthStatus.Good -> HealthStatusData(
            Color(0xFF8BC34A),
            Icons.Filled.CheckCircle,
            "Bon",
            "10k-50k écritures"
        )
        ImplantHealthStatus.Fair -> HealthStatusData(
            Color(0xFFFFC107),
            Icons.Filled.Info,
            "Correct",
            "50k-80k écritures"
        )
        ImplantHealthStatus.Warning -> HealthStatusData(
            Color(0xFFFF9800),
            Icons.Filled.Warning,
            "Attention",
            "80k-100k écritures"
        )
        ImplantHealthStatus.Critical -> HealthStatusData(
            Color(0xFFFF5722),
            Icons.Filled.Error,
            "Critique",
            "> 100k écritures"
        )
        ImplantHealthStatus.Unknown -> HealthStatusData(
            Color.Gray,
            Icons.Filled.Help,
            "Inconnu",
            "Non détectable"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        statusData.color.copy(alpha = 0.15f),
                        statusData.color.copy(alpha = 0.05f)
                    )
                )
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = statusData.icon,
            contentDescription = null,
            tint = statusData.color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "État de santé: ${statusData.text}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = statusData.color
            )
            Text(
                text = statusData.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Info,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "•",
            modifier = Modifier.padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RecommendationChip(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = Color(0xFFFFC107).copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.TipsAndUpdates,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun WarningChip(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = Color(0xFFFF5722).copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = Color(0xFFFF5722),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun getImplantColor(type: String): Color {
    return when (type.uppercase()) {
        "XNT", "FLEX_NT" -> Color(0xFF4CAF50)
        "XM1", "FLEX_M1" -> Color(0xFF2196F3)
        "XEM", "FLEX_EM" -> Color(0xFFFF9800)
        "XAC" -> Color(0xFF9C27B0)
        "NEXT" -> Color(0xFF00BCD4)
        "VIVOKEY" -> Color(0xFFE91E63)
        "SPARK" -> Color(0xFFFFEB3B)
        else -> Color(0xFF81C784)
    }
}
