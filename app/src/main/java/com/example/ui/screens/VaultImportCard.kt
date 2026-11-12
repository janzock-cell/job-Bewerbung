package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.VaultDocument
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VaultImportCard(
    importText: String,
    importTitle: String,
    isProcessing: Boolean,
    statusMessage: String?,
    errorMessage: String?,
    onImportTextChange: (String) -> Unit,
    onImportTitleChange: (String) -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onFilePickClick: () -> Unit,
    onExtractClick: () -> Unit,
    onDismissMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("vault_import_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Schritt 1: Unterlagen & Notizen hochladen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Fotos (z. B. alte Anschreiben/Notizen), PDFs oder Text einlesen – die KI übernimmt deine Daten automatisch.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onCameraClick,
                    enabled = !isProcessing,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("vault_upload_camera_button")
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Kamera", maxLines = 1)
                }

                FilledTonalButton(
                    onClick = onGalleryClick,
                    enabled = !isProcessing,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("vault_upload_gallery_button")
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Foto", maxLines = 1)
                }

                FilledTonalButton(
                    onClick = onFilePickClick,
                    enabled = !isProcessing,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("vault_upload_file_button")
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PDF / Datei", maxLines = 1)
                }
            }

            AnimatedVisibility(visible = isProcessing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        text = "Unterlagen werden analysiert…",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            errorMessage?.let { err ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissMessages) {
                            Icon(Icons.Default.Close, contentDescription = "Schließen")
                        }
                    }
                }
            }

            statusMessage?.let { status ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissMessages) {
                            Icon(Icons.Default.Close, contentDescription = "Schließen")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = importTitle,
                onValueChange = onImportTitleChange,
                label = { Text("Bezeichnung (optional)") },
                placeholder = { Text("z. B. Lebenslauf, Altes Anschreiben, Notizen") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vault_import_title_input")
            )

            OutlinedTextField(
                value = importText,
                onValueChange = onImportTextChange,
                label = { Text("Text aus Dokumenten / Notizen") },
                placeholder = { Text("Erkannter Text erscheint hier – oder Notizen/Anschreiben direkt einfügen…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 110.dp)
                    .testTag("vault_import_text_input")
            )

            Button(
                onClick = onExtractClick,
                enabled = importText.isNotBlank() && !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("vault_ai_extract_button"),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Mit KI auslesen & in Tresor übernehmen")
            }
        }
    }
}

@Composable
fun VaultDocumentCard(
    doc: VaultDocument,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(doc.createdAt))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_document_card_${doc.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = when (doc.sourceType) {
                    "PDF" -> Icons.Default.PictureAsPdf
                    "Foto" -> Icons.Default.Image
                    else -> Icons.Default.Description
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${doc.sourceType} • $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = doc.extractedText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Dokument löschen",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
