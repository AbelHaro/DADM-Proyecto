package dadm.grupo.dadmproyecto.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapLibreModule {

    @Provides
    @Singleton
    fun provideMapLibre(@ApplicationContext context: Context): MapLibre {
        return MapLibre.getInstance(context, null, WellKnownTileServer.MapLibre)
    }
}
