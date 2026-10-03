package ru.mcn.knowledgebase.presentation.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import ru.mcn.knowledgebase.data.local.OfflineStatus

@Composable
internal fun OfflineDownloadPanel(status: OfflineStatus, onCancel: () -> Unit, onRetry: () -> Unit,
    onDismiss: () -> Unit, enabled: Boolean) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(status.title, style = MaterialTheme.typography.titleSmall)
            Text(when {
                status.complete -> "Готово: все статьи и изображения сохранены"
                status.state == WorkInfo.State.CANCELLED -> "Загрузка отменена. Готовые файлы сохранены."
                status.state == WorkInfo.State.FAILED -> "Загрузка завершена не полностью. Повторите, чтобы докачать недостающие файлы."
                status.state == WorkInfo.State.RUNNING -> "Загрузка для чтения офлайн"
                else -> "Ожидание сети или запуска Android"
            }, style = MaterialTheme.typography.bodyMedium)
            Text("Сохранено статей: ${status.progress.saved} из ${status.total}" +
                if (status.progress.failed > 0) " · С ошибкой: ${status.progress.failed}" else "",
                style = MaterialTheme.typography.bodySmall)
            if (status.progress.failed > 0) Text(
                "У этих статей не загрузился текст или часть изображений. " +
                    if (status.active) "После завершения можно нажать «Докачать»." else "«Докачать» повторит попытку для недостающих файлов.",
                style = MaterialTheme.typography.bodySmall)
            if (status.active) {
                LinearProgressIndicator(progress = {
                    if (status.total == 0) 0f else status.progress.saved.toFloat() / status.total
                }, modifier = Modifier.fillMaxWidth())
                if (status.currentArticle.isNotBlank()) Text(status.currentArticle, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                TextButton(onClick = onCancel, enabled = enabled) { Text("Отменить загрузку") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!status.complete) TextButton(onClick = onRetry, enabled = enabled) { Text("Докачать") }
                    TextButton(onClick = onDismiss) { Text("Скрыть") }
                }
            }
        }
    }
}
