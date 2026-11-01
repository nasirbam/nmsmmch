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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NurseHierarchy
import com.example.data.model.NursePendingUpdate
import com.example.ui.NmsViewModel
import com.example.ui.theme.TealPrimary

@Composable
fun ProfileScreen(
    viewModel: NmsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val loggedNurseId = state.currentUser?.nurseId
    val nurse = state.nurses.find { it.id == loggedNurseId }

    var newPassword by remember { mutableStateOf("") }
    var showUpdateDialog by remember { mutableStateOf(false) }

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
            Text("My Profile & Authority Details", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TealPrimary)
        }

        if (nurse == null) {
            Text("Nurse profile not found.")
            return@Column
        }

        // Profile Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(nurse.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TealPrimary)
                        Text(nurse.cat, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFFD97706))
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFCCFBF1)
                    ) {
                        Text(
                            text = "Ward: ${nurse.ward.ifEmpty { "Unassigned" }}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF0F766E),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                ProfileFieldRow("Phone (Login ID)", nurse.phone)
                ProfileFieldRow("HRIS ID", nurse.hris)
                ProfileFieldRow("BNMC Reg. No.", nurse.reg.ifEmpty { "—" })
                ProfileFieldRow("Workplace", nurse.workplace.ifEmpty { "—" })
                ProfileFieldRow("Course", nurse.course.ifEmpty { "—" })
                ProfileFieldRow("Joining Date", nurse.joinDate.ifEmpty { "—" })
                ProfileFieldRow("Date of Birth", nurse.dob.ifEmpty { "—" })
                ProfileFieldRow("Father's Name", nurse.father.ifEmpty { "—" })
                ProfileFieldRow("Mother's Name", nurse.mother.ifEmpty { "—" })

                if (nurse.pend != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Text(
                            text = "⏳ You have a pending profile update waiting for Admin approval.",
                            fontSize = 12.sp,
                            color = Color(0xFFB45309),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showUpdateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Request Profile Update")
                }
            }
        }

        // Controlling Officers Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Controlling Officers & Hierarchy", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    "Your departmental supervisory structure for duty approval and complaints",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                val controllingNurses = remember(state.nurses, nurse.ward) {
                    val incharge = state.nurses.find { it.cat == "Nurse Incharge" && it.ward == nurse.ward }
                    val supervisor = state.nurses.find { it.cat == "Nursing Supervisor" }
                    val dns = state.nurses.find { it.cat == "Deputy Nursing Superintendent" }
                    val supt = state.nurses.find { it.cat == "Nursing Superintendent" }
                    listOfNotNull(
                        incharge?.let { "Nurse Incharge" to it },
                        supervisor?.let { "Nursing Supervisor" to it },
                        dns?.let { "Deputy Nursing Supt." to it },
                        supt?.let { "Nursing Superintendent" to it }
                    )
                }

                if (controllingNurses.isEmpty()) {
                    Text("No controlling officers configured yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                } else {
                    controllingNurses.forEach { (role, officer) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text(role, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Text(officer.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                            Text(officer.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        // Change Password Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Change Login Password", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (newPassword.isNotBlank()) {
                            viewModel.repository.saveNurse(nurse.copy(pass = newPassword.trim()))
                            newPassword = ""
                            Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Save New Password")
                }
            }
        }
    }

    if (showUpdateDialog && nurse != null) {
        val currentNurse = nurse
        var upName by remember { mutableStateOf(currentNurse.name) }
        var upPhone by remember { mutableStateOf(currentNurse.phone) }
        var upFather by remember { mutableStateOf(currentNurse.father) }
        var upMother by remember { mutableStateOf(currentNurse.mother) }
        var upWorkplace by remember { mutableStateOf(currentNurse.workplace) }
        var upCourse by remember { mutableStateOf(currentNurse.course) }

        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Request Profile Update") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = upName, onValueChange = { upName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = upPhone, onValueChange = { upPhone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = upWorkplace, onValueChange = { upWorkplace = it }, label = { Text("Workplace") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = upCourse, onValueChange = { upCourse = it }, label = { Text("Course") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = upFather, onValueChange = { upFather = it }, label = { Text("Father's Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = upMother, onValueChange = { upMother = it }, label = { Text("Mother's Name") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.repository.requestProfileUpdate(
                            nurseId = currentNurse.id,
                            pend = NursePendingUpdate(
                                name = upName.trim(),
                                phone = upPhone.trim(),
                                workplace = upWorkplace.trim(),
                                course = upCourse.trim(),
                                father = upFather.trim(),
                                mother = upMother.trim(),
                                cat = currentNurse.cat,
                                ward = currentNurse.ward
                            )
                        )
                        showUpdateDialog = false
                        Toast.makeText(context, "Profile update sent for Admin approval", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProfileFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
