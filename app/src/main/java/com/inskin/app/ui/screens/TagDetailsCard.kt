package com.inskin.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inskin.app.R
import com.inskin.app.TagInfo
import com.inskin.app.ui.components.BigCircle
import com.inskin.app.ui.theme.DarkCircle
import com.inskin.app.ui.theme.UiDims

@Composable
fun TagDetailsCard(info: TagInfo) {
    val title = info.name
    val uid   = info.uid
    val used  = 0
    val total = 0
    val progress = 0f

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Black, color = DarkCircle)
        Spacer(Modifier.height(6.dp))
        Text(uid, style = MaterialTheme.typography.titleMedium, color = DarkCircle)
        Spacer(Modifier.height(12.dp))

        Box(
            Modifier.fillMaxWidth().offset(y = UiDims.HomeCenterY - UiDims.TagCircleSize / 2),
            contentAlignment = Alignment.Center
        ) {
            BigCircle(size = UiDims.TagCircleSize) {
                Image(
                    painter = painterResource(R.drawable.antenna_tag),
                    contentDescription = null,
                    modifier = Modifier.size(UiDims.TagCircleSize * 0.72f)
                )
            }
        }

        Spacer(Modifier.height(UiDims.HomeCenterY * 0.35f))
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = DarkCircle)
        Spacer(Modifier.height(10.dp))

        Box(Modifier.fillMaxWidth(0.9f).height(14.dp).clip(RoundedCornerShape(8.dp))) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF36D27F),
                trackColor = Color(0xFF5A5A5A)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("$used / $total bytes", style = MaterialTheme.typography.titleMedium, color = DarkCircle)
    }
}
