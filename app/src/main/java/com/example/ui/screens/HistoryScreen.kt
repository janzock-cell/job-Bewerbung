package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CoverLetter
import com.example.ui.ApplicationViewModel
import java.text.SimpleDateFormat
import java.util.*

private val APPLICATION_STATUS_OPTIONS = listOf("Entwurf", "Abgeschickt", "Einladung", "Absage")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: ApplicationViewModel,
    onOpenInEditor: () -> Unit = {}
) {
    val letters by viewModel.coverLetters.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var letterPendingDeletion by remember { mutableStateOf<CoverLetter?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        if (letters.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Noch keine Anschreiben erstellt.")
            }
        } else {
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
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
                ) {
                    items(letters, key = { it.id }) { letter ->
                        CoverLetterItem(
                            letter = letter,
                            onOpen = {
                                viewModel.loadCoverLetterIntoEditor(letter, duplicate = false)
                                onOpenInEditor()
                            },
                            onDuplicate = {
                                viewModel.loadCoverLetterIntoEditor(letter, duplicate = true)
                                onOpenInEditor()
                            },
                            onStatusChange = { newStatus ->
                                viewModel.updateCoverLetterStatus(letter, newStatus)
                            },
                            onDeleteRequest = { letterPendingDeletion = letter },
                            onExport = {
                                val file = viewModel.generatePdf(letter)
                                file?.let {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", it)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "PDF öffnen"))
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    letterPendingDeletion?.let { target ->
        AlertDialog(
            onDismissRequest = { letterPendingDeletion = null },
            title = { Text("Anschreiben löschen?") },
            text = { Text("„${target.title}“ wird dauerhaft entfernt.") },
            dismissButton = {
                TextButton(onClick = { letterPendingDeletion = null }) {
                    Text("Abbrechen")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCoverLetter(target)
                        letterPendingDeletion = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("history_confirm_delete_button")
                ) {
                    Text("Löschen")
                }
            }
        )
    }
}

@Composable
fun CoverLetterItem(
    letter: CoverLetter,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onStatusChange: (String) -> Unit,
    onDeleteRequest: () -> Unit,
    onExport: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(letter.date))
    val currentStatus = letter.status.ifBlank { "Entwurf" }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var moreMenuExpanded by remember { mutableStateOf(false) }

    val recipientSummary = listOf(letter.recipientCompany, letter.recipientPerson)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" • ")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_letter_card_${letter.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = letter.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(dateStr, style = MaterialTheme.typography.labelSmall)
                Box {
                    IconButton(
                        onClick = { moreMenuExpanded = true },
                        modifier = Modifier.testTag("history_more_button_${letter.id}")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Weitere Optionen")
                    }
                    DropdownMenu(
                        expanded = moreMenuExpanded,
                        onDismissRequest = { moreMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Als Vorlage duplizieren") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                moreMenuExpanded = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Löschen", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                moreMenuExpanded = false
                                onDeleteRequest()
                            }
                        )
                    }
                }
            }

            if (recipientSummary.isNotEmpty()) {
                Text(
                    text = recipientSummary,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(4.dp))
            Text(letter.content, style = MaterialTheme.typography.bodySmall, maxLines = 3)
            Spacer(Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    FilterChip(
                        selected = currentStatus != "Entwurf",
                        onClick = { statusMenuExpanded = true },
                        label = { Text(currentStatus) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Status ändern",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("history_status_chip_${letter.id}")
                    )
                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false }
                    ) {
                        APPLICATION_STATUS_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                leadingIcon = if (option == currentStatus) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null,
                                onClick = {
                                    statusMenuExpanded = false
                                    onStatusChange(option)
                                }
                            )
                        }
                    }
                }

                AssistChip(
                    onClick = {},
                    label = { Text(letter.template) },
                    leadingIcon = { Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )

                if (letter.scannedNotes.isNotBlank()) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Notizen") },
                        leadingIcon = { Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            if (letter.attachments.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Anlagen: ${letter.attachments}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onOpen,
                    modifier = Modifier.testTag("history_open_button_${letter.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Öffnen")
                }
                Button(
                    onClick = onExport,
                    modifier = Modifier.testTag("history_export_button_${letter.id}")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("PDF")
                }
            }
        }
    }
}
