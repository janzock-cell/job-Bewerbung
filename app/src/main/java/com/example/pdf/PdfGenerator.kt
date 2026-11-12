package com.example.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfGenerator(private val context: Context) {

    fun generateCoverLetterPdf(
        name: String,
        address: String,
        cityZip: String,
        phone: String,
        email: String,
        recipient: String = "Personalabteilung",
        subject: String,
        body: String,
        template: String = "Classic",
        attachments: String = "",
        recipientLines: List<String> = emptyList()
    ): File? {
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        var canvas = page.canvas
        val paint = Paint().apply {
            textSize = 10f
            isAntiAlias = true
            color = Color.BLACK
        }

        val leftMargin = 50f
        val rightMargin = 545f
        val maxBodyY = 785f
        var y = 50f

        val resolvedRecipientLines = recipientLines
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty {
                recipient.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            }
            .ifEmpty { listOf("Personalabteilung") }

        fun nextPage(isModern: Boolean) {
            pdfDocument.finishPage(page)
            pageNumber++
            page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            canvas = page.canvas
            if (isModern) {
                canvas.drawRect(0f, 0f, 150f, 842f, Paint().apply { color = Color.parseColor("#F1F3F4") })
            }
            y = 60f
        }

        when (template) {
            "Modern" -> {
                val primaryColor = Color.parseColor("#1A73E8")
                val headerPaint = Paint(paint).apply { color = primaryColor }

                canvas.drawRect(0f, 0f, 150f, 842f, Paint().apply { color = Color.parseColor("#F1F3F4") })

                headerPaint.textSize = 16f
                headerPaint.isFakeBoldText = true
                canvas.drawText(name, 20f, 100f, headerPaint)

                paint.textSize = 8f
                paint.color = Color.DKGRAY
                var sideY = 130f
                canvas.drawText(address, 20f, sideY, paint)
                sideY += 12f
                canvas.drawText(cityZip, 20f, sideY, paint)
                sideY += 20f
                canvas.drawText(phone, 20f, sideY, paint)
                sideY += 12f
                canvas.drawText(email, 20f, sideY, paint)

                val mainX = 180f
                y = 100f

                paint.textSize = 10f
                paint.color = Color.BLACK
                resolvedRecipientLines.forEachIndexed { idx, line ->
                    val linePaint = if (idx == 0) Paint(paint).apply { isFakeBoldText = true } else paint
                    canvas.drawText(line, mainX, y, linePaint)
                    y += 14f
                }

                y += 46f
                val date = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date())
                canvas.drawText(date, rightMargin - paint.measureText(date), y, paint)

                y += 36f
                val boldPaint = Paint(paint).apply { isFakeBoldText = true; textSize = 12f }
                for (subLine in wrapText(subject, boldPaint, rightMargin - mainX)) {
                    canvas.drawText(subLine, mainX, y, boldPaint)
                    y += 16f
                }

                y += 24f
                val lines = wrapText(body, paint, rightMargin - mainX)
                for (line in lines) {
                    if (y > maxBodyY) nextPage(isModern = true)
                    canvas.drawText(line, mainX, y, paint)
                    y += 15f
                }

                if (attachments.isNotBlank()) {
                    y += 14f
                    val attachPaint = Paint(paint).apply { textSize = 9f; color = Color.DKGRAY }
                    val attachLines = wrapText("Anlagen: $attachments", attachPaint, rightMargin - mainX)
                    for (line in attachLines) {
                        if (y > maxBodyY) nextPage(isModern = true)
                        canvas.drawText(line, mainX, y, attachPaint)
                        y += 13f
                    }
                }
            }
            "Creative" -> {
                val accentColor = Color.parseColor("#E91E63")
                val accentPaint = Paint(paint).apply { color = accentColor; textSize = 24f; isFakeBoldText = true }

                val nameWidth = accentPaint.measureText(name)
                canvas.drawText(name, (595f - nameWidth) / 2f, 80f, accentPaint)

                paint.textSize = 9f
                paint.color = Color.GRAY
                val contactLine = "$address | $cityZip | $phone | $email"
                val contactWidth = paint.measureText(contactLine)
                canvas.drawText(contactLine, (595f - contactWidth) / 2f, 100f, paint)

                val linePaint = Paint().apply { color = accentColor; strokeWidth = 2f }
                canvas.drawLine(leftMargin, 110f, rightMargin, 110f, linePaint)

                y = 155f
                paint.color = Color.BLACK
                paint.textSize = 10f
                resolvedRecipientLines.forEachIndexed { idx, line ->
                    val linePaintRec = if (idx == 0) Paint(paint).apply { isFakeBoldText = true } else paint
                    canvas.drawText(line, leftMargin, y, linePaintRec)
                    y += 14f
                }

                y += 30f
                val date = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date())
                canvas.drawText(date, rightMargin - paint.measureText(date), y, paint)

                y += 36f
                val boldPaint = Paint(paint).apply { isFakeBoldText = true; color = accentColor; textSize = 12f }
                for (subLine in wrapText(subject, boldPaint, rightMargin - leftMargin)) {
                    canvas.drawText(subLine, leftMargin, y, boldPaint)
                    y += 16f
                }

                y += 24f
                paint.color = Color.BLACK
                paint.textSize = 10f
                val lines = wrapText(body, paint, rightMargin - leftMargin)
                for (line in lines) {
                    if (y > maxBodyY) nextPage(isModern = false)
                    canvas.drawText(line, leftMargin, y, paint)
                    y += 15f
                }

                if (attachments.isNotBlank()) {
                    y += 14f
                    val attachPaint = Paint(paint).apply { textSize = 9f; color = Color.DKGRAY }
                    val attachLines = wrapText("Anlagen: $attachments", attachPaint, rightMargin - leftMargin)
                    for (line in attachLines) {
                        if (y > maxBodyY) nextPage(isModern = false)
                        canvas.drawText(line, leftMargin, y, attachPaint)
                        y += 13f
                    }
                }
            }
            else -> {
                // Classic (DIN 5008-nah)
                y = 50f
                canvas.drawText(name, leftMargin, y, paint)
                y += 14f
                canvas.drawText(address, leftMargin, y, paint)
                y += 14f
                canvas.drawText(cityZip, leftMargin, y, paint)
                y += 14f
                canvas.drawText(phone, leftMargin, y, paint)
                y += 14f
                canvas.drawText(email, leftMargin, y, paint)

                y += 36f
                val miniSender = listOf(name, address, cityZip).filter { it.isNotBlank() }.joinToString(" • ")
                if (miniSender.isNotBlank()) {
                    val miniPaint = Paint(paint).apply { textSize = 7.5f; color = Color.DKGRAY }
                    canvas.drawText(miniSender, leftMargin, y, miniPaint)
                    y += 14f
                }

                resolvedRecipientLines.forEachIndexed { idx, line ->
                    val linePaint = if (idx == 0) Paint(paint).apply { isFakeBoldText = true } else paint
                    canvas.drawText(line, leftMargin, y, linePaint)
                    y += 14f
                }

                y += 28f
                val date = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date())
                val dateText = "Datum: $date"
                canvas.drawText(dateText, rightMargin - paint.measureText(dateText), y, paint)

                y += 34f
                val boldPaint = Paint(paint).apply { isFakeBoldText = true; textSize = 11f }
                for (subLine in wrapText(subject, boldPaint, rightMargin - leftMargin)) {
                    canvas.drawText(subLine, leftMargin, y, boldPaint)
                    y += 15f
                }

                y += 24f
                val lines = wrapText(body, paint, rightMargin - leftMargin)
                for (line in lines) {
                    if (y > maxBodyY) nextPage(isModern = false)
                    canvas.drawText(line, leftMargin, y, paint)
                    y += 15f
                }

                if (attachments.isNotBlank()) {
                    y += 14f
                    val attachPaint = Paint(paint).apply { textSize = 9f; color = Color.DKGRAY }
                    val attachLines = wrapText("Anlagen: $attachments", attachPaint, rightMargin - leftMargin)
                    for (line in attachLines) {
                        if (y > maxBodyY) nextPage(isModern = false)
                        canvas.drawText(line, leftMargin, y, attachPaint)
                        y += 13f
                    }
                }
            }
        }

        pdfDocument.finishPage(page)

        val fileName = "Anschreiben_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        val paragraphs = text.split("\n")
        
        for (paragraph in paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add("")
                continue
            }
            
            val words = paragraph.split(" ")
            var currentLine = ""
            
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine = testLine
                } else {
                    lines.add(currentLine)
                    currentLine = word
                }
            }
            lines.add(currentLine)
        }
        return lines
    }
}
