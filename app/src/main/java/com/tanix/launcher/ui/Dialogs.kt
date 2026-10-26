package com.tanix.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tanix.launcher.data.AppItem
import com.tanix.launcher.data.City
import com.tanix.launcher.data.Repo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DialogCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.widthIn(max = 520.dp).clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(24.dp),
        content = content
    )
}

@Composable
fun DialogTitle(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(14.dp))
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        DialogCard {
            DialogTitle(title)
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip("Отмена", onClick = onDismiss)
                Chip(confirm, selected = true, onClick = onConfirm)
            }
        }
    }
}

@Composable
fun TextInputDialog(title: String, hint: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(150); runCatching { fr.requestFocus() } }
    Dialog(onDismissRequest = onDismiss) {
        DialogCard {
            DialogTitle(title)
            OutlinedTextField(
                value = text, onValueChange = { text = it }, singleLine = true, label = { Text(hint) },
                modifier = Modifier.fillMaxWidth().focusRequester(fr),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave(text) })
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip("Отмена", onClick = onDismiss)
                Chip("Сохранить", selected = true) { onSave(text) }
            }
        }
    }
}

@Composable
fun AppListDialog(
    title: String, apps: List<AppItem>, checked: Set<String>? = null,
    onClick: (AppItem) -> Unit, onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss) {
        DialogCard {
            DialogTitle(title)
            LazyColumn(Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(apps, key = { it.pkg }) { app ->
                    FocusBox(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), 1.01f, { onClick(app) }) { f ->
                        Row(
                            Modifier.fillMaxWidth().background(if (f) cs.primaryContainer else cs.surfaceContainer).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(app, 36.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(app.label, Modifier.weight(1f), color = if (f) cs.onPrimaryContainer else cs.onSurface)
                            if (checked != null) Switch(checked = app.pkg in checked, onCheckedChange = null)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Chip("Закрыть", onClick = onDismiss)
        }
    }
}

enum class AppAction { OPEN, MOVE, FAVORITE, HIDE, INFO, UNINSTALL }

/** Контекстное меню приложения (долгое нажатие OK). */
@Composable
fun AppMenuDialog(app: AppItem, isFav: Boolean, onAction: (AppAction) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        DialogCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app, 44.dp)
                Spacer(Modifier.width(14.dp))
                Text(app.label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(12.dp))
            SettingRow("Открыть", onClick = { onAction(AppAction.OPEN) })
            SettingRow("Переместить", "Стрелками на пульте, OK — готово", onClick = { onAction(AppAction.MOVE) })
            SettingRow(if (isFav) "Убрать из избранного" else "В избранное", onClick = { onAction(AppAction.FAVORITE) })
            SettingRow("Скрыть с главного экрана", onClick = { onAction(AppAction.HIDE) })
            SettingRow("О приложении", onClick = { onAction(AppAction.INFO) })
            SettingRow("Удалить", onClick = { onAction(AppAction.UNINSTALL) })
        }
    }
}

@Composable
fun CityDialog(onDismiss: () -> Unit, onPick: (City) -> Unit) {
    val cs = MaterialTheme.colorScheme
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<City>()) }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(150); runCatching { fr.requestFocus() } }

    fun search() {
        scope.launch {
            msg = "Ищем…"
            results = withContext(Dispatchers.IO) { runCatching { Repo.searchCity(q) }.getOrDefault(emptyList()) }
            msg = if (results.isEmpty()) "Ничего не найдено" else ""
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        DialogCard {
            DialogTitle("Город для погоды")
            OutlinedTextField(
                value = q, onValueChange = { q = it }, singleLine = true, label = { Text("Название города") },
                modifier = Modifier.fillMaxWidth().focusRequester(fr),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search() })
            )
            Spacer(Modifier.height(10.dp))
            Chip("Найти", selected = true) { search() }
            if (msg.isNotEmpty()) Text(msg, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(8.dp))
            results.forEach { c -> SettingRow(c.name, c.region, onClick = { onPick(c) }) }
            Spacer(Modifier.height(10.dp))
            Chip("Закрыть", onClick = onDismiss)
        }
    }
}
