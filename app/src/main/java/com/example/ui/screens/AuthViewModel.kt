package com.example.ui.screens

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AuthState {
    INPUT_PHONE,
    INPUT_OTP,
    LOADING,
    SUCCESS,
    ERROR
}

class AuthViewModel : ViewModel() {
    private val auth by lazy { try { FirebaseAuth.getInstance() } catch (e: Exception) { null } }
    private val db by lazy { try { FirebaseFirestore.getInstance() } catch (e: Exception) { null } }

    private val _authState = MutableStateFlow(AuthState.INPUT_PHONE)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun sendVerificationCode(phone: String, activity: Activity) {
        if (auth == null) {
             _errorMessage.value = "Erreur configuration Firebase"
             _authState.value = AuthState.ERROR
             return
        }
        _authState.value = AuthState.LOADING
        val options = PhoneAuthOptions.newBuilder(auth!!)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithCredential(credential)
                }

                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    val msg = e.message ?: ""
                    _errorMessage.value = if (msg.contains("CONFIGURATION_NOT_FOUND") || msg.contains("Firebase parameters are not specified")) {
                        "Erreur Configuration: Veuillez renseigner FIREBASE_API_KEY, APP_ID, etc. dans les Secrets de AI Studio, et ajouter les clés SHA-1 dans la console Firebase."
                    } else {
                        msg
                    }
                    _authState.value = AuthState.ERROR
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    storedVerificationId = verificationId
                    resendToken = token
                    _authState.value = AuthState.INPUT_OTP
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyCode(code: String) {
        val verificationId = storedVerificationId ?: return
        _authState.value = AuthState.LOADING
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithCredential(credential)
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        checkUserInFirestore(user.uid, user.phoneNumber ?: "")
                    } else {
                        _authState.value = AuthState.ERROR
                        _errorMessage.value = "Utilisateur null après connexion"
                    }
                } else {
                    _authState.value = AuthState.ERROR
                    _errorMessage.value = task.exception?.message
                }
            }
    }

    private fun checkUserInFirestore(uid: String, phone: String) {
        db?.collection("users")?.document(uid)?.get()
            ?.addOnSuccessListener { document ->
                if (!document.exists()) {
                    val userData = hashMapOf(
                        "uid" to uid,
                        "phone" to phone,
                        "displayName" to "Utilisateur",
                        "createdAt" to com.google.firebase.Timestamp.now(),
                        "isVerified" to false
                    )
                    db?.collection("users")?.document(uid)?.set(userData)
                        ?.addOnSuccessListener {
                            _authState.value = AuthState.SUCCESS
                        }
                        ?.addOnFailureListener { e ->
                            _errorMessage.value = e.message
                            _authState.value = AuthState.ERROR
                        }
                } else {
                    _authState.value = AuthState.SUCCESS
                }
            }
            ?.addOnFailureListener { e ->
                _errorMessage.value = e.message
                _authState.value = AuthState.ERROR
            }
    }

    fun resetState() {
        _authState.value = AuthState.INPUT_PHONE
        _errorMessage.value = null
    }
}
