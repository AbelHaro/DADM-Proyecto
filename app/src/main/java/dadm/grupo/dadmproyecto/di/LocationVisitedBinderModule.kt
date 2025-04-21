package dadm.grupo.dadmproyecto.di

import dadm.grupo.dadmproyecto.data.db.LocationsVisitedRepository
import dadm.grupo.dadmproyecto.data.db.LocationsVisitedSupabaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationVisitedBinderModule {

    @Binds
    @Singleton
    abstract fun bindLocationVisitedRepository(
        impl: LocationsVisitedSupabaseImpl
    ): LocationsVisitedRepository
}
