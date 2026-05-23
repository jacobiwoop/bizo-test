package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.PrimaryButton
import com.example.ui.components.TransactionType
import com.example.ui.theme.Black
import com.example.ui.theme.GrayBorder
import com.example.ui.theme.GraySurface
import com.example.ui.theme.GrayText
import com.example.ui.theme.White

@Composable
fun PublishScreen(onBack: () -> Unit, viewModel: PublishViewModel = viewModel()) {
    LaunchedEffect(viewModel.publishSuccess) {
        if (viewModel.publishSuccess) {
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (viewModel.currentStep > 1) {
                    viewModel.currentStep--
                } else {
                    onBack()
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Black)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Publier une annonce (${viewModel.currentStep}/5)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Black)
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (viewModel.currentStep) {
            1 -> Step1Photos(viewModel)
            2 -> Step2Infos(viewModel)
            3 -> Step3Category(viewModel)
            4 -> Step4TransactionType(viewModel)
            5 -> Step5Location(viewModel)
        }

        Spacer(modifier = Modifier.weight(1f))

        if (viewModel.isPublishing) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Black)
        } else {
            PrimaryButton(
                text = if (viewModel.currentStep == 5) "Publier l'annonce" else "Continuer ->",
                onClick = {
                    if (viewModel.currentStep < 5) {
                        viewModel.currentStep++
                    } else {
                        viewModel.publish()
                    }
                }
            )
        }
    }
}

@Composable
fun Step1Photos(viewModel: PublishViewModel) {
    Text("Ajoute des photos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Black)
    Spacer(modifier = Modifier.height(8.dp))
    Text("Maximum 10 photos. La première sera la photo principale.", color = GrayText)
    Spacer(modifier = Modifier.height(24.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(GraySurface, RoundedCornerShape(12.dp))
            .clickable { /* Simulate picking photo */ },
        contentAlignment = Alignment.Center
    ) {
        Text("📷 Ajouter une photo", color = Black, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Step2Infos(viewModel: PublishViewModel) {
    Text("Infos de base", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Black)
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = viewModel.title,
        onValueChange = { viewModel.title = it },
        label = { Text("Titre de l'annonce") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = viewModel.description,
        onValueChange = { viewModel.description = it },
        label = { Text("Description") },
        modifier = Modifier.fillMaxWidth().height(150.dp)
    )
}

@Composable
fun Step3Category(viewModel: PublishViewModel) {
    Text("Catégorie", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Black)
    Spacer(modifier = Modifier.height(16.dp))
    val categories = listOf("Électronique", "Vêtements", "Automobile", "Maison", "Services")
    Column {
        categories.forEach { category ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.category = category }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = viewModel.category == category,
                    onClick = { viewModel.category = category }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(category, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun Step4TransactionType(viewModel: PublishViewModel) {
    Text("Comment tu veux vendre ?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Black)
    Spacer(modifier = Modifier.height(24.dp))

    TransactionCardOption(
        title = "VENTE",
        desc = "Vends ton article au prix fort.",
        icon = "💰",
        selected = viewModel.type == TransactionType.VENTE,
        onClick = { viewModel.type = TransactionType.VENTE }
    )
    if (viewModel.type == TransactionType.VENTE) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = viewModel.price,
            onValueChange = { viewModel.price = it },
            label = { Text("Prix (en FCFA)") },
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    TransactionCardOption(
        title = "TROC",
        desc = "Échange ton article.",
        icon = "🔄",
        selected = viewModel.type == TransactionType.TROC,
        onClick = { viewModel.type = TransactionType.TROC }
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    TransactionCardOption(
        title = "TROC+CASH",
        desc = "Échange avec complément.",
        icon = "🤝",
        selected = viewModel.type == TransactionType.TROC_CASH,
        onClick = { viewModel.type = TransactionType.TROC_CASH }
    )
    
    if (viewModel.type == TransactionType.TROC || viewModel.type == TransactionType.TROC_CASH) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = viewModel.exchangeFor,
            onValueChange = { viewModel.exchangeFor = it },
            label = { Text("Ce que je cherche...") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun Step5Location(viewModel: PublishViewModel) {
    Text("Localisation", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Black)
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = viewModel.city,
        onValueChange = { viewModel.city = it },
        label = { Text("Ville") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = viewModel.neighborhood,
        onValueChange = { viewModel.neighborhood = it },
        label = { Text("Quartier") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun TransactionCardOption(
    title: String,
    desc: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) Black else GrayBorder
    val bgColor = if (selected) GraySurface else White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Black)
            Text(desc, style = MaterialTheme.typography.bodyMedium, color = GrayText)
        }
    }
}
