package com.example.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface OcrResult {
    data class Success(
        val recognizedText: String,
        val blockCount: Int,
        val lineCount: Int
    ) : OcrResult

    data object Empty : OcrResult

    data class Error(val message: String) : OcrResult
}

class OcrService(private val context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeHandwrittenNotes(imageUri: Uri): OcrResult {
        return try {
            val image = InputImage.fromFilePath(context, imageUri)
            processInputImage(image)
        } catch (e: Exception) {
            OcrResult.Error(e.localizedMessage ?: "Unbekannter Fehler beim Lesen des Bildes")
        }
    }

    suspend fun recognizePdfPages(pdfUri: Uri, maxPages: Int = 8): OcrResult = withContext(Dispatchers.IO) {
        try {
            val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
                ?: return@withContext OcrResult.Error("PDF-Datei konnte nicht geöffnet werden.")

            pfd.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    val pageCount = minOf(renderer.pageCount, maxPages)
                    if (pageCount <= 0) return@withContext OcrResult.Empty

                    val pageTexts = mutableListOf<String>()
                    var totalBlocks = 0
                    var totalLines = 0

                    for (index in 0 until pageCount) {
                        renderer.openPage(index).use { page ->
                            val scale = 2
                            val width = (page.width * scale).coerceAtMost(2400)
                            val height = (page.height * scale).coerceAtMost(3400)
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            try {
                                val canvas = Canvas(bitmap)
                                canvas.drawColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                val result = processInputImage(InputImage.fromBitmap(bitmap, 0))
                                if (result is OcrResult.Success) {
                                    pageTexts.add(result.recognizedText)
                                    totalBlocks += result.blockCount
                                    totalLines += result.lineCount
                                }
                            } finally {
                                bitmap.recycle()
                            }
                        }
                    }

                    val combined = pageTexts.joinToString("\n\n").trim()
                    if (combined.isEmpty()) {
                        OcrResult.Empty
                    } else {
                        OcrResult.Success(
                            recognizedText = combined,
                            blockCount = totalBlocks,
                            lineCount = totalLines
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            OcrResult.Error("Passwortgeschützte PDFs können nicht automatisch ausgelesen werden.")
        } catch (e: Exception) {
            OcrResult.Error(e.localizedMessage ?: "Fehler beim Auslesen der PDF-Datei")
        }
    }

    suspend fun readTextFile(uri: Uri): OcrResult = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }?.trim().orEmpty()
            if (text.isEmpty()) {
                OcrResult.Empty
            } else {
                val lines = text.lines().count { it.isNotBlank() }
                OcrResult.Success(recognizedText = text, blockCount = 1, lineCount = lines)
            }
        } catch (e: Exception) {
            OcrResult.Error(e.localizedMessage ?: "Textdatei konnte nicht gelesen werden")
        }
    }

    private suspend fun processInputImage(image: InputImage): OcrResult {
        val visionText = recognizer.process(image).await()

        val sortedBlocks = visionText.textBlocks
            .sortedWith(
                compareBy<com.google.mlkit.vision.text.Text.TextBlock>(
                    { it.boundingBox?.top ?: 0 },
                    { it.boundingBox?.left ?: 0 }
                )
            )

        val structuredText = sortedBlocks
            .map { block ->
                block.lines
                    .sortedBy { it.boundingBox?.top ?: 0 }
                    .joinToString("\n") { it.text.trim() }
                    .trim()
            }
            .filter { it.isNotEmpty() }
            .joinToString("\n\n")
            .trim()

        return if (structuredText.isEmpty()) {
            OcrResult.Empty
        } else {
            val totalLines = sortedBlocks.sumOf { it.lines.size }
            OcrResult.Success(
                recognizedText = structuredText,
                blockCount = sortedBlocks.size,
                lineCount = totalLines
            )
        }
    }

    fun close() {
        recognizer.close()
    }
}
