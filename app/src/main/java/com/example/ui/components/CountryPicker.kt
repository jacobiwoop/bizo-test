package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.Black
import com.example.ui.theme.GraySurface
import com.example.ui.theme.GrayText

data class CountryInfo(val name: String, val code: String, val flag: String)

val frequentCountries = listOf(
    CountryInfo("Bénin", "+229", "🇧🇯"),
    CountryInfo("Sénégal", "+221", "🇸🇳"),
    CountryInfo("Côte d'Ivoire", "+225", "🇨🇮"),
    CountryInfo("Cameroun", "+237", "🇨🇲"),
    CountryInfo("France", "+33", "🇫🇷")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryPicker(
    selectedCountry: CountryInfo,
    onCountrySelected: (CountryInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .clickable { showDialog = true }
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(selectedCountry.flag, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.width(8.dp))
        Text(selectedCountry.code, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Black)
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    Text(
                        "Choisir un pays",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(color = GraySurface)
                    LazyColumn {
                        items(frequentCountries) { country ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCountrySelected(country)
                                        showDialog = false
                                    }
                                    .padding(16.dp)
                            ) {
                                Text(country.flag)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(country.name, modifier = Modifier.weight(1f))
                                Text(country.code, color = GrayText)
                            }
                            HorizontalDivider(color = GraySurface)
                        }
                    }
                }
            }
        }
    }
}
