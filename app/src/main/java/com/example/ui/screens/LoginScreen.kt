package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Nurse
import com.example.data.model.NurseHierarchy
import com.example.ui.NmsViewModel
import com.example.ui.theme.TealPrimary
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: NmsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    var isAdminTab by remember { mutableStateOf(false) }
    var usernameOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRegisterDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F766E),
                        Color(0xFF1E3A8A),
                        Color(0xFF0F172A)
                    )
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hospital Logo Badge
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF14B8A6), Color(0xFF2563EB))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalHospital,
                        contentDescription = "NMS Logo",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "NMS",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = TealPrimary,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "NURSES MANAGEMENT SYSTEM",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "✚ Care · Compassion · Commitment ✚",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0D9488),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Role Selector Switch
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = !isAdminTab,
                        onClick = {
                            isAdminTab = false
                            errorMessage = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            Icon(
                                Icons.Default.MedicalServices,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    ) {
                        Text("Nurse Login")
                    }

                    SegmentedButton(
                        selected = isAdminTab,
                        onClick = {
                            isAdminTab = true
                            errorMessage = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    ) {
                        Text("Admin Login")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (errorMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFB91C1C),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Input: Username / Phone
                OutlinedTextField(
                    value = usernameOrPhone,
                    onValueChange = {
                        usernameOrPhone = it
                        errorMessage = null
                    },
                    label = {
                        Text(if (isAdminTab) "Admin Username" else "Phone No. (Login ID)")
                    },
                    placeholder = {
                        Text(if (isAdminTab) "admin" else "01711000001")
                    },
                    leadingIcon = {
                        Icon(
                            if (isAdminTab) Icons.Default.Person else Icons.Default.Phone,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isAdminTab) KeyboardType.Text else KeyboardType.Phone
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Input: Password
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Password") },
                    placeholder = {
                        Text(if (isAdminTab) "admin123" else "1234")
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Login Button
                Button(
                    onClick = {
                        if (isAdminTab) {
                            val success = viewModel.repository.loginAdmin(usernameOrPhone, password)
                            if (!success) {
                                errorMessage = "Invalid admin username or password (default: admin / admin123)"
                            }
                        } else {
                            val err = viewModel.repository.loginNurse(usernameOrPhone, password)
                            if (err != null) {
                                errorMessage = err
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Icon(Icons.Default.Login, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Login to System", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                if (!isAdminTab) {
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showRegisterDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Register as New Nurse")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Admin: admin / admin123  •  Nurse default: 1234\nNasir, RN — Bangladesh Hospital NMS",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showRegisterDialog) {
        RegisterNurseDialog(
            wards = state.wards,
            onDismiss = { showRegisterDialog = false },
            onSubmit = { nurse ->
                val success = viewModel.repository.registerNurse(nurse)
                showRegisterDialog = false
                if (success) {
                    errorMessage = "Registration submitted successfully! You can login once Admin approves."
                } else {
                    errorMessage = "This phone number is already registered in the system."
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterNurseDialog(
    wards: List<String>,
    onDismiss: () -> Unit,
    onSubmit: (Nurse) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var hris by remember { mutableStateOf("") }
    var bnmc by remember { mutableStateOf("") }
    var ward by remember { mutableStateOf(wards.firstOrNull() ?: "Medicine") }
    var category by remember { mutableStateOf(NurseHierarchy.CATEGORIES.first()) }
    var father by remember { mutableStateOf("") }
    var mother by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var workplace by remember { mutableStateOf("") }
    var joinDate by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("1234") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = TealPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nurse Registration", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Submit your profile details. Registration requires Admin approval before login.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone No. (Login ID) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
                    value = bnmc,
                    onValueChange = { bnmc = it },
                    label = { Text("BNMC Reg. No.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Ward selection
                var wardExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = wardExpanded,
                    onExpandedChange = { wardExpanded = !wardExpanded }
                ) {
                    OutlinedTextField(
                        value = ward,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assigned Ward") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wardExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = wardExpanded,
                        onDismissRequest = { wardExpanded = false }
                    ) {
                        wards.forEach { w ->
                            DropdownMenuItem(
                                text = { Text(w) },
                                onClick = {
                                    ward = w
                                    wardExpanded = false
                                }
                            )
                        }
                    }
                }

                // Category selection
                var catExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category / Designation") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        NurseHierarchy.CATEGORIES.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = {
                                    category = c
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = course,
                    onValueChange = { course = it },
                    label = { Text("Course / Qualification") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = workplace,
                    onValueChange = { workplace = it },
                    label = { Text("Workplace / Hospital") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = father,
                    onValueChange = { father = it },
                    label = { Text("Father's Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = mother,
                    onValueChange = { mother = it },
                    label = { Text("Mother's Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("Date of Birth (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = joinDate,
                    onValueChange = { joinDate = it },
                    label = { Text("Joining Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Set Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank() || hris.isBlank()) {
                        validationError = "Name, Phone No. and HRIS ID are required."
                        return@Button
                    }
                    val newNurse = Nurse(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        phone = phone.trim(),
                        hris = hris.trim(),
                        reg = bnmc.trim(),
                        ward = ward,
                        cat = category,
                        father = father.trim(),
                        mother = mother.trim(),
                        dob = dob.trim(),
                        workplace = workplace.trim(),
                        joinDate = joinDate.trim(),
                        course = course.trim(),
                        pass = pass.ifBlank { "1234" },
                        status = "pend"
                    )
                    onSubmit(newNurse)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Submit Registration")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
