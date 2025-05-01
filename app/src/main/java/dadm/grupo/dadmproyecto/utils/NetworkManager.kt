package dadm.grupo.dadmproyecto.utils

import android.util.Log
import dadm.grupo.dadmproyecto.data.network.NetworkConnectivityChecker // Import the checker
import dadm.grupo.dadmproyecto.di.ApplicationScope // Import custom scope annotation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn // Import launchIn
import kotlinx.coroutines.flow.onEach // Import onEach
import javax.inject.Inject
import javax.inject.Singleton // Use Singleton for NetworkManager

@Singleton // Make NetworkManager a Singleton to ensure one instance manages the callback
class NetworkManager @Inject constructor(
    private val networkConnectivityChecker: NetworkConnectivityChecker,
    // Inject the application-level scope
    @ApplicationScope private val externalScope: CoroutineScope
) {
    // Initialize with a default value, the flow will update it.
    // Consider checking initial state synchronously if critical, but flow handles it.
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    init {
        Log.d("MainActivityInternet", "Initializing and starting network status collection.")
        // Call the method that returns the Flow
        networkConnectivityChecker.isNetworkAvailable()
            .onEach { isNowConnected ->
                Log.d("MainActivityInternet", "Received network status update: $isNowConnected")
                _isConnected.value = isNowConnected
            }
            // Launch collection in the injected application scope
            .launchIn(externalScope)
    }

    // No manual check method needed
}
