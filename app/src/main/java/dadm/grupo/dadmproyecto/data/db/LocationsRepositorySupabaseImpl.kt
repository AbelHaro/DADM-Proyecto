package dadm.grupo.dadmproyecto.data.db

import dadm.grupo.dadmproyecto.domain.model.Location
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
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
}
