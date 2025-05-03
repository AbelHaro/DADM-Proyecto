package dadm.grupo.dadmproyecto.data.auth

import io.github.jan.supabase.gotrue.user.UserInfo

interface AuthRepository {
    suspend fun signInWithEmail(email: String, password: String): Result<Boolean>
    suspend fun signUpWithEmail(email: String, password: String): Result<Boolean>
    suspend fun signOut(): Result<Boolean>
    suspend fun isUserLoggedIn(): Boolean
    suspend fun getCurrentUser(): UserInfo?
    suspend fun forgotPassword(email: String)
}
