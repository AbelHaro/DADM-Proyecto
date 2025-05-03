package dadm.grupo.dadmproyecto.data.db

import dadm.grupo.dadmproyecto.data.auth.AuthRepository
import dadm.grupo.dadmproyecto.domain.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.utils.EmptyContent.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import java.lang.reflect.Array.set
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
            val updatedUser = supabaseClient
                .from("users").update(
                    {
                        set("display_name", user.displayName)
                        set("bio", user.bio)
                        set("updated_at", System.currentTimeMillis().toString())
                    }
                ) {
                    select()
                    filter {
                        eq("user_id", authRepository.getCurrentUser()?.id ?: "")
                    }
                }
                .decodeList<User>()
                .firstOrNull()

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
