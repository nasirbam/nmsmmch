package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Complaint
import com.example.ui.NmsViewModel
import com.example.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintsScreen(
    viewModel: NmsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val isAdmin = state.currentUser?.role == "admin"
    val loggedNurseId = state.currentUser?.nurseId
    val currentRank = viewModel.repository.getCurrentRank()

    var showNewComplaintDialog by remember { mutableStateOf(false) }
    var selectedComplaintForReply by remember { mutableStateOf<Complaint?>(null) }

    val authorityNames = listOf("", "", "Nurse Incharge", "Nursing Supervisor", "Deputy Nursing Supt", "Nursing Superintendent", "Admin")

    val visibleComplaints = remember(state.complaints, isAdmin, loggedNurseId, currentRank) {
        state.complaints.filter { c ->
            isAdmin || c.byNurseId == loggedNurseId || currentRank >= c.level
        }.reversed()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.selectMoreSubTab(null) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Hospital Complaint Box", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TealPrimary)
            }

            if (!isAdmin) {
                Button(
                    onClick = { showNewComplaintDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Complaint", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (visibleComplaints.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No complaints submitted.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visibleComplaints) { comp ->
                    val nurse = viewModel.repository.getNurse(comp.byNurseId)
                    val isOpen = comp.status == "open"
                    val canHandle = isOpen && (isAdmin || (currentRank >= comp.level && comp.byNurseId != loggedNurseId))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(comp.subject, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        text = "By ${nurse?.name ?: "Nurse"} (${nurse?.ward ?: ""}) • ${comp.date}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isOpen) Color(0xFFFEF3C7) else Color(0xFFDCFCE7)
                                ) {
                                    Text(
                                        text = if (isOpen) "With: ${authorityNames.getOrElse(comp.level) { "Authority" }}" else "Closed ✔",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isOpen) Color(0xFFB45309) else Color(0xFF15803D),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(comp.text, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))

                            // Action Logs Trail
                            if (comp.logs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    comp.logs.forEach { log ->
                                        Text(
                                            text = "↳ ${log.role} ${log.byName} (${log.action}, ${log.date}): ${log.text}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Authority Actions
                            if (canHandle) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { selectedComplaintForReply = comp },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                                ) {
                                    Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reply & Forward / Close")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Submit Dialog
    if (showNewComplaintDialog) {
        var subject by remember { mutableStateOf("") }
        var details by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewComplaintDialog = false },
            title = { Text("Submit Complaint", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Goes to your Controlling Officer, then escalates upward; the Nursing Superintendent takes final action.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("Details *") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subject.isNotBlank() && details.isNotBlank()) {
                            viewModel.repository.submitComplaint(subject.trim(), details.trim())
                            showNewComplaintDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewComplaintDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reply / Action Dialog
    selectedComplaintForReply?.let { comp ->
        var replyNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { selectedComplaintForReply = null },
            title = { Text("Action on Complaint") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Subject: ${comp.subject}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = replyNote,
                        onValueChange = { replyNote = it },
                        label = { Text("Action Note / Official Reply *") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (comp.level < 5 && !isAdmin) {
                        Button(
                            onClick = {
                                if (replyNote.isNotBlank()) {
                                    viewModel.repository.actOnComplaint(comp.id, replyNote.trim(), forward = true)
                                    selectedComplaintForReply = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) {
                            Text("Reply & Forward ↑")
                        }
                    }
                    Button(
                        onClick = {
                            if (replyNote.isNotBlank()) {
                                viewModel.repository.actOnComplaint(comp.id, replyNote.trim(), forward = false)
                                selectedComplaintForReply = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                    ) {
                        Text("Close / Final")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedComplaintForReply = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
