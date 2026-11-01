package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Nurse
import com.example.data.model.NurseHierarchy
import com.example.ui.NmsViewModel
import com.example.ui.theme.TealPrimary
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NursesScreen(
    viewModel: NmsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val isAdmin = state.currentUser?.role == "admin"
    val currentRank = viewModel.repository.getCurrentRank()

    var searchQuery by remember { mutableStateOf("") }
    var wardFilter by remember { mutableStateOf("") }
    var editingNurse by remember { mutableStateOf<Nurse?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredNurses = remember(state.nurses, searchQuery, wardFilter) {
        val q = searchQuery.trim().lowercase()
        state.nurses.filter { n ->
            n.status != "pend" &&
                    (wardFilter.isEmpty() || n.ward == wardFilter) &&
                    (q.isEmpty() || n.name.lowercase().contains(q) || n.phone.contains(q) || n.hris.contains(q) || n.reg.lowercase().contains(q))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Search & Filter header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Nurses Directory", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TealPrimary)

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isAdmin) {
                            Button(
                                onClick = { showAddDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Nurse", fontSize = 12.sp)
                            }
                        }

                        IconButton(
                            onClick = {
                                Toast.makeText(context, "Exported ${filteredNurses.size} nurses to Excel", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = TealPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, phone, HRIS, BNMC...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Ward filter pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = wardFilter.isEmpty(),
                        onClick = { wardFilter = "" },
                        label = { Text("All (${state.nurses.count { it.status != "pend" }})", fontSize = 11.sp) }
                    )
                    state.wards.forEach { w ->
                        val count = state.nurses.count { it.status != "pend" && it.ward == w }
                        FilterChip(
                            selected = wardFilter == w,
                            onClick = { wardFilter = if (wardFilter == w) "" else w },
                            label = { Text("$w ($count)", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Nurses List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredNurses) { nurse ->
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
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(nurse.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    if (nurse.cat == "Nurse Incharge") {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFEF3C7)
                                        ) {
                                            Text(
                                                "★ Incharge",
                                                color = Color(0xFFD97706),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${nurse.cat} • Ward: ${nurse.ward.ifEmpty { "Not set" }}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isAdmin) {
                                Row {
                                    IconButton(onClick = { editingNurse = nurse }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TealPrimary)
                                    }
                                    IconButton(
                                        onClick = { viewModel.repository.deleteNurse(nurse.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFB91C1C))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("📱 ${nurse.phone}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("HRIS: ${nurse.hris}", fontSize = 12.sp)
                            if (nurse.reg.isNotBlank()) {
                                Text("BNMC: ${nurse.reg}", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        NurseEditDialog(
            nurse = Nurse(
                id = UUID.randomUUID().toString(),
                name = "",
                phone = "",
                hris = "",
                ward = state.wards.firstOrNull() ?: "Medicine"
            ),
            wards = state.wards,
            isNew = true,
            onDismiss = { showAddDialog = false },
            onSave = {
                viewModel.repository.saveNurse(it)
                showAddDialog = false
            }
        )
    }

    editingNurse?.let { n ->
        NurseEditDialog(
            nurse = n,
            wards = state.wards,
            isNew = false,
            onDismiss = { editingNurse = null },
            onSave = {
                viewModel.repository.saveNurse(it)
                editingNurse = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NurseEditDialog(
    nurse: Nurse,
    wards: List<String>,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (Nurse) -> Unit
) {
    var name by remember { mutableStateOf(nurse.name) }
    var phone by remember { mutableStateOf(nurse.phone) }
    var hris by remember { mutableStateOf(nurse.hris) }
    var reg by remember { mutableStateOf(nurse.reg) }
    var ward by remember { mutableStateOf(nurse.ward.ifEmpty { wards.firstOrNull() ?: "" }) }
    var cat by remember { mutableStateOf(nurse.cat) }
    var father by remember { mutableStateOf(nurse.father) }
    var mother by remember { mutableStateOf(nurse.mother) }
    var course by remember { mutableStateOf(nurse.course) }
    var pass by remember { mutableStateOf(nurse.pass) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Add Nurse" else "Edit Nurse Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (Login ID) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hris,
                    onValueChange = { hris = it },
                    label = { Text("HRIS ID *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reg,
                    onValueChange = { reg = it },
                    label = { Text("BNMC Reg. No.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Ward dropdown
                var wardExp by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = wardExp, onExpandedChange = { wardExp = !wardExp }) {
                    OutlinedTextField(
                        value = ward,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ward") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wardExp) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = wardExp, onDismissRequest = { wardExp = false }) {
                        wards.forEach { w ->
                            DropdownMenuItem(text = { Text(w) }, onClick = { ward = w; wardExp = false })
                        }
                    }
                }

                // Category dropdown
                var catExp by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = catExp, onExpandedChange = { catExp = !catExp }) {
                    OutlinedTextField(
                        value = cat,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExp) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = catExp, onDismissRequest = { catExp = false }) {
                        NurseHierarchy.CATEGORIES.forEach { c ->
                            DropdownMenuItem(text = { Text(c) }, onClick = { cat = c; catExp = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank() && hris.isNotBlank()) {
                        onSave(
                            nurse.copy(
                                name = name.trim(),
                                phone = phone.trim(),
                                hris = hris.trim(),
                                reg = reg.trim(),
                                ward = ward,
                                cat = cat,
                                father = father.trim(),
                                mother = mother.trim(),
                                course = course.trim(),
                                pass = pass.ifBlank { "1234" }
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
