package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.DutyRequest
import com.example.data.model.LeaveRecord
import com.example.data.model.Nurse
import com.example.data.model.NurseHierarchy
import com.example.ui.NmsViewModel
import com.example.ui.theme.ShiftLeaveBg
import com.example.ui.theme.ShiftLeaveColor
import com.example.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovalsScreen(
    viewModel: NmsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val currentRank = viewModel.repository.getCurrentRank()
    val isAdmin = state.currentUser?.role == "admin"

    val pendingRegistrations = remember(state.nurses, isAdmin) {
        if (isAdmin) state.nurses.filter { it.status == "pend" } else emptyList()
    }

    val pendingProfileUpdates = remember(state.nurses, isAdmin) {
        if (isAdmin) state.nurses.filter { it.pend != null } else emptyList()
    }

    val pendingLeaves = remember(state.leaves, currentRank, isAdmin) {
        state.leaves.filter { l ->
            l.stage in listOf("NI", "SUP", "FIN")
        }
    }

    val pendingDutyRequests = remember(state.dutyRequests, currentRank, isAdmin) {
        state.dutyRequests.filter { q ->
            q.stage in listOf("NI", "SUP")
        }
    }

    val totalPending = pendingRegistrations.size + pendingProfileUpdates.size + pendingLeaves.size + pendingDutyRequests.size

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Approvals Center", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TealPrimary)
                        Text(
                            "Hospital hierarchical approval hub for Duty, Leave, Registrations",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (totalPending > 0) Color(0xFFFEF3C7) else Color(0xFFDCFCE7)
                    ) {
                        Text(
                            text = if (totalPending > 0) "$totalPending Pending" else "All Clear ✔",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (totalPending > 0) Color(0xFFB45309) else Color(0xFF15803D),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Section 1: Nurse Registrations (Admin only)
        if (pendingRegistrations.isNotEmpty()) {
            item {
                Text(
                    text = "📝 Pending Registrations (${pendingRegistrations.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TealPrimary
                )
            }
            items(pendingRegistrations) { nurse ->
                RegistrationApprovalCard(
                    nurse = nurse,
                    onApprove = { cat -> viewModel.repository.approveNurseRegistration(nurse.id, cat) },
                    onReject = { viewModel.repository.rejectNurseRegistration(nurse.id) }
                )
            }
        }

        // Section 2: Profile Update Requests (Admin only)
        if (pendingProfileUpdates.isNotEmpty()) {
            item {
                Text(
                    text = "✏ Profile Update Requests (${pendingProfileUpdates.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TealPrimary
                )
            }
            items(pendingProfileUpdates) { nurse ->
                val p = nurse.pend ?: return@items
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Nurse: ${nurse.name}", fontWeight = FontWeight.Bold)
                        Text("Requested changes: ${p.name}, Phone: ${p.phone}, Cat: ${p.cat}, Ward: ${p.ward}", fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.repository.approveProfileUpdate(nurse.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                            ) {
                                Text("Approve")
                            }
                            OutlinedButton(
                                onClick = { viewModel.repository.rejectProfileUpdate(nurse.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB91C1C))
                            ) {
                                Text("Reject")
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Leave Requests
        if (pendingLeaves.isNotEmpty()) {
            item {
                Text(
                    text = "🏖 Pending Leave Requests (${pendingLeaves.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TealPrimary
                )
            }
            items(pendingLeaves) { leave ->
                val nurse = viewModel.repository.getNurse(leave.nurseId)
                val days = viewModel.repository.calculateDays(leave.from, leave.to)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(nurse?.name ?: "Nurse", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${nurse?.ward ?: ""} • ${nurse?.cat ?: ""}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = ShiftLeaveBg) {
                                Text(
                                    text = "${leave.type} ($days days)",
                                    color = ShiftLeaveColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text("${leave.from} to ${leave.to} • Reason: ${leave.reason.ifEmpty { "None" }}", fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Stage: ${
                                when (leave.stage) {
                                    "NI" -> "Awaiting Nurse Incharge"
                                    "SUP" -> "Awaiting Nursing Supervisor"
                                    "FIN" -> "Awaiting DNS / Superintendent"
                                    else -> leave.stage
                                }
                            }",
                            fontSize = 11.sp,
                            color = Color(0xFFD97706),
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.repository.approveLeave(leave.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Approve / Forward")
                            }
                            OutlinedButton(
                                onClick = { viewModel.repository.rejectLeave(leave.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB91C1C))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reject")
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Duty Roster Requests
        if (pendingDutyRequests.isNotEmpty()) {
            item {
                Text(
                    text = "📅 Pending Duty Roster Requests (${pendingDutyRequests.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TealPrimary
                )
            }
            items(pendingDutyRequests) { req ->
                val nurse = viewModel.repository.getNurse(req.nurseId)
                var currentShiftChoice by remember { mutableStateOf(req.shift) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(nurse?.name ?: "Nurse", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Ward: ${req.ward} • Date: ${req.date}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = if (req.stage == "NI") "Needs Incharge" else "Needs Supervisor",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Shift:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            listOf("M", "E", "N", "D").forEach { s ->
                                FilterChip(
                                    selected = currentShiftChoice == s,
                                    onClick = { currentShiftChoice = s },
                                    label = { Text(if (s == "D") "D/O" else s, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.repository.approveDutyRequest(req.id, currentShiftChoice) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Approve Duty")
                            }
                            OutlinedButton(
                                onClick = { viewModel.repository.rejectDutyRequest(req.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB91C1C))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reject")
                            }
                        }
                    }
                }
            }
        }

        if (totalPending == 0) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Nothing pending approval right now!", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("All registrations, leave and duty roster requests are cleared.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationApprovalCard(
    nurse: Nurse,
    onApprove: (String) -> Unit,
    onReject: () -> Unit
) {
    var selectedCat by remember { mutableStateOf(nurse.cat) }
    var catExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(nurse.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                "Phone: ${nurse.phone} • HRIS: ${nurse.hris} • Ward: ${nurse.ward.ifEmpty { "Not set" }}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (nurse.reg.isNotBlank()) {
                Text("BNMC: ${nurse.reg} • Course: ${nurse.course}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Assign Category
            ExposedDropdownMenuBox(
                expanded = catExpanded,
                onExpandedChange = { catExpanded = !catExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCat,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Assign Designation / Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = catExpanded,
                    onDismissRequest = { catExpanded = false }
                ) {
                    NurseHierarchy.CATEGORIES.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c) },
                            onClick = {
                                selectedCat = c
                                catExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onApprove(selectedCat) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve Registration")
                }
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB91C1C))
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject")
                }
            }
        }
    }
}
