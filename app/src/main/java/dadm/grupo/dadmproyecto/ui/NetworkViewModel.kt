// No changes needed in this file.
// It correctly injects NetworkManager and exposes its isConnected StateFlow.
package dadm.grupo.dadmproyecto.ui.main

import androidx.lifecycle.ViewModel
import dadm.grupo.dadmproyecto.utils.NetworkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(
    // Keep injecting NetworkManager
    networkManager: NetworkManager
) : ViewModel() {

    // Expose the StateFlow directly from the manager
    val isConnected = networkManager.isConnected

}
