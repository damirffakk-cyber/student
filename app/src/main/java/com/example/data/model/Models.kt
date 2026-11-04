package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: String = "STUDENT", // STUDENT, TEACHER, ADMIN
    val status: String = "ACTIVE", // ACTIVE, PENDING, DISABLED
    val groupId: String? = null,
    val studentId: String? = null,
    val phone: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AcademicGroup(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val name: String = "",
    val code: String = "",
    val specialty: String = "",
    val studentCount: Int = 0,
    val teacherIds: List<String> = emptyList(),
    val subjectIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Subject(
    @DocumentId val id: String = "",
    val subjectId: String = "",
    val name: String = "",
    val code: String = "",
    val teacherIds: List<String> = emptyList(),
    val groupIds: List<String> = emptyList(),
    val hoursTotal: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Semester(
    @DocumentId val id: String = "",
    val semesterId: String = "",
    val name: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val isCurrent: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Lesson(
    @DocumentId val id: String = "",
    val lessonId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val durationMinutes: Int = 90,
    val status: String = "SCHEDULED", // SCHEDULED, ACTIVE, COMPLETED, CANCELLED
    val lateThresholdMinutes: Int = 5,
    val qrToken: String = "",
    val qrExpiresAt: Timestamp? = null,
    val room: String = "",
    val topic: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val attendanceId: String = "",
    val lessonId: String = "",
    val studentUid: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val groupId: String = "",
    val status: String = "PRESENT", // PRESENT, LATE, EXCUSED, UNEXCUSED, PENDING
    val registeredAt: Timestamp? = null,
    val minutesLate: Int = 0,
    val markedBy: String = "QR", // QR, TEACHER, ADMIN
    val qrToken: String? = null,
    val note: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AbsenceRequest(
    @DocumentId val id: String = "",
    val requestId: String = "",
    val studentUid: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val groupId: String = "",
    val lessonId: String? = null,
    val subjectName: String? = null,
    val date: String = "",
    val reason: String = "",
    val documentUrl: String? = null,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val reviewComment: String? = null,
    val reviewedBy: String? = null,
    val reviewedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Invitation(
    @DocumentId val id: String = "",
    val invitationId: String = "",
    val code: String = "",
    val groupId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val email: String? = null,
    val role: String = "STUDENT",
    val isUsed: Boolean = false,
    val usedAt: Timestamp? = null,
    val usedBy: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val logId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userRole: String = "",
    val action: String = "",
    val details: String = "",
    val targetId: String? = null,
    @ServerTimestamp val timestamp: Timestamp? = null
)

@IgnoreExtraProperties
data class SyncLog(
    @DocumentId val id: String = "",
    val syncId: String = "",
    val status: String = "SUCCESS",
    val recordsCount: Int = 0,
    val details: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
)

data class AttendanceSummary(
    val totalLessons: Int = 0,
    val attendedCount: Int = 0,
    val lateCount: Int = 0,
    val excusedCount: Int = 0,
    val unexcusedCount: Int = 0,
    val attendancePercentage: Float = 0f
)
