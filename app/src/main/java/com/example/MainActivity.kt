package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.model.UserProfile
import com.example.data.repository.AttendanceRepository
import com.example.ui.components.appSignOut
import com.example.ui.components.attemptAutoSignIn
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.StudentScreen
import com.example.ui.screens.TeacherScreen
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.StudentAttendanceTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StudentAttendanceTheme {
                AppRootContent()
            }
        }
    }
}

enum class NavigationTab(val label: String, val icon: ImageVector) {
    TEACHER("Преподаватель", Icons.Default.School),
    STUDENT("Студент", Icons.Default.Person),
    JOURNAL("Журнал", Icons.Default.Assessment),
    ADMIN("Админ", Icons.Default.Shield)
}

@Composable
fun AppRootContent() {
    val context = LocalContext.current
    val repository = remember { AttendanceRepository(context) }
    val credentialManager = remember { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()

    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

    // Listen to Firebase Auth state
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    // Attempt silent auto sign in once on startup
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    if (currentUser == null) {
        AuthScreen(
            repository = repository,
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        MainAuthenticatedApp(
            user = currentUser!!,
            repository = repository,
            onSignOut = {
                appSignOut(
                    context = context,
                    credentialManager = credentialManager,
                    onSignOutComplete = { currentUser = null },
                    scope = scope
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAuthenticatedApp(
    user: FirebaseUser,
    repository: AttendanceRepository,
    onSignOut: () -> Unit
) {
    val userProfileFlow = remember(user.uid) { repository.observeUserProfile(user.uid) }
    val userProfile by userProfileFlow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    // Determine initial role and seed data
    LaunchedEffect(user.uid) {
        repository.seedDefaultDataIfEmpty()
    }

    val adminEmails = listOf("ddauenov853@gmail.com", "damirffakk@gmail.com")
    val isAdminUser = user.email in adminEmails

    // Auto-create or promote profile if needed
    LaunchedEffect(user.uid, userProfile) {
        if (isAdminUser) {
            repository.createAdminDoc(user.uid)
        }
        if (userProfile == null || (isAdminUser && userProfile?.role != "ADMIN")) {
            val initialProfile = UserProfile(
                userId = user.uid,
                email = user.email ?: "",
                fullName = user.displayName ?: user.email?.substringBefore("@") ?: "Администратор",
                role = if (isAdminUser) "ADMIN" else (userProfile?.role ?: "STUDENT"),
                status = "ACTIVE",
                groupId = "po-2404",
                studentId = if (isAdminUser) null else (userProfile?.studentId ?: "ST-240401")
            )
            repository.saveUserProfile(initialProfile)
        }
    }

    val actualRole = if (isAdminUser) "ADMIN" else (userProfile?.role ?: "STUDENT")

    // Initial tab based on role
    var currentTab by remember(actualRole) {
        mutableStateOf(
            when (actualRole) {
                "TEACHER" -> NavigationTab.TEACHER
                "ADMIN" -> NavigationTab.TEACHER // Default to teacher for active lesson presentation
                else -> NavigationTab.STUDENT
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("2.0", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "STUDENTATTENDANCE",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Группа ПО-2404 • ${currentTab.label}",
                                fontSize = 11.sp,
                                color = Color(0xFFC7D2FE)
                            )
                        }
                    }
                },
                actions = {
                    // Role Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = actualRole,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Logout Button
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Выйти",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary)
            )
        },
        bottomBar = {
            // Navigation Bar for switching views
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = NavyPrimary,
                tonalElevation = 8.dp
            ) {
                NavigationTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NavyPrimary,
                            selectedTextColor = NavyPrimary,
                            indicatorColor = Color(0xFFE0E7FF),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val profile = userProfile ?: UserProfile(
                userId = user.uid,
                email = user.email ?: "",
                fullName = user.displayName ?: "Пользователь",
                role = actualRole,
                status = "ACTIVE",
                groupId = "po-2404",
                studentId = "ST-240401"
            )

            when (currentTab) {
                NavigationTab.TEACHER -> {
                    TeacherScreen(
                        repository = repository,
                        currentUser = profile
                    )
                }
                NavigationTab.STUDENT -> {
                    StudentScreen(
                        repository = repository,
                        currentUser = profile
                    )
                }
                NavigationTab.JOURNAL -> {
                    JournalScreen(
                        repository = repository
                    )
                }
                NavigationTab.ADMIN -> {
                    AdminDashboardScreen(
                        repository = repository
                    )
                }
            }
        }
    }
}
