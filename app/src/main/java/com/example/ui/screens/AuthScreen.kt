package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.SessionManager
import com.example.ui.components.BizoScreen
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.Accent
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color
)

private val onboardingPages = listOf(
    OnboardingPage(
        title = "Tout ce qu’il faut pour acheter ou troquer",
        subtitle = "Parcours les annonces, retrouve les bonnes affaires et commence à échanger sans friction.",
        icon = Icons.Default.Search,
        accent = Color(0xFFF0B429)
    ),
    OnboardingPage(
        title = "Une seule app pour vendre, discuter et conclure",
        subtitle = "Gère tes annonces, parle directement avec les acheteurs et avance rapidement jusqu’à l’accord.",
        icon = Icons.Default.ChatBubbleOutline,
        accent = Color(0xFF6FCF97)
    ),
    OnboardingPage(
        title = "Publie vite, sois visible, reste local",
        subtitle = "Ajoute tes photos, précise ton offre et trouve des personnes proches de toi en quelques étapes.",
        icon = Icons.Default.Place,
        accent = Color(0xFF8B8EF9)
    )
)

@Composable
fun AuthScreen(navController: NavController) {
    SignInScreen(navController = navController)
}

@Composable
fun OnboardingScreen(
    navController: NavController,
    sessionManager: SessionManager
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()

    BizoScreen { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        sessionManager.setHasSeenOnboarding(true)
                        navController.navigate("sign_in") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                ) {
                    Text("Passer", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                OnboardingPageContent(page = onboardingPages[page])
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                PagerIndicator(
                    count = onboardingPages.size,
                    currentPage = pagerState.currentPage
                )

                PrimaryButton(
                    text = if (pagerState.currentPage == onboardingPages.lastIndex) "Commencer" else "Continuer",
                    onClick = {
                        if (pagerState.currentPage == onboardingPages.lastIndex) {
                            sessionManager.setHasSeenOnboarding(true)
                            navController.navigate("sign_in") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = { navController.navigate("sign_in") }) {
                        Text("J’ai déjà un compte")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = { navController.navigate("register") }) {
                        Text("Créer un compte")
                    }
                }

                Text(
                    text = buildAnnotatedString {
                        append("En continuant, tu acceptes nos ")
                        withStyle(SpanStyle(color = Accent, fontWeight = FontWeight.SemiBold)) {
                            append("conditions")
                        }
                        append(" et notre ")
                        withStyle(SpanStyle(color = Accent, fontWeight = FontWeight.SemiBold)) {
                            append("politique de confidentialité")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SignInScreen(navController: NavController) {
    val viewModel: AuthViewModel = hiltViewModel()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.clearMessages() }

    EntryScaffold(
        title = "Connexion",
        subtitle = "Retrouve tes annonces, tes messages et ton activité.",
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Pas encore de compte ?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { navController.navigate("register") }) {
                    Text("S'inscrire")
                }
            }
        }
    ) {
        EntryField(
            value = email,
            onValueChange = {
                email = it
                viewModel.clearMessages()
            },
            label = "Email",
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(14.dp))

        EntryPasswordField(
            value = password,
            onValueChange = {
                password = it
                viewModel.clearMessages()
            },
            label = "Mot de passe",
            passwordVisible = passwordVisible,
            onToggleVisibility = { passwordVisible = !passwordVisible }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { navController.navigate("forgot_password") }) {
                Text("Mot de passe oublié ?")
            }
        }

        EntryFeedback(error = error)

        Spacer(modifier = Modifier.height(12.dp))

        PrimaryButton(
            text = "Se connecter",
            enabled = email.isNotBlank() && password.isNotBlank(),
            isLoading = isLoading,
            onClick = {
                viewModel.login(email.trim(), password) {
                    navController.navigate("home") {
                        popUpTo("sign_in") { inclusive = true }
                    }
                }
            }
        )
    }
}

@Composable
fun RegisterScreen(navController: NavController) {
    val viewModel: AuthViewModel = hiltViewModel()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmationVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.clearMessages() }

    EntryScaffold(
        title = "Créer un compte",
        subtitle = "Commence avec un profil propre pour publier et discuter rapidement.",
        onBack = { navController.popBackStack() },
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Déjà membre ?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(
                    onClick = {
                        navController.navigate("sign_in") {
                            popUpTo("register") { inclusive = true }
                        }
                    }
                ) {
                    Text("Se connecter")
                }
            }
        }
    ) {
        EntryField(value = name, onValueChange = { name = it; viewModel.clearMessages() }, label = "Nom complet")
        Spacer(modifier = Modifier.height(14.dp))
        EntryField(
            value = email,
            onValueChange = { email = it; viewModel.clearMessages() },
            label = "Email",
            keyboardType = KeyboardType.Email
        )
        Spacer(modifier = Modifier.height(14.dp))
        EntryPasswordField(
            value = password,
            onValueChange = { password = it; viewModel.clearMessages() },
            label = "Mot de passe",
            passwordVisible = passwordVisible,
            onToggleVisibility = { passwordVisible = !passwordVisible }
        )
        Spacer(modifier = Modifier.height(14.dp))
        EntryPasswordField(
            value = confirmation,
            onValueChange = { confirmation = it; viewModel.clearMessages() },
            label = "Confirmer le mot de passe",
            passwordVisible = confirmationVisible,
            onToggleVisibility = { confirmationVisible = !confirmationVisible }
        )

        EntryFeedback(error = error)

        Spacer(modifier = Modifier.height(12.dp))

        PrimaryButton(
            text = "Créer mon compte",
            enabled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && confirmation == password,
            isLoading = isLoading,
            onClick = {
                viewModel.register(name.trim(), email.trim(), password) {
                    navController.navigate("home") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            }
        )
    }
}

@Composable
fun ForgotPasswordScreen(navController: NavController) {
    val viewModel: AuthViewModel = hiltViewModel()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val infoMessage by viewModel.infoMessage.collectAsStateWithLifecycle()

    var email by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.clearMessages() }

    EntryScaffold(
        title = "Mot de passe oublié",
        subtitle = "Entre ton email et on t’envoie les instructions de réinitialisation.",
        onBack = { navController.popBackStack() }
    ) {
        EntryField(
            value = email,
            onValueChange = { email = it; viewModel.clearMessages() },
            label = "Email",
            keyboardType = KeyboardType.Email
        )

        EntryFeedback(error = error, info = infoMessage)

        Spacer(modifier = Modifier.height(12.dp))

        PrimaryButton(
            text = "Envoyer les instructions",
            enabled = email.isNotBlank(),
            isLoading = isLoading,
            onClick = { viewModel.requestPasswordReset(email.trim()) }
        )

        AnimatedVisibility(visible = infoMessage != null) {
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryButton(
                    text = "J’ai déjà mon token de réinitialisation",
                    onClick = { navController.navigate("create_new_password") }
                )
            }
        }
    }
}

@Composable
fun CreateNewPasswordScreen(navController: NavController) {
    val viewModel: AuthViewModel = hiltViewModel()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var token by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmationVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.clearMessages() }

    EntryScaffold(
        title = "Créer un nouveau mot de passe",
        subtitle = "Renseigne le token reçu puis choisis un nouveau mot de passe.",
        onBack = { navController.popBackStack() }
    ) {
        EntryField(
            value = token,
            onValueChange = { token = it; viewModel.clearMessages() },
            label = "Token de réinitialisation"
        )

        Spacer(modifier = Modifier.height(14.dp))

        EntryField(
            value = email,
            onValueChange = { email = it; viewModel.clearMessages() },
            label = "Email",
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(14.dp))

        EntryPasswordField(
            value = password,
            onValueChange = { password = it; viewModel.clearMessages() },
            label = "Nouveau mot de passe",
            passwordVisible = passwordVisible,
            onToggleVisibility = { passwordVisible = !passwordVisible }
        )

        Spacer(modifier = Modifier.height(14.dp))

        EntryPasswordField(
            value = confirmation,
            onValueChange = { confirmation = it; viewModel.clearMessages() },
            label = "Confirmer le mot de passe",
            passwordVisible = confirmationVisible,
            onToggleVisibility = { confirmationVisible = !confirmationVisible }
        )

        EntryFeedback(error = error)

        Spacer(modifier = Modifier.height(12.dp))

        PrimaryButton(
            text = "Mettre à jour le mot de passe",
            enabled = token.isNotBlank() && email.isNotBlank() && password.isNotBlank() && confirmation == password,
            isLoading = isLoading,
            onClick = {
                viewModel.updatePassword(token.trim(), email.trim(), password, confirmation) {
                    navController.navigate("sign_in") {
                        popUpTo("sign_in") { inclusive = true }
                    }
                }
            }
        )
    }
}

@Composable
private fun EntryScaffold(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()

    BizoScreen(
        onBack = onBack,
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    EntryHero(title = title, subtitle = subtitle)
                    Spacer(modifier = Modifier.height(24.dp))
                    ElevatedCard(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            content = content
                        )
                    }
                }

                footer?.let {
                    Spacer(modifier = Modifier.height(24.dp))
                    it()
                }
            }
        }
    )
}

@Composable
private fun EntryHero(
    title: String,
    subtitle: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = "Bizo",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EntryField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun EntryPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    passwordVisible: Boolean,
    onToggleVisibility: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            TextButton(onClick = onToggleVisibility) {
                Text(if (passwordVisible) "Masquer" else "Voir")
            }
        },
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun EntryFeedback(
    error: String? = null,
    info: String? = null
) {
    AnimatedVisibility(visible = !error.isNullOrBlank() || !info.isNullOrBlank()) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = when {
                !error.isNullOrBlank() -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text(
                text = error ?: info.orEmpty(),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                color = if (!error.isNullOrBlank()) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        OnboardingIllustration(page = page)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = page.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingIllustration(page: OnboardingPage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(page.accent.copy(alpha = 0.22f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        ElevatedCard(
            shape = RoundedCornerShape(34.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .height(250.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = page.accent.copy(alpha = 0.14f)
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(3) { index ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (index == 0) page.accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (index) {
                                        0 -> page.icon
                                        1 -> Icons.Default.Bolt
                                        else -> Icons.Default.Lock
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (index) {
                                        0 -> "Rapide"
                                        1 -> "Simple"
                                        else -> "Fiable"
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Publie, échange et conclue dans le même flux.",
                            modifier = Modifier.weight(1f),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            shape = CircleShape,
                            color = page.accent
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PagerIndicator(
    count: Int,
    currentPage: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .then(
                        if (index == currentPage) Modifier.width(26.dp) else Modifier.width(8.dp)
                    )
                    .height(8.dp)
                    .background(
                        color = if (index == currentPage) Color.Black else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(999.dp)
                    )
            )
        }
    }
}
