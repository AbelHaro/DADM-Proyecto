package dadm.grupo.dadmproyecto.ui.destinationmap

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object GeofenceEventChannel {
    private val _geofenceEvents = MutableSharedFlow<Long>(replay = 0)
    val geofenceEvents: SharedFlow<Long> = _geofenceEvents.asSharedFlow()

    suspend fun triggerGeofenceEvent(locationId: Long) {
        _geofenceEvents.emit(locationId)
    }
}
