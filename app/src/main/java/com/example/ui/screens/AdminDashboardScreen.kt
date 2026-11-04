package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AbsenceRequest
import com.example.data.model.AcademicGroup
import com.example.data.model.AttendanceRecord
import com.example.data.model.AuditLog
import com.example.data.model.Invitation
import com.example.data.model.Lesson
import com.example.data.model.SyncLog
import com.example.data.repository.AttendanceRepository
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.StatusPresent
import com.example.ui.theme.StatusUnexcused
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    repository: AttendanceRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val groups by repository.observeGroups().collectAsState(initial = emptyList())
    val lessons by repository.observeLessons().collectAsState(initial = emptyList())
    val invitations by repository.observeInvitations("po-2404").collectAsState(initial = emptyList())
    val absenceRequests by repository.observeAbsenceRequests().collectAsState(initial = emptyList())
    val auditLogs by repository.observeAuditLogs().collectAsState(initial = emptyList())
    val syncLogs by repository.observeSyncLogs().collectAsState(initial = emptyList())

    val pendingAbsences = absenceRequests.filter { it.status == "PENDING" }

    var selectedTab by remember { mutableIntStateOf(0) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var reviewingRequest by remember { mutableStateOf<AbsenceRequest?>(null) }
    var reviewComment by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("admin_dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dashboard Title
        item {
            Column {
                Text(
                    text = "Панель администратора",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                Text(
                    text = "Колледж: Управление посещаемостью и синхронизация",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Metrics Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Студентов",
                    value = "${invitations.size}",
                    subtitle = "Группа ПО-2404",
                    icon = Icons.Default.Group,
                    accentColor = NavyPrimary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Занятий",
                    value = "${lessons.size}",
                    subtitle = "В базе данных",
                    icon = Icons.Default.Assignment,
                    accentColor = CobaltBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Action Toolbar (External Sync & Report Export)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Интеграция с АИС колледжа и Экспорт", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                isSyncing = true
                                syncMessage = null
                                scope.launch {
                                    val res = repository.triggerCollegeSync()
                                    isSyncing = false
                                    res.onSuccess {
                                        syncMessage = "Синхронизировано успешно (${it.recordsCount} записей)"
                                    }.onFailure {
                                        syncMessage = "Ошибка синхронизации: ${it.localizedMessage}"
                                    }
                                }
                            },
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sync_college_api_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Синхронизация", fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                exportReport(context, invitations, lessons)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_report_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Экспорт отчета", fontSize = 12.sp)
                        }
                    }

                    if (syncMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = syncMessage!!,
                            color = StatusPresent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Tabs
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = NavyPrimary,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Студенты ПО-2404 (${invitations.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Заявления (${pendingAbsences.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Аудит & Логи", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        // Tab Content
        when (selectedTab) {
            0 -> {
                // Students & Invitations
                items(invitations) { inv ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(inv.studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Билет: ${inv.studentId} • Код: ${inv.code}", fontSize = 12.sp, color = CobaltBlue)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (inv.isUsed) Color(0xFFDCFCE7) else Color(0xFFFEF3C7))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (inv.isUsed) "Активирован" else "Ожидает входа",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (inv.isUsed) StatusPresent else Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                // Absence Requests Review
                if (absenceRequests.isEmpty()) {
                    item {
                        Text(
                            text = "Нет поданных заявлений от студентов.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(absenceRequests) { req ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(req.studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${req.studentId} • Дата: ${req.date}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusBadge(status = req.status)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Причина: ${req.reason}", fontSize = 13.sp)
                                if (!req.documentUrl.isNullOrBlank()) {
                                    Text("Документ: ${req.documentUrl}", fontSize = 12.sp, color = CobaltBlue)
                                }

                                if (req.status == "PENDING") {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { reviewingRequest = req },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                                        ) {
                                            Text("Рассмотреть", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Audit Logs & Sync Logs
                item {
                    Text("Журнал интеграции с системой колледжа:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                items(syncLogs) { sLog ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Синхронизация ${sLog.syncId}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(sLog.status, color = StatusPresent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Text(sLog.details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Журнал действий (Audit Trail):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                items(auditLogs.take(15)) { aLog ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(aLog.action, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NavyPrimary)
                                Text(aLog.userName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(aLog.details, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    if (reviewingRequest != null) {
        AlertDialog(
            onDismissRequest = { reviewingRequest = null },
            title = { Text("Рассмотрение заявления: ${reviewingRequest!!.studentName}") },
            text = {
                Column {
                    Text("Причина: ${reviewingRequest!!.reason}", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        label = { Text("Комментарий деканата") },
                        placeholder = { Text("Справка проверена...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val req = reviewingRequest ?: return@Button
                        scope.launch {
                            repository.reviewAbsenceRequest(req.requestId, "APPROVED", reviewComment.ifBlank { "Подтверждено деканатом" })
                            reviewingRequest = null
                            reviewComment = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusPresent)
                ) {
                    Text("Одобрить (Уважительная)")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        val req = reviewingRequest ?: return@Button
                        scope.launch {
                            repository.reviewAbsenceRequest(req.requestId, "REJECTED", reviewComment.ifBlank { "Отклонено: нет документа" })
                            reviewingRequest = null
                            reviewComment = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusUnexcused)
                ) {
                    Text("Отклонить")
                }
            }
        )
    }
}

private fun exportReport(
    context: Context,
    invitations: List<Invitation>,
    lessons: List<Lesson>
) {
    val reportText = buildString {
        appendLine("ОТЧЕТ ПО ПОСЕЩАЕМОСТИ ГРУППЫ ПО-2404")
        appendLine("Специальность: 09.02.07 Программирование в КС")
        appendLine("Дата генерации: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}")
        appendLine("Всего студентов: ${invitations.size}")
        appendLine("Всего проведенных занятий: ${lessons.size}")
        appendLine("----------------------------------------")
        appendLine("№ | ФИО Студента | Студ.билет | Статус")
        invitations.forEachIndexed { index, inv ->
            appendLine("${index + 1} | ${inv.studentName} | ${inv.studentId} | ${if (inv.isUsed) "Активен" else "Приглашен"}")
        }
        appendLine("----------------------------------------")
        appendLine("Сформировано в АИС StudentAttendance 2.0")
    }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, reportText)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Экспорт ведомости ПО-2404"))
}
