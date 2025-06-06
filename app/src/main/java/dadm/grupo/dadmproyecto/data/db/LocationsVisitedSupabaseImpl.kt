package dadm.grupo.dadmproyecto.data.db

import dadm.grupo.dadmproyecto.data.auth.AuthRepository
import dadm.grupo.dadmproyecto.domain.model.LocationVisited
import dadm.grupo.dadmproyecto.domain.model.LocationVisitedWithUser
import dadm.grupo.dadmproyecto.domain.model.UserVisitCount
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.runBlocking
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

    override suspend fun insertLocationVisited(
        userId: String,
        locationId: Long
    ): Boolean {
        return try {
            supabaseClient.from("locations_visited")
                .insert(LocationVisited(userId, locationId))
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getUsersOrderedByLocationsVisited(): List<UserVisitCount> = runBlocking {
        val visits = supabaseClient.from("locations_visited")
            .select(Columns.raw("user_id, location_id, created_at, users(display_name)"))
            .decodeList<LocationVisitedWithUser>()

        visits.groupBy { it.userId }
            .mapNotNull { (userId, userVisits) ->
                val user = userVisits.firstOrNull()?.users
                if (user != null) {
                    UserVisitCount(
                        userId = userId,
                        displayName = user.displayName,
                        visitCount = userVisits.size
                    )
                } else null
            }
            .sortedByDescending { it.visitCount }
    }
}
