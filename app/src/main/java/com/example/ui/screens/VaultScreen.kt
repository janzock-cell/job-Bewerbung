package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.ApplicationViewModel
import com.example.data.UserProfile
import com.example.data.Experience
import com.example.data.Skill
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VaultScreen(viewModel: ApplicationViewModel) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val experiences by viewModel.experienceList.collectAsStateWithLifecycle()
    val skills by viewModel.skillsList.collectAsStateWithLifecycle()
    val vaultDocuments by viewModel.vaultDocuments.collectAsStateWithLifecycle()
    val importText by viewModel.vaultImportText.collectAsStateWithLifecycle()
    val importTitle by viewModel.vaultImportTitle.collectAsStateWithLifecycle()
    val isVaultProcessing by viewModel.isVaultProcessing.collectAsStateWithLifecycle()
    val vaultStatusMessage by viewModel.vaultStatusMessage.collectAsStateWithLifecycle()
    val vaultErrorMessage by viewModel.vaultErrorMessage.collectAsStateWithLifecycle()

    var showProfileDialog by remember { mutableStateOf(false) }
    var showExperienceDialog by remember { mutableStateOf(false) }
    var showSkillDialog by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val cameraDeniedMessage = stringResource(R.string.scan_camera_permission_denied)

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUri?.let { viewModel.importImageIntoVault(it) }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = viewModel.createCameraCaptureUri()
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            scope.launch { snackbarHostState.showSnackbar(cameraDeniedMessage) }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.importImageIntoVault(it) }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importFileIntoVault(it) }
    }

    fun launchVaultCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val uri = viewModel.createCameraCaptureUri()
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
            ) {
                item {
                    VaultImportCard(
                        importText = importText,
                        importTitle = importTitle,
                        isProcessing = isVaultProcessing,
                        statusMessage = vaultStatusMessage,
                        errorMessage = vaultErrorMessage,
                        onImportTextChange = { viewModel.setVaultImportText(it) },
                        onImportTitleChange = { viewModel.setVaultImportTitle(it) },
                        onCameraClick = { launchVaultCamera() },
                        onGalleryClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onFilePickClick = {
                            documentPickerLauncher.launch(arrayOf("application/pdf", "text/plain", "image/*"))
                        },
                        onExtractClick = { viewModel.extractVaultDataWithAi() },
                        onDismissMessages = { viewModel.dismissVaultMessages() }
                    )
                }
            item {
                SectionHeader("Persönliche Daten", onAdd = { showProfileDialog = true }, icon = Icons.Default.Person)
                profile?.let {
                    ProfileCard(it)
                } ?: Text("Noch keine Profildaten hinterlegt.", style = MaterialTheme.typography.bodyMedium)
            }

            item {
                SectionHeader("Berufserfahrung", onAdd = { showExperienceDialog = true }, icon = Icons.Default.Work)
                if (experiences.isEmpty()) {
                    Text("Keine Erfahrungen hinzugefügt.", style = MaterialTheme.typography.bodyMedium)
                }
            }

            items(experiences) { exp ->
                ExperienceCard(exp, onDelete = { viewModel.deleteExperience(exp) })
            }

            item {
                SectionHeader("Fähigkeiten", onAdd = { showSkillDialog = true }, icon = Icons.Default.Star)
            }

            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    skills.forEach { skill ->
                        InputChip(
                            selected = true,
                            onClick = { viewModel.deleteSkill(skill) },
                            label = { Text(skill.name) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Löschen", modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            if (vaultDocuments.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Abgelegte Dokumente & Notizen (${vaultDocuments.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(vaultDocuments, key = { it.id }) { doc ->
                    VaultDocumentCard(
                        doc = doc,
                        onDelete = { viewModel.deleteVaultDocument(doc) }
                    )
                }
            }
        }
        }
    }

    if (showProfileDialog) {
        ProfileEditDialog(profile ?: UserProfile(), onDismiss = { showProfileDialog = false }, onSave = {
            viewModel.saveProfile(it)
            showProfileDialog = false
        })
    }

    if (showExperienceDialog) {
        ExperienceAddDialog(onDismiss = { showExperienceDialog = false }, onSave = {
            viewModel.addExperience(it)
            showExperienceDialog = false
        })
    }

    if (showSkillDialog) {
        SkillAddDialog(onDismiss = { showSkillDialog = false }, onSave = {
            viewModel.addSkill(it)
            showSkillDialog = false
        })
    }
}

@Composable
fun SectionHeader(title: String, onAdd: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = "Hinzufügen")
        }
    }
}

@Composable
fun ProfileCard(profile: UserProfile) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("${profile.firstName} ${profile.lastName}", style = MaterialTheme.typography.headlineSmall)
            Text("${profile.street}, ${profile.zipCode} ${profile.city}", style = MaterialTheme.typography.bodyMedium)
            Text("Email: ${profile.email}", style = MaterialTheme.typography.bodySmall)
            Text("Tel: ${profile.phone}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Text(profile.aboutMe, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ExperienceCard(exp: Experience, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(exp.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(exp.company, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                Text(exp.duration, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Text(exp.description, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun ProfileEditDialog(profile: UserProfile, onDismiss: () -> Unit, onSave: (UserProfile) -> Unit) {
    var firstName by remember { mutableStateOf(profile.firstName) }
    var lastName by remember { mutableStateOf(profile.lastName) }
    var email by remember { mutableStateOf(profile.email) }
    var phone by remember { mutableStateOf(profile.phone) }
    var street by remember { mutableStateOf(profile.street) }
    var zip by remember { mutableStateOf(profile.zipCode) }
    var city by remember { mutableStateOf(profile.city) }
    var aboutMe by remember { mutableStateOf(profile.aboutMe) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profil bearbeiten") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("Vorname") })
                OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Nachname") })
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telefon") })
                OutlinedTextField(value = street, onValueChange = { street = it }, label = { Text("Straße") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = zip, onValueChange = { zip = it }, label = { Text("PLZ") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Stadt") }, modifier = Modifier.weight(2f))
                }
                OutlinedTextField(value = aboutMe, onValueChange = { aboutMe = it }, label = { Text("Über mich / Kurzprofil") }, modifier = Modifier.height(100.dp))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
        confirmButton = {
            Button(onClick = { onSave(profile.copy(firstName = firstName, lastName = lastName, email = email, phone = phone, street = street, zipCode = zip, city = city, aboutMe = aboutMe)) }) {
                Text("Speichern")
            }
        }
    )
}

@Composable
fun ExperienceAddDialog(onDismiss: () -> Unit, onSave: (Experience) -> Unit) {
    var title by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Erfahrung hinzufügen") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Position/Titel") })
                OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Unternehmen") })
                OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Zeitraum (z.B. 2020-2023)") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Beschreibung/Tätigkeiten") }, modifier = Modifier.height(100.dp))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
        confirmButton = {
            Button(onClick = { onSave(Experience(title = title, company = company, duration = duration, description = description)) }) {
                Text("Hinzufügen")
            }
        }
    )
}


@Composable
fun SkillAddDialog(onDismiss: () -> Unit, onSave: (Skill) -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fähigkeit hinzufügen") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Fähigkeit (z.B. Projektmanagement)") })
        },
        confirmButton = {
            Button(onClick = { onSave(Skill(name = name)) }) {
                Text("Hinzufügen")
            }
        }
    )
}
