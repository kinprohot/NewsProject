package com.gk.news_pro.page.screen.settings_screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Icon back
import androidx.compose.material.icons.filled.ArrowDropDown // Giữ lại icon này nếu bạn dùng
import androidx.compose.material.icons.filled.*  // Icon cho ngôn ngữ
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gk.news_pro.data.model.User
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gk.news_pro.data.repository.UserPreferencesRepository
import com.gk.news_pro.data.repository.UserRepository
import com.gk.news_pro.page.main_viewmodel.ViewModelFactory




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeUpdated: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val userRepository = remember { UserRepository() }
    val userPreferencesRepository = remember { UserPreferencesRepository(context.applicationContext) }

    val viewModel: SettingsViewModel = viewModel(
        factory = ViewModelFactory(listOf(userRepository, userPreferencesRepository))
    )

    val selectedLanguageCode by viewModel.selectedLanguage.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val adminUser by viewModel.admin.collectAsState()
    val allUsersForAdmin by viewModel.allUsers.collectAsState()
    val adminActionStatus by viewModel.adminActionStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }


    LaunchedEffect(adminActionStatus) {
        when (val status = adminActionStatus) {
            is AdminActionStatus.Success -> {
                snackbarHostState.showSnackbar(status.message)
                viewModel.clearAdminActionStatus()
            }
            is AdminActionStatus.Error -> {
                snackbarHostState.showSnackbar(status.message, duration = SnackbarDuration.Long)
                viewModel.clearAdminActionStatus()
            }
            else -> Unit
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") }, // ĐÃ DỊCH
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, // GIỮ NGUYÊN ICON
                            contentDescription = "Back" // ĐÃ DỊCH
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsGroupTitle("Location") // ĐÃ DỊCH (Giữ "Location" nếu đó là text gốc của bạn)
                // Nếu text gốc là "Ngôn ngữ Tin tức", thì dịch thành "News Language"
                LanguageDropdownSelector(
                    icon = Icons.Default.Place, // GIỮ NGUYÊN ICON GỐC CỦA BẠN (Place)
                    items = supportedLanguageSettings,
                    selectedLanguageCode = selectedLanguageCode,
                    onLanguageSelected = { selectedItem ->
                        viewModel.updateSelectedLanguage(selectedItem.languageCode)
                    }
                )
            }

            item {
                SettingsGroupTitle("Interface") // ĐÃ DỊCH (Giữ "Interface" nếu đó là text gốc)
                // Nếu text gốc là "Giao diện", thì dịch thành "Appearance"
                ThemeToggleItem(
                    isDarkTheme = isDarkTheme,
                    onThemeChange = { newThemeState ->
                        viewModel.toggleTheme(newThemeState)
                        onThemeUpdated(newThemeState)
                    }
                )
            }

            if (adminUser) {
                item {
                    AdminPanel(
                        users = allUsersForAdmin,
                        currentUser = viewModel.currentUser.collectAsState().value,
                        onAddUser = { username, email, password, makeAdmin ->
                            viewModel.adminCreateNewUser(username, email, password, makeAdmin)
                        },
                        onEditUser = { userId, username, email, password, admin ->
                            viewModel.adminUpdateUser(userId, username, email, password, admin)
                        },
                        onDeleteUser = { userId ->
                            viewModel.adminDeleteUser(userId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsGroupTitle(title: String) {
    Text(
        text = title, // title này sẽ được truyền vào là tiếng Anh từ nơi gọi
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDropdownSelector(
    label: String = "Select Your Location", // ĐÃ DỊCH (Nếu text gốc là "Chọn ngôn ngữ")
    icon: ImageVector, // Icon được truyền từ nơi gọi, giữ nguyên
    items: List<LanguageSettingItem>,
    selectedLanguageCode: String,
    onLanguageSelected: (LanguageSettingItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentSelectedItem = items.find { it.languageCode == selectedLanguageCode }
        ?: items.find { it.languageCode == "en" } // Giả sử 'en' là một lựa chọn mặc định nếu 'vi' không có
        ?: items.firstOrNull()

    if (currentSelectedItem == null) {
        Text("Error: No languages available or selected language not found.") // ĐÃ DỊCH
        return
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = currentSelectedItem.displayName, // Sẽ hiển thị tiếng Anh nếu LanguageSettingItem có displayName tiếng Anh
            onValueChange = {},
            label = { Text(label) }, // Label sẽ là tiếng Anh
            leadingIcon = { Icon(icon, contentDescription = label) }, // contentDescription sẽ là tiếng Anh
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            items.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption.displayName) }, // Hiển thị displayName (có thể cần dịch trong supportedLanguageSettings)
                    onClick = {
                        onLanguageSelected(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ThemeToggleItem(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onThemeChange(!isDarkTheme) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isDarkTheme) Icons.Default.Settings else Icons.Default.Star, // GIỮ NGUYÊN ICON GỐC CỦA BẠN
            contentDescription = "Toggle Theme", // ĐÃ DỊCH
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "Dark Theme", // ĐÃ DỊCH (Nếu text gốc là "Giao diện tối" hoặc "Darktheme")
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isDarkTheme,
            onCheckedChange = onThemeChange
        )
    }
}

@Composable
fun AdminPanel(
    users: List<User>,
    currentUser: User?,
    onAddUser: (username: String, email: String, password: String, makeAdmin: Boolean) -> Unit,
    onEditUser: (userId: String, username: String?, email: String?, password: String?, admin: Boolean?) -> Unit,
    onDeleteUser: (userId: String) -> Unit
) {
    var showAddUserDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<User?>(null) }

    SettingsGroupTitle("Admin - User Management") // ĐÃ DỊCH

    Button(
        onClick = { showAddUserDialog = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add User", modifier = Modifier.padding(end = 8.dp)) // ĐÃ DỊCH (contentDescription)
        Text("Add New User") // ĐÃ DỊCH
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (users.isEmpty()) {
        Text("No users found or still loading...") // ĐÃ DỊCH
    } else {
        users.forEach { user ->
            UserManagementItem(
                user = user,
                isCurrentUser = user.id == currentUser?.id,
                onEdit = { editingUser = user },
                onDelete = { onDeleteUser(user.id) },
                onToggleAdmin = { newAdminStatus ->
                    onEditUser(user.id, null, null, null, newAdminStatus)
                }
            )
            Divider()
        }
    }

    if (showAddUserDialog) {
        UserEditDialog(
            user = null,
            onDismiss = { showAddUserDialog = false },
            onSave = { _, username, email, password, admin ->
                onAddUser(username, email, password, admin)
                showAddUserDialog = false
            }
        )
    }

    editingUser?.let { userToEdit ->
        UserEditDialog(
            user = userToEdit,
            onDismiss = { editingUser = null },
            onSave = { userId, username, email, password, admin ->
                onEditUser(userId, username, email, password, admin)
                editingUser = null
            }
        )
    }
}

@Composable
fun UserManagementItem(
    user: User,
    isCurrentUser: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleAdmin: (Boolean) -> Unit
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(user.username.ifEmpty { "[No Username]" }, fontWeight = FontWeight.Bold) // ĐÃ DỊCH
            Text(user.email, style = MaterialTheme.typography.bodySmall)
            Text(if (user.admin) "Role: Admin" else "Role: User", style = MaterialTheme.typography.bodySmall) // ĐÃ DỊCH
        }
        if (!isCurrentUser) {
            Switch(
                checked = user.admin,
                onCheckedChange = onToggleAdmin,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit User") // ĐÃ DỊCH
            }
            IconButton(onClick = { showDeleteConfirmDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete User", tint = MaterialTheme.colorScheme.error) // ĐÃ DỊCH
            }
        } else {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Profile") // ĐÃ DỊCH
            }
            Spacer(modifier = Modifier.width(80.dp))
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Confirm Deletion") }, // ĐÃ DỊCH
            text = { Text("Are you sure you want to delete the data for user ${user.username} (${user.email})? This action only removes the database entry, not the authentication account.") }, // ĐÃ DỊCH
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") } // ĐÃ DỊCH
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") } // ĐÃ DỊCH
            }
        )
    }
}

@Composable
fun UserEditDialog(
    user: User?,
    onDismiss: () -> Unit,
    onSave: (userId: String, username: String, email: String, password: String, admin: Boolean) -> Unit
) {
    var username by remember { mutableStateOf(user?.username ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var admin by remember { mutableStateOf(user?.admin ?: false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    if (user == null) "Add New User" else "Edit User: ${user.username}", // ĐÃ DỊCH
                    style = MaterialTheme.typography.titleLarge
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = user != null
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (user == null) "Password (Required)" else "New Password (Optional)") }, // ĐÃ DỊCH
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = admin, onCheckedChange = { admin = it })
                    Text("Set as Admin") // ĐÃ DỊCH
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") } // ĐÃ DỊCH
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        if (user == null && (username.isBlank() || email.isBlank() || password.isBlank())) {
                            return@Button
                        }
                        onSave(user?.id ?: "", username, email, password, admin)
                    }) {
                        Text("Save") // ĐÃ DỊCH
                    }
                }
            }
        }
    }
}