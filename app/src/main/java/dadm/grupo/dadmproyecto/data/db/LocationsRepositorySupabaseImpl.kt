package dadm.grupo.dadmproyecto.data.db

import dadm.grupo.dadmproyecto.domain.model.Location
import dadm.grupo.dadmproyecto.utils.NetworkUtils
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import javax.inject.Inject

class LocationsRepositorySupabaseImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val networkUtils: NetworkUtils
) : LocationsRepository {

    override suspend fun getLocations(): List<Location> =
        networkUtils.runIfConnected {
            supabaseClient
                .from("locations")
                .select(Columns.ALL)
                .decodeList<Location>()
        } ?: emptyList()

    @Serializable
    private data class LocationWrapper(val locations: Location)

    override suspend fun getMyLocationsVisited(userId: String): List<Location> =
        networkUtils.runIfConnected {
            supabaseClient
                .from("locations_visited")
                .select(Columns.raw("locations(*)")) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<LocationWrapper>()
                .map { it.locations }
        } ?: emptyList()
}
