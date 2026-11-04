package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.Subject
import com.example.data.repository.AttendanceRepository
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun SubjectManagementDialog(
    subjects: List<Subject>,
    repository: AttendanceRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var editingSubject by remember { mutableStateOf<Subject?>(null) }
    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Мои предметы") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Название предмета") },
                    singleLine = true
                )
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    if (editingSubject == null) "Добавление предмета" else "Редактирование предмета",
                    style = MaterialTheme.typography.labelMedium
                )
                Button(
                    onClick = {
                        val cleanName = name.trim()
                        if (cleanName.isEmpty()) {
                            errorMessage = "Введите название предмета"
                        } else if (subjects.any {
                                it.subjectId != editingSubject?.subjectId && it.name.trim().equals(cleanName, true)
                            }) {
                            errorMessage = "Такой предмет уже есть в списке"
                        } else {
                            isSaving = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    val current = editingSubject
                                    if (current == null) {
                                        val newId = "sub_${UUID.randomUUID().toString().take(12)}"
                                        repository.createSubject(
                                            Subject(
                                                subjectId = newId,
                                                name = cleanName,
                                                code = "SUB-${newId.takeLast(8).uppercase()}",
                                                teacherIds = emptyList(),
                                                groupIds = listOf("po-2404")
                                            )
                                        )
                                    } else {
                                        repository.updateSubject(current.copy(name = cleanName))
                                    }
                                    name = ""
                                    editingSubject = null
                                } catch (e: Exception) {
                                    errorMessage = e.localizedMessage ?: "Не удалось сохранить предмет"
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                    },
                    enabled = !isSaving && name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSaving) "Сохранение..." else if (editingSubject == null) "Добавить предмет" else "Сохранить изменения")
                }
                if (editingSubject != null) {
                    TextButton(onClick = { editingSubject = null; name = ""; errorMessage = null }) {
                        Text("Отменить редактирование")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Сохранённые предметы", style = MaterialTheme.typography.titleSmall)
                if (subjects.isEmpty()) {
                    Text("Список пока пуст", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        items(subjects, key = { it.subjectId }) { subject ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(subject.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(subject.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { editingSubject = subject; name = subject.name; errorMessage = null }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                                }
                                IconButton(onClick = { subjectToDelete = subject }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Готово") }
        }
    )

    subjectToDelete?.let { subject ->
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text("Удалить предмет?") },
            text = { Text("«${subject.name}» будет удалён из списка предметов.") },
            confirmButton = {
                TextButton(onClick = {
                    subjectToDelete = null
                    scope.launch {
                        try {
                            repository.deleteSubject(subject)
                        } catch (e: Exception) {
                            errorMessage = e.localizedMessage ?: "Не удалось удалить предмет"
                        }
                    }
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) { Text("Отмена") }
            }
        )
    }
}
