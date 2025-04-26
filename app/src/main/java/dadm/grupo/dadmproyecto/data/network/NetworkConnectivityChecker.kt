package dadm.grupo.dadmproyecto.data.network


interface NetworkConnectivityChecker {
    fun isNetworkAvailable(): Boolean
}
