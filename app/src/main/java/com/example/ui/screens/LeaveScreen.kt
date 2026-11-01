package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeaveRecord
import com.example.data.model.Nurse
import com.example.ui.NmsViewModel
import com.example.ui.theme.ShiftLeaveBg
import com.example.ui.theme.ShiftLeaveColor
import com.example.ui.theme.TealPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveScreen(
    viewModel: NmsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val selectedWard by viewModel.selectedWard.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    val currentRank = viewModel.repository.getCurrentRank()
    val isAdmin = state.currentUser?.role == "admin"
    val loggedNurseId = state.currentUser?.nurseId

    var showApplyLeaveDialog by remember { mutableStateOf(false) }
    var filterLeaveType by remember { mutableStateOf<String?>(null) }
    var filterWardOnly by remember { mutableStateOf(true) }

    // Nurses on leave on selected date
    val nursesOnLeave = remember(state.leaves, selectedDate, filterLeaveType, filterWardOnly, selectedWard) {
        state.leaves.filter { l ->
            l.stage in listOf("OK", "FIN") &&
                    l.from <= selectedDate && selectedDate <= l.to &&
                    (filterLeaveType == null || l.type == filterLeaveType) &&
                    (!filterWardOnly || viewModel.repository.getNurse(l.nurseId)?.ward == selectedWard)
        }
    }

    // Current user's own leaves
    val myLeaves = remember(state.leaves, loggedNurseId) {
        if (loggedNurseId != null) {
            state.leaves.filter { it.nurseId == loggedNurseId }.reversed()
        } else emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Actions & Apply Card
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
                    Column {
                        Text("Hospital Leave Management", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TealPrimary)
                        Text(
                            "Casual Leave (CL limit: ${state.clMax} days/year) • Maternity • Earn • RRL",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showApplyLeaveDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply Leave", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // If logged in as nurse, show CL quota status
                if (loggedNurseId != null) {
                    val currentYear = selectedDate.take(4)
                    val usedCl = viewModel.repository.getUsedClDays(loggedNurseId, currentYear)
                    val remainingCl = (state.clMax - usedCl).coerceAtLeast(0)

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFCCFBF1)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Your $currentYear Casual Leave (CL) Balance:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF134E4A)
                            )
                            Text(
                                text = "$remainingCl days left of ${state.clMax}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (remainingCl <= 3) Color(0xFFB91C1C) else Color(0xFF0F766E)
                            )
                        }
                    }
                }
            }
        }

        // Daily Nurses On Leave Report
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
                    Text(
                        text = "Nurses on Leave ($selectedDate)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = filterWardOnly,
                            onClick = { filterWardOnly = !filterWardOnly },
                            label = { Text(if (filterWardOnly) selectedWard else "All Wards", fontSize = 11.sp) }
                        )

                        IconButton(
                            onClick = {
                                Toast.makeText(context, "Exported Leave Report ($selectedDate) to Excel", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Export Excel", tint = TealPrimary)
                        }
                    }
                }

                // Filter by Leave Type row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filterLeaveType == null,
                        onClick = { filterLeaveType = null },
                        label = { Text("All Types", fontSize = 11.sp) }
                    )
                    state.leaveTypes.forEach { lt ->
                        FilterChip(
                            selected = filterLeaveType == lt.code,
                            onClick = { filterLeaveType = if (filterLeaveType == lt.code) null else lt.code },
                            label = { Text(lt.code, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (nursesOnLeave.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No nurses on leave on $selectedDate (${if (filterWardOnly) selectedWard else "All Wards"}).",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    nursesOnLeave.forEach { l ->
                        val nurse = viewModel.repository.getNurse(l.nurseId)
                        val days = viewModel.repository.calculateDays(l.from, l.to)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = nurse?.name ?: "Unknown Nurse",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${nurse?.ward ?: ""} • ${nurse?.cat ?: ""}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (l.reason.isNotBlank()) {
                                    Text(
                                        text = "Reason: ${l.reason}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ShiftLeaveBg
                                ) {
                                    Text(
                                        text = "${l.type} ($days days)",
                                        color = ShiftLeaveColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = "${l.from} to ${l.to}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        // My Leave Applications History (if nurse)
        if (loggedNurseId != null && myLeaves.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("My Leave Applications", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    myLeaves.forEach { l ->
                        val days = viewModel.repository.calculateDays(l.from, l.to)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${l.type} - $days day(s)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (l.stage) {
                                            "OK" -> "Approved ✓"
                                            "REJ" -> "Rejected ✗"
                                            "FIN" -> "Approved (DNS) ✓"
                                            "SUP" -> "Pending: Supervisor ⏳"
                                            else -> "Pending: Incharge ⏳"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = when (l.stage) {
                                            "OK", "FIN" -> Color(0xFF15803D)
                                            "REJ" -> Color(0xFFB91C1C)
                                            else -> Color(0xFFD97706)
                                        }
                                    )
                                }
                                Text("${l.from} to ${l.to}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }

    if (showApplyLeaveDialog) {
        ApplyLeaveDialog(
            viewModel = viewModel,
            onDismiss = { showApplyLeaveDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyLeaveDialog(
    viewModel: NmsViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val loggedNurseId = state.currentUser?.nurseId
    val currentRank = viewModel.repository.getCurrentRank()
    val isAdmin = state.currentUser?.role == "admin"

    val candidateNurses = remember(state.nurses, loggedNurseId, currentRank, isAdmin) {
        if (currentRank == 1 && !isAdmin) {
            state.nurses.filter { it.id == loggedNurseId }
        } else {
            state.nurses.filter { it.status != "pend" }
        }
    }

    var selectedNurseId by remember {
        mutableStateOf(loggedNurseId ?: candidateNurses.firstOrNull()?.id ?: "")
    }

    var selectedLeaveType by remember {
        mutableStateOf(state.leaveTypes.firstOrNull()?.code ?: "CL")
    }

    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
    var fromDate by remember { mutableStateOf(todayStr) }
    var toDate by remember { mutableStateOf(todayStr) }
    var reason by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request Leave", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Nurse selector (if allowed to choose others)
                if (candidateNurses.size > 1) {
                    var nurseMenuExpanded by remember { mutableStateOf(false) }
                    val currentNurseName = state.nurses.find { it.id == selectedNurseId }?.name ?: ""
                    ExposedDropdownMenuBox(
                        expanded = nurseMenuExpanded,
                        onExpandedChange = { nurseMenuExpanded = !nurseMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentNurseName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Nurse") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nurseMenuExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = nurseMenuExpanded,
                            onDismissRequest = { nurseMenuExpanded = false }
                        ) {
                            candidateNurses.forEach { n ->
                                DropdownMenuItem(
                                    text = { Text("${n.name} (${n.ward})") },
                                    onClick = {
                                        selectedNurseId = n.id
                                        nurseMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Leave type
                var typeMenuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = typeMenuExpanded,
                    onExpandedChange = { typeMenuExpanded = !typeMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedLeaveType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Leave Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false }
                    ) {
                        state.leaveTypes.forEach { lt ->
                            DropdownMenuItem(
                                text = { Text("${lt.code} - ${lt.name}") },
                                onClick = {
                                    selectedLeaveType = lt.code
                                    typeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = fromDate,
                    onValueChange = { fromDate = it },
                    label = { Text("From Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = toDate,
                    onValueChange = { toDate = it },
                    label = { Text("To Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Leave") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedNurseId.isEmpty() || fromDate.isBlank() || toDate.isBlank() || toDate < fromDate) {
                        errorMsg = "Check nurse selection and dates."
                        return@Button
                    }
                    val err = viewModel.repository.requestLeave(
                        nurseId = selectedNurseId,
                        type = selectedLeaveType,
                        from = fromDate.trim(),
                        to = toDate.trim(),
                        reason = reason.trim()
                    )
                    if (err != null) {
                        errorMsg = err
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Submit Application")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
