package dadm.grupo.dadmproyecto.data.auth

interface AuthRepository {
    suspend fun signInWithEmail(email: String, password: String): Result<Boolean>
    suspend fun signUpWithEmail(email: String, password: String): Result<Boolean>
    suspend fun signOut(): Result<Boolean>
    fun isUserLoggedIn(): Boolean
}
