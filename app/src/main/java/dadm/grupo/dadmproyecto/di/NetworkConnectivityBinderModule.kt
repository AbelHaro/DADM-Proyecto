package dadm.grupo.dadmproyecto.di

import android.content.Context
import dadm.grupo.dadmproyecto.data.network.NetworkConnectivityChecker
import dadm.grupo.dadmproyecto.data.network.NetworkConnectivityCheckerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkConnectivityBinderModule {

    @Provides
    @Singleton
    fun provideNetworkConnectivityChecker(
        @ApplicationContext context: Context
    ): NetworkConnectivityChecker {
        return NetworkConnectivityCheckerImpl(context)
    }
}
