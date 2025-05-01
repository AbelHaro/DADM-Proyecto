package dadm.grupo.dadmproyecto.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Make the implementation a Singleton as well
class NetworkConnectivityCheckerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkConnectivityChecker {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun isNetworkAvailable(): Flow<Boolean> = callbackFlow {
        // Function to check current network state
        fun checkCurrentNetworkState(): Boolean {
            return try {
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) // Check for actual internet access
            } catch (e: Exception) {
                Log.e("NetworkChecker", "Error checking network state", e)
                false
            }
        }

        // Send the initial state
        trySend(checkCurrentNetworkState())
        Log.d("NetworkChecker", "Initial network state sent: ${checkCurrentNetworkState()}")


        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("NetworkChecker", "Network available: $network")
                // It's better to re-query the overall state as 'available' doesn't guarantee internet
                trySend(checkCurrentNetworkState())
            }

            override fun onLost(network: Network) {
                Log.d("NetworkChecker", "Network lost: $network")
                // Wait a bit before declaring disconnected, as another network might become active
                // However, for simplicity here, we check the overall state immediately.
                trySend(checkCurrentNetworkState())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                Log.d("NetworkChecker", "Network capabilities changed: $network")
                trySend(checkCurrentNetworkState())
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        Log.d("NetworkChecker", "Registering network callback")
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Unregister the callback when the flow is cancelled
        awaitClose {
            Log.d("NetworkChecker", "Unregistering network callback")
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged() // Only emit changes
}
