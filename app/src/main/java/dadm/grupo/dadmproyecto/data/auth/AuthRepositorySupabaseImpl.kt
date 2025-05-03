package dadm.grupo.dadmproyecto.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import javax.inject.Inject

class AuthRepositorySupabaseImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : AuthRepository {

    private val auth = supabaseClient.auth

    override suspend fun signInWithEmail(email: String, password: String): Result<Boolean> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<Boolean> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Boolean> {
        return try {
            auth.signOut()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isUserLoggedIn(): Boolean {
        return auth.loadFromStorage(autoRefresh = true)
    }

    override suspend fun getCurrentUser(): UserInfo? {
        return auth.currentUserOrNull()
    }

    override suspend fun forgotPassword(email: String) {
        auth.resetPasswordForEmail(email, "https://ahararm.upv.edu.es/reset_password/index.html")
    }
}
