package com.inskin.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inskin.app.ui.PasswordUtils
import com.inskin.app.ui.theme.AccentColor
import com.inskin.app.ui.theme.MonochromeColors

@Composable
fun LockScreen(
    passwordHash: String,
    accentColor: AccentColor = AccentColor.ELECTRIC_BLUE,
    onUnlock: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var attempts by remember { mutableStateOf(0) }

    // Animation de pulsation pour l'icône
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Animation de shake en cas d'erreur
    val shakeOffset by animateFloatAsState(
        targetValue = if (isError) 1f else 0f,
        animationSpec = tween(50),
        label = "shake"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MonochromeColors.almostBlack,
                        MonochromeColors.darkGray1
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .offset(x = (shakeOffset * 10).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Icône de verrouillage
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.color.copy(alpha = 0.3f),
                                accentColor.color.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = accentColor.color,
                    modifier = Modifier.size(56.dp)
                )
            }

            // Titre
            Text(
                text = "Inskin NFC",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MonochromeColors.white,
                letterSpacing = 2.sp
            )

            Text(
                text = "Entrez votre mot de passe",
                fontSize = 16.sp,
                color = MonochromeColors.lightGray2,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // Champ mot de passe
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    isError = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mot de passe") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Masquer" else "Afficher",
                            tint = accentColor.color
                        )
                    }
                },
                isError = isError,
                supportingText = if (isError) {
                    { Text("Mot de passe incorrect (${attempts}/3)", color = MaterialTheme.colorScheme.error) }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor.color,
                    focusedLabelColor = accentColor.color,
                    cursorColor = accentColor.color,
                    unfocusedBorderColor = MonochromeColors.gray2,
                    unfocusedLabelColor = MonochromeColors.lightGray1
                ),
                shape = RoundedCornerShape(16.dp)
            )

            // Bouton déverrouiller
            Button(
                onClick = {
                    if (PasswordUtils.verifyPassword(password, passwordHash)) {
                        onUnlock()
                    } else {
                        isError = true
                        attempts++
                        password = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor.color,
                    contentColor = MonochromeColors.black
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = password.isNotEmpty()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.LockOpen, null)
                    Text(
                        "Déverrouiller",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Aide
            if (attempts >= 3) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Trop de tentatives échouées. Contactez l'administrateur.",
                            fontSize = 13.sp,
                            color = MonochromeColors.lightGray2
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SetPasswordScreen(
    accentColor: AccentColor = AccentColor.ELECTRIC_BLUE,
    onPasswordSet: (String) -> Unit,
    onCancel: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    val passwordsMatch = password == confirmPassword && password.isNotEmpty()
    val passwordStrong = password.length >= 4

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MonochromeColors.almostBlack,
                        MonochromeColors.darkGray1
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = accentColor.color,
                modifier = Modifier.size(80.dp)
            )

            Text(
                text = "Définir un mot de passe",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MonochromeColors.white
            )

            Text(
                text = "Protégez votre application avec un mot de passe",
                fontSize = 14.sp,
                color = MonochromeColors.lightGray2,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // Nouveau mot de passe
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nouveau mot de passe") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = accentColor.color
                        )
                    }
                },
                supportingText = {
                    Text(
                        "Minimum 4 caractères",
                        color = if (passwordStrong) accentColor.color else MonochromeColors.gray2
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor.color,
                    focusedLabelColor = accentColor.color,
                    cursorColor = accentColor.color,
                    unfocusedBorderColor = MonochromeColors.gray2
                ),
                shape = RoundedCornerShape(16.dp)
            )

            // Confirmer mot de passe
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    isError = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Confirmer le mot de passe") },
                visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { confirmVisible = !confirmVisible }) {
                        Icon(
                            imageVector = if (confirmVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = accentColor.color
                        )
                    }
                },
                isError = isError,
                supportingText = if (isError) {
                    { Text("Les mots de passe ne correspondent pas", color = MaterialTheme.colorScheme.error) }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor.color,
                    focusedLabelColor = accentColor.color,
                    cursorColor = accentColor.color,
                    unfocusedBorderColor = MonochromeColors.gray2
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(8.dp))

            // Boutons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MonochromeColors.lightGray2
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Annuler")
                }

                Button(
                    onClick = {
                        if (passwordsMatch && passwordStrong) {
                            onPasswordSet(PasswordUtils.hashPassword(password))
                        } else {
                            isError = true
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.color,
                        contentColor = MonochromeColors.black
                    ),
                    enabled = passwordsMatch && passwordStrong,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Définir", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
