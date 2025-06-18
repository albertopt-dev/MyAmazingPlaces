package edu.albertoperez.myamazingplaces1.fragments


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import edu.albertoperez.myamazingplaces1.model.Lugar
import edu.albertoperez.myamazingplaces1.R
import edu.albertoperez.myamazingplaces1.utils.SQLite

/** Fragmento para mostrar los detalles de un lugar en concreto */
class FragmentDetallesLugar : Fragment(R.layout.fragment_detalles_lugar) {

    private lateinit var tvNombre: TextView
    private lateinit var tvTipo: TextView
    private lateinit var tvTelefono: TextView
    private lateinit var tvDireccion: TextView
    private lateinit var tvPaginaWeb: TextView
    private lateinit var rbValoracion: RatingBar
    private lateinit var ivLugar: ImageView
    private lateinit var btnRegresar: Button

    private var lugarId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_detalles_lugar, container, false)

        // Inicializar vistas
        tvNombre = view.findViewById(R.id.tvNombreDetalle)
        tvTipo = view.findViewById(R.id.tvTipoDetalle)
        tvTelefono = view.findViewById(R.id.tvTelefonoDetalle)
        tvDireccion = view.findViewById(R.id.tvDireccionDetalle)
        tvPaginaWeb = view.findViewById(R.id.tvPaginaWebDetalle)
        rbValoracion = view.findViewById(R.id.ratingBarDetalle)
        ivLugar = view.findViewById(R.id.ivLugarDetalle)
        btnRegresar = view.findViewById(R.id.btnRegresar)

        // Obtener el ID del lugar desde los argumentos
        lugarId = arguments?.getString("lugarId")

        lugarId?.let { loadLugar(it) }

        // Configurar el botón de regresar
        btnRegresar.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view
    }
    //Carga la informacion del lugar
    private fun loadLugar(lugarId: String) {
        val sqliteDb = SQLite(requireContext())

        // Intentar obtener el lugar desde SQLite primero
        val lugarDesdeSQLite = sqliteDb.getLugarById(lugarId)

        if (lugarDesdeSQLite != null) {
            populateUI(lugarDesdeSQLite) // Mostrar datos desde SQLite
        } else {
            // Si no está en SQLite, cargar desde Firestore y sincronizar con SQLite
            Firebase.firestore.collection("lugares").document(lugarId)
                .get()
                .addOnSuccessListener { document ->
                    val lugar = document.toObject(Lugar::class.java)
                    lugar?.let {
                        populateUI(it)
                        sqliteDb.insertLugar(it) // Guardar en SQLite para futuras consultas offline
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error al cargar lugar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    //Rellena la informacion del lugar cargado
    private fun populateUI(lugar: Lugar) {
        tvNombre.text = lugar.nombre
        tvTipo.text = lugar.tipo
        tvTelefono.text = lugar.telefono
        tvDireccion.text = lugar.direccion
        tvPaginaWeb.text = lugar.paginaWeb
        rbValoracion.rating = lugar.valoracion.toFloat()

        val imagenBase64 = lugar.imagen // Variable local inmutable
        if (!imagenBase64.isNullOrEmpty()) {
            val bitmap = base64ToBitmap(imagenBase64)
            ivLugar.setImageBitmap(bitmap)
        } else {
            ivLugar.setImageResource(R.drawable.icon)
        }
    }

    //Convierte una cadena base64 en un objeto bitmap par mostrar la imagen
    private fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT) // Decodificar Base64
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size) // Convertir a Bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("Base64", "Error al decodificar imagen Base64")
            null // Retorna null si hay un error
        }
    }
}
