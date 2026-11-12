package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RecipientAddressCard(
    company: String,
    person: String,
    street: String,
    cityZip: String,
    canExtract: Boolean,
    isExtracting: Boolean,
    onCompanyChange: (String) -> Unit,
    onPersonChange: (String) -> Unit,
    onStreetChange: (String) -> Unit,
    onCityZipChange: (String) -> Unit,
    onExtractFromJobAd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasRecipientData = company.isNotBlank() || person.isNotBlank() || street.isNotBlank() || cityZip.isNotBlank()
    var expanded by remember { mutableStateOf(hasRecipientData) }

    LaunchedEffect(hasRecipientData) {
        if (hasRecipientData) expanded = true
    }

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("editor_recipient_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Empfängerdaten (DIN 5008)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val summary = listOf(company, person).filter { it.isNotBlank() }.joinToString(" • ")
                        Text(
                            text = summary.ifEmpty { "Optional: Firma, Ansprechpartner & Adresse" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.testTag("editor_recipient_toggle_button")
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Einklappen" else "Ausklappen"
                    )
                }
            }

            if (canExtract) {
                FilledTonalButton(
                    onClick = {
                        expanded = true
                        onExtractFromJobAd()
                    },
                    enabled = !isExtracting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_recipient_extract_button")
                ) {
                    if (isExtracting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Empfänger wird aus Anzeige erkannt…")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Empfänger aus Stellenanzeige übernehmen")
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = company,
                        onValueChange = onCompanyChange,
                        label = { Text("Unternehmen / Firma") },
                        placeholder = { Text("z. B. Musterfirma GmbH") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_recipient_company_input")
                    )
                    OutlinedTextField(
                        value = person,
                        onValueChange = onPersonChange,
                        label = { Text("Ansprechpartner / Abteilung") },
                        placeholder = { Text("z. B. Frau Anna Müller / Personalabteilung") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_recipient_person_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = street,
                            onValueChange = onStreetChange,
                            label = { Text("Straße & Nr.") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("editor_recipient_street_input")
                        )
                        OutlinedTextField(
                            value = cityZip,
                            onValueChange = onCityZipChange,
                            label = { Text("PLZ & Ort") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("editor_recipient_city_input")
                        )
                    }
                }
            }
        }
    }
}
