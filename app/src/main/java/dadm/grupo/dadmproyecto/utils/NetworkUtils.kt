package dadm.grupo.dadmproyecto.utils

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkUtils @Inject constructor(
    // Inject NetworkManager to get the current state
    private val networkManager: NetworkManager
) {

    /**
     * Executes the given suspend function [action] only if the network is currently connected.
     * Checks the latest value from NetworkManager's StateFlow.
     *
     * @param T The return type of the action.
     * @param action The suspend function to execute.
     * @return The result of the action if executed, null otherwise.
     */
    suspend fun <T> runIfConnected(action: suspend () -> T): T? {
        // Access the current value of the StateFlow from NetworkManager
        return if (networkManager.isConnected.value) {
            action()
        } else {
            null
        }
    }

    // Remove this function as NetworkManager provides the state flow
    /*
    fun isNetworkAvailable(): Flow<Boolean> {
        // This is synchronous and doesn't fit the Flow pattern well here.
        // Rely on NetworkManager.isConnected StateFlow instead.
        return networkConnectivityChecker.isNetworkAvailable() // This was incorrect anyway
    }
    */
}
