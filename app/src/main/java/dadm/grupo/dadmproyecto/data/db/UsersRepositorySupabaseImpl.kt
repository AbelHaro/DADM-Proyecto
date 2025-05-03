package dadm.grupo.dadmproyecto.data.db

import android.util.Log
import dadm.grupo.dadmproyecto.data.auth.AuthRepository
import dadm.grupo.dadmproyecto.domain.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject

class UsersRepositorySupabaseImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) : UsersRepository {
    override suspend fun getMyUserData(): User? {
        return supabaseClient
            .from("users")
            .select(Columns.ALL) {
                filter {
                    eq("user_id", authRepository.getCurrentUser()?.id ?: "")
                }
            }
            .decodeList<User>()
            .firstOrNull()
    }

    override suspend fun updateUserData(user: User): Result<Boolean> {
        return try {
            Log.d("SettingsDebug", "Updating user data: $user")

            val updatedUser = supabaseClient
                .from("users").update(
                    user
                ) {
                    select()
                    filter {
                        eq("user_id", authRepository.getCurrentUser()?.id ?: "")
                    }
                }
                .decodeList<User>()
                .firstOrNull()

            Log.d("SettingsDebug", "Updated user data: $updatedUser")

            if (updatedUser == null) {
                return Result.failure(Exception("Error updating user data"))
            }
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
