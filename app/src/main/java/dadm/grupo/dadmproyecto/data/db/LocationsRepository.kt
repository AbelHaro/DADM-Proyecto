package dadm.grupo.dadmproyecto.data.db

import dadm.grupo.dadmproyecto.domain.model.Location

interface LocationsRepository {
    suspend fun getLocations(): List<Location>
    suspend fun getMyLocationsVisited(userId: String): List<Location>
}
