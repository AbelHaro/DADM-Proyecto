package dadm.grupo.dadmproyecto.ui.settings

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dadm.grupo.dadmproyecto.data.db.UsersRepository
import dadm.grupo.dadmproyecto.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val usersRepository: UsersRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _updateResult = MutableLiveData<Result<Boolean>>()
    val updateResult: LiveData<Result<Boolean>> = _updateResult

    init {
        viewModelScope.launch {
            _user.value = usersRepository.getMyUserData()
        }
    }

    fun updateUser(
        username: String,
        bio: String
    ) {
        viewModelScope.launch {
            val currentUser = _user.value ?: return@launch
            Log.d("SettingsDebug", "Current user: $currentUser")

            val updatedUser = currentUser.copy(displayName = username, bio = bio)
            Log.d("SettingsDebug", "Updated user: $updatedUser")

            val result = usersRepository.updateUserData(updatedUser)
            Log.d("SettingsDebug", "Update result: $result")

            if (result.isSuccess) {
                _user.value = updatedUser
                Log.d("SettingsDebug", "User updated successfully")
                // No language preference saving - using system language
            } else {
                result.exceptionOrNull()?.printStackTrace()
            }

            _updateResult.value = result
        }
    }
}
