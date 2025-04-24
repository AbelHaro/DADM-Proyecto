package dadm.grupo.dadmproyecto.domain.model

data class RankedUser(
    val userId: String,
    val displayName: String,
    val bio: String,
    val position: Int,
    val visitCount: Int
)
