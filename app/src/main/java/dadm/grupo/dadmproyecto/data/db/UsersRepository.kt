package dadm.grupo.dadmproyecto.data.db

import dadm.grupo.dadmproyecto.domain.model.User

interface UsersRepository {
    suspend fun getMyUserData(): User?
}
