package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttachmentsEditorCard(
    attachments: List<String>,
    onAddAttachment: (String) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onUpdateAttachment: (String, String) -> Unit,
    onToggleSuggestion: (String) -> Unit,
    onMoveAttachment: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var newAttachmentText by remember { mutableStateOf("") }
    var editingAttachment by remember { mutableStateOf<String?>(null) }
    var editedText by remember { mutableStateOf("") }

    val suggestions = remember {
        listOf(
            "Lebenslauf",
            "Zeugnisse",
            "Arbeitszeugnisse",
            "Abschlusszeugnis",
            "Zertifikate",
            "Referenzen",
            "Arbeitsproben"
        )
    }

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("editor_attachments_card")
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
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.editor_attachments_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (attachments.size == 1) "1 Anlage" else "${attachments.size} Anlagen",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.editor_attachments_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (attachments.isEmpty()) {
                Text(
                    text = stringResource(R.string.editor_attachments_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.testTag("editor_attachments_empty_text")
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    attachments.forEachIndexed { index, item ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_attachment_item_$index")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (attachments.size > 1) {
                                    IconButton(
                                        onClick = { onMoveAttachment(index, -1) },
                                        enabled = index > 0,
                                        modifier = Modifier.testTag("editor_attachment_up_$index")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Nach oben verschieben"
                                        )
                                    }
                                    IconButton(
                                        onClick = { onMoveAttachment(index, 1) },
                                        enabled = index < attachments.lastIndex,
                                        modifier = Modifier.testTag("editor_attachment_down_$index")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Nach unten verschieben"
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        editingAttachment = item
                                        editedText = item
                                    },
                                    modifier = Modifier.testTag("editor_attachment_edit_$index")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Anlage umbenennen"
                                    )
                                }
                                IconButton(
                                    onClick = { onRemoveAttachment(item) },
                                    modifier = Modifier.testTag("editor_attachment_remove_$index")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Anlage entfernen",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newAttachmentText,
                    onValueChange = { newAttachmentText = it },
                    label = { Text(stringResource(R.string.editor_attachments_add_label)) },
                    placeholder = { Text(stringResource(R.string.editor_attachments_add_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newAttachmentText.isNotBlank()) {
                                onAddAttachment(newAttachmentText)
                                newAttachmentText = ""
                            }
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("editor_attachment_input")
                )

                FilledTonalButton(
                    onClick = {
                        if (newAttachmentText.isNotBlank()) {
                            onAddAttachment(newAttachmentText)
                            newAttachmentText = ""
                        }
                    },
                    enabled = newAttachmentText.isNotBlank(),
                    modifier = Modifier
                        .heightIn(min = 52.dp)
                        .testTag("editor_attachment_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.editor_attachments_add_button))
                }
            }

            Text(
                text = stringResource(R.string.editor_attachments_suggestions),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                suggestions.forEach { suggestion ->
                    val isSelected = attachments.any { it.equals(suggestion, ignoreCase = true) }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleSuggestion(suggestion) },
                        label = { Text(suggestion) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        modifier = Modifier.testTag("editor_attachment_suggestion_${suggestion.lowercase()}")
                    )
                }
            }
        }
    }

    editingAttachment?.let { oldName ->
        AlertDialog(
            onDismissRequest = { editingAttachment = null },
            title = { Text(stringResource(R.string.editor_attachments_edit_title)) },
            text = {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    label = { Text(stringResource(R.string.editor_attachments_edit_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_attachment_rename_input")
                )
            },
            dismissButton = {
                TextButton(onClick = { editingAttachment = null }) {
                    Text("Abbrechen")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateAttachment(oldName, editedText)
                        editingAttachment = null
                    },
                    enabled = editedText.isNotBlank(),
                    modifier = Modifier.testTag("editor_attachment_rename_save")
                ) {
                    Text("Speichern")
                }
            }
        )
    }
}
