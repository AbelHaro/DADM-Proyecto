package dadm.grupo.dadmproyecto.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dadm.grupo.dadmproyecto.data.db.UsersRepository
import dadm.grupo.dadmproyecto.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val usersRepository: UsersRepository
) : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    init {
        viewModelScope.launch {
            _user.value = usersRepository.getMyUserData()
        }
    }

    fun updateUser(username: String, bio: String, password: String?, language: String): Result<Boolean> {
        var result: Result<Boolean> = Result.failure(Exception("Operación no iniciada"))
        viewModelScope.launch {
            val currentUser = _user.value ?: return@launch

            val updatedUser = currentUser.copy(displayName = username, bio = bio)

            result = usersRepository.updateUserData(updatedUser)

            if (result.isSuccess) {
                _user.value = updatedUser
            } else {
                result.exceptionOrNull()?.printStackTrace()
                // Aquí puedes manejar el error, como mostrar un mensaje al usuario
            }

            password?.let {
                //usersRepository.updatePassword(it)
            }
        }
        return result
    }
}
