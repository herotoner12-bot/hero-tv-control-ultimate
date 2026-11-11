package com.herotoner.herotv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herotoner.herotv.ui.theme.HeroColors

/** شاشة مؤقتة صريحة: لا تعرض بيانات وهمية، وتوضح في أي مرحلة ستُبنى الصفحة. */
@Composable
fun PlaceholderScreen(
    title: String,
    phase: String,
    description: String,
    icon: ImageVector,
    onBack: (() -> Unit)?
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = HeroColors.TextPrimary)
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = HeroColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(48.dp))
        }
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(icon, contentDescription = null, tint = HeroColors.BlueBright, modifier = Modifier.size(72.dp))
                Text("قيد التطوير", color = HeroColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("ستُضاف هذه الصفحة في $phase", color = HeroColors.Cyan, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(description, color = HeroColors.TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
