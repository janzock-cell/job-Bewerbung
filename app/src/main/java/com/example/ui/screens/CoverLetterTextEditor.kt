package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverLetterEditorSection(
    letterText: String,
    aiDraftText: String,
    selectedTemplate: String,
    userProfile: UserProfile?,
    attachments: List<String> = emptyList(),
    recipientLines: List<String> = emptyList(),
    onTextChange: (String) -> Unit,
    onResetToAiDraft: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    var isPreviewMode by rememberSaveable { mutableStateOf(false) }
    var isFullscreenOpen by rememberSaveable { mutableStateOf(false) }

    // Local undo/redo stacks for smooth in-editor text editing
    val undoStack = remember { mutableStateListOf<String>() }
    val redoStack = remember { mutableStateListOf<String>() }

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = letterText, selection = TextRange(letterText.length)))
    }

    // Keep TextFieldValue synced when external state changes (e.g., new AI generation or reset)
    LaunchedEffect(letterText) {
        if (letterText != textFieldValue.text) {
            val safeCursor = textFieldValue.selection.end.coerceIn(0, letterText.length)
            textFieldValue = TextFieldValue(
                text = letterText,
                selection = TextRange(safeCursor)
            )
        }
    }

    fun applyUserTextEdit(newValue: TextFieldValue) {
        val previousText = textFieldValue.text
        textFieldValue = newValue
        if (newValue.text != previousText) {
            if (undoStack.lastOrNull() != previousText) {
                undoStack.add(previousText)
                if (undoStack.size > 40) {
                    undoStack.removeAt(0)
                }
            }
            redoStack.clear()
            onTextChange(newValue.text)
        }
    }

    fun performUndo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(textFieldValue.text)
            textFieldValue = TextFieldValue(text = prev, selection = TextRange(prev.length))
            onTextChange(prev)
        }
    }

    fun performRedo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(textFieldValue.text)
            textFieldValue = TextFieldValue(text = next, selection = TextRange(next.length))
            onTextChange(next)
        }
    }

    fun insertSnippetAtCursor(snippet: String) {
        val current = textFieldValue.text
        val start = textFieldValue.selection.min.coerceIn(0, current.length)
        val end = textFieldValue.selection.max.coerceIn(0, current.length)
        val prefix = current.substring(0, start)
        val suffix = current.substring(end)
        val needsLeadingSpace = prefix.isNotEmpty() && !prefix.endsWith("\n") && !prefix.endsWith(" ")
        val formattedSnippet = if (needsLeadingSpace) "\n\n$snippet" else snippet
        val updatedText = prefix + formattedSnippet + suffix
        val newCursorPos = (prefix.length + formattedSnippet.length).coerceIn(0, updatedText.length)

        applyUserTextEdit(
            TextFieldValue(
                text = updatedText,
                selection = TextRange(newCursorPos)
            )
        )
    }

    val clipboardManager = LocalClipboardManager.current
    val copyMessage = stringResource(R.string.editor_copy_snackbar)
    val resetMessage = stringResource(R.string.editor_reset_snackbar)

    val isModifiedFromAi = aiDraftText.isNotEmpty() && letterText != aiDraftText
    val wordCount = remember(letterText) {
        letterText.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    }
    val charCount = letterText.length
    val isWithinSinglePage = charCount <= 2500

    val fullName = remember(userProfile) {
        listOfNotNull(userProfile?.firstName?.trim(), userProfile?.lastName?.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" ")
    }

    val snippets = remember(fullName) {
        listOf(
            "Anrede" to "Sehr geehrte Damen und Herren,",
            "Gesprächswunsch" to "Über die Einladung zu einem persönlichen Vorstellungsgespräch freue ich mich sehr.",
            "Eintrittstermin" to "Für die Position stehe ich Ihnen ab dem nächstmöglichen Zeitpunkt zur Verfügung.",
            "Grußformel" to if (fullName.isNotEmpty()) {
                "Mit freundlichen Grüßen\n\n$fullName"
            } else {
                "Mit freundlichen Grüßen"
            }
        )
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cover_letter_editor_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mode Switcher Row: Bearbeiten (Editor Mode) vs Vorschau
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("editor_mode_segmented_row")
                ) {
                    SegmentedButton(
                        selected = !isPreviewMode,
                        onClick = { isPreviewMode = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = !isPreviewMode) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                )
                            }
                        },
                        modifier = Modifier.testTag("editor_mode_edit_tab")
                    ) {
                        Text(stringResource(R.string.editor_mode_edit))
                    }

                    SegmentedButton(
                        selected = isPreviewMode,
                        onClick = { isPreviewMode = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = isPreviewMode) {
                                Icon(
                                    Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                )
                            }
                        },
                        modifier = Modifier.testTag("editor_mode_preview_tab")
                    ) {
                        Text(stringResource(R.string.editor_mode_preview))
                    }
                }

                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(letterText))
                        onShowMessage(copyMessage)
                    },
                    modifier = Modifier.testTag("editor_copy_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Kopieren")
                }

                if (!isPreviewMode) {
                    IconButton(
                        onClick = { isFullscreenOpen = true },
                        modifier = Modifier.testTag("editor_fullscreen_button")
                    ) {
                        Icon(
                            Icons.Default.Fullscreen,
                            contentDescription = stringResource(R.string.editor_fullscreen_title)
                        )
                    }
                }
            }

            // Status & Toolbar inside Editor Mode
            if (!isPreviewMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { performUndo() },
                            enabled = undoStack.isNotEmpty(),
                            modifier = Modifier.testTag("editor_undo_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Rückgängig")
                        }
                        IconButton(
                            onClick = { performRedo() },
                            enabled = redoStack.isNotEmpty(),
                            modifier = Modifier.testTag("editor_redo_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Wiederholen")
                        }
                    }

                    AnimatedVisibility(visible = isModifiedFromAi) {
                        TextButton(
                            onClick = {
                                undoStack.add(textFieldValue.text)
                                redoStack.clear()
                                onResetToAiDraft()
                                onShowMessage(resetMessage)
                            },
                            modifier = Modifier.testTag("editor_reset_ai_draft_button")
                        ) {
                            Icon(
                                Icons.Default.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.editor_reset_to_ai))
                        }
                    }
                }

                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { applyUserTextEdit(it) },
                    label = { Text(stringResource(R.string.editor_text_field_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp)
                        .testTag("editor_generated_letter_input"),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                // Quick Text Snippets Row
                Text(
                    text = stringResource(R.string.editor_quick_snippets_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    snippets.forEachIndexed { idx, (label, snippetContent) ->
                        AssistChip(
                            onClick = { insertSnippetAtCursor(snippetContent) },
                            label = { Text(label) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("editor_snippet_chip_$idx")
                        )
                    }
                }
            } else {
                // Formatted Preview Card
                DocumentPreviewSurface(
                    letterText = letterText,
                    selectedTemplate = selectedTemplate,
                    fullName = fullName,
                    attachments = attachments,
                    recipientLines = recipientLines
                )
            }

            HorizontalDivider()

            // Live Word / Character Count & A4 Fit Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$wordCount Wörter • $charCount Zeichen",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("editor_word_char_stats")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isModifiedFromAi) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = stringResource(R.string.editor_edited_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isWithinSinglePage) {
                            stringResource(R.string.editor_length_optimal)
                        } else {
                            stringResource(R.string.editor_length_long)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isWithinSinglePage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }
    }

    // Fullscreen Distraction-Free Editor Dialog
    if (isFullscreenOpen) {
        Dialog(
            onDismissRequest = { isFullscreenOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.editor_fullscreen_title)) },
                        navigationIcon = {
                            IconButton(onClick = { isFullscreenOpen = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Schließen")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { performUndo() },
                                enabled = undoStack.isNotEmpty()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Rückgängig")
                            }
                            IconButton(
                                onClick = { performRedo() },
                                enabled = redoStack.isNotEmpty()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Wiederholen")
                            }
                            TextButton(
                                onClick = { isFullscreenOpen = false },
                                modifier = Modifier.testTag("editor_fullscreen_done_button")
                            ) {
                                Text(stringResource(R.string.editor_fullscreen_done))
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = { applyUserTextEdit(it) },
                        label = { Text(stringResource(R.string.editor_text_field_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("editor_fullscreen_text_input"),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$wordCount Wörter • $charCount Zeichen",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isModifiedFromAi) {
                            TextButton(onClick = {
                                undoStack.add(textFieldValue.text)
                                redoStack.clear()
                                onResetToAiDraft()
                            }) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.editor_reset_to_ai))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentPreviewSurface(
    letterText: String,
    selectedTemplate: String,
    fullName: String,
    attachments: List<String>,
    recipientLines: List<String>
) {
    val accentColor = when (selectedTemplate) {
        "Modern" -> MaterialTheme.colorScheme.primary
        "Creative" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("editor_document_preview_surface")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (fullName.isNotEmpty()) {
                Text(
                    text = fullName,
                    style = if (selectedTemplate == "Creative") {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    fontWeight = FontWeight.Bold,
                    color = if (selectedTemplate == "Classic") {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        accentColor
                    },
                    textAlign = if (selectedTemplate == "Creative") TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(color = accentColor.copy(alpha = 0.3f))
            }

            if (recipientLines.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.testTag("editor_preview_recipient_block")
                ) {
                    recipientLines.forEachIndexed { idx, line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (idx == 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = letterText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (attachments.isNotEmpty()) {
                HorizontalDivider(color = accentColor.copy(alpha = 0.2f))
                Text(
                    text = "Anlagen: ${attachments.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("editor_preview_attachments_text")
                )
            }
        }
    }
}
