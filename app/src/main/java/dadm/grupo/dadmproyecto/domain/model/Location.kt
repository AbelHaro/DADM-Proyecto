package dadm.grupo.dadmproyecto.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Location(
    @SerialName("id") val id: Long,
    @SerialName("created_at") val createdAt: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double
)
