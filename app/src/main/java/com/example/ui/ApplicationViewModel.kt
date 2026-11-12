package com.example.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ocr.OcrResult
import com.example.ocr.OcrService
import com.example.pdf.PdfGenerator
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
private data class ExtractedExperienceDto(
    val title: String = "",
    val company: String = "",
    val duration: String = "",
    val description: String = ""
)

@Serializable
private data class ExtractedVaultPayload(
    val firstName: String = "",
    val lastName: String = "",
    val street: String = "",
    val zipCode: String = "",
    val city: String = "",
    val email: String = "",
    val phone: String = "",
    val aboutMe: String = "",
    val experiences: List<ExtractedExperienceDto> = emptyList(),
    val skills: List<String> = emptyList()
)

class ApplicationViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = DatabaseProvider.getDatabase(application).applicationDao()
    private val ocrService = OcrService(application)
    private val pdfGenerator = PdfGenerator(application)

    private val geminiModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-2.0-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
            }
        )
    }

    val userProfile = dao.getUserProfile().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val experienceList = dao.getAllExperience().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val skillsList = dao.getAllSkills().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val coverLetters = dao.getAllCoverLetters().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val vaultDocuments = dao.getAllVaultDocuments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _vaultImportText = MutableStateFlow("")
    val vaultImportText = _vaultImportText.asStateFlow()

    private val _vaultImportSource = MutableStateFlow("Notiz / Text")
    val vaultImportSource = _vaultImportSource.asStateFlow()

    private val _vaultImportTitle = MutableStateFlow("")
    val vaultImportTitle = _vaultImportTitle.asStateFlow()

    private val _isVaultProcessing = MutableStateFlow(false)
    val isVaultProcessing = _isVaultProcessing.asStateFlow()

    private val _vaultStatusMessage = MutableStateFlow<String?>(null)
    val vaultStatusMessage = _vaultStatusMessage.asStateFlow()

    private val _vaultErrorMessage = MutableStateFlow<String?>(null)
    val vaultErrorMessage = _vaultErrorMessage.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _scannedText = MutableStateFlow("")
    val scannedText = _scannedText.asStateFlow()

    private val _lastScannedImageUri = MutableStateFlow<Uri?>(null)
    val lastScannedImageUri = _lastScannedImageUri.asStateFlow()

    private val _scanInfoMessage = MutableStateFlow<String?>(null)
    val scanInfoMessage = _scanInfoMessage.asStateFlow()

    private val _scanErrorMessage = MutableStateFlow<String?>(null)
    val scanErrorMessage = _scanErrorMessage.asStateFlow()

    private val _generatedLetter = MutableStateFlow("")
    val generatedLetter = _generatedLetter.asStateFlow()

    private val _generationError = MutableStateFlow<String?>(null)
    val generationError = _generationError.asStateFlow()

    private val _aiDraftLetter = MutableStateFlow("")
    val aiDraftLetter = _aiDraftLetter.asStateFlow()

    private val _originalLetter = MutableStateFlow("")
    val originalLetter = _originalLetter.asStateFlow()

    private val _selectedTemplate = MutableStateFlow("Classic")
    val selectedTemplate = _selectedTemplate.asStateFlow()

    private val _attachmentsList = MutableStateFlow(listOf("Lebenslauf", "Zeugnisse"))
    val attachmentsList = _attachmentsList.asStateFlow()

    private val _editingLetterId = MutableStateFlow<Long?>(null)
    val editingLetterId = _editingLetterId.asStateFlow()

    private val _editingLetterStatus = MutableStateFlow("Entwurf")
    val editingLetterStatus = _editingLetterStatus.asStateFlow()

    private val _letterTitle = MutableStateFlow("Mein Anschreiben")
    val letterTitle = _letterTitle.asStateFlow()

    private val _jobDescription = MutableStateFlow("")
    val jobDescription = _jobDescription.asStateFlow()

    private val _recipientCompany = MutableStateFlow("")
    val recipientCompany = _recipientCompany.asStateFlow()

    private val _recipientPerson = MutableStateFlow("")
    val recipientPerson = _recipientPerson.asStateFlow()

    private val _recipientStreet = MutableStateFlow("")
    val recipientStreet = _recipientStreet.asStateFlow()

    private val _recipientCityZip = MutableStateFlow("")
    val recipientCityZip = _recipientCityZip.asStateFlow()

    private val _isExtractingRecipient = MutableStateFlow(false)
    val isExtractingRecipient = _isExtractingRecipient.asStateFlow()

    fun addAttachment(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val current = _attachmentsList.value
        if (current.none { it.equals(trimmed, ignoreCase = true) }) {
            _attachmentsList.value = current + trimmed
        }
    }

    fun removeAttachment(name: String) {
        _attachmentsList.value = _attachmentsList.value.filterNot { it == name }
    }

    fun updateAttachment(oldName: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val current = _attachmentsList.value
        if (!oldName.equals(trimmed, ignoreCase = true) && current.any { it.equals(trimmed, ignoreCase = true) }) {
            return
        }
        _attachmentsList.value = current.map { if (it == oldName) trimmed else it }
    }

    fun toggleSuggestedAttachment(name: String) {
        val current = _attachmentsList.value
        val existing = current.firstOrNull { it.equals(name, ignoreCase = true) }
        _attachmentsList.value = if (existing != null) {
            current.filterNot { it == existing }
        } else {
            current + name
        }
    }

    fun moveAttachment(index: Int, delta: Int) {
        val current = _attachmentsList.value.toMutableList()
        val target = index + delta
        if (index !in current.indices || target !in current.indices) return
        val item = current.removeAt(index)
        current.add(target, item)
        _attachmentsList.value = current
    }

    fun dismissGenerationError() {
        _generationError.value = null
    }

    fun updateGeneratedLetter(updatedText: String) {
        _generatedLetter.value = updatedText
    }

    fun resetGeneratedLetterToAiDraft() {
        if (_aiDraftLetter.value.isNotEmpty()) {
            _generatedLetter.value = _aiDraftLetter.value
        }
    }

    fun setLetterTitle(value: String) {
        _letterTitle.value = value
    }

    fun setJobDescription(value: String) {
        _jobDescription.value = value
    }

    fun setRecipientCompany(value: String) {
        _recipientCompany.value = value
    }

    fun setRecipientPerson(value: String) {
        _recipientPerson.value = value
    }

    fun setRecipientStreet(value: String) {
        _recipientStreet.value = value
    }

    fun setRecipientCityZip(value: String) {
        _recipientCityZip.value = value
    }

    fun setEditingLetterStatus(value: String) {
        _editingLetterStatus.value = value
    }

    fun startNewLetter() {
        _editingLetterId.value = null
        _editingLetterStatus.value = "Entwurf"
        _letterTitle.value = "Mein Anschreiben"
        _jobDescription.value = ""
        _recipientCompany.value = ""
        _recipientPerson.value = ""
        _recipientStreet.value = ""
        _recipientCityZip.value = ""
        _generatedLetter.value = ""
        _aiDraftLetter.value = ""
        _generationError.value = null
    }

    fun loadCoverLetterIntoEditor(letter: CoverLetter, duplicate: Boolean = false) {
        _editingLetterId.value = if (duplicate) null else letter.id
        _editingLetterStatus.value = if (duplicate) "Entwurf" else letter.status.ifBlank { "Entwurf" }
        _letterTitle.value = if (duplicate) "${letter.title} (Kopie)" else letter.title
        _jobDescription.value = letter.jobDescription
        _scannedText.value = letter.scannedNotes
        _selectedTemplate.value = letter.template.ifBlank { "Classic" }
        _generatedLetter.value = letter.content
        _aiDraftLetter.value = letter.content
        _recipientCompany.value = letter.recipientCompany
        _recipientPerson.value = letter.recipientPerson
        _recipientStreet.value = letter.recipientStreet
        _recipientCityZip.value = letter.recipientCityZip
        val parsedAttachments = letter.attachments
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        _attachmentsList.value = parsedAttachments
        _generationError.value = null
    }

    fun updateCoverLetterStatus(letter: CoverLetter, newStatus: String) {
        viewModelScope.launch {
            dao.updateCoverLetterStatus(letter.id, newStatus)
        }
    }

    fun deleteCoverLetter(letter: CoverLetter) {
        viewModelScope.launch {
            dao.deleteCoverLetter(letter)
            if (_editingLetterId.value == letter.id) {
                _editingLetterId.value = null
            }
        }
    }

    fun extractRecipientFromJobAd(jobAd: String) {
        if (jobAd.isBlank()) return
        viewModelScope.launch {
            _isExtractingRecipient.value = true
            _generationError.value = null
            val prompt = """
                Extrahiere aus der folgenden Stellenanzeige (falls vorhanden) die Empfängerdaten.
                Antworte exakt in 4 Zeilen ohne Markdown und ohne Erklärungen (lasse unbekannte Felder leer):
                FIRMA: <Unternehmen>
                ANSPRECHPARTNER: <Name oder Abteilung>
                STRASSE: <Straße und Hausnummer>
                ORT: <PLZ und Ort>

                Stellenanzeige:
                $jobAd
            """.trimIndent()
            try {
                val response = withTimeout(30_000L) {
                    geminiModel.generateContent(prompt)
                }
                val text = response.text.orEmpty()
                text.lines().forEach { rawLine ->
                    val line = rawLine.trim()
                    when {
                        line.startsWith("FIRMA:", ignoreCase = true) ->
                            _recipientCompany.value = line.substringAfter(":").trim()
                        line.startsWith("ANSPRECHPARTNER:", ignoreCase = true) ->
                            _recipientPerson.value = line.substringAfter(":").trim()
                        line.startsWith("STRASSE:", ignoreCase = true) ->
                            _recipientStreet.value = line.substringAfter(":").trim()
                        line.startsWith("ORT:", ignoreCase = true) ->
                            _recipientCityZip.value = line.substringAfter(":").trim()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ApplicationViewModel", "Empfänger-Extraktion fehlgeschlagen", e)
                _generationError.value = "Empfängerdaten konnten nicht automatisch erkannt werden: ${e.localizedMessage ?: e.message}"
            } finally {
                _isExtractingRecipient.value = false
            }
        }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            dao.saveUserProfile(profile)
        }
    }

    fun addExperience(exp: Experience) {
        viewModelScope.launch {
            dao.insertExperience(exp)
        }
    }

    fun deleteExperience(exp: Experience) {
        viewModelScope.launch {
            dao.deleteExperience(exp)
        }
    }

    fun addSkill(skill: Skill) {
        viewModelScope.launch {
            dao.insertSkill(skill)
        }
    }

    fun deleteSkill(skill: Skill) {
        viewModelScope.launch {
            dao.deleteSkill(skill)
        }
    }

    fun deleteVaultDocument(doc: VaultDocument) {
        viewModelScope.launch {
            dao.deleteVaultDocument(doc)
        }
    }

    fun setVaultImportText(value: String) {
        _vaultImportText.value = value
    }

    fun setVaultImportTitle(value: String) {
        _vaultImportTitle.value = value
    }

    fun dismissVaultMessages() {
        _vaultStatusMessage.value = null
        _vaultErrorMessage.value = null
    }

    fun importImageIntoVault(uri: Uri) {
        viewModelScope.launch {
            _isVaultProcessing.value = true
            _vaultErrorMessage.value = null
            _vaultStatusMessage.value = null
            val displayName = resolveUriDisplayName(uri) ?: "Foto-Scan"
            when (val result = ocrService.recognizeHandwrittenNotes(uri)) {
                is OcrResult.Success -> {
                    appendVaultImportedText(result.recognizedText, sourceType = "Foto", defaultTitle = displayName)
                    _vaultStatusMessage.value = "Text aus Foto erkannt (${result.lineCount} Zeilen). Jetzt mit KI auslesen!"
                }
                is OcrResult.Empty -> {
                    _vaultErrorMessage.value = "Auf dem Foto konnte kein Text erkannt werden."
                }
                is OcrResult.Error -> {
                    _vaultErrorMessage.value = "Foto-Erkennung fehlgeschlagen: ${result.message}"
                }
            }
            _isVaultProcessing.value = false
        }
    }

    fun importFileIntoVault(uri: Uri) {
        viewModelScope.launch {
            _isVaultProcessing.value = true
            _vaultErrorMessage.value = null
            _vaultStatusMessage.value = null
            val context = getApplication<Application>()
            val mime = context.contentResolver.getType(uri).orEmpty().lowercase()
            val displayName = resolveUriDisplayName(uri) ?: "Dokument"

            val (sourceLabel, result) = when {
                mime == "application/pdf" || displayName.endsWith(".pdf", ignoreCase = true) ->
                    "PDF" to ocrService.recognizePdfPages(uri)
                mime.startsWith("image/") ->
                    "Foto" to ocrService.recognizeHandwrittenNotes(uri)
                else ->
                    "Textdatei" to ocrService.readTextFile(uri)
            }

            when (result) {
                is OcrResult.Success -> {
                    appendVaultImportedText(result.recognizedText, sourceType = sourceLabel, defaultTitle = displayName)
                    _vaultStatusMessage.value = "$displayName eingelesen (${result.lineCount} Zeilen). Jetzt mit KI auslesen!"
                }
                is OcrResult.Empty -> {
                    _vaultErrorMessage.value = "Die ausgewählte Datei enthält keinen lesbaren Text."
                }
                is OcrResult.Error -> {
                    _vaultErrorMessage.value = "Import fehlgeschlagen: ${result.message}"
                }
            }
            _isVaultProcessing.value = false
        }
    }

    private fun appendVaultImportedText(newText: String, sourceType: String, defaultTitle: String) {
        val current = _vaultImportText.value.trim()
        _vaultImportText.value = if (current.isEmpty()) newText else "$current\n\n$newText"
        _vaultImportSource.value = sourceType
        if (_vaultImportTitle.value.isBlank()) {
            _vaultImportTitle.value = defaultTitle
        }
    }

    private fun resolveUriDisplayName(uri: Uri): String? {
        val resolver = getApplication<Application>().contentResolver
        return try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun extractVaultDataWithAi() {
        val rawText = _vaultImportText.value.trim()
        if (rawText.isEmpty()) {
            _vaultErrorMessage.value = "Bitte zuerst ein Foto/Dokument hochladen oder Text einfügen."
            return
        }

        viewModelScope.launch {
            _isVaultProcessing.value = true
            _vaultErrorMessage.value = null
            _vaultStatusMessage.value = null

            val prompt = """
                Analysiere den folgenden Rohtext eines Bewerbers (Lebenslauf, altes Anschreiben, Foto-OCR oder Notizen).
                Extrahiere alle verlässlichen Informationen exakt als gültiges JSON-Objekt ohne Markdown-Codeblock und ohne Erklärungen.
                Schema:
                {
                  "firstName": "",
                  "lastName": "",
                  "street": "",
                  "zipCode": "",
                  "city": "",
                  "email": "",
                  "phone": "",
                  "aboutMe": "",
                  "experiences": [
                    { "title": "", "company": "", "duration": "", "description": "" }
                  ],
                  "skills": [""]
                }
                Regeln:
                - Erfinde nichts. Lasse unbekannte Felder als "" oder [].
                - "aboutMe": kurze prägnante Zusammenfassung des Bewerberprofils auf Deutsch (max. 2 Sätze).
                - "skills": Liste einzelner Fachkenntnisse, Tools, Sprachen oder Zertifikate.

                Rohtext:
                $rawText
            """.trimIndent()

            try {
                val response = withTimeout(60_000L) {
                    geminiModel.generateContent(prompt)
                }
                val cleanedJson = response.text
                    .orEmpty()
                    .trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val payload = jsonParser.decodeFromString<ExtractedVaultPayload>(cleanedJson)

                val existingProfile = userProfile.value ?: UserProfile()
                val updatedProfile = existingProfile.copy(
                    firstName = existingProfile.firstName.ifBlank { payload.firstName.trim() },
                    lastName = existingProfile.lastName.ifBlank { payload.lastName.trim() },
                    street = existingProfile.street.ifBlank { payload.street.trim() },
                    zipCode = existingProfile.zipCode.ifBlank { payload.zipCode.trim() },
                    city = existingProfile.city.ifBlank { payload.city.trim() },
                    email = existingProfile.email.ifBlank { payload.email.trim() },
                    phone = existingProfile.phone.ifBlank { payload.phone.trim() },
                    aboutMe = existingProfile.aboutMe.ifBlank { payload.aboutMe.trim() }
                )
                val profileChanged = updatedProfile != existingProfile
                if (profileChanged) {
                    dao.saveUserProfile(updatedProfile)
                }

                val existingExps = experienceList.value
                var addedExpCount = 0
                payload.experiences
                    .filter { it.title.isNotBlank() || it.company.isNotBlank() }
                    .forEach { expDto ->
                        val duplicate = existingExps.any {
                            it.title.equals(expDto.title.trim(), ignoreCase = true) &&
                                it.company.equals(expDto.company.trim(), ignoreCase = true)
                        }
                        if (!duplicate) {
                            dao.insertExperience(
                                Experience(
                                    title = expDto.title.trim(),
                                    company = expDto.company.trim(),
                                    duration = expDto.duration.trim(),
                                    description = expDto.description.trim()
                                )
                            )
                            addedExpCount++
                        }
                    }

                val existingSkills = skillsList.value
                var addedSkillCount = 0
                payload.skills
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinctBy { it.lowercase() }
                    .forEach { skillName ->
                        if (existingSkills.none { it.name.equals(skillName, ignoreCase = true) }) {
                            dao.insertSkill(Skill(name = skillName))
                            addedSkillCount++
                        }
                    }

                val docTitle = _vaultImportTitle.value.trim().ifEmpty {
                    "Import (${_vaultImportSource.value})"
                }
                dao.insertVaultDocument(
                    VaultDocument(
                        title = docTitle,
                        sourceType = _vaultImportSource.value,
                        extractedText = rawText
                    )
                )

                _vaultImportText.value = ""
                _vaultImportTitle.value = ""
                _vaultImportSource.value = "Notiz / Text"

                val summaryParts = buildList {
                    if (profileChanged) add("Profildaten ergänzt")
                    if (addedExpCount > 0) add("$addedExpCount Erfahrungen")
                    if (addedSkillCount > 0) add("$addedSkillCount Fähigkeiten")
                    add("Dokument abgelegt")
                }
                _vaultStatusMessage.value = "KI-Import abgeschlossen: ${summaryParts.joinToString(", ")}."
            } catch (e: TimeoutCancellationException) {
                Log.e("ApplicationViewModel", "Tresor-KI-Extraktion Timeout", e)
                _vaultErrorMessage.value = "Zeitüberschreitung bei der KI-Analyse. Bitte erneut versuchen."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ApplicationViewModel", "Tresor-KI-Extraktion fehlgeschlagen", e)
                _vaultErrorMessage.value = "KI-Analyse fehlgeschlagen: ${e.localizedMessage ?: e.message}"
            } finally {
                _isVaultProcessing.value = false
            }
        }
    }

    fun createCameraCaptureUri(): Uri {
        val context = getApplication<Application>()
        val scanDir = File(context.cacheDir, "scans").apply { mkdirs() }
        val imageFile = File(scanDir, "note_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            imageFile
        )
    }

    fun scanImage(uri: Uri, append: Boolean = false) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanErrorMessage.value = null
            _scanInfoMessage.value = null
            _lastScannedImageUri.value = uri

            when (val result = ocrService.recognizeHandwrittenNotes(uri)) {
                is OcrResult.Success -> {
                    val current = _scannedText.value.trim()
                    _scannedText.value = if (append && current.isNotEmpty()) {
                        "$current\n\n${result.recognizedText}"
                    } else {
                        result.recognizedText
                    }
                    val blockLabel = if (result.blockCount == 1) "1 Textblock" else "${result.blockCount} Textblöcke"
                    val lineLabel = if (result.lineCount == 1) "1 Zeile" else "${result.lineCount} Zeilen"
                    _scanInfoMessage.value = "$blockLabel ($lineLabel) erfolgreich erkannt"
                }
                is OcrResult.Empty -> {
                    _scanErrorMessage.value =
                        "Kein Text auf dem Bild erkannt. Bitte achte auf gute Beleuchtung und deutlichen Kontrast."
                }
                is OcrResult.Error -> {
                    _scanErrorMessage.value = "Fehler bei der Texterkennung: ${result.message}"
                }
            }
            _isScanning.value = false
        }
    }

    fun setScannedText(text: String) {
        _scannedText.value = text
    }

    fun clearScannedNotes() {
        _scannedText.value = ""
        _lastScannedImageUri.value = null
        _scanInfoMessage.value = null
        _scanErrorMessage.value = null
    }

    fun dismissScanMessages() {
        _scanInfoMessage.value = null
        _scanErrorMessage.value = null
    }

    fun setOriginalLetter(text: String) {
        _originalLetter.value = text
    }

    fun setSelectedTemplate(template: String) {
        _selectedTemplate.value = template
    }

    fun generateLetter(jobAd: String) {
        viewModelScope.launch {
            _isGenerating.value = true
            _generationError.value = null
            val profile = userProfile.value
            val experiences = experienceList.value
            val skills = skillsList.value
            val notes = scannedText.value.trim()

            val notesInstruction = if (notes.isNotEmpty()) {
                """
                Handschriftliche Notizen & Stichpunkte des Bewerbers (per ML Kit Texterkennung erfasst):
                $notes
                
                Berücksichtige diese handschriftlichen Notizen vorrangig und arbeite alle darin enthaltenen Motivationen, Kernargumente und persönlichen Bezüge natürlich in das Anschreiben ein.
                """.trimIndent()
            } else {
                "Handschriftliche Notizen: Keine angegeben"
            }

            val recipientContext = listOfNotNull(
                _recipientCompany.value.trim().takeIf { it.isNotEmpty() }?.let { "Unternehmen: $it" },
                _recipientPerson.value.trim().takeIf { it.isNotEmpty() }?.let { "Ansprechpartner: $it" }
            ).joinToString(", ")

            val vaultContext = vaultDocuments.value
                .take(4)
                .joinToString("\n---\n") { doc -> "[${doc.title}]: ${doc.extractedText.take(900)}" }

            val prompt = """
                Erstelle ein professionelles Anschreiben für eine Bewerbung basierend auf den folgenden Informationen:
                
                Nutzerprofil: ${profile?.firstName.orEmpty()} ${profile?.lastName.orEmpty()}, ${profile?.aboutMe.orEmpty()}
                Berufserfahrung: ${experiences.joinToString { "${it.title} bei ${it.company} (${it.duration})" }}
                Fähigkeiten: ${skills.joinToString { it.name }}
                Stellenanzeige: ${jobAd.ifBlank { "Initiativbewerbung / basierend auf den Notizen" }}
                ${if (recipientContext.isNotEmpty()) "Empfänger: $recipientContext (nutze eine passende persönliche Anrede, falls ein Ansprechpartner genannt ist)" else ""}
                ${if (vaultContext.isNotEmpty()) "Hinterlegte Unterlagen & Notizen aus dem Daten-Tresor:\n$vaultContext" else ""}
                
                $notesInstruction
                
                Das Anschreiben soll professionell, überzeugend und in flüssigem Deutsch verfasst sein. 
                Formatiere es so, dass es in ein formelles Brieflayout passt. 
                Gib NUR den Text des Anschreibens zurück (Betreff, Anrede, Textkörper, Grußformel).
            """.trimIndent()

            try {
                val response = withTimeout(60_000L) {
                    geminiModel.generateContent(prompt)
                }
                val letterText = response.text?.trim()
                if (!letterText.isNullOrBlank()) {
                    _aiDraftLetter.value = letterText
                    _generatedLetter.value = letterText
                } else {
                    _generationError.value = "Gemini hat eine leere Antwort zurückgegeben. Bitte erneut versuchen."
                }
            } catch (e: TimeoutCancellationException) {
                Log.e("ApplicationViewModel", "Gemini Generierung Timeout", e)
                _generationError.value = "Zeitüberschreitung: Die KI-Generierung hat länger als 60 Sekunden gedauert. Bitte erneut versuchen."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ApplicationViewModel", "Gemini Generierung fehlgeschlagen", e)
                _generationError.value = "Fehler bei der KI-Generierung: ${e.localizedMessage ?: e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun improveLetter(jobAd: String) {
        val currentLetter = _originalLetter.value
        if (currentLetter.isEmpty()) return

        viewModelScope.launch {
            _isGenerating.value = true
            _generationError.value = null
            val profile = userProfile.value
            val experiences = experienceList.value
            val skills = skillsList.value
            val notes = scannedText.value.trim()

            val notesSection = if (notes.isNotEmpty()) {
                """
                Ergänzende handschriftliche Notizen & Stichpunkte (per ML Kit Texterkennung erfasst):
                $notes
                Bitte baue diese zusätzlichen Stichpunkte sinnvoll in die Überarbeitung ein.
                """.trimIndent()
            } else {
                ""
            }

            val vaultContext = vaultDocuments.value
                .take(4)
                .joinToString("\n---\n") { doc -> "[${doc.title}]: ${doc.extractedText.take(900)}" }

            val prompt = """
                Verbessere das folgende Anschreiben basierend auf dem Nutzerprofil, eventuellen handschriftlichen Notizen und der Stellenanzeige. 
                Mache es professioneller, flüssiger und hebe die relevanten Stärken hervor.
                
                Aktuelles Anschreiben: $currentLetter
                
                Nutzerprofil: ${profile?.firstName.orEmpty()} ${profile?.lastName.orEmpty()}, ${profile?.aboutMe.orEmpty()}
                Berufserfahrung: ${experiences.joinToString { "${it.title} bei ${it.company} (${it.duration})" }}
                Fähigkeiten: ${skills.joinToString { it.name }}
                Stellenanzeige: $jobAd
                ${if (vaultContext.isNotEmpty()) "Hinterlegte Unterlagen & Notizen aus dem Daten-Tresor:\n$vaultContext" else ""}
                $notesSection
                
                Gib NUR den verbesserten Text zurück.
            """.trimIndent()

            try {
                val response = withTimeout(60_000L) {
                    geminiModel.generateContent(prompt)
                }
                val letterText = response.text?.trim()
                if (!letterText.isNullOrBlank()) {
                    _aiDraftLetter.value = letterText
                    _generatedLetter.value = letterText
                } else {
                    _generationError.value = "Gemini hat eine leere Antwort zurückgegeben. Bitte erneut versuchen."
                }
            } catch (e: TimeoutCancellationException) {
                Log.e("ApplicationViewModel", "Gemini Optimierung Timeout", e)
                _generationError.value = "Zeitüberschreitung: Die KI-Optimierung hat länger als 60 Sekunden gedauert. Bitte erneut versuchen."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ApplicationViewModel", "Gemini Optimierung fehlgeschlagen", e)
                _generationError.value = "Fehler bei der KI-Verbesserung: ${e.localizedMessage ?: e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun saveCoverLetter(
        title: String,
        content: String,
        jobAd: String,
        saveAsNew: Boolean = false
    ) {
        viewModelScope.launch {
            val targetId = if (saveAsNew) 0L else (_editingLetterId.value ?: 0L)
            dao.insertCoverLetter(
                CoverLetter(
                    id = targetId,
                    title = title,
                    content = content,
                    jobDescription = jobAd,
                    scannedNotes = scannedText.value,
                    template = _selectedTemplate.value,
                    attachments = _attachmentsList.value.joinToString(", "),
                    recipientCompany = _recipientCompany.value.trim(),
                    recipientPerson = _recipientPerson.value.trim(),
                    recipientStreet = _recipientStreet.value.trim(),
                    recipientCityZip = _recipientCityZip.value.trim(),
                    status = _editingLetterStatus.value
                )
            )
        }
    }

    fun generatePdf(letter: CoverLetter): File? {
        val profile = userProfile.value ?: UserProfile()
        val recLines = listOf(
            letter.recipientCompany,
            letter.recipientPerson,
            letter.recipientStreet,
            letter.recipientCityZip
        ).map { it.trim() }.filter { it.isNotEmpty() }

        return pdfGenerator.generateCoverLetterPdf(
            name = "${profile.firstName} ${profile.lastName}",
            address = profile.street,
            cityZip = "${profile.zipCode} ${profile.city}",
            phone = profile.phone,
            email = profile.email,
            recipient = recLines.firstOrNull() ?: "Personalabteilung",
            subject = letter.title,
            body = letter.content,
            template = letter.template,
            attachments = letter.attachments,
            recipientLines = recLines
        )
    }

    override fun onCleared() {
        super.onCleared()
        ocrService.close()
    }
}
