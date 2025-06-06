package dadm.grupo.dadmproyecto.di

import dadm.grupo.dadmproyecto.data.db.LocationsRepository
import dadm.grupo.dadmproyecto.data.db.LocationsRepositorySupabaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationBinderModule {

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        impl: LocationsRepositorySupabaseImpl
    ): LocationsRepository

}
