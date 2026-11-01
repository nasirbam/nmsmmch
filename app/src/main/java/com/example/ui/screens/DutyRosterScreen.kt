package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Holiday
import com.example.data.model.Nurse
import com.example.data.model.NurseHierarchy
import com.example.ui.NmsViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DutyRosterScreen(
    viewModel: NmsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val selectedWard by viewModel.selectedWard.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    val currentRank = viewModel.repository.getCurrentRank()
    val isAdmin = state.currentUser?.role == "admin"
    val loggedNurseId = state.currentUser?.nurseId

    // Calculate month days
    val calendar = remember(selectedMonth) {
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            val d = sdf.parse(selectedMonth) ?: Date()
            cal.time = d
            cal.set(Calendar.DAY_OF_MONTH, 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cal
    }

    val daysInMonth = remember(selectedMonth) {
        calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val monthName = remember(selectedMonth) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.US)
        sdf.format(calendar.time)
    }

    val dayDates = remember(selectedMonth, daysInMonth) {
        val list = mutableListOf<DayInfo>()
        val cal = calendar.clone() as Calendar
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        for (i in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, i)
            val dateStr = String.format(Locale.US, "%s-%02d", selectedMonth, i)
            val isFriday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
            val holiday = state.holidays.find { it.date == dateStr }
            list.add(
                DayInfo(
                    dayNumber = i,
                    dateString = dateStr,
                    dayOfWeekName = dayFormat.format(cal.time),
                    isFriday = isFriday,
                    holiday = holiday
                )
            )
        }
        list
    }

    // Ward nurses
    val wardNurses = remember(state.nurses, selectedWard) {
        state.nurses
            .filter { it.status != "pend" && it.ward == selectedWard }
            .sortedWith(
                compareByDescending<Nurse> { it.cat == "Nurse Incharge" }
                    .thenBy { it.name }
            )
    }

    val holidaysThisMonth = remember(state.holidays, selectedMonth) {
        state.holidays.filter { it.date.startsWith(selectedMonth) }
    }

    val dayOffAllowed = remember(selectedMonth) {
        viewModel.repository.getDayOffAllowanceForMonth(selectedMonth)
    }

    // Shift Assignment Dialog State
    var cellEditDialogData by remember { mutableStateOf<CellEditData?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Controls Row: Ward Selector & Month Navigation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ward dropdown
                    var wardMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { wardMenuExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.LocalHospital, contentDescription = null, tint = TealPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ward: $selectedWard",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = wardMenuExpanded,
                            onDismissRequest = { wardMenuExpanded = false }
                        ) {
                            state.wards.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w, fontWeight = if (w == selectedWard) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        viewModel.setSelectedWard(w)
                                        wardMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Month navigator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.shiftPrevMonth() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Month")
                        }

                        Text(
                            text = monthName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TealPrimary,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = { viewModel.shiftNextMonth() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                        }
                    }
                }

                // Month Holidays Banner
                if (holidaysThisMonth.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Govt Holidays:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        holidaysThisMonth.forEach { h ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = HolidayHighlight,
                                shadowElevation = 1.dp
                            ) {
                                Text(
                                    text = "${h.date.takeLast(2)}: ${h.name}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF14532D),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Shift Legend Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShiftBadge("M", "Morning 8a-2p", ShiftMorningBg, ShiftMorningColor)
            ShiftBadge("E", "Evening 2p-8p", ShiftEveningBg, ShiftEveningColor)
            ShiftBadge("N", "Night 8p-8a", ShiftNightBg, ShiftNightColor)
            ShiftBadge("D/O", "Day Off", ShiftDayOffBg, ShiftDayOffColor)
            ShiftBadge("CL", "Leave", ShiftLeaveBg, ShiftLeaveColor)
            ShiftBadge("Fri", "Weekly Off", FridayHighlight, Color(0xFF78350F))
            ShiftBadge("H", "Gazetted Holiday", HolidayHighlight, Color(0xFF14532D))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main Roster Grid Table
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            val horizontalScrollState = rememberScrollState()

            Column(modifier = Modifier.fillMaxSize()) {
                // Table Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TealPrimary)
                        .padding(vertical = 8.dp)
                        .horizontalScroll(horizontalScrollState),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SL",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp)
                    )
                    Text(
                        text = "Nurse Name / Quota",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.width(150.dp)
                    )
                    dayDates.forEach { d ->
                        Column(
                            modifier = Modifier
                                .width(38.dp)
                                .background(
                                    when {
                                        d.holiday != null -> HolidayHighlight
                                        d.isFriday -> FridayHighlight
                                        else -> Color.Transparent
                                    }
                                )
                                .padding(vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = String.format(Locale.US, "%02d", d.dayNumber),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (d.holiday != null || d.isFriday) Color(0xFF1E293B) else Color.White
                            )
                            Text(
                                text = d.dayOfWeekName,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (d.holiday != null || d.isFriday) Color(0xFF475569) else Color(0xFFCCFBF1)
                            )
                        }
                    }
                }

                if (wardNurses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No nurses assigned to $selectedWard ward.\nAdd nurses or change ward from directory.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        itemsIndexed(wardNurses) { index, nurse ->
                            val doUsed = viewModel.repository.getDayOffUsedForMonth(nurse.id, selectedMonth)
                            val isIncharge = nurse.cat == "Nurse Incharge"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (index % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .horizontalScroll(horizontalScrollState),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // SL
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(36.dp)
                                )

                                // Name & Quota
                                Column(
                                    modifier = Modifier
                                        .width(150.dp)
                                        .padding(vertical = 4.dp, horizontal = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = nurse.name,
                                            fontWeight = if (isIncharge) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                        if (isIncharge) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "★ Incharge",
                                                color = Color(0xFFD97706),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = "D/O: $doUsed / $dayOffAllowed",
                                        fontSize = 10.sp,
                                        color = if (doUsed >= dayOffAllowed) Color(0xFFB91C1C) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Days Cells
                                dayDates.forEach { day ->
                                    val cellDate = day.dateString

                                    // Check leave
                                    val leave = state.leaves.find { l ->
                                        l.nurseId == nurse.id && l.stage in listOf("OK", "FIN") && l.from <= cellDate && cellDate <= l.to
                                    }

                                    // Check roster shift
                                    var currentShift = ""
                                    listOf("M", "E", "N", "D").forEach { s ->
                                        val key = "$cellDate|$selectedWard|$s"
                                        if ((state.roster[key] ?: emptyList()).contains(nurse.id)) {
                                            currentShift = s
                                        }
                                    }

                                    // Check pending duty request
                                    val pendingReq = state.dutyRequests.find { q ->
                                        q.nurseId == nurse.id && q.date == cellDate && q.ward == selectedWard && q.stage in listOf("NI", "SUP")
                                    }

                                    val isPending = pendingReq != null
                                    val displayShift = pendingReq?.shift ?: currentShift

                                    val isEditable = isAdmin || currentRank >= 3 || (currentRank == 2 && loggedNurseId == nurse.id) || (currentRank == 1 && loggedNurseId == nurse.id)

                                    Box(
                                        modifier = Modifier
                                            .width(38.dp)
                                            .height(44.dp)
                                            .border(
                                                width = 0.5.dp,
                                                color = if (isPending) Color(0xFFD97706) else MaterialTheme.colorScheme.outlineVariant
                                            )
                                            .background(
                                                when {
                                                    leave != null -> ShiftLeaveBg
                                                    displayShift == "M" -> ShiftMorningBg
                                                    displayShift == "E" -> ShiftEveningBg
                                                    displayShift == "N" -> ShiftNightBg
                                                    displayShift == "D" -> ShiftDayOffBg
                                                    day.holiday != null -> HolidayHighlight.copy(alpha = 0.4f)
                                                    day.isFriday -> FridayHighlight.copy(alpha = 0.4f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable(enabled = isEditable && leave == null) {
                                                cellEditDialogData = CellEditData(
                                                    nurse = nurse,
                                                    date = cellDate,
                                                    currentShift = displayShift,
                                                    ward = selectedWard,
                                                    isPending = isPending
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (leave != null) {
                                            Text(
                                                text = leave.type,
                                                color = ShiftLeaveColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        } else if (displayShift.isNotEmpty()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                if (isPending) {
                                                    Text("⏳", fontSize = 9.sp)
                                                }
                                                Text(
                                                    text = if (displayShift == "D") "D/O" else displayShift,
                                                    color = when (displayShift) {
                                                        "M" -> ShiftMorningColor
                                                        "E" -> ShiftEveningColor
                                                        "N" -> ShiftNightColor
                                                        "D" -> ShiftDayOffColor
                                                        else -> Color.DarkGray
                                                    },
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = if (displayShift == "D") 10.sp else 12.sp
                                                )
                                            }
                                        } else if (isEditable) {
                                            Text("+", color = MaterialTheme.colorScheme.outlineVariant, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons Row (Export CSV, Help)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tap any cell with '+' to assign or request duty.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    Toast.makeText(context, "Exported Duty Roster ($selectedWard $monthName) successfully", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export Excel", fontSize = 12.sp)
            }
        }
    }

    // Cell Shift Assignment Dialog
    cellEditDialogData?.let { data ->
        ShiftPickerDialog(
            data = data,
            onDismiss = { cellEditDialogData = null },
            onSelectShift = { shift ->
                viewModel.repository.setRosterShift(
                    ward = data.ward,
                    date = data.date,
                    nurseId = data.nurse.id,
                    shift = shift
                )
                cellEditDialogData = null
            }
        )
    }
}

data class DayInfo(
    val dayNumber: Int,
    val dateString: String,
    val dayOfWeekName: String,
    val isFriday: Boolean,
    val holiday: Holiday?
)

data class CellEditData(
    val nurse: Nurse,
    val date: String,
    val currentShift: String,
    val ward: String,
    val isPending: Boolean
)

@Composable
fun ShiftBadge(label: String, desc: String, bg: Color, textCol: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textCol
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = desc,
                fontSize = 10.sp,
                color = textCol.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun ShiftPickerDialog(
    data: CellEditData,
    onDismiss: () -> Unit,
    onSelectShift: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Assign Duty Shift", fontWeight = FontWeight.Bold)
                Text(
                    text = "${data.nurse.name} • ${data.date}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShiftOptionButton(
                    code = "M",
                    title = "Morning Shift",
                    time = "8:00 AM – 2:00 PM",
                    color = ShiftMorningColor,
                    bg = ShiftMorningBg,
                    isSelected = data.currentShift == "M",
                    onClick = { onSelectShift("M") }
                )
                ShiftOptionButton(
                    code = "E",
                    title = "Evening Shift",
                    time = "2:00 PM – 8:00 PM",
                    color = ShiftEveningColor,
                    bg = ShiftEveningBg,
                    isSelected = data.currentShift == "E",
                    onClick = { onSelectShift("E") }
                )
                ShiftOptionButton(
                    code = "N",
                    title = "Night Shift",
                    time = "8:00 PM – 8:00 AM",
                    color = ShiftNightColor,
                    bg = ShiftNightBg,
                    isSelected = data.currentShift == "N",
                    onClick = { onSelectShift("N") }
                )
                ShiftOptionButton(
                    code = "D",
                    title = "Day Off (D/O)",
                    time = "Official Off-Day",
                    color = ShiftDayOffColor,
                    bg = ShiftDayOffBg,
                    isSelected = data.currentShift == "D",
                    onClick = { onSelectShift("D") }
                )
                if (data.currentShift.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { onSelectShift("") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remove Duty Shift")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ShiftOptionButton(
    code: String,
    title: String,
    time: String,
    color: Color,
    bg: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, color) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (code == "D") "D/O" else code,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
                Text(time, fontSize = 12.sp, color = color.copy(alpha = 0.8f))
            }
        }
    }
}
