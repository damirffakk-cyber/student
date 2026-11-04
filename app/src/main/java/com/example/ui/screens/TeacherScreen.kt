package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AcademicGroup
import com.example.data.model.AttendanceRecord
import com.example.data.model.Lesson
import com.example.data.model.Subject
import com.example.data.model.UserProfile
import com.example.data.repository.AttendanceRepository
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.StatusLate
import com.example.ui.theme.StatusPresent
import com.example.util.QrCodeGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherScreen(
    repository: AttendanceRepository,
    currentUser: UserProfile,
    modifier: Modifier = Modifier
) {
    val groups by repository.observeGroups().collectAsState(initial = emptyList())
    val subjects by repository.observeSubjects().collectAsState(initial = emptyList())
    val lessons by repository.observeLessons(teacherId = currentUser.userId).collectAsState(initial = emptyList())

    val activeLesson = lessons.firstOrNull { it.status == "ACTIVE" }
    var selectedLessonForDetails by remember { mutableStateOf<Lesson?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showSubjectManagementDialog by remember { mutableStateOf(false) }

    // If an active lesson exists and none selected, auto-select it for live monitor
    val displayedLesson = selectedLessonForDetails ?: activeLesson

    Scaffold(
        modifier = modifier.testTag("teacher_screen"),
        floatingActionButton = {
            if (activeLesson == null) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = NavyPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("create_lesson_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Создать занятие")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header stats
            item {
                Column {
                    Text(
                        text = "Панель преподавателя",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                    Text(
                        text = "Преподаватель: ${currentUser.fullName.ifBlank { currentUser.email }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Overview cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Группа",
                        value = "ПО-2404",
                        subtitle = "15 студентов",
                        icon = Icons.Default.School,
                        accentColor = NavyPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Занятий",
                        value = "${lessons.size}",
                        subtitle = if (activeLesson != null) "1 активное" else "Все завершены",
                        icon = Icons.Default.History,
                        accentColor = if (activeLesson != null) StatusPresent else CobaltBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Live Active Lesson Card with Dynamic QR Code
            if (activeLesson != null) {
                item {
                    ActiveLessonCard(
                        lesson = activeLesson,
                        repository = repository
                    )
                }
            } else if (displayedLesson != null && displayedLesson.status != "ACTIVE") {
                // Completed lesson details
                item {
                    CompletedLessonCard(
                        lesson = displayedLesson,
                        repository = repository,
                        onCloseDetails = { selectedLessonForDetails = null }
                    )
                }
            }

            // Groups & Subjects section
            item {
                Text(
                    text = "Назначенные предметы и группы",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE0E7FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, tint = NavyPrimary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Группа ПО-2404", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Специальность: 09.02.07 Программирование", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Мои предметы:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { showSubjectManagementDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Добавить")
                            }
                        }
                        if (subjects.isEmpty()) {
                            Text(
                                "Предметов пока нет. Нажмите «Добавить», чтобы создать первый.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        subjects.forEach { subject ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• ${subject.name}", fontSize = 13.sp)
                                Text(subject.code, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CobaltBlue)
                            }
                        }
                    }
                }
            }

            // Lesson history
            item {
                Text(
                    text = "История занятий",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (lessons.isEmpty()) {
                item {
                    Text(
                        text = "Занятия ещё не созданы. Нажмите «+» чтобы создать занятие с QR-кодом.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(lessons) { lesson ->
                    LessonHistoryItem(
                        lesson = lesson,
                        isSelected = displayedLesson?.lessonId == lesson.lessonId,
                        onClick = { selectedLessonForDetails = lesson }
                    )
                }
            }
        }

        if (showSubjectManagementDialog) {
            SubjectManagementDialog(
                subjects = subjects,
                repository = repository,
                onDismiss = { showSubjectManagementDialog = false }
            )
        }

        if (showCreateDialog) {
            CreateLessonDialog(
                subjects = subjects,
                groups = groups,
                teacherName = currentUser.fullName.ifBlank { "Преподаватель" },
                repository = repository,
                onDismiss = { showCreateDialog = false }
            )
        }
    }
}

@Composable
fun ActiveLessonCard(
    lesson: Lesson,
    repository: AttendanceRepository
) {
    val attendanceList by repository.observeLessonAttendance(lesson.lessonId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var secondsLeft by remember { mutableIntStateOf(45) }
    var selectedRecordForEdit by remember { mutableStateOf<AttendanceRecord?>(null) }

    // Dynamic QR countdown timer (rolling token every 45 seconds)
    LaunchedEffect(lesson.lessonId, lesson.qrToken) {
        secondsLeft = 45
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        }
        repository.refreshLessonQrToken(lesson.lessonId)
    }

    val presentCount = attendanceList.count { it.status == "PRESENT" }
    val lateCount = attendanceList.count { it.status == "LATE" }

    // Generate QR bitmap for active lesson
    val qrContent = "STUDENT_ATTENDANCE:${lesson.lessonId}:${lesson.qrToken}"
    val qrBitmap = remember(lesson.qrToken) {
        QrCodeGenerator.generateQrBitmap(qrContent, 400)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_lesson_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(StatusPresent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ИДЕТ РЕГИСТРАЦИЯ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusPresent
                    )
                }

                Button(
                    onClick = {
                        scope.launch {
                            repository.closeLessonRegistration(lesson.lessonId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("close_registration_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Завершить", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = lesson.subjectName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = NavyDark
            )
            Text(
                text = "${lesson.groupName} • ${lesson.room.ifBlank { "Ауд. 302" }} • ${lesson.startTime}–${lesson.endTime}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // QR Code display container
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(2.dp, NavyPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = "Динамический QR-код занятия",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Token & Countdown
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = CobaltBlue, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "QR обновляется через: ${secondsLeft} сек",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = CobaltBlue
                )
            }

            LinearProgressIndicator(
                progress = { (secondsLeft.toFloat() / 45f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .padding(top = 4.dp)
                    .height(3.dp),
                color = CobaltBlue
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick live counters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$presentCount", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = StatusPresent)
                    Text("Вовремя", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$lateCount", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = StatusLate)
                    Text("Опоздали", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${attendanceList.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Text("Всего в базе", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Live list of attendees
            Text(
                text = "Список отметившихся студентов (${attendanceList.size}):",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (attendanceList.isEmpty()) {
                Text(
                    text = "Ожидание сканирования студентами группы ПО-2404...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    attendanceList.forEach { record ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedRecordForEdit = record }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(record.studentName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    "${record.studentId} • ${record.markedBy}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusBadge(status = record.status, minutesLate = record.minutesLate)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Редактировать",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedRecordForEdit != null) {
        EditAttendanceDialog(
            record = selectedRecordForEdit!!,
            repository = repository,
            onDismiss = { selectedRecordForEdit = null }
        )
    }
}

@Composable
fun CompletedLessonCard(
    lesson: Lesson,
    repository: AttendanceRepository,
    onCloseDetails: () -> Unit
) {
    val attendanceList by repository.observeLessonAttendance(lesson.lessonId).collectAsState(initial = emptyList())
    var selectedRecordForEdit by remember { mutableStateOf<AttendanceRecord?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(lesson.subjectName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${lesson.groupName} • ${lesson.date} (${lesson.startTime}–${lesson.endTime})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onCloseDetails) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            Text("Итоги посещаемости (${attendanceList.size}):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))

            attendanceList.forEach { record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { selectedRecordForEdit = record }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(record.studentName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        if (!record.note.isNullOrBlank()) {
                            Text("Примечание: ${record.note}", fontSize = 11.sp, color = CobaltBlue)
                        }
                    }
                    StatusBadge(status = record.status, minutesLate = record.minutesLate)
                }
            }
        }
    }

    if (selectedRecordForEdit != null) {
        EditAttendanceDialog(
            record = selectedRecordForEdit!!,
            repository = repository,
            onDismiss = { selectedRecordForEdit = null }
        )
    }
}

@Composable
fun LessonHistoryItem(
    lesson: Lesson,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE0E7FF) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(lesson.subjectName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${lesson.groupName} • ${lesson.date} • ${lesson.startTime}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = if (lesson.status == "ACTIVE") "АКТИВНО" else "ЗАВЕРШЕНО",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (lesson.status == "ACTIVE") StatusPresent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLessonDialog(
    subjects: List<Subject>,
    groups: List<AcademicGroup>,
    teacherName: String,
    repository: AttendanceRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
    var selectedGroup by remember { mutableStateOf(groups.firstOrNull { it.groupId == "po-2404" } ?: groups.firstOrNull()) }
    var expandedSubjects by remember { mutableStateOf(false) }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val nowTimeStr = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }

    var dateInput by remember { mutableStateOf(todayStr) }
    var startTimeInput by remember { mutableStateOf("09:00") }
    var endTimeInput by remember { mutableStateOf("10:30") }
    var roomInput by remember { mutableStateOf("302") }
    var topicInput by remember { mutableStateOf("Практическая работа №4") }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Создать занятие и QR-код", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Subject picker
                ExposedDropdownMenuBox(
                    expanded = expandedSubjects,
                    onExpandedChange = { expandedSubjects = !expandedSubjects }
                ) {
                    OutlinedTextField(
                        value = selectedSubject?.name ?: "Выберите предмет",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Предмет") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubjects) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSubjects,
                        onDismissRequest = { expandedSubjects = false }
                    ) {
                        subjects.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = {
                                    selectedSubject = s
                                    expandedSubjects = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text("Дата (ГГГГ-ММ-ДД)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTimeInput,
                        onValueChange = { startTimeInput = it },
                        label = { Text("Начало") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTimeInput,
                        onValueChange = { endTimeInput = it },
                        label = { Text("Конец") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = roomInput,
                    onValueChange = { roomInput = it },
                    label = { Text("Аудитория") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = topicInput,
                    onValueChange = { topicInput = it },
                    label = { Text("Тема занятия") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val subj = selectedSubject ?: return@Button
                    val grp = selectedGroup ?: return@Button
                    isSubmitting = true
                    scope.launch {
                        repository.createLesson(
                            subjectId = subj.subjectId,
                            subjectName = subj.name,
                            groupId = grp.groupId,
                            groupName = grp.name,
                            teacherName = teacherName,
                            date = dateInput,
                            startTime = startTimeInput,
                            endTime = endTimeInput,
                            room = roomInput,
                            topic = topicInput
                        )
                        isSubmitting = false
                        onDismiss()
                    }
                },
                enabled = !isSubmitting && selectedSubject != null
            ) {
                Text("Создать и показать QR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun EditAttendanceDialog(
    record: AttendanceRecord,
    repository: AttendanceRepository,
    onDismiss: () -> Unit
) {
    var statusChoice by remember { mutableStateOf(record.status) }
    var noteInput by remember { mutableStateOf(record.note ?: "") }
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    val options = listOf(
        "PRESENT" to "Присутствовал",
        "LATE" to "Опоздал",
        "EXCUSED" to "Уважительная",
        "UNEXCUSED" to "Неуважительная"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Корректировка: ${record.studentName}") },
        text = {
            Column {
                Text("Установите статус посещаемости:", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                options.forEach { (key, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { statusChoice = key }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = statusChoice == key,
                            onClick = { statusChoice = key }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Примечание преподавателя") },
                    placeholder = { Text("Причина изменения...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSaving = true
                    scope.launch {
                        repository.updateAttendanceStatus(record.attendanceId, statusChoice, noteInput)
                        isSaving = false
                        onDismiss()
                    }
                },
                enabled = !isSaving
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
