# Security Specification for StudentAttendance 2.0

## 1. Data Invariants
1. **User Identity & Role Immutability:** Users cannot grant themselves `ADMIN` or `TEACHER` roles. The runtime user `ddauenov853@gmail.com` is bootstrapped as ADMIN. Other users must have roles assigned or approved by an Admin.
2. **Attendance Integrity:** Students can only mark attendance for their own `studentUid == request.auth.uid`. A student cannot modify another student's attendance.
3. **QR Token Expiration:** Students can only self-register attendance if the lesson is in `ACTIVE` status, the student's `groupId` matches the lesson's `groupId`, the `qrToken` matches, and `request.time <= qrExpiresAt`.
4. **Lesson Management:** Only the assigned `teacherId` or an `ADMIN` can create or modify lessons, close registration, or change status.
5. **Absence Requests:** Students can only submit absence requests for themselves (`studentUid == request.auth.uid`). Only teachers or admins can update status (`APPROVED`/`REJECTED`) and review comments.
6. **Immutable Timestamps:** `createdAt` and `userId`/`studentUid` can never be altered on update.

## 2. Dirty Dozen Payloads (Red Team Test Cases)
1. **Ghost Admin Escalation:** User sets `role: "ADMIN"` during initial profile creation. (Must be rejected).
2. **Cross-User Attendance Forgery:** Student A writes attendance record with `studentUid: "studentB"`. (Must be rejected).
3. **Expired QR Registration:** Student submits attendance after `qrExpiresAt`. (Must be rejected).
4. **Wrong Group Poisoning:** Student in group `po-2405` attempts to mark attendance for lesson of group `po-2404`. (Must be rejected).
5. **Direct Status Modification:** Student directly changes their attendance status from `LATE` to `PRESENT`. (Must be rejected).
6. **Teacher Impersonation:** Student creates a lesson under another teacher's ID. (Must be rejected).
7. **Absence Self-Approval:** Student updates their own absence request from `PENDING` to `APPROVED`. (Must be rejected).
8. **Replay Token Attack:** Student sends attendance with wrong `qrToken`. (Must be rejected).
9. **Creation Timestamp Manipulation:** Student creates attendance with future `createdAt` timestamp. (Must be rejected).
10. **Group Hijacking:** Non-admin attempts to create or delete an academic group. (Must be rejected).
11. **Invitation Reuse:** Student attempts to claim an invitation where `isUsed: true`. (Must be rejected).
12. **Audit Log Tampering:** User attempts to delete or modify entries in `auditLogs`. (Must be rejected - audit logs are append-only by admins/teachers or system, read-only to admins).
