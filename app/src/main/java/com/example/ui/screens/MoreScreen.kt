package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NmsViewModel
import com.example.ui.theme.TealPrimary

@Composable
fun MoreScreen(
    viewModel: NmsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val currentSubTab by viewModel.currentMoreTab.collectAsState()
    val isAdmin = state.currentUser?.role == "admin"

    // Sub-screens routing
    when (currentSubTab) {
        NmsViewModel.MoreSubTab.PROFILE -> ProfileScreen(viewModel = viewModel)
        NmsViewModel.MoreSubTab.NURSES -> NursesScreen(viewModel = viewModel)
        NmsViewModel.MoreSubTab.ACR -> AcrScreen(viewModel = viewModel)
        NmsViewModel.MoreSubTab.COMPLAINTS -> ComplaintsScreen(viewModel = viewModel)
        NmsViewModel.MoreSubTab.WARDS -> WardsScreen(viewModel = viewModel)
        NmsViewModel.MoreSubTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
        NmsViewModel.MoreSubTab.WEB_VIEW -> WebVersionScreen(viewModel = viewModel)
        null -> {
            // Main More Hub Menu
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "Hospital Management Hub",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TealPrimary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                if (!isAdmin) {
                    item {
                        MoreMenuItem(
                            title = "My Profile & Controlling Officers",
                            subtitle = "View HRIS profile, controlling hierarchy, change password",
                            icon = Icons.Default.Person,
                            color = TealPrimary,
                            onClick = { viewModel.selectMoreSubTab(NmsViewModel.MoreSubTab.PROFILE) }
                        )
                    }
                }

                item {
                    MoreMenuItem(
                        title = "Nurses Directory",
                        subtitle = "Hospital nurse list, phone numbers, HRIS, BNMC registration",
                        icon = Icons.Default.People,
                        color = Color(0xFF2563EB),
                        onClick = { viewModel.selectMoreSubTab(NmsViewModel.MoreSubTab.NURSES) }
                    )
                }

                item {
                    MoreMenuItem(
                        title = "ACR (Annual Confidential Report)",
                        subtitle = "Track and submit annual performance evaluation status",
                        icon = Icons.Default.Assignment,
                        color = Color(0xFF7C3AED),
                        onClick = { viewModel.selectMoreSubTab(NmsViewModel.MoreSubTab.ACR) }
                    )
                }

                item {
                    MoreMenuItem(
                        title = "Hospital Complaint Box",
                        subtitle = "Lodge complaints, monitor escalation up to Nursing Superintendent",
                        icon = Icons.Default.Feedback,
                        color = Color(0xFFDC2626),
                        onClick = { viewModel.selectMoreSubTab(NmsViewModel.MoreSubTab.COMPLAINTS) }
                    )
                }

                if (isAdmin) {
                    item {
                        MoreMenuItem(
                            title = "Hospital Wards Management",
                            subtitle = "Add, rename, or delete clinical departments and wards",
                            icon = Icons.Default.LocalHospital,
                            color = Color(0xFF0D9488),
                            onClick = { viewModel.selectMoreSubTab(NmsViewModel.MoreSubTab.WARDS) }
                        )
                    }
                }

                item {
                    MoreMenuItem(
                        title = "System Settings & Backup",
                        subtitle = "Admin credentials, leave rules, 2026 gazette holidays, JSON backup",
                        icon = Icons.Default.Settings,
                        color = Color(0xFF475569),
                        onClick = { viewModel.selectMoreSubTab(NmsViewModel.MoreSubTab.SETTINGS) }
                    )
                }

                item {
                    MoreMenuItem(
                        title = "Web Version (Embedded NMS)",
                        subtitle = "Run the full original web interface inside this mobile app offline",
                        icon = Icons.Default.Language,
                        color = Color(0xFF0284C7),
                        onClick = { viewModel.selectMoreSubTab(NmsViewModel.MoreSubTab.WEB_VIEW) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "NURSES MANAGEMENT SYSTEM (NMS)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TealPrimary
                            )
                            Text(
                                "By Nasir, RN • Built with Jetpack Compose for Android",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoreMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.12f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
