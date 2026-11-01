package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NmsViewModel
import com.example.ui.theme.HolidayHighlight
import com.example.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NmsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val state by viewModel.state.collectAsState()
    val isAdmin = state.currentUser?.role == "admin"

    var adminUsername by remember(state.adminUser) { mutableStateOf(state.adminUser) }
    var adminPassword by remember(state.adminPass) { mutableStateOf(state.adminPass) }
    var hospitalName by remember(state.footer.hospitalName) { mutableStateOf(state.footer.hospitalName) }
    var hospitalAddress by remember(state.footer.address) { mutableStateOf(state.footer.address) }
    var clMaxInput by remember(state.clMax) { mutableStateOf(state.clMax.toString()) }

    var newHolDate by remember { mutableStateOf("") }
    var newHolName by remember { mutableStateOf("") }

    var showImportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.selectMoreSubTab(null) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("System Settings & Data Management", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TealPrimary)
        }

        // Card 1: App Store & Play Store Publishing Guide
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shop, contentDescription = null, tint = Color(0xFF2563EB))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play Store & App Store Deployment", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E40AF))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Package ID: com.aistudio.nms.nurseapp\n" +
                            "• Version: 1.0 (VersionCode 1)\n" +
                            "• Target SDK: Android 15 (API 36)\n" +
                            "• Release Keystore: Pre-configured signing in Gradle build.\n" +
                            "• How to export: Open the project settings menu in AI Studio to download the signed APK or generate an Android App Bundle (.AAB) for Google Play Store upload.",
                    fontSize = 12.sp,
                    color = Color(0xFF1E3A8A),
                    lineHeight = 18.sp
                )
            }
        }

        // Card 2: Backup & Restore (JSON format, 100% interoperable with Web app)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Backup, contentDescription = null, tint = TealPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backup & Restore Data (Web Compatible)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Export and import data in JSON format. The backup format matches the web version so you can sync data between the web app and this mobile app at any time.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val json = viewModel.repository.exportJsonString()
                            clipboardManager.setText(AnnotatedString(json))
                            Toast.makeText(context, "Backup JSON copied to clipboard! Save or send it.", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Backup JSON")
                    }

                    OutlinedButton(
                        onClick = { showImportDialog = true }
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore JSON")
                    }
                }
            }
        }

        // Card 3: Admin Credentials (Admin only)
        if (isAdmin) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Admin Login Credentials", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    OutlinedTextField(
                        value = adminUsername,
                        onValueChange = { adminUsername = it },
                        label = { Text("Admin Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = adminPassword,
                        onValueChange = { adminPassword = it },
                        label = { Text("Admin Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.repository.saveData(
                                state.copy(
                                    adminUser = adminUsername.trim(),
                                    adminPass = adminPassword.trim()
                                )
                            )
                            Toast.makeText(context, "Admin credentials saved", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text("Save Credentials")
                    }
                }
            }

            // Card 4: Hospital Details & Rules
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hospital Profile & Leave Rules", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    OutlinedTextField(
                        value = hospitalName,
                        onValueChange = { hospitalName = it },
                        label = { Text("Hospital Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = hospitalAddress,
                        onValueChange = { hospitalAddress = it },
                        label = { Text("Hospital Address / Location") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = clMaxInput,
                        onValueChange = { clMaxInput = it },
                        label = { Text("Max Casual Leave (CL) Days Per Year") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val newCl = clMaxInput.toIntOrNull() ?: 20
                            viewModel.repository.saveData(
                                state.copy(
                                    footer = state.footer.copy(
                                        hospitalName = hospitalName.trim(),
                                        address = hospitalAddress.trim()
                                    ),
                                    clMax = newCl
                                )
                            )
                            Toast.makeText(context, "Hospital settings updated", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text("Save Settings")
                    }
                }
            }
        }

        // Card 5: Official Bangladesh 2026 Gazetted Holidays
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Official Govt Holidays (${state.holidays.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    TextButton(onClick = {
                        viewModel.repository.reload2026Holidays()
                        Toast.makeText(context, "Reloaded official 2026 Bangladesh Gazetted Holidays", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Reload 2026 Gazette")
                    }
                }

                if (isAdmin) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newHolDate,
                            onValueChange = { newHolDate = it },
                            placeholder = { Text("YYYY-MM-DD") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = newHolName,
                            onValueChange = { newHolName = it },
                            placeholder = { Text("Holiday Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1.8f)
                        )
                        Button(
                            onClick = {
                                if (newHolDate.isNotBlank() && newHolName.isNotBlank()) {
                                    viewModel.repository.addHoliday(newHolDate.trim(), newHolName.trim())
                                    newHolDate = ""
                                    newHolName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) {
                            Text("Add")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                state.holidays.take(15).forEach { h ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = HolidayHighlight
                        ) {
                            Text(
                                text = h.date,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF14532D),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = h.name,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        if (isAdmin) {
                            IconButton(
                                onClick = { viewModel.repository.deleteHoliday(h.date) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color(0xFFB91C1C), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }

                if (state.holidays.size > 15) {
                    Text(
                        "+ ${state.holidays.size - 15} more gazetted holidays loaded in Duty Roster.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }

    if (showImportDialog) {
        var importJsonInput by remember { mutableStateOf("") }
        var importError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Restore Data from JSON", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Paste a valid NMS backup JSON string below (from this app or the web application):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (importError != null) {
                        Text(importError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = {
                            importJsonInput = it
                            importError = null
                        },
                        placeholder = { Text("{\"admin\":{\"u\":\"admin\",...}}") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonInput.isNotBlank()) {
                            val success = viewModel.repository.importJsonString(importJsonInput.trim())
                            if (success) {
                                Toast.makeText(context, "Data restored successfully!", Toast.LENGTH_SHORT).show()
                                showImportDialog = false
                            } else {
                                importError = "Invalid JSON data structure. Please check the backup content."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            }
        )
    }
}
