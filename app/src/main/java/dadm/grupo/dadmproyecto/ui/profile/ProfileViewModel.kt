package dadm.grupo.dadmproyecto.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dadm.grupo.dadmproyecto.domain.model.User
import dadm.grupo.dadmproyecto.data.db.UsersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UsersRepository
) : ViewModel() {

    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> get() = _userState

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                userRepository.getMyUserData()?.let { user ->
                    _userState.value = user
                } ?: run {
                    // Manejo de error, opcional
                    _userState.value = null
                }
            } catch (e: Exception) {
                // Manejo de error, opcional
                e.printStackTrace()
            }
        }
    }
}
