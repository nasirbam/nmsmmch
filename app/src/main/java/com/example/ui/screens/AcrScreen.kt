package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.ui.NmsViewModel
import com.example.ui.theme.TealPrimary
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcrScreen(
    viewModel: NmsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val currentRank = viewModel.repository.getCurrentRank()
    val isAdmin = state.currentUser?.role == "admin"
    val loggedNurseId = state.currentUser?.nurseId

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR).toString() }
    var selectedAcrYear by remember { mutableStateOf(currentYear) }
    val years = remember { (2020..2027).reversed().map { it.toString() } }

    val canEdit = isAdmin || currentRank >= 3

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.selectMoreSubTab(null) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Annual Confidential Report (ACR)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TealPrimary)
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                // Year Dropdown
                var yearExp by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { yearExp = true },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Year: $selectedAcrYear", fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(expanded = yearExp, onDismissRequest = { yearExp = false }) {
                        years.forEach { y ->
                            DropdownMenuItem(
                                text = { Text(y) },
                                onClick = {
                                    selectedAcrYear = y
                                    yearExp = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        Toast.makeText(context, "Exported ACR report for $selectedAcrYear", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Table
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            val displayNurses = if (currentRank == 1 && !isAdmin) {
                state.nurses.filter { it.id == loggedNurseId }
            } else {
                state.nurses.filter { it.status != "pend" }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TealPrimary)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Nurse", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text("Ward", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(80.dp))
                        Text("ACR $selectedAcrYear", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(90.dp))
                    }
                }

                itemsIndexed(displayNurses) { index, nurse ->
                    val status = state.acr["$selectedAcrYear|${nurse.id}"] ?: ""

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (index % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(nurse.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("HRIS: ${nurse.hris}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Text(nurse.ward.ifEmpty { "—" }, fontSize = 12.sp, modifier = Modifier.width(80.dp))

                        Box(modifier = Modifier.width(90.dp)) {
                            if (canEdit) {
                                var acrMenuExp by remember { mutableStateOf(false) }
                                OutlinedButton(
                                    onClick = { acrMenuExp = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = when (status) {
                                            "Y" -> "Yes ✔"
                                            "N" -> "No ✘"
                                            else -> "—"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (status) {
                                            "Y" -> Color(0xFF15803D)
                                            "N" -> Color(0xFFB91C1C)
                                            else -> Color.Gray
                                        }
                                    )
                                }

                                DropdownMenu(expanded = acrMenuExp, onDismissRequest = { acrMenuExp = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Yes ✔", color = Color(0xFF15803D), fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            viewModel.repository.setAcr(selectedAcrYear, nurse.id, "Y")
                                            acrMenuExp = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("No ✘", color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            viewModel.repository.setAcr(selectedAcrYear, nurse.id, "N")
                                            acrMenuExp = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Clear (—)") },
                                        onClick = {
                                            viewModel.repository.setAcr(selectedAcrYear, nurse.id, "")
                                            acrMenuExp = false
                                        }
                                    )
                                }
                            } else {
                                Text(
                                    text = when (status) {
                                        "Y" -> "Yes ✔"
                                        "N" -> "No ✘"
                                        else -> "Pending"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (status) {
                                        "Y" -> Color(0xFF15803D)
                                        "N" -> Color(0xFFB91C1C)
                                        else -> Color.Gray
                                    }
                                )
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
