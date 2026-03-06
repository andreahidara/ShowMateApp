package com.example.showmateapp.ui.screens.login

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    // Estados de la pantalla (Cargando, Error o Éxito)
    // Usamos StateFlow para que la pantalla "escuche" estos cambios al instante
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isLoginSuccessful = MutableStateFlow(false)
    val isLoginSuccessful: StateFlow<Boolean> = _isLoginSuccessful

    // Función que llamaremos al pulsar el botón "Sign In"
    fun signIn(email: String, pass: String) {
        // 1. Comprobamos que no estén vacíos
        if (email.isBlank() || pass.isBlank()) {
            _errorMessage.value = "Please fill in all fields."
            return
        }

        // 2. Empezamos a cargar y limpiamos errores anteriores
        _isLoading.value = true
        _errorMessage.value = null

        // 3. Enviamos los datos a Firebase
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                _isLoading.value = false // Terminamos de cargar

                if (task.isSuccessful) {
                    // ¡Login correcto!
                    _isLoginSuccessful.value = true
                } else {
                    // Algo falló (contraseña mal, correo no existe...)
                    _errorMessage.value = task.exception?.localizedMessage ?: "Login failed."
                }
            }
    }
}
