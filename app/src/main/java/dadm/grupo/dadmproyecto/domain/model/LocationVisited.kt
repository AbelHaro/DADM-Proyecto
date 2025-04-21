package dadm.grupo.dadmproyecto.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationVisited(
    @SerialName("user_id") val id: String,
    @SerialName("location_id") val locationId: Long,
    @SerialName("created_at") val createdAt: String? = null,
)
