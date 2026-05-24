package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.Dependencies
import com.example.data.BizoService
import com.example.data.UserResource
import com.example.ui.components.PrimaryButton
import com.example.ui.theme.Black
import com.example.ui.theme.GraySurface
import com.example.ui.theme.GrayText
import com.example.ui.theme.White
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

class EditProfileViewModel(private val bizoService: BizoService) : ViewModel() {
    var user by mutableStateOf<UserResource?>(null)
    var displayName by mutableStateOf("")
    var bio by mutableStateOf("")
    var username by mutableStateOf("")
    
    var isLoading by mutableStateOf(false)
    var isSaving by mutableStateOf(false)
    var saveSuccess by mutableStateOf(false)

    init {
        loadProfile()
    }

    private fun loadProfile() {
        isLoading = true
        viewModelScope.launch {
            try {
                val profile = bizoService.getProfile()
                user = profile
                displayName = profile.display_name
                bio = profile.bio ?: ""
                username = profile.username ?: ""
            } catch (e: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    fun saveProfile() {
        isSaving = true
        viewModelScope.launch {
            try {
                val params = mapOf(
                    "display_name" to displayName,
                    "bio" to bio,
                    "username" to if (username.isBlank()) null else username
                )
                bizoService.updateProfile(params)
                saveSuccess = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isSaving = false
            }
        }
    }

    fun uploadAvatar(bytes: ByteArray, filename: String) {
        viewModelScope.launch {
            try {
                val updatedUser = bizoService.uploadAvatar(bytes, filename)
                user = updatedUser
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun EditProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: EditProfileViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditProfileViewModel(Dependencies.getBizoService(context)) as T
        }
    })

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    viewModel.uploadAvatar(stream.readBytes(), "avatar.jpg")
                }
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(viewModel.saveSuccess) {
        if (viewModel.saveSuccess) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Black)
                }
                Text("Éditer le profil", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = White
    ) { padding ->
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Black)
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                // Avatar Section
                Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    val photoUrl = viewModel.user?.photo_url?.let { if (it.startsWith("http")) it else "https://bizo.aiko.qzz.io$it" }
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(GraySurface)
                            .clickable { launcher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUrl != null) {
                            AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Text(viewModel.displayName.take(1).uppercase(), style = MaterialTheme.typography.displaySmall)
                        }
                        Box(
                            modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📷", color = White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = viewModel.displayName,
                    onValueChange = { viewModel.displayName = it },
                    label = { Text("Nom d'affichage") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = viewModel.username,
                    onValueChange = { viewModel.username = it },
                    label = { Text("Username (@)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = viewModel.bio,
                    onValueChange = { viewModel.bio = it },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                if (viewModel.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Black)
                } else {
                    PrimaryButton(text = "Enregistrer", onClick = { viewModel.saveProfile() })
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
