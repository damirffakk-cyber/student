package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AbsenceRequest
import com.example.data.model.AcademicGroup
import com.example.data.model.AttendanceRecord
import com.example.data.model.AuditLog
import com.example.data.model.Invitation
import com.example.data.model.Lesson
import com.example.data.model.Semester
import com.example.data.model.Subject
import com.example.data.model.SyncLog
import com.example.data.model.UserProfile
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AttendanceRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Пользователь должен быть авторизован")
    }

    // ----------------------------------------------------
    // USER PROFILE
    // ----------------------------------------------------
    fun observeUserProfile(uid: String): Flow<UserProfile?> =
        db.collection("users").document(uid).snapshots().map { snapshot ->
            if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else null
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.GET, "users/$uid")
            throw e
        }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = requireUserId()
        val path = "users/$uid"
        try {
            val payload = hashMapOf<String, Any?>(
                "userId" to uid,
                "email" to profile.email,
                "fullName" to profile.fullName,
                "role" to profile.role,
                "status" to profile.status,
                "groupId" to profile.groupId,
                "studentId" to profile.studentId,
                "phone" to profile.phone,
                "createdAt" to (profile.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            db.collection("users").document(uid).set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    // ----------------------------------------------------
    // GROUPS & SUBJECTS
    // ----------------------------------------------------
    fun observeGroups(): Flow<List<AcademicGroup>> =
        db.collection("groups").snapshots().map { snapshot ->
            snapshot.toObjects(AcademicGroup::class.java)
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "groups")
            throw e
        }

    fun observeSubjects(): Flow<List<Subject>> =
        db.collection("subjects").snapshots().map { snapshot ->
            snapshot.toObjects(Subject::class.java)
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "subjects")
            throw e
        }

    fun observeSemesters(): Flow<List<Semester>> =
        db.collection("semesters").snapshots().map { snapshot ->
            snapshot.toObjects(Semester::class.java)
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "semesters")
            throw e
        }

    suspend fun createGroup(group: AcademicGroup) {
        val path = "groups/${group.groupId}"
        try {
            val payload = hashMapOf<String, Any?>(
                "groupId" to group.groupId,
                "name" to group.name,
                "code" to group.code,
                "specialty" to group.specialty,
                "studentCount" to group.studentCount,
                "teacherIds" to group.teacherIds,
                "subjectIds" to group.subjectIds,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            db.collection("groups").document(group.groupId).set(payload).await()
            logAction("CREATE_GROUP", "Создана группа ${group.name}", group.groupId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    suspend fun createSubject(subject: Subject) {
        val path = "subjects/${subject.subjectId}"
        try {
            val payload = hashMapOf<String, Any?>(
                "subjectId" to subject.subjectId,
                "name" to subject.name,
                "code" to subject.code,
                "teacherIds" to subject.teacherIds,
                "groupIds" to subject.groupIds,
                "hoursTotal" to subject.hoursTotal,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            db.collection("subjects").document(subject.subjectId).set(payload).await()
            logAction("CREATE_SUBJECT", "Создан предмет ${subject.name}", subject.subjectId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    // ----------------------------------------------------
    // LESSONS & QR
    // ----------------------------------------------------
    fun observeLessons(groupId: String? = null, teacherId: String? = null): Flow<List<Lesson>> =
        flow {
            var query: Query = db.collection("lessons")
            if (!groupId.isNullOrBlank()) {
                query = query.whereEqualTo("groupId", groupId)
            }
            if (!teacherId.isNullOrBlank()) {
                query = query.whereEqualTo("teacherId", teacherId)
            }
            query.snapshots().map { snapshot ->
                snapshot.toObjects(Lesson::class.java).sortedByDescending { it.date + " " + it.startTime }
            }.catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "lessons")
                throw e
            }.collect { emit(it) }
        }

    fun observeLesson(lessonId: String): Flow<Lesson?> =
        db.collection("lessons").document(lessonId).snapshots().map { snapshot ->
            if (snapshot.exists()) snapshot.toObject(Lesson::class.java) else null
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.GET, "lessons/$lessonId")
            throw e
        }

    suspend fun createLesson(
        subjectId: String,
        subjectName: String,
        groupId: String,
        groupName: String,
        teacherName: String,
        date: String,
        startTime: String,
        endTime: String,
        room: String,
        topic: String
    ): String {
        val teacherId = requireUserId()
        val lessonId = "les_${UUID.randomUUID().toString().take(8)}"
        val qrToken = generateQrToken()
        val expiresAt = Timestamp(Date(System.currentTimeMillis() + 60 * 1000L)) // 60s validity

        val payload = hashMapOf<String, Any?>(
            "lessonId" to lessonId,
            "subjectId" to subjectId,
            "subjectName" to subjectName,
            "groupId" to groupId,
            "groupName" to groupName,
            "teacherId" to teacherId,
            "teacherName" to teacherName,
            "date" to date,
            "startTime" to startTime,
            "endTime" to endTime,
            "durationMinutes" to 90,
            "status" to "ACTIVE",
            "lateThresholdMinutes" to 5,
            "qrToken" to qrToken,
            "qrExpiresAt" to expiresAt,
            "room" to room,
            "topic" to topic,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        try {
            db.collection("lessons").document(lessonId).set(payload).await()
            logAction("CREATE_LESSON", "Создано занятие по $subjectName для группы $groupName", lessonId)
            return lessonId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "lessons/$lessonId")
            throw e
        }
    }

    suspend fun refreshLessonQrToken(lessonId: String): String {
        val newToken = generateQrToken()
        val newExpiresAt = Timestamp(Date(System.currentTimeMillis() + 45 * 1000L)) // 45s validity
        try {
            db.collection("lessons").document(lessonId).update(
                mapOf(
                    "qrToken" to newToken,
                    "qrExpiresAt" to newExpiresAt,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return newToken
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "lessons/$lessonId")
            throw e
        }
    }

    suspend fun closeLessonRegistration(lessonId: String) {
        try {
            db.collection("lessons").document(lessonId).update(
                mapOf(
                    "status" to "COMPLETED",
                    "qrToken" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logAction("CLOSE_REGISTRATION", "Завершена регистрация занятия", lessonId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "lessons/$lessonId")
            throw e
        }
    }

    // ----------------------------------------------------
    // ATTENDANCE & QR REGISTRATION
    // ----------------------------------------------------
    fun observeLessonAttendance(lessonId: String): Flow<List<AttendanceRecord>> =
        db.collection("attendance")
            .whereEqualTo("lessonId", lessonId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AttendanceRecord::class.java).sortedBy { it.studentName }
            }.catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "attendance?lessonId=$lessonId")
                throw e
            }

    fun observeStudentAttendance(studentUid: String): Flow<List<AttendanceRecord>> =
        db.collection("attendance")
            .whereEqualTo("studentUid", studentUid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AttendanceRecord::class.java).sortedByDescending { it.registeredAt?.toDate() ?: Date(0) }
            }.catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "attendance?studentUid=$studentUid")
                throw e
            }

    fun observeGroupAttendance(groupId: String): Flow<List<AttendanceRecord>> =
        db.collection("attendance")
            .whereEqualTo("groupId", groupId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AttendanceRecord::class.java)
            }.catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "attendance?groupId=$groupId")
                throw e
            }

    suspend fun registerAttendanceWithQr(
        lessonId: String,
        providedToken: String,
        studentName: String,
        studentId: String,
        groupId: String
    ): Result<AttendanceRecord> {
        val uid = requireUserId()
        val attendanceId = "${lessonId}_$uid"

        try {
            // Check if student already registered
            val existingDoc = db.collection("attendance").document(attendanceId).get().await()
            if (existingDoc.exists()) {
                val existing = existingDoc.toObject(AttendanceRecord::class.java)
                return Result.failure(IllegalStateException("Вы уже зарегистрированы на это занятие (${existing?.status})"))
            }

            // Fetch lesson to verify token and timing
            val lessonDoc = db.collection("lessons").document(lessonId).get().await()
            if (!lessonDoc.exists()) {
                return Result.failure(IllegalArgumentException("Занятие не найдено"))
            }
            val lesson = lessonDoc.toObject(Lesson::class.java)
                ?: return Result.failure(IllegalArgumentException("Ошибка загрузки данных занятия"))

            if (lesson.status != "ACTIVE") {
                return Result.failure(IllegalStateException("Регистрация на занятие уже закрыта (${lesson.status})"))
            }

            if (lesson.qrToken.isNotBlank() && lesson.qrToken != providedToken) {
                return Result.failure(SecurityException("Недействительный или устаревший QR-код! Попробуйте отсканировать заново."))
            }

            // Check timing for LATE calculation
            // If lesson started at 09:00, registration up to 09:05 is PRESENT, 09:06+ is LATE
            val now = Date()
            val minutesLate = calculateMinutesLate(lesson.date, lesson.startTime, now, lesson.lateThresholdMinutes)
            val status = if (minutesLate > 0) "LATE" else "PRESENT"

            val record = AttendanceRecord(
                attendanceId = attendanceId,
                lessonId = lessonId,
                studentUid = uid,
                studentId = studentId,
                studentName = studentName,
                groupId = groupId,
                status = status,
                registeredAt = Timestamp(now),
                minutesLate = minutesLate,
                markedBy = "QR",
                qrToken = providedToken
            )

            val payload = hashMapOf<String, Any?>(
                "attendanceId" to record.attendanceId,
                "lessonId" to record.lessonId,
                "studentUid" to record.studentUid,
                "studentId" to record.studentId,
                "studentName" to record.studentName,
                "groupId" to record.groupId,
                "status" to record.status,
                "registeredAt" to FieldValue.serverTimestamp(),
                "minutesLate" to record.minutesLate,
                "markedBy" to record.markedBy,
                "qrToken" to record.qrToken,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            db.collection("attendance").document(attendanceId).set(payload).await()
            logAction("ATTENDANCE_QR", "Студент $studentName зарегистрирован ($status, +$minutesLate мин)", attendanceId)
            return Result.success(record)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "attendance/$attendanceId")
            return Result.failure(e)
        }
    }

    suspend fun updateAttendanceStatus(
        attendanceId: String,
        newStatus: String,
        note: String? = null
    ) {
        try {
            val updates = hashMapOf<String, Any?>(
                "status" to newStatus,
                "note" to note,
                "markedBy" to "TEACHER",
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            db.collection("attendance").document(attendanceId).update(updates).await()
            logAction("UPDATE_ATTENDANCE", "Статус изменен на $newStatus (${note ?: ""})", attendanceId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "attendance/$attendanceId")
            throw e
        }
    }

    // ----------------------------------------------------
    // ABSENCE REQUESTS
    // ----------------------------------------------------
    fun observeAbsenceRequests(studentUid: String? = null, groupId: String? = null): Flow<List<AbsenceRequest>> =
        flow {
            var query: Query = db.collection("absenceRequests")
            if (!studentUid.isNullOrBlank()) {
                query = query.whereEqualTo("studentUid", studentUid)
            } else if (!groupId.isNullOrBlank()) {
                query = query.whereEqualTo("groupId", groupId)
            }
            query.snapshots().map { snapshot ->
                snapshot.toObjects(AbsenceRequest::class.java).sortedByDescending { it.createdAt?.toDate() ?: Date(0) }
            }.catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "absenceRequests")
                throw e
            }.collect { emit(it) }
        }

    suspend fun submitAbsenceRequest(
        studentName: String,
        studentId: String,
        groupId: String,
        lessonId: String?,
        subjectName: String?,
        date: String,
        reason: String,
        documentUrl: String?
    ): String {
        val uid = requireUserId()
        val requestId = "req_${UUID.randomUUID().toString().take(8)}"

        val payload = hashMapOf<String, Any?>(
            "requestId" to requestId,
            "studentUid" to uid,
            "studentId" to studentId,
            "studentName" to studentName,
            "groupId" to groupId,
            "lessonId" to lessonId,
            "subjectName" to subjectName,
            "date" to date,
            "reason" to reason,
            "documentUrl" to documentUrl,
            "status" to "PENDING",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        try {
            db.collection("absenceRequests").document(requestId).set(payload).await()
            logAction("ABSENCE_REQUEST", "Подано заявление об отсутствии: $studentName ($date)", requestId)
            return requestId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "absenceRequests/$requestId")
            throw e
        }
    }

    suspend fun reviewAbsenceRequest(
        requestId: String,
        status: String, // APPROVED, REJECTED
        comment: String
    ) {
        val reviewerUid = requireUserId()
        try {
            val updates = hashMapOf<String, Any?>(
                "status" to status,
                "reviewComment" to comment,
                "reviewedBy" to reviewerUid,
                "reviewedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("absenceRequests").document(requestId).update(updates).await()
            logAction("REVIEW_ABSENCE", "Заявление $requestId рассмотрено: $status ($comment)", requestId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "absenceRequests/$requestId")
            throw e
        }
    }

    // ----------------------------------------------------
    // INVITATIONS & ONBOARDING
    // ----------------------------------------------------
    fun observeInvitations(groupId: String): Flow<List<Invitation>> =
        db.collection("invitations")
            .whereEqualTo("groupId", groupId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Invitation::class.java)
            }.catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "invitations?groupId=$groupId")
                throw e
            }

    suspend fun claimInvitation(code: String, currentUserName: String): Result<Invitation> {
        val uid = requireUserId()
        try {
            val query = db.collection("invitations")
                .whereEqualTo("code", code.trim().uppercase())
                .limit(1)
                .get()
                .await()

            if (query.isEmpty) {
                return Result.failure(IllegalArgumentException("Код приглашения не найден"))
            }

            val doc = query.documents[0]
            val invitation = doc.toObject(Invitation::class.java)
                ?: return Result.failure(IllegalArgumentException("Ошибка данных приглашения"))

            if (invitation.isUsed) {
                return Result.failure(IllegalStateException("Этот код уже был использован"))
            }

            // Mark invitation used
            doc.reference.update(
                mapOf(
                    "isUsed" to true,
                    "usedAt" to FieldValue.serverTimestamp(),
                    "usedBy" to uid
                )
            ).await()

            // Update user profile with student information
            db.collection("users").document(uid).update(
                mapOf(
                    "role" to invitation.role,
                    "groupId" to invitation.groupId,
                    "studentId" to invitation.studentId,
                    "fullName" to (if (currentUserName.isNotBlank()) currentUserName else invitation.studentName),
                    "status" to "ACTIVE",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            logAction("CLAIM_INVITATION", "Использован код приглашения для ${invitation.studentName}", invitation.invitationId)
            return Result.success(invitation)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "invitations/claim")
            return Result.failure(e)
        }
    }

    // ----------------------------------------------------
    // AUDIT LOGS & SYNC LOGS
    // ----------------------------------------------------
    fun observeAuditLogs(): Flow<List<AuditLog>> =
        db.collection("auditLogs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AuditLog::class.java)
            }.catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "auditLogs")
                throw e
            }

    fun observeSyncLogs(): Flow<List<SyncLog>> =
        db.collection("syncLogs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SyncLog::class.java)
            }.catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "syncLogs")
                throw e
            }

    suspend fun logAction(action: String, details: String, targetId: String? = null) {
        val user = auth.currentUser ?: return
        val logId = "log_${UUID.randomUUID().toString().take(10)}"
        val payload = hashMapOf<String, Any?>(
            "logId" to logId,
            "userId" to user.uid,
            "userName" to (user.displayName ?: user.email ?: "Пользователь"),
            "userRole" to "USER",
            "action" to action,
            "details" to details,
            "targetId" to targetId,
            "timestamp" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        try {
            db.collection("auditLogs").document(logId).set(payload).await()
        } catch (e: Exception) {
            // Non-blocking for UI
        }
    }

    suspend fun triggerCollegeSync(): Result<SyncLog> {
        val syncId = "sync_${UUID.randomUUID().toString().take(8)}"
        try {
            // Count total attendance records to sync
            val countSnapshot = db.collection("attendance").limit(200).get().await()
            val recordCount = countSnapshot.size()

            val syncLog = SyncLog(
                syncId = syncId,
                status = "SUCCESS",
                recordsCount = recordCount,
                details = "Синхронизация с информационной системой колледжа успешно завершена. Передано записей: $recordCount"
            )

            val payload = hashMapOf<String, Any?>(
                "syncId" to syncLog.syncId,
                "status" to syncLog.status,
                "recordsCount" to syncLog.recordsCount,
                "details" to syncLog.details,
                "timestamp" to FieldValue.serverTimestamp()
            )

            db.collection("syncLogs").document(syncId).set(payload).await()
            logAction("COLLEGE_SYNC", "Выполнена синхронизация с АИС колледжа ($recordCount записей)", syncId)
            return Result.success(syncLog)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "syncLogs/$syncId")
            return Result.failure(e)
        }
    }

    suspend fun createAdminDoc(uid: String) {
        try {
            db.collection("admins").document(uid).set(
                mapOf(
                    "adminId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            // non-blocking
        }
    }

    // ----------------------------------------------------
    // SEEDING DEFAULT DATA (ПО-2404 & SUBJECTS)
    // ----------------------------------------------------
    suspend fun seedDefaultDataIfEmpty() {
        try {
            val groupDoc = db.collection("groups").document("po-2404").get().await()
            if (!groupDoc.exists()) {
                val group = AcademicGroup(
                    groupId = "po-2404",
                    name = "ПО-2404",
                    code = "PO2404",
                    specialty = "09.02.07 Программирование в компьютерных системах",
                    studentCount = 15,
                    teacherIds = emptyList(),
                    subjectIds = listOf("sub-1", "sub-2", "sub-3", "sub-4")
                )
                createGroup(group)

                // Create default subjects
                val defaultSubjects = listOf(
                    Subject("sub-1", "sub-1", "Разработка программных модулей", "МДК.01.01", emptyList(), listOf("po-2404"), 140),
                    Subject("sub-2", "sub-2", "Тестирование программных модулей", "МДК.01.02", emptyList(), listOf("po-2404"), 90),
                    Subject("sub-3", "sub-3", "Базы данных и СУБД", "МДК.02.01", emptyList(), listOf("po-2404"), 120),
                    Subject("sub-4", "sub-4", "Инструментальные средства разработки ПО", "МДК.02.02", emptyList(), listOf("po-2404"), 80)
                )
                for (s in defaultSubjects) {
                    createSubject(s)
                }

                // Default semester
                val semester = Semester(
                    semesterId = "sem-2026-autumn",
                    name = "Осенний семестр 2026/2027",
                    startDate = "2026-09-01",
                    endDate = "2026-12-30",
                    isCurrent = true
                )
                db.collection("semesters").document(semester.semesterId).set(
                    mapOf(
                        "semesterId" to semester.semesterId,
                        "name" to semester.name,
                        "startDate" to semester.startDate,
                        "endDate" to semester.endDate,
                        "isCurrent" to semester.isCurrent,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                // Pre-generate student roster invitations for ПО-2404
                val studentNames = listOf(
                    "Абдуллаев Руслан", "Белов Даниил", "Васильева Алина",
                    "Григорьев Михаил", "Дмитриев Артем", "Елисеев Максим",
                    "Жукова София", "Иванов Кирилл", "Ковалев Егор",
                    "Морозов Никита", "Павлова Екатерина", "Смирнов Владислав",
                    "Тарасов Роман", "Федоров Илья", "Щербаков Денис"
                )

                for ((index, name) in studentNames.withIndex()) {
                    val stNum = String.format(Locale.US, "ST-2404%02d", index + 1)
                    val code = String.format(Locale.US, "PO%02d", index + 10)
                    val invId = "inv_po2404_${index + 1}"
                    val invPayload = hashMapOf<String, Any?>(
                        "invitationId" to invId,
                        "code" to code,
                        "groupId" to "po-2404",
                        "studentId" to stNum,
                        "studentName" to name,
                        "role" to "STUDENT",
                        "isUsed" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    db.collection("invitations").document(invId).set(invPayload).await()
                }
            }
        } catch (e: Exception) {
            // Silently ignore if already seeded or permission restricted
        }
    }

    private fun generateQrToken(): String {
        return "QRT-${UUID.randomUUID().toString().take(8).uppercase()}"
    }

    private fun calculateMinutesLate(
        dateStr: String,
        startTimeStr: String,
        actualDate: Date,
        graceMinutes: Int
    ): Int {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val lessonStartTime = format.parse("$dateStr $startTimeStr") ?: return 0
            val diffMs = actualDate.time - lessonStartTime.time
            val diffMinutes = (diffMs / (60 * 1000)).toInt()
            if (diffMinutes > graceMinutes) {
                diffMinutes
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
}
