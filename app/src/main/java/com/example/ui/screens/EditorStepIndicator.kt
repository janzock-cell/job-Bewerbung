package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class EditorWizardStep(
    val stepNumber: Int,
    val shortTitle: String,
    val guideText: String
) {
    JOB_DATA(
        stepNumber = 1,
        shortTitle = "Jobdaten",
        guideText = "Wähle den Modus und füge die Stellenanzeige sowie optionale Empfängerdaten ein."
    ),
    NOTES(
        stepNumber = 2,
        shortTitle = "Notizen",
        guideText = "Scanne optional handschriftliche Stichpunkte oder trage persönliche Argumente ein."
    ),
    GENERATE_AND_EDIT(
        stepNumber = 3,
        shortTitle = "KI & Text",
        guideText = "Generiere deinen Entwurf mit KI und passe den Brieftext direkt an."
    ),
    FINISH(
        stepNumber = 4,
        shortTitle = "Abschluss",
        guideText = "Passe deine Anlagen und das Brief-Design an und speichere das Anschreiben."
    )
}

@Composable
fun EditorStepHeader(
    currentStep: EditorWizardStep,
    hasJobInput: Boolean,
    hasNotes: Boolean,
    hasGeneratedLetter: Boolean,
    onStepSelected: (EditorWizardStep) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("editor_step_header")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Schritt ${currentStep.stepNumber} von ${EditorWizardStep.entries.size}: ${currentStep.shortTitle}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${(currentStep.stepNumber * 100) / EditorWizardStep.entries.size}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { currentStep.stepNumber.toFloat() / EditorWizardStep.entries.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EditorWizardStep.entries.forEach { step ->
                    val isSelected = step == currentStep
                    val isCompleted = when (step) {
                        EditorWizardStep.JOB_DATA -> hasJobInput && currentStep != step
                        EditorWizardStep.NOTES -> hasNotes && currentStep.stepNumber > step.stepNumber
                        EditorWizardStep.GENERATE_AND_EDIT -> hasGeneratedLetter && currentStep.stepNumber > step.stepNumber
                        EditorWizardStep.FINISH -> false
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onStepSelected(step) },
                        label = { Text("${step.stepNumber}. ${step.shortTitle}") },
                        leadingIcon = if (isCompleted) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.testTag("editor_step_chip_${step.stepNumber}")
                    )
                }
            }

            Text(
                text = currentStep.guideText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WizardNavigationRow(
    canGoBack: Boolean,
    nextLabel: String?,
    nextEnabled: Boolean = true,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (canGoBack) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
                    .testTag("editor_wizard_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Zurück")
            }
        }
        if (nextLabel != null) {
            Button(
                onClick = onNext,
                enabled = nextEnabled,
                modifier = Modifier
                    .weight(if (canGoBack) 1.4f else 1f)
                    .heightIn(min = 50.dp)
                    .testTag("editor_wizard_next_button")
            ) {
                Text(nextLabel)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}
