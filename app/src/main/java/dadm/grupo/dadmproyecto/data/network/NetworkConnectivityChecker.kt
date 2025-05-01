package dadm.grupo.dadmproyecto.data.network

import kotlinx.coroutines.flow.Flow


interface NetworkConnectivityChecker {
    // Method signature is correct - returns a Flow
    fun isNetworkAvailable(): Flow<Boolean>
}
