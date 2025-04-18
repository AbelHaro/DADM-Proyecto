package dadm.grupo.dadmproyecto.model

import org.maplibre.android.geometry.LatLng

data class POI(
    val id: String,
    val name: String,
    val location: LatLng,
    var discovered: Boolean = false
    // Podemos agregar una variable para modificar el radio de visibilidad del POI en el mapa
)
