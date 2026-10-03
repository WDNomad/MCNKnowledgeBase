package ru.mcn.knowledgebase.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun BrandTitle() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small) {
            Text("MCN", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), style = MaterialTheme.typography.titleMedium)
        }
        Text("База знаний", style = MaterialTheme.typography.titleMedium)
    }
}
@Composable
fun BrandHero(title: String, description: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = McnDeepBlue) {
        Column(
            Modifier.background(Brush.linearGradient(listOf(McnDeepBlue, McnBlue, McnTeal))).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = Color.White)
        }
    }
}
@Composable
fun CategoryIcon(icon: ImageVector, modifier: Modifier = Modifier, secondary: Boolean = false) {
    Surface(
        modifier = modifier.size(44.dp), shape = MaterialTheme.shapes.small,
        color = if (secondary) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (secondary) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp)) }
    }
}
