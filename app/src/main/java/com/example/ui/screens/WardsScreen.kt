package com.example.ui.screens

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
import com.example.ui.NmsViewModel
import com.example.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardsScreen(
    viewModel: NmsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var showAddWardDialog by remember { mutableStateOf(false) }
    var wardToRename by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.selectMoreSubTab(null) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Hospital Wards", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TealPrimary)
            }

            Button(
                onClick = { showAddWardDialog = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Ward", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.wards) { ward ->
                val nurseCount = state.nurses.count { it.status != "pend" && it.ward == ward }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(ward, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("$nurseCount active nurses assigned", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row {
                            IconButton(onClick = { wardToRename = ward }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = TealPrimary)
                            }
                            IconButton(
                                onClick = { viewModel.repository.deleteWard(ward) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFB91C1C))
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Ward Dialog
    if (showAddWardDialog) {
        var newWardName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddWardDialog = false },
            title = { Text("Add Hospital Ward") },
            text = {
                OutlinedTextField(
                    value = newWardName,
                    onValueChange = { newWardName = it },
                    label = { Text("Ward Name (e.g. CCU, Burn Unit)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newWardName.isNotBlank()) {
                            viewModel.repository.addWard(newWardName.trim())
                            showAddWardDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWardDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Rename Ward Dialog
    wardToRename?.let { oldName ->
        var renameInput by remember { mutableStateOf(oldName) }
        AlertDialog(
            onDismissRequest = { wardToRename = null },
            title = { Text("Rename Ward") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("New Ward Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank() && renameInput.trim() != oldName) {
                            viewModel.repository.renameWard(oldName, renameInput.trim())
                        }
                        wardToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { wardToRename = null }) { Text("Cancel") }
            }
        )
    }
}
