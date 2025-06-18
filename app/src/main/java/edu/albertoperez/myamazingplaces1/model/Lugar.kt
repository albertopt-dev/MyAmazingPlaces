package edu.albertoperez.myamazingplaces1.model

data class Lugar(
    var id: String = "",
    var nombre: String,
    var tipo: String,
    var telefono: String?,
    var direccion: String?,
    var paginaWeb: String?,
    var valoracion: Int,
    var imagen: String? = null,
    var fechaEdicion: Long = System.currentTimeMillis(),
    var latitud: Double? = null,
    var longitud: Double? = null
)
{
    // Constructor vacío requerido por Firebase Firestore
    constructor() : this("", "", "", "", "", "", 0, null, System.currentTimeMillis(), null, null)
}
