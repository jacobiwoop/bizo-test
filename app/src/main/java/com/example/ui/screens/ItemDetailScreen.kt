package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.mockProducts
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.Dependencies
import com.example.ui.components.PrimaryButton
import com.example.ui.theme.Black
import com.example.ui.theme.GrayBorder
import com.example.ui.theme.GraySurface
import com.example.ui.theme.GrayText
import com.example.ui.theme.White
import androidx.compose.material.icons.filled.Favorite
import com.example.ui.components.SecondaryButton
import com.example.ui.components.TransactionBadge

@Composable
fun ItemDetailScreen(itemId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: ItemDetailViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ItemDetailViewModel(Dependencies.getBizoService(context)) as T
        }
    })
    val state by viewModel.state.collectAsState()

    LaunchedEffect(itemId) {
        viewModel.loadItem(itemId)
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(White), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Black)
        }
        return
    }

    val product = state.product
    if (product == null) {
        Box(modifier = Modifier.fillMaxSize().background(White), contentAlignment = Alignment.Center) {
            Text("Annonce introuvable ou erreur.", color = Black)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(White)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Image Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Black)
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Top Bar overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .background(White.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Black)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { /* Share */ },
                        modifier = Modifier
                            .background(White.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Black)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.toggleFavorite(itemId) },
                        modifier = Modifier
                            .background(White.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(
                            if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Fav",
                            tint = if (state.isFavorite) Color.Red else Black
                        )
                    }
                }
            }

            // Content
            Column(modifier = Modifier.padding(20.dp)) {
                TransactionBadge(type = product.type)
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = product.price,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                // User Profile Snippet
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(GraySurface))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(product.sellerName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Black)
                        Text("⭐ 4.8 (12 avis)", style = MaterialTheme.typography.bodyMedium, color = GrayText)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Voir le profil", style = MaterialTheme.typography.labelMedium, color = Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "📍 ${product.location}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayText
                )
                Text(
                    text = "Publié ${product.timeAgo}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayText
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = GrayBorder)
                Spacer(modifier = Modifier.height(24.dp))

                Text("Description", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Black)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Article en parfait état. Toujours protégé. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Black
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Additional Specs mockup
                Row(modifier = Modifier.fillMaxWidth()) {
                    SpecBox(title = "ÉTAT", value = "Excellent", modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(12.dp))
                    SpecBox(title = "REMISE", value = "En main propre", modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(100.dp)) // space for sticky bottom bar
            }
        }

        // Sticky Bottom Actions
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(White)
                .padding(16.dp)
        ) {
            Row {
                SecondaryButton(
                    text = "Contacter",
                    onClick = { /* TODO */ },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                PrimaryButton(
                    text = "Proposer un troc",
                    onClick = { /* TODO */ },
                    modifier = Modifier.weight(1.5f)
                )
            }
        }
    }
}

@Composable
fun SpecBox(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(GraySurface, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = GrayText)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Black)
    }
}
