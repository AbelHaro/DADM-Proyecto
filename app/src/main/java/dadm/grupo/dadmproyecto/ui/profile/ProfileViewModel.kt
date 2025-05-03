package dadm.grupo.dadmproyecto.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dadm.grupo.dadmproyecto.data.auth.AuthRepository
import dadm.grupo.dadmproyecto.data.db.LocationsVisitedRepository
import dadm.grupo.dadmproyecto.domain.model.User
import dadm.grupo.dadmproyecto.data.db.UsersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UsersRepository,
    private val vistedRepository: LocationsVisitedRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> get() = _userState

    private val _locationsVisited = MutableStateFlow(0)
    val locationsVisited: StateFlow<Int> get() = _locationsVisited

    private val _logoutState = MutableStateFlow<Result<Boolean>?>(null)
    val logoutState: StateFlow<Result<Boolean>?> get() = _logoutState

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                userRepository.getMyUserData()?.let { user ->
                    _userState.value = user
                    loadLocationsVisited(user.userId)
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

    private suspend fun loadLocationsVisited(userId: String) {
        try {
            val visitedLocations = vistedRepository.getMyLocationsVisited(userId)
            _locationsVisited.value = visitedLocations.size
        } catch (e: Exception) {
            e.printStackTrace()
            _locationsVisited.value = 0
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                val result = authRepository.signOut()
                _logoutState.value = result
            } catch (e: Exception) {
                _logoutState.value = Result.failure(e)
            }
        }
    }
}
