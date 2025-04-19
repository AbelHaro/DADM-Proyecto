package dadm.grupo.dadmproyecto.data.db

import dadm.grupo.dadmproyecto.data.auth.AuthRepository
import dadm.grupo.dadmproyecto.domain.model.LocationVisited
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject

class LocationsVisitedSupabaseImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) : LocationsVisitedRepository {
    override suspend fun getMyLocationsVisited(userId: String): List<LocationVisited> {
        return supabaseClient
            .from("locations_visited")
            .select(Columns.ALL) {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<LocationVisited>()
    }
}
