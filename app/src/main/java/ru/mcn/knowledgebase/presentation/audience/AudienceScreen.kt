package ru.mcn.knowledgebase.presentation.audience

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import ru.mcn.knowledgebase.ui.theme.BrandTitle
import ru.mcn.knowledgebase.ui.theme.BrandHero
import ru.mcn.knowledgebase.ui.theme.CategoryIcon
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ru.mcn.knowledgebase.data.local.AudiencePreferences
import ru.mcn.knowledgebase.domain.model.Audience

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudienceScreen(preferences: AudiencePreferences, onChoose: (Audience) -> Unit, onBack: (() -> Unit)? = null) {
    var rememberChoice by rememberSaveable { mutableStateOf(preferences.remembered() != null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun choose(audience: Audience) {
        if (saving) return
        saving = true
        error = null
        scope.launch {
            try {
                preferences.choose(audience, rememberChoice)
                onChoose(audience)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "Не удалось сохранить выбор. Попробуйте ещё раз."
            } finally {
                saving = false
            }
        }
    }
    Scaffold(topBar = {
        TopAppBar(title = { BrandTitle() }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background), navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack, enabled = !saving) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
            }
        })
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BrandHero("Всё о связи —\nв одном месте", "Инструкции и ответы на вопросы\nоб услугах MCN Telecom")
            Spacer(Modifier.height(8.dp))
            Text("С чего начнём?", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleLarge)
            AudienceCard("Для юридических лиц", "Все разделы базы знаний", Icons.Default.Business, false, !saving) { choose(Audience.BUSINESS) }
            AudienceCard("Частным лицам", "Мобильная связь MCNmobile", Icons.Default.Smartphone, true, !saving) { choose(Audience.PRIVATE) }
            Row(
                Modifier.fillMaxWidth().toggleable(value = rememberChoice, enabled = !saving, role = Role.Checkbox, onValueChange = { rememberChoice = it }).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = rememberChoice, onCheckedChange = null, enabled = !saving)
                Spacer(Modifier.width(12.dp))
                Text("Запомнить выбор")
            }
            Text("Выбор можно изменить в меню каталога.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (saving) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun AudienceCard(title: String, description: String, icon: ImageVector, secondary: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CategoryIcon(icon, secondary = secondary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
