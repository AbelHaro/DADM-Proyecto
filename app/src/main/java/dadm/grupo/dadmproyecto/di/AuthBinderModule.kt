package dadm.grupo.dadmproyecto.di

import dadm.grupo.dadmproyecto.data.auth.AuthRepository
import dadm.grupo.dadmproyecto.data.auth.AuthRepositorySupabaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBinderModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositorySupabaseImpl
    ): AuthRepository
}
