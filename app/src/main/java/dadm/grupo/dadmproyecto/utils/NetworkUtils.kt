package dadm.grupo.dadmproyecto.utils

import dadm.grupo.dadmproyecto.data.network.NetworkConnectivityChecker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkUtils @Inject constructor(
    private val networkConnectivityChecker: NetworkConnectivityChecker
) {

    suspend fun <T> runIfConnected(action: suspend () -> T): T? {
        return if (networkConnectivityChecker.isNetworkAvailable()) {
            action()
        } else {
            null
        }
    }
}
