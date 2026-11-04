package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AcademicGroup
import com.example.data.model.AttendanceRecord
import com.example.data.model.Invitation
import com.example.data.model.Lesson
import com.example.data.model.Subject
import com.example.data.repository.AttendanceRepository
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.StatusExcused
import com.example.ui.theme.StatusLate
import com.example.ui.theme.StatusPresent
import com.example.ui.theme.StatusUnexcused

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    repository: AttendanceRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val groups by repository.observeGroups().collectAsState(initial = emptyList())
    val subjects by repository.observeSubjects().collectAsState(initial = emptyList())
    val lessons by repository.observeLessons(groupId = "po-2404").collectAsState(initial = emptyList())
    val roster by repository.observeInvitations("po-2404").collectAsState(initial = emptyList())
    val allAttendance by repository.observeGroupAttendance("po-2404").collectAsState(initial = emptyList())

    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    var expandedSubjectMenu by remember { mutableStateOf(false) }

    val filteredLessons = remember(lessons, selectedSubjectId) {
        if (selectedSubjectId == null) lessons else lessons.filter { it.subjectId == selectedSubjectId }
    }

    val distinctDates = remember(filteredLessons) {
        filteredLessons.map { it.date }.distinct().sorted()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("journal_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Электронный журнал",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                Text(
                    text = "Группа ПО-2404 • Ведомость учета посещаемости",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = {
                    exportJournalCsv(context, roster, distinctDates, filteredLessons, allAttendance)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Экспорт CSV", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filters bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = null, tint = CobaltBlue)

                ExposedDropdownMenuBox(
                    expanded = expandedSubjectMenu,
                    onExpandedChange = { expandedSubjectMenu = !expandedSubjectMenu },
                    modifier = Modifier.weight(1f)
                ) {
                    val currentSubjectName = subjects.firstOrNull { it.subjectId == selectedSubjectId }?.name ?: "Все дисциплины"
                    OutlinedTextField(
                        value = currentSubjectName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Фильтр по предмету") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubjectMenu) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSubjectMenu,
                        onDismissRequest = { expandedSubjectMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Все дисциплины") },
                            onClick = {
                                selectedSubjectId = null
                                expandedSubjectMenu = false
                            }
                        )
                        subjects.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = {
                                    selectedSubjectId = s.subjectId
                                    expandedSubjectMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LegendItem(symbol = "+", label = "Был", color = StatusPresent)
            LegendItem(symbol = "О", label = "Опоздал", color = StatusLate)
            LegendItem(symbol = "У", label = "Уважительная", color = StatusExcused)
            LegendItem(symbol = "Н", label = "Пропуск", color = StatusUnexcused)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Matrix Table
        val horizontalScrollState = rememberScrollState()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Table header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9))
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Студент",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NavyDark,
                        modifier = Modifier.width(130.dp)
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        if (distinctDates.isEmpty()) {
                            Text(
                                text = "Нет данных занятий",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            distinctDates.forEach { dateStr ->
                                val shortDate = dateStr.takeLast(5).replace("-", ".")
                                Text(
                                    text = shortDate,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(44.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(42.dp)
                    )
                }

                HorizontalDivider()

                // Roster rows
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(roster) { student ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = student.studentName.take(15),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(130.dp)
                            )

                            // Status cells per date
                            var studentAttended = 0
                            var studentTotal = 0

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(horizontalScrollState)
                            ) {
                                distinctDates.forEach { date ->
                                    val lessonOnDate = filteredLessons.firstOrNull { it.date == date }
                                    val attRecord = allAttendance.firstOrNull {
                                        it.studentId == student.studentId &&
                                                (lessonOnDate != null && it.lessonId == lessonOnDate.lessonId)
                                    }

                                    studentTotal += 1
                                    val (symbol, color) = when (attRecord?.status) {
                                        "PRESENT" -> {
                                            studentAttended += 1
                                            "+" to StatusPresent
                                        }
                                        "LATE" -> {
                                            studentAttended += 1
                                            "О" to StatusLate
                                        }
                                        "EXCUSED" -> {
                                            "У" to StatusExcused
                                        }
                                        "UNEXCUSED" -> {
                                            "Н" to StatusUnexcused
                                        }
                                        else -> {
                                            "·" to Color(0xFFCBD5E1)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(44.dp)
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = symbol,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            color = color
                                        )
                                    }
                                }
                            }

                            val pct = if (studentTotal > 0) (studentAttended.toFloat() / studentTotal) * 100f else 100f
                            Text(
                                text = "${pct.toInt()}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = if (pct >= 80f) StatusPresent else StatusLate,
                                modifier = Modifier.width(42.dp)
                            )
                        }
                        HorizontalDivider(color = BorderSubtle)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(symbol: String, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = symbol, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun exportJournalCsv(
    context: Context,
    roster: List<Invitation>,
    dates: List<String>,
    lessons: List<Lesson>,
    attendance: List<AttendanceRecord>
) {
    val csv = buildString {
        append("Студент,Билет")
        dates.forEach { d -> append(",$d") }
        appendLine(",Итого%")

        roster.forEach { student ->
            append("${student.studentName},${student.studentId}")
            var attended = 0
            dates.forEach { d ->
                val lesson = lessons.firstOrNull { it.date == d }
                val rec = attendance.firstOrNull { it.studentId == student.studentId && lesson != null && it.lessonId == lesson.lessonId }
                val sym = when (rec?.status) {
                    "PRESENT" -> { attended++; "+" }
                    "LATE" -> { attended++; "О" }
                    "EXCUSED" -> "У"
                    "UNEXCUSED" -> "Н"
                    else -> "·"
                }
                append(",$sym")
            }
            val pct = if (dates.isNotEmpty()) (attended.toFloat() / dates.size) * 100f else 100f
            appendLine(",${pct.toInt()}%")
        }
    }

    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, csv)
        type = "text/csv"
    }
    context.startActivity(Intent.createChooser(intent, "Экспорт ведомости журнала ПО-2404 (CSV)"))
}
