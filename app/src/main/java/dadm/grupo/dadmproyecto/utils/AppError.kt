package dadm.grupo.dadmproyecto.utils

sealed class AppError {
    data class NetworkError(val message: String) : AppError() // Error de red
}
