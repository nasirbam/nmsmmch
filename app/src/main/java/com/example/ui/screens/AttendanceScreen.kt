package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Nurse
import com.example.ui.NmsViewModel
import com.example.ui.theme.TealPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
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

    var showAddTimeDialogForNurse by remember { mutableStateOf<Nurse?>(null) }

    // If Staff Nurse (Rank 1): show personal monthly attendance card
    val isStaffNurse = currentRank == 1 && !isAdmin

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Controls Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ward selector (if not staff nurse)
                if (!isStaffNurse) {
                    var wardExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { wardExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.LocalHospital, contentDescription = null, tint = TealPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ward: $selectedWard", fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = wardExpanded,
                            onDismissRequest = { wardExpanded = false }
                        ) {
                            state.wards.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w) },
                                    onClick = {
                                        viewModel.setSelectedWard(w)
                                        wardExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = TealPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("My Monthly Attendance", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                // Date Picker Quick Controller
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedDate,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isStaffNurse) {
            // Staff Nurse personal monthly attendance
            val myNurse = state.nurses.find { it.id == loggedNurseId }
            val cal = Calendar.getInstance()
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val monthPrefix = selectedDate.take(7)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    item {
                        Text(
                            text = "${myNurse?.name ?: ""} (${myNurse?.ward ?: ""}) - $monthPrefix",
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(daysInMonth) { index ->
                        val dayNum = index + 1
                        val dateStr = String.format(Locale.US, "%s-%02d", monthPrefix, dayNum)
                        val attStatus = state.attendance["$dateStr|$loggedNurseId"] ?: "—"
                        val times = state.arrivalTimes["$dateStr|$loggedNurseId"] ?: emptyList()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dateStr, fontSize = 13.sp, fontWeight = FontWeight.Medium)

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (attStatus) {
                                        "P" -> Color(0xFFDCFCE7)
                                        "A" -> Color(0xFFFEE2E2)
                                        "Late" -> Color(0xFFFEF3C7)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = when (attStatus) {
                                            "P" -> "Present"
                                            "A" -> "Absent"
                                            "Late" -> "Late"
                                            else -> "—"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (attStatus) {
                                            "P" -> Color(0xFF15803D)
                                            "A" -> Color(0xFFB91C1C)
                                            "Late" -> Color(0xFFB45309)
                                            else -> Color.Gray
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                if (times.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = times.joinToString(", "),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        } else {
            // Incharge / Supervisor / Admin daily ward attendance
            val wardNurses = state.nurses.filter { it.status != "pend" && it.ward == selectedWard }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                if (wardNurses.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No nurses assigned to $selectedWard", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(wardNurses) { index, nurse ->
                            val currentAtt = state.attendance["$selectedDate|${nurse.id}"] ?: ""
                            val arrivalTimes = state.arrivalTimes["$selectedDate|${nurse.id}"] ?: emptyList()

                            // Check shift
                            var assignedShift = ""
                            listOf("M", "E", "N", "D").forEach { s ->
                                if ((state.roster["$selectedDate|$selectedWard|$s"] ?: emptyList()).contains(nurse.id)) {
                                    assignedShift = s
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (index % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = nurse.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "HRIS: ${nurse.hris} • Shift: ${if (assignedShift.isEmpty()) "Not assigned" else assignedShift}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Attendance Status selector chips
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        state.attTypes.forEach { attType ->
                                            val isSelected = currentAtt == attType.code
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    viewModel.repository.setAttendance(
                                                        date = selectedDate,
                                                        nurseId = nurse.id,
                                                        status = if (isSelected) "" else attType.code
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = attType.code,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = when (attType.code) {
                                                        "P" -> Color(0xFF22C55E)
                                                        "A" -> Color(0xFFEF4444)
                                                        else -> Color(0xFFF59E0B)
                                                    },
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Arrival Timestamps row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Arrivals:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))

                                    arrivalTimes.forEachIndexed { tIndex, timeStr ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(timeStr, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable {
                                                            viewModel.repository.removeArrivalTime(
                                                                date = selectedDate,
                                                                nurseId = nurse.id,
                                                                index = tIndex
                                                            )
                                                        }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    IconButton(
                                        onClick = { showAddTimeDialogForNurse = nurse },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AddCircle,
                                            contentDescription = "Add Arrival Time",
                                            tint = TealPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Export Attendance button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        Toast.makeText(context, "Exported Attendance for $selectedWard ($selectedDate) to Excel", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Month Attendance", fontSize = 12.sp)
                }
            }
        }
    }

    // Add Arrival Time Dialog
    showAddTimeDialogForNurse?.let { nurse ->
        var timeInput by remember {
            val sdf = SimpleDateFormat("HH:mm", Locale.US)
            mutableStateOf(sdf.format(Date()))
        }

        AlertDialog(
            onDismissRequest = { showAddTimeDialogForNurse = null },
            title = { Text("Add Ward Arrival Time") },
            text = {
                Column {
                    Text(
                        text = "Nurse: ${nurse.name} • $selectedDate",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = timeInput,
                        onValueChange = { timeInput = it },
                        label = { Text("Time (e.g. 07:55 AM or 14:15)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (timeInput.isNotBlank()) {
                            viewModel.repository.addArrivalTime(
                                date = selectedDate,
                                nurseId = nurse.id,
                                time = timeInput.trim()
                            )
                        }
                        showAddTimeDialogForNurse = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTimeDialogForNurse = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
