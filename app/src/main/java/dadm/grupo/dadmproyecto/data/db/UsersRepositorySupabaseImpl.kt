package dadm.grupo.dadmproyecto.data.db

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
}
