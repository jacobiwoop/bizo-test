package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BadgeTrocBg
import com.example.ui.theme.BadgeTrocCashBg
import com.example.ui.theme.BadgeTrocCashText
import com.example.ui.theme.BadgeTrocText
import com.example.ui.theme.BadgeVenteBg
import com.example.ui.theme.BadgeVenteText
import com.example.ui.theme.Black
import com.example.ui.theme.GraySurface
import com.example.ui.theme.White

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Black, contentColor = White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GraySurface, contentColor = Black),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

enum class TransactionType { VENTE, TROC, TROC_CASH }

@Composable
fun TransactionBadge(type: TransactionType, modifier: Modifier = Modifier) {
    val (bg, text) = when (type) {
        TransactionType.VENTE -> BadgeVenteBg to BadgeVenteText
        TransactionType.TROC -> BadgeTrocBg to BadgeTrocText
        TransactionType.TROC_CASH -> BadgeTrocCashBg to BadgeTrocCashText
    }
    
    val textStr = when(type) {
        TransactionType.VENTE -> "VENTE"
        TransactionType.TROC -> "TROC"
        TransactionType.TROC_CASH -> "TROC+CASH"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = textStr,
            color = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
