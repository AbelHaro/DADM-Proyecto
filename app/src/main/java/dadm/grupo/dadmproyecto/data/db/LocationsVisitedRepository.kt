package dadm.grupo.dadmproyecto.data.db

import dadm.grupo.dadmproyecto.domain.model.LocationVisited

interface LocationsVisitedRepository {
    suspend fun getMyLocationsVisited(userId: String): List<LocationVisited>
}
