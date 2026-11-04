const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085
    }
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read groups or users", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthedDb.collection("groups").get());
  await assertFails(unauthedDb.collection("users").doc("u1").get());
});

test("Student cannot create account with ADMIN role", async () => {
  const studentDb = testEnv.authenticatedContext("student_1", { email: "student@college.edu", email_verified: true }).firestore();
  await assertFails(
    studentDb.collection("users").doc("student_1").set({
      userId: "student_1",
      email: "student@college.edu",
      fullName: "Иванов Иван",
      role: "ADMIN",
      status: "ACTIVE",
      createdAt: new Date(),
      updatedAt: new Date()
    })
  );
});

test("Student can create account with STUDENT role", async () => {
  const studentDb = testEnv.authenticatedContext("student_1", { email: "student@college.edu", email_verified: true }).firestore();
  await assertSucceeds(
    studentDb.collection("users").doc("student_1").set({
      userId: "student_1",
      email: "student@college.edu",
      fullName: "Иванов Иван",
      role: "STUDENT",
      status: "PENDING",
      createdAt: new Date(),
      updatedAt: new Date()
    })
  );
});

test("Admin can create a group and subject", async () => {
  const adminDb = testEnv.authenticatedContext("admin_1", { email: "ddauenov853@gmail.com", email_verified: true }).firestore();
  await assertSucceeds(
    adminDb.collection("groups").doc("po-2404").set({
      groupId: "po-2404",
      name: "ПО-2404",
      code: "PO2404",
      specialty: "09.02.07 Программирование в КС",
      studentCount: 25,
      teacherIds: [],
      subjectIds: [],
      createdAt: new Date(),
      updatedAt: new Date()
    })
  );
});

test("Student cannot create a group", async () => {
  const studentDb = testEnv.authenticatedContext("student_1", { email: "student@college.edu", email_verified: true }).firestore();
  await assertFails(
    studentDb.collection("groups").doc("po-9999").set({
      groupId: "po-9999",
      name: "ПО-9999",
      code: "PO9999",
      createdAt: new Date()
    })
  );
});

test("Student cannot forge attendance for another student", async () => {
  const studentDb = testEnv.authenticatedContext("student_1", { email: "student@college.edu", email_verified: true }).firestore();
  
  // Setup lesson with admin bypass
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("lessons").doc("lesson_1").set({
      lessonId: "lesson_1",
      subjectId: "sub_1",
      subjectName: "Разработка ПО",
      groupId: "po-2404",
      groupName: "ПО-2404",
      teacherId: "teacher_1",
      date: "2026-11-04",
      startTime: "09:00",
      endTime: "10:30",
      status: "ACTIVE",
      createdAt: new Date()
    });
  });

  // Attempt to forge student_2 attendance
  await assertFails(
    studentDb.collection("attendance").doc("lesson_1_student_2").set({
      attendanceId: "lesson_1_student_2",
      lessonId: "lesson_1",
      studentUid: "student_2",
      studentName: "Петров Петр",
      groupId: "po-2404",
      status: "PRESENT",
      registeredAt: new Date(),
      markedBy: "QR",
      createdAt: new Date()
    })
  );
});

test("Student can register own attendance with QR for active lesson", async () => {
  const studentDb = testEnv.authenticatedContext("student_1", { email: "student@college.edu", email_verified: true }).firestore();
  
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("lessons").doc("lesson_1").set({
      lessonId: "lesson_1",
      subjectId: "sub_1",
      subjectName: "Разработка ПО",
      groupId: "po-2404",
      groupName: "ПО-2404",
      teacherId: "teacher_1",
      date: "2026-11-04",
      startTime: "09:00",
      endTime: "10:30",
      status: "ACTIVE",
      createdAt: new Date()
    });
  });

  await assertSucceeds(
    studentDb.collection("attendance").doc("lesson_1_student_1").set({
      attendanceId: "lesson_1_student_1",
      lessonId: "lesson_1",
      studentUid: "student_1",
      studentName: "Иванов Иван",
      groupId: "po-2404",
      status: "PRESENT",
      registeredAt: new Date(),
      markedBy: "QR",
      createdAt: new Date()
    })
  );
});
