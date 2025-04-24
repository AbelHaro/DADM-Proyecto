package dadm.grupo.dadmproyecto.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationVisitedWithUser(
    @SerialName("user_id") val userId: String,
    @SerialName("location_id") val locationId: Long,
    @SerialName("created_at") val createdAt: String,
    val users: UserDisplayInfo?
)

@Serializable
data class UserDisplayInfo(
    @SerialName("display_name") val displayName: String
)
