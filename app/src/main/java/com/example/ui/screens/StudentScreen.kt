package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AbsenceRequest
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceSummary
import com.example.data.model.Lesson
import com.example.data.model.UserProfile
import com.example.data.repository.AttendanceRepository
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.StatusExcused
import com.example.ui.theme.StatusLate
import com.example.ui.theme.StatusPresent
import com.example.ui.theme.StatusUnexcused
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StudentScreen(
    repository: AttendanceRepository,
    currentUser: UserProfile,
    modifier: Modifier = Modifier
) {
    val groupId = currentUser.groupId ?: "po-2404"
    val studentAttendance by repository.observeStudentAttendance(currentUser.userId).collectAsState(initial = emptyList())
    val activeLessons by repository.observeLessons(groupId = groupId).collectAsState(initial = emptyList())
    val absenceRequests by repository.observeAbsenceRequests(studentUid = currentUser.userId).collectAsState(initial = emptyList())

    val currentActiveLesson = activeLessons.firstOrNull { it.status == "ACTIVE" }

    var qrCodeInput by remember { mutableStateOf("") }
    var registrationResult by remember { mutableStateOf<String?>(null) }
    var registrationError by remember { mutableStateOf<String?>(null) }
    var isRegistering by remember { mutableStateOf(false) }
    var showAbsenceDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Calculate semester statistics
    val attendedCount = studentAttendance.count { it.status == "PRESENT" || it.status == "LATE" }
    val lateCount = studentAttendance.count { it.status == "LATE" }
    val excusedCount = studentAttendance.count { it.status == "EXCUSED" }
    val unexcusedCount = studentAttendance.count { it.status == "UNEXCUSED" }
    val totalRecords = studentAttendance.size
    val attendancePercentage = if (totalRecords > 0) (attendedCount.toFloat() / totalRecords) * 100f else 100f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("student_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Student Info Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = currentUser.fullName.ifBlank { "Студент" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Группа: ПО-2404 • Билет: ${currentUser.studentId ?: "ST-240401"}",
                                fontSize = 13.sp,
                                color = Color(0xFFE0E7FF)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Студент",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Progress Overview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Посещаемость за семестр:",
                                fontSize = 12.sp,
                                color = Color(0xFFC7D2FE)
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f%%", attendancePercentage),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            MiniStat(label = "Посетил", value = "$attendedCount", color = Color(0xFF86EFAC))
                            MiniStat(label = "Опоздал", value = "$lateCount", color = Color(0xFFFDE047))
                            MiniStat(label = "Пропуск", value = "${excusedCount + unexcusedCount}", color = Color(0xFFFCA5A5))
                        }
                    }
                }
            }
        }

        // Active Lesson Notification & Quick Registration
        if (currentActiveLesson != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, StatusPresent.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(StatusPresent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "СЕЙЧАС ИДЕТ ЗАНЯТИЕ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusPresent
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentActiveLesson.subjectName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                        Text(
                            text = "${currentActiveLesson.room} • Преподаватель: ${currentActiveLesson.teacherName} • ${currentActiveLesson.startTime}–${currentActiveLesson.endTime}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val alreadyRegistered = studentAttendance.any { it.lessonId == currentActiveLesson.lessonId }

                        if (alreadyRegistered) {
                            val record = studentAttendance.first { it.lessonId == currentActiveLesson.lessonId }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusPresent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Вы уже отмечены на этом занятии: ", fontSize = 13.sp)
                                StatusBadge(status = record.status, minutesLate = record.minutesLate)
                            }
                        } else {
                            Button(
                                onClick = {
                                    isRegistering = true
                                    registrationError = null
                                    registrationResult = null
                                    scope.launch {
                                        val res = repository.registerAttendanceWithQr(
                                            lessonId = currentActiveLesson.lessonId,
                                            providedToken = currentActiveLesson.qrToken,
                                            studentName = currentUser.fullName.ifBlank { "Студент" },
                                            studentId = currentUser.studentId ?: "ST-240401",
                                            groupId = groupId
                                        )
                                        isRegistering = false
                                        res.onSuccess {
                                            registrationResult = "Успешно! Статус: ${it.status} (+${it.minutesLate} мин)"
                                        }.onFailure {
                                            registrationError = it.localizedMessage ?: "Ошибка отметки"
                                        }
                                    }
                                },
                                enabled = !isRegistering,
                                colors = ButtonDefaults.buttonColors(containerColor = StatusPresent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("one_click_register_button")
                            ) {
                                if (isRegistering) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Отметиться на текущем занятии")
                                }
                            }
                        }
                    }
                }
            }
        }

        // QR Code or Token Manual Input
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ввод QR-кода занятия",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Отсканируйте камерой или введите токен занятия (например, QRT-ABC12345):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = qrCodeInput,
                            onValueChange = { qrCodeInput = it.trim() },
                            placeholder = { Text("QRT-...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("qr_token_input")
                        )

                        Button(
                            onClick = {
                                val token = qrCodeInput.trim()
                                if (token.isBlank()) {
                                    registrationError = "Введите токен или отсканируйте QR"
                                    return@Button
                                }
                                val targetLesson = activeLessons.firstOrNull { it.qrToken == token }
                                    ?: currentActiveLesson

                                if (targetLesson == null) {
                                    registrationError = "Занятие с данным токеном не найдено"
                                    return@Button
                                }

                                isRegistering = true
                                registrationError = null
                                registrationResult = null
                                scope.launch {
                                    val res = repository.registerAttendanceWithQr(
                                        lessonId = targetLesson.lessonId,
                                        providedToken = token,
                                        studentName = currentUser.fullName.ifBlank { "Студент" },
                                        studentId = currentUser.studentId ?: "ST-240401",
                                        groupId = groupId
                                    )
                                    isRegistering = false
                                    res.onSuccess {
                                        registrationResult = "Вы успешно отмечены! Статус: ${it.status}"
                                        qrCodeInput = ""
                                    }.onFailure {
                                        registrationError = it.localizedMessage ?: "Ошибка регистрации"
                                    }
                                }
                            },
                            enabled = !isRegistering,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                            modifier = Modifier.testTag("submit_token_button")
                        ) {
                            Text("Отметить")
                        }
                    }

                    if (registrationResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = registrationResult!!,
                            color = StatusPresent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (registrationError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = registrationError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Absence Petitions Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Заявления о пропуске",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = { showAbsenceDialog = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Подать заявление", fontSize = 12.sp)
                }
            }
        }

        if (absenceRequests.isNotEmpty()) {
            items(absenceRequests) { req ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(req.date, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            StatusBadge(status = req.status)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Причина: ${req.reason}", fontSize = 13.sp)
                        if (!req.reviewComment.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Ответ деканата: ${req.reviewComment}",
                                fontSize = 12.sp,
                                color = if (req.status == "APPROVED") StatusPresent else StatusUnexcused,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Attendance History
        item {
            Text(
                text = "История моих занятий (${studentAttendance.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (studentAttendance.isEmpty()) {
            item {
                Text(
                    text = "У вас пока нет записей о посещениях.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(studentAttendance) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val timeStr = record.registeredAt?.toDate()?.let {
                                SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(it)
                            } ?: "Не указано"
                            Text("Занятие ID: ${record.lessonId.take(12)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Регистрация: $timeStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!record.note.isNullOrBlank()) {
                                Text("Заметка: ${record.note}", fontSize = 11.sp, color = CobaltBlue)
                            }
                        }
                        StatusBadge(status = record.status, minutesLate = record.minutesLate)
                    }
                }
            }
        }
    }

    if (showAbsenceDialog) {
        SubmitAbsenceDialog(
            currentUser = currentUser,
            repository = repository,
            onDismiss = { showAbsenceDialog = false }
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 11.sp, color = Color(0xFFC7D2FE))
    }
}

@Composable
fun SubmitAbsenceDialog(
    currentUser: UserProfile,
    repository: AttendanceRepository,
    onDismiss: () -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var dateInput by remember { mutableStateOf(today) }
    var reasonInput by remember { mutableStateOf("По состоянию здоровья (справка от врача)") }
    var documentInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Заявление о пропуске занятий") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text("Дата отсутствия (ГГГГ-ММ-ДД)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    label = { Text("Уважительная причина") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = documentInput,
                    onValueChange = { documentInput = it },
                    label = { Text("Номер справки / документа") },
                    placeholder = { Text("Справка №482 от поликлиники №2") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reasonInput.isBlank()) return@Button
                    isSubmitting = true
                    scope.launch {
                        repository.submitAbsenceRequest(
                            studentName = currentUser.fullName.ifBlank { "Студент" },
                            studentId = currentUser.studentId ?: "ST-240401",
                            groupId = currentUser.groupId ?: "po-2404",
                            lessonId = null,
                            subjectName = null,
                            date = dateInput,
                            reason = reasonInput,
                            documentUrl = documentInput.ifBlank { null }
                        )
                        isSubmitting = false
                        onDismiss()
                    }
                },
                enabled = !isSubmitting
            ) {
                Text("Отправить в деканат")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
