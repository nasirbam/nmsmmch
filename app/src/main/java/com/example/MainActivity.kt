package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NmsViewModel
import com.example.ui.components.TopNewsTicker
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TealPrimary
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val viewModel: NmsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                NmsApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NmsApp(viewModel: NmsViewModel) {
    val state by viewModel.state.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val currentMoreTab by viewModel.currentMoreTab.collectAsState()
    val showNotifications by viewModel.showNotifications.collectAsState()

    // Handle Back Button
    BackHandler(enabled = currentTab == NmsViewModel.MainTab.MORE && currentMoreTab != null) {
        viewModel.selectMoreSubTab(null)
    }

    if (state.currentUser == null) {
        LoginScreen(viewModel = viewModel)
    } else {
        val pendingCount = remember(state) { viewModel.getPendingApprovalCount() }
        val unreadNotifCount = remember(state) { viewModel.getUnreadNotificationCount() }
        val loggedNurse = state.nurses.find { it.id == state.currentUser?.nurseId }
        val isAdmin = state.currentUser?.role == "admin"

        // Live Clock Header Text
        var currentClockTime by remember {
            mutableStateOf(SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.US).format(Date()))
        }

        LaunchedEffect(Unit) {
            while (true) {
                currentClockTime = SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.US).format(Date())
                kotlinx.coroutines.delay(10000)
            }
        }

        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TealPrimary)
                        .statusBarsPadding()
                ) {
                    // System Bar Clock
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0B1F26))
                            .padding(horizontal = 12.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 $currentClockTime",
                            color = Color(0xFFCCFBF1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Hospital Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🩺 NMS",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF59E0B)
                            ) {
                                Text(
                                    text = if (isAdmin) "Admin" else loggedNurse?.cat ?: "Staff",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Notifications bell
                            BadgedBox(
                                badge = {
                                    if (unreadNotifCount > 0) {
                                        Badge(containerColor = Color(0xFFEF4444)) {
                                            Text("$unreadNotifCount")
                                        }
                                    }
                                }
                            ) {
                                IconButton(onClick = { viewModel.toggleNotifications(true) }) {
                                    Icon(
                                        Icons.Default.Notifications,
                                        contentDescription = "Notifications",
                                        tint = Color.White
                                    )
                                }
                            }

                            // Logout
                            IconButton(onClick = { viewModel.repository.logout() }) {
                                Icon(
                                    Icons.Default.Logout,
                                    contentDescription = "Logout",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Notice Ticker Banner
                    TopNewsTicker(news = state.news)
                }
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == NmsViewModel.MainTab.ROSTER,
                        onClick = { viewModel.selectTab(NmsViewModel.MainTab.ROSTER) },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Roster") },
                        label = { Text("Roster") }
                    )

                    NavigationBarItem(
                        selected = currentTab == NmsViewModel.MainTab.ATTENDANCE,
                        onClick = { viewModel.selectTab(NmsViewModel.MainTab.ATTENDANCE) },
                        icon = { Icon(Icons.Default.Schedule, contentDescription = "Attendance") },
                        label = { Text("Attendance") }
                    )

                    NavigationBarItem(
                        selected = currentTab == NmsViewModel.MainTab.LEAVE,
                        onClick = { viewModel.selectTab(NmsViewModel.MainTab.LEAVE) },
                        icon = { Icon(Icons.Default.BeachAccess, contentDescription = "Leave") },
                        label = { Text("Leave") }
                    )

                    NavigationBarItem(
                        selected = currentTab == NmsViewModel.MainTab.APPROVALS,
                        onClick = { viewModel.selectTab(NmsViewModel.MainTab.APPROVALS) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (pendingCount > 0) {
                                        Badge(containerColor = Color(0xFFEF4444)) {
                                            Text("$pendingCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Approvals")
                            }
                        },
                        label = { Text("Approvals") }
                    )

                    NavigationBarItem(
                        selected = currentTab == NmsViewModel.MainTab.MORE,
                        onClick = { viewModel.selectTab(NmsViewModel.MainTab.MORE) },
                        icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
                        label = { Text("More") }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    NmsViewModel.MainTab.ROSTER -> DutyRosterScreen(viewModel = viewModel)
                    NmsViewModel.MainTab.ATTENDANCE -> AttendanceScreen(viewModel = viewModel)
                    NmsViewModel.MainTab.LEAVE -> LeaveScreen(viewModel = viewModel)
                    NmsViewModel.MainTab.APPROVALS -> ApprovalsScreen(viewModel = viewModel)
                    NmsViewModel.MainTab.MORE -> MoreScreen(viewModel = viewModel)
                }
            }
        }

        if (showNotifications) {
            NotificationsDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.toggleNotifications(false) }
            )
        }
    }
}
