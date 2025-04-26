package dadm.grupo.dadmproyecto.data.auth

import dadm.grupo.dadmproyecto.utils.NetworkUtils
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import javax.inject.Inject

class AuthRepositorySupabaseImpl @Inject constructor(
    supabaseClient: SupabaseClient,
    private val networkUtils: NetworkUtils
) : AuthRepository {

    private val auth = supabaseClient.auth

    override suspend fun signInWithEmail(email: String, password: String): Result<Boolean> {
        return networkUtils.runIfConnected {
            try {
                auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } ?: Result.failure(Exception("No network connection"))
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<Boolean> {
        return networkUtils.runIfConnected {
            try {
                auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } ?: Result.failure(Exception("No network connection"))
    }

    override suspend fun signOut(): Result<Boolean> {
        return networkUtils.runIfConnected {
            try {
                auth.signOut()
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } ?: Result.failure(Exception("No network connection"))
    }

    override suspend fun isUserLoggedIn(): Boolean {
        return networkUtils.runIfConnected {
            auth.loadFromStorage(autoRefresh = true)
        } == true
    }

    override suspend fun getCurrentUser(): UserInfo? {
        return networkUtils.runIfConnected {
            auth.currentUserOrNull()
        }
    }
}
