package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.data.supabase
import com.example.ui.theme.Black
import com.example.ui.theme.White
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1500)
        val user = try {
            supabase.auth.currentSessionOrNull()
        } catch (e: Exception) {
            null
        }
        if (user != null) {
            onNavigateToHome()
        } else {
            onNavigateToOnboarding()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Bizo",
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp),
            fontWeight = FontWeight.Bold,
            color = Black
        )
    }
}
