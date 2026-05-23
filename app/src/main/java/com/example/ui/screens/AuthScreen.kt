package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.CountryInfo
import com.example.ui.components.CountryPicker
import com.example.ui.components.PrimaryButton
import com.example.ui.components.frequentCountries
import com.example.ui.theme.Black
import com.example.ui.theme.GraySurface
import com.example.ui.theme.GrayText

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(frequentCountries.first()) }

    LaunchedEffect(authState) {
        if (authState == AuthState.SUCCESS) {
            onAuthenticated()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.theme.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Bizo",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Black
        )

        Spacer(modifier = Modifier.height(64.dp))

        if (authState == AuthState.INPUT_PHONE || authState == AuthState.ERROR) {
            Text(
                text = "Entre ton numéro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Black
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "On t'envoie un code SMS pour te connecter. Pas de mot de passe.",
                style = MaterialTheme.typography.bodyLarge,
                color = GrayText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(GraySurface, RoundedCornerShape(12.dp))
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CountryPicker(
                        selectedCountry = selectedCountry,
                        onCountrySelected = { selectedCountry = it }
                    )
                    Box(
                        modifier = Modifier.width(1.dp).height(24.dp).background(GrayText.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    BasicTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Black),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (phone.isEmpty()) {
                                Text("Numéro de téléphone", color = GrayText)
                            }
                            innerTextField()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(text = "Recevoir le code ->", onClick = {
                val fullPhone = "${selectedCountry.code}$phone"
                viewModel.sendVerificationCode(fullPhone, context as Activity)
            })

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
            }
        } else if (authState == AuthState.INPUT_OTP) {
            Text(
                text = "Vérification",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Saisis le code à 6 chiffres envoyé au ${selectedCountry.code} $phone",
                style = MaterialTheme.typography.bodyLarge,
                color = GrayText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            BasicTextField(
                value = otp,
                onValueChange = { if (it.length <= 6) otp = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.headlineMedium.copy(color = Black, textAlign = TextAlign.Center),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GraySurface, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                decorationBox = { innerTextField ->
                    if (otp.isEmpty()) {
                        Text("------", color = GrayText, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(text = "Valider ->", onClick = {
                if (otp.length == 6) {
                    viewModel.verifyCode(otp)
                }
            })
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = { viewModel.resetState() }) {
                Text("Modifier le numéro", color = GrayText, fontWeight = FontWeight.Bold)
            }
        } else if (authState == AuthState.LOADING) {
            CircularProgressIndicator(color = Black)
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            "🔒 Ton numéro reste privé et n'est jamais partagé.",
            color = GrayText,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}
