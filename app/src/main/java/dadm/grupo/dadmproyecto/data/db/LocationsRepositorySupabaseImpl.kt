package dadm.grupo.dadmproyecto.data.db

import dadm.grupo.dadmproyecto.domain.model.Location
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import javax.inject.Inject

class LocationsRepositorySupabaseImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : LocationsRepository {
    override suspend fun getLocations(): List<Location> {
        return supabaseClient
            .from("locations")
            .select(Columns.ALL)
            .decodeList<Location>()
    }

    override suspend fun getMyLocationsVisited(userId: String): List<Location> {
        // Create a data class to match the nested JSON structure
        @Serializable
        data class LocationWrapper(val locations: Location)

        return supabaseClient
            .from("locations_visited")
            .select(Columns.raw("locations(*)")) {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<LocationWrapper>()
            .map { it.locations }
    }
}
