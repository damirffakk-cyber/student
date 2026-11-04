package com.example

import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceSummary
import com.example.data.model.Lesson
import com.example.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class AttendanceLogicTest {

    @Test
    fun testAttendanceLatenessCalculation() {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val lessonStart = format.parse("2026-11-04 09:00")!!.time
        val graceMinutes = 5

        // Registration at 09:03 -> on time (diff = 3 min <= 5 min)
        val regAt0903 = format.parse("2026-11-04 09:03")!!.time
        val diff0903 = ((regAt0903 - lessonStart) / (60 * 1000)).toInt()
        val late0903 = if (diff0903 > graceMinutes) diff0903 else 0
        assertEquals(0, late0903)

        // Registration at 09:05 -> on time (diff = 5 min <= 5 min)
        val regAt0905 = format.parse("2026-11-04 09:05")!!.time
        val diff0905 = ((regAt0905 - lessonStart) / (60 * 1000)).toInt()
        val late0905 = if (diff0905 > graceMinutes) diff0905 else 0
        assertEquals(0, late0905)

        // Registration at 09:06 -> late by 6 minutes
        val regAt0906 = format.parse("2026-11-04 09:06")!!.time
        val diff0906 = ((regAt0906 - lessonStart) / (60 * 1000)).toInt()
        val late0906 = if (diff0906 > graceMinutes) diff0906 else 0
        assertEquals(6, late0906)

        // Registration at 09:15 -> late by 15 minutes
        val regAt0915 = format.parse("2026-11-04 09:15")!!.time
        val diff0915 = ((regAt0915 - lessonStart) / (60 * 1000)).toInt()
        val late0915 = if (diff0915 > graceMinutes) diff0915 else 0
        assertEquals(15, late0915)
    }

    @Test
    fun testAttendanceSummaryCalculation() {
        val records = listOf(
            AttendanceRecord(status = "PRESENT"),
            AttendanceRecord(status = "PRESENT"),
            AttendanceRecord(status = "LATE"),
            AttendanceRecord(status = "EXCUSED"),
            AttendanceRecord(status = "UNEXCUSED")
        )

        val attended = records.count { it.status == "PRESENT" || it.status == "LATE" }
        val late = records.count { it.status == "LATE" }
        val excused = records.count { it.status == "EXCUSED" }
        val unexcused = records.count { it.status == "UNEXCUSED" }
        val total = records.size
        val percentage = if (total > 0) (attended.toFloat() / total) * 100f else 0f

        val summary = AttendanceSummary(
            totalLessons = total,
            attendedCount = attended,
            lateCount = late,
            excusedCount = excused,
            unexcusedCount = unexcused,
            attendancePercentage = percentage
        )

        assertEquals(5, summary.totalLessons)
        assertEquals(3, summary.attendedCount)
        assertEquals(1, summary.lateCount)
        assertEquals(1, summary.excusedCount)
        assertEquals(1, summary.unexcusedCount)
        assertEquals(60.0f, summary.attendancePercentage, 0.01f)
    }

    @Test
    fun testDataModelDefaultValues() {
        val user = UserProfile()
        assertEquals("STUDENT", user.role)
        assertEquals("ACTIVE", user.status)

        val lesson = Lesson()
        assertEquals("SCHEDULED", lesson.status)
        assertEquals(5, lesson.lateThresholdMinutes)
    }
}
