package dadm.grupo.dadmproyecto.di

import dadm.grupo.dadmproyecto.data.db.UsersRepository
import dadm.grupo.dadmproyecto.data.db.UsersRepositorySupabaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UsersBinderModule {

    @Binds
    @Singleton
    abstract fun bindUsersRepository(
        impl: UsersRepositorySupabaseImpl
    ): UsersRepository
}
