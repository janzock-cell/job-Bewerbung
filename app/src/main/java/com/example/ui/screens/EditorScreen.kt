package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.ApplicationViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: ApplicationViewModel,
    onFinish: () -> Unit,
    onOpenScanner: () -> Unit = {}
) {
    val jobDescription by viewModel.jobDescription.collectAsStateWithLifecycle()
    val letterTitle by viewModel.letterTitle.collectAsStateWithLifecycle()
    val editingLetterId by viewModel.editingLetterId.collectAsStateWithLifecycle()
    val recipientCompany by viewModel.recipientCompany.collectAsStateWithLifecycle()
    val recipientPerson by viewModel.recipientPerson.collectAsStateWithLifecycle()
    val recipientStreet by viewModel.recipientStreet.collectAsStateWithLifecycle()
    val recipientCityZip by viewModel.recipientCityZip.collectAsStateWithLifecycle()
    val isExtractingRecipient by viewModel.isExtractingRecipient.collectAsStateWithLifecycle()
    val generatedLetter by viewModel.generatedLetter.collectAsStateWithLifecycle()
    val generationError by viewModel.generationError.collectAsStateWithLifecycle()
    val aiDraftLetter by viewModel.aiDraftLetter.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val selectedTemplate by viewModel.selectedTemplate.collectAsStateWithLifecycle()
    val attachmentsList by viewModel.attachmentsList.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scannedText by viewModel.scannedText.collectAsStateWithLifecycle()
    val scanInfoMessage by viewModel.scanInfoMessage.collectAsStateWithLifecycle()
    val scanErrorMessage by viewModel.scanErrorMessage.collectAsStateWithLifecycle()
    val originalLetter by viewModel.originalLetter.collectAsStateWithLifecycle()

    val recipientLines = remember(recipientCompany, recipientPerson, recipientStreet, recipientCityZip) {
        listOf(recipientCompany, recipientPerson, recipientStreet, recipientCityZip)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var currentStep by rememberSaveable {
        mutableStateOf(
            if (editingLetterId != null || generatedLetter.isNotBlank()) {
                EditorWizardStep.GENERATE_AND_EDIT
            } else {
                EditorWizardStep.JOB_DATA
            }
        )
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(currentStep) {
        scrollState.animateScrollTo(0)
    }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val cameraDeniedMessage = stringResource(R.string.scan_camera_permission_denied)

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUri?.let { uri ->
                viewModel.scanImage(uri, append = scannedText.isNotBlank())
            }
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
            scope.launch {
                snackbarHostState.showSnackbar(cameraDeniedMessage)
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.scanImage(it, append = scannedText.isNotBlank())
        }
    }

    fun launchQuickCameraScan() {
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

    val hasInputForGeneration = if (selectedTab == 0) {
        jobDescription.isNotBlank() || scannedText.isNotBlank()
    } else {
        originalLetter.isNotBlank() && (jobDescription.isNotBlank() || scannedText.isNotBlank())
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            EditorStepHeader(
                currentStep = currentStep,
                hasJobInput = jobDescription.isNotBlank() || originalLetter.isNotBlank(),
                hasNotes = scannedText.isNotBlank(),
                hasGeneratedLetter = generatedLetter.isNotBlank(),
                onStepSelected = { currentStep = it }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
                    .imePadding()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (editingLetterId != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Gespeichertes Anschreiben bearbeiten",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                viewModel.startNewLetter()
                                currentStep = EditorWizardStep.JOB_DATA
                            }) {
                                Text("Neu starten")
                            }
                        }
                    }
                }

                when (currentStep) {
                    EditorWizardStep.JOB_DATA -> {
                        val tabs = listOf("Neu generieren", "Vorhandenes verbessern")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            tabs.forEachIndexed { index, title ->
                                SegmentedButton(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                                    modifier = Modifier.testTag("editor_mode_tab_$index")
                                ) {
                                    Text(title)
                                }
                            }
                        }

                        if (selectedTab == 1) {
                            OutlinedTextField(
                                value = originalLetter,
                                onValueChange = { viewModel.setOriginalLetter(it) },
                                label = { Text("Dein aktuelles Anschreiben") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 130.dp)
                                    .testTag("editor_original_letter_input")
                            )
                        }

                        OutlinedTextField(
                            value = jobDescription,
                            onValueChange = { viewModel.setJobDescription(it) },
                            label = { Text("Stellenanzeige / Anforderungen") },
                            placeholder = { Text("Füge hier die Stellenanzeige oder wichtige Stichpunkte ein…") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 140.dp)
                                .testTag("editor_job_description_input")
                        )

                        RecipientAddressCard(
                            company = recipientCompany,
                            person = recipientPerson,
                            street = recipientStreet,
                            cityZip = recipientCityZip,
                            canExtract = jobDescription.isNotBlank(),
                            isExtracting = isExtractingRecipient,
                            onCompanyChange = { viewModel.setRecipientCompany(it) },
                            onPersonChange = { viewModel.setRecipientPerson(it) },
                            onStreetChange = { viewModel.setRecipientStreet(it) },
                            onCityZipChange = { viewModel.setRecipientCityZip(it) },
                            onExtractFromJobAd = { viewModel.extractRecipientFromJobAd(jobDescription) }
                        )

                        WizardNavigationRow(
                            canGoBack = false,
                            nextLabel = "Weiter: Notizen",
                            onBack = {},
                            onNext = { currentStep = EditorWizardStep.NOTES }
                        )
                    }

                    EditorWizardStep.NOTES -> {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_handwritten_notes_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.DocumentScanner,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = stringResource(R.string.editor_notes_section_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (scannedText.isNotBlank()) {
                                        IconButton(
                                            onClick = { viewModel.clearScannedNotes() },
                                            modifier = Modifier.testTag("editor_clear_notes_button")
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = stringResource(R.string.scan_clear_notes)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { launchQuickCameraScan() },
                                        enabled = !isScanning && !isGenerating,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("editor_quick_camera_scan_button")
                                    ) {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.scan_take_photo))
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        enabled = !isScanning && !isGenerating,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("editor_quick_gallery_scan_button")
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.scan_pick_gallery))
                                    }

                                    IconButton(
                                        onClick = onOpenScanner,
                                        modifier = Modifier.testTag("editor_open_full_scanner_button")
                                    ) {
                                        Icon(
                                            Icons.Default.OpenInFull,
                                            contentDescription = stringResource(R.string.editor_open_scanner)
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = isScanning) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        Text(
                                            text = stringResource(R.string.scan_processing),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                scanErrorMessage?.let { err ->
                                    Text(text = err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }

                                scanInfoMessage?.let { info ->
                                    Text(text = info, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }

                                OutlinedTextField(
                                    value = scannedText,
                                    onValueChange = { viewModel.setScannedText(it) },
                                    label = { Text("Erkannte Stichpunkte & Notizen") },
                                    placeholder = { Text(stringResource(R.string.editor_notes_placeholder)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp)
                                        .testTag("editor_scanned_notes_input")
                                )
                            }
                        }

                        WizardNavigationRow(
                            canGoBack = true,
                            nextLabel = "Weiter: KI & Text",
                            onBack = { currentStep = EditorWizardStep.JOB_DATA },
                            onNext = { currentStep = EditorWizardStep.GENERATE_AND_EDIT }
                        )
                    }

                    EditorWizardStep.GENERATE_AND_EDIT -> {
                        if (!hasInputForGeneration && generatedLetter.isBlank()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Hinweis: Füge zuerst in Schritt 1 eine Stellenanzeige oder in Schritt 2 handschriftliche Notizen hinzu, um das Anschreiben zu generieren.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (selectedTab == 0) viewModel.generateLetter(jobDescription)
                                else viewModel.improveLetter(jobDescription)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("editor_generate_button"),
                            enabled = hasInputForGeneration && !isGenerating && !isScanning,
                            shape = MaterialTheme.shapes.large
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                val icon = if (selectedTab == 0) Icons.Default.AutoAwesome else Icons.Default.EditNote
                                val text = if (selectedTab == 0) "Anschreiben generieren" else "Anschreiben verbessern"
                                Icon(icon, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(text)
                            }
                        }

                        generationError?.let { errorMsg ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("editor_generation_error_card")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                    Text(
                                        text = errorMsg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { viewModel.dismissGenerationError() }) {
                                        Icon(Icons.Default.Close, contentDescription = "Schließen", tint = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            }
                        }

                        if (generatedLetter.isNotEmpty() || aiDraftLetter.isNotEmpty()) {
                            CoverLetterEditorSection(
                                letterText = generatedLetter,
                                aiDraftText = aiDraftLetter,
                                selectedTemplate = selectedTemplate,
                                userProfile = userProfile,
                                attachments = attachmentsList,
                                recipientLines = recipientLines,
                                onTextChange = { viewModel.updateGeneratedLetter(it) },
                                onResetToAiDraft = { viewModel.resetGeneratedLetterToAiDraft() },
                                onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                            )
                        }

                        WizardNavigationRow(
                            canGoBack = true,
                            nextLabel = if (generatedLetter.isNotBlank()) "Weiter: Abschluss" else null,
                            onBack = { currentStep = EditorWizardStep.NOTES },
                            onNext = { currentStep = EditorWizardStep.FINISH }
                        )
                    }

                    EditorWizardStep.FINISH -> {
                        OutlinedTextField(
                            value = letterTitle,
                            onValueChange = { viewModel.setLetterTitle(it) },
                            label = { Text("Titel für dieses Anschreiben") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_letter_title_input")
                        )

                        AttachmentsEditorCard(
                            attachments = attachmentsList,
                            onAddAttachment = { viewModel.addAttachment(it) },
                            onRemoveAttachment = { viewModel.removeAttachment(it) },
                            onUpdateAttachment = { oldName, newName -> viewModel.updateAttachment(oldName, newName) },
                            onToggleSuggestion = { viewModel.toggleSuggestedAttachment(it) },
                            onMoveAttachment = { idx, delta -> viewModel.moveAttachment(idx, delta) }
                        )

                        Text("Layout-Design wählen:", style = MaterialTheme.typography.titleMedium)
                        val templates = listOf("Classic", "Modern", "Creative")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            templates.forEach { template ->
                                FilterChip(
                                    selected = selectedTemplate == template,
                                    onClick = { viewModel.setSelectedTemplate(template) },
                                    label = { Text(template) },
                                    leadingIcon = if (selectedTemplate == template) {
                                        {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    } else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("template_chip_${template.lowercase()}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { currentStep = EditorWizardStep.GENERATE_AND_EDIT },
                                modifier = Modifier
                                    .heightIn(min = 52.dp)
                                    .testTag("editor_wizard_back_button")
                            ) {
                                Text("Zurück")
                            }
                            Button(
                                onClick = {
                                    viewModel.saveCoverLetter(letterTitle, generatedLetter, jobDescription, saveAsNew = false)
                                    onFinish()
                                },
                                enabled = generatedLetter.isNotBlank(),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                                    .testTag("editor_save_button")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (editingLetterId != null) "Aktualisieren" else "Speichern")
                            }
                            if (editingLetterId != null) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.saveCoverLetter(letterTitle, generatedLetter, jobDescription, saveAsNew = true)
                                        onFinish()
                                    },
                                    enabled = generatedLetter.isNotBlank(),
                                    modifier = Modifier
                                        .heightIn(min = 52.dp)
                                        .testTag("editor_save_copy_button")
                                ) {
                                    Text("Kopie")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
