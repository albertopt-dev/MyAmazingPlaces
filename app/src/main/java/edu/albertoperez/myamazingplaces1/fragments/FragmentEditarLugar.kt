package edu.albertoperez.myamazingplaces1.fragments

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.*
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import edu.albertoperez.myamazingplaces1.model.Lugar
import edu.albertoperez.myamazingplaces1.R
import edu.albertoperez.myamazingplaces1.utils.SQLite
import java.io.ByteArrayOutputStream
import java.io.IOException
/** Fragmento para editar un lugar */
class FragmentEditarLugar : Fragment(R.layout.fragment_editar_lugar) {
    //Elementos de la interfaz
    private lateinit var spinnerTipo: Spinner
    private lateinit var nombreEditText: EditText
    private lateinit var telefonoEditText: EditText
    private lateinit var direccionEditText: EditText
    private lateinit var paginaWebEditText: EditText
    private lateinit var valoracionBar: RatingBar
    private lateinit var imagenButton: Button
    private lateinit var btnGuardar: Button
    private lateinit var btnCancelar: Button
    private lateinit var imagen: ImageView
    //Variables para lugar e imagenes
    private var lugar: Lugar? = null
    private var imagenUri: Uri? = null
    private val cameraRequestCode = 100
    private val galleryRequestCode = 101
    private val tiposLugar = listOf("Restaurante", "Museo", "Parque", "Monumento", "Pais")
    private var lugarId: String? = null
    private lateinit var sqliteDb: SQLite
    private var callback: OnLugarUpdatedListener? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_editar_lugar, container, false)

        // Inicializar SQLite
        sqliteDb = SQLite(requireContext())

        // Inicializar vistas
        spinnerTipo = view.findViewById(R.id.spinnerTipo)
        nombreEditText = view.findViewById(R.id.editNombre)
        telefonoEditText = view.findViewById(R.id.editTelefono)
        direccionEditText = view.findViewById(R.id.editDireccion)
        paginaWebEditText = view.findViewById(R.id.editPaginaWeb)
        valoracionBar = view.findViewById(R.id.ratingBar)
        imagenButton = view.findViewById(R.id.btnAddImagen)
        btnGuardar = view.findViewById(R.id.btnGuardarCambios)
        btnCancelar = view.findViewById(R.id.btnEliminarLugar)
        imagen = view.findViewById(R.id.ivLugarImagen)

        // Configurar Spinner
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tiposLugar)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipo.adapter = adapter

        // Obtener ID del lugar
        lugarId = arguments?.getString("lugarId")
        lugarId?.let { loadLugar(it) }

        requestPermissionsIfNecessary()

        // Listeners para los botones
        imagenButton.setOnClickListener { showImagePickerDialog() }
        btnGuardar.setOnClickListener { updateLugar() }
        btnCancelar.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro de que deseas eliminar este lugar?")
                .setPositiveButton("Sí") { _, _ ->
                    deleteLugar()
                }
                .setNegativeButton("No", null)
                .show()
        }
        return view
    }
    // Cargar lugar desde Firestore y sincronizar con SQLite
    private fun loadLugar(lugarId: String) {
        Firebase.firestore.collection("lugares").document(lugarId)
            .get()
            .addOnSuccessListener { document ->
                val lugar = document.toObject(Lugar::class.java)
                lugar?.id = document.id // Asignar el ID desde Firestore
                lugar?.let {
                    populateUI(it) // Llenar los datos en la interfaz

                    // Verificar si ya existe en SQLite antes de insertarlo
                    if (sqliteDb.getLugarById(it.id) == null) {
                        sqliteDb.insertLugar(it)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error al cargar lugar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Metodo par eliminar un lugar de SQLite y Firestore
    private fun deleteLugar() {
        if (lugar == null || lugar?.id.isNullOrEmpty()) {
            Toast.makeText(context, "No se puede eliminar el lugar. ID no válido.", Toast.LENGTH_SHORT).show()
            return
        }

        // Eliminar de Firestore
        Firebase.firestore.collection("lugares")
            .document(lugar!!.id)
            .delete()
            .addOnSuccessListener {
                //eliminar de SQLite
                sqliteDb.deleteLugar(lugar!!.id)

                Toast.makeText(context, "Lugar eliminado correctamente", Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.popBackStack() // Regresar a la pantalla principal
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error al eliminar lugar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    //Metodo para llenar los campos del formulario con los datos del lugar recibido
    private fun populateUI(lugar: Lugar) {
        nombreEditText.setText(lugar.nombre)
        spinnerTipo.setSelection(tiposLugar.indexOf(lugar.tipo))
        telefonoEditText.setText(lugar.telefono)
        direccionEditText.setText(lugar.direccion)
        paginaWebEditText.setText(lugar.paginaWeb)
        valoracionBar.rating = lugar.valoracion.toFloat()
        // Verifica si el lugar tiene una imagen guardada en Base64
        if (!lugar.imagen.isNullOrEmpty()) {
            val bitmap = base64ToBitmap(lugar.imagen!!)
            if (bitmap != null) {
                imagen.setImageBitmap(bitmap)
                Log.d("EditarLugar", "Imagen cargada correctamente desde Firestore") // <-- Agrega este Log
            } else {
                // Si la conversión falla, mostrar una imagen predeterminada
                imagen.setImageResource(R.drawable.icon)
            }
        } else {
            // Si no hay una imagen guardada para el lugar, mostrar una imagen predeterminada
            imagen.setImageResource(R.drawable.icon)
        }
        this.lugar = lugar
    }

    // Función para convertir Base64 a Bitmap
    private fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: IllegalArgumentException) {
            Log.e("Base64", "Error al decodificar Base64: ${e.message}")
            null
        }
    }

    //Metodo para mostrar un cuadro de dialogo para poder seleccionar la imagen
    private fun showImagePickerDialog() {
        val options = arrayOf("Tomar foto", "Elegir desde galería")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Selecciona una opción")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openCamera()
                1 -> openGallery()
            }
        }
        builder.show()
    }

    //metodo para abrir la camara
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, cameraRequestCode)
    }
    //metodo para abrir la galaria
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivityForResult(intent, galleryRequestCode)
    }

    //Convierte una imagen en una cadena base 64 para poder almacenarla
    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("uriToBase64", "Error convirtiendo URI a Base64: ${e.message}")
            null
        }
    }
    // Metodo para actualizar un lugar en Firestore y SQLite
    private fun updateLugar() {
        val nombre = nombreEditText.text.toString()
        val tipo = spinnerTipo.selectedItem.toString()
        val telefono = telefonoEditText.text.toString()
        val direccion = direccionEditText.text.toString()
        val paginaWeb = paginaWebEditText.text.toString()
        val valoracion = valoracionBar.rating.toInt()

        if (nombre.isEmpty()) {
            Toast.makeText(context, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }

        val imagenBase64 = if (imagenUri != null) {
            uriToBase64(imagenUri!!) // Convertir la imagen a Base64 si se cambió
        } else {
            lugar?.imagen // Mantener la imagen original
        }

        // Si la dirección es la misma, usar las coordenadas antiguas
        if (direccion == lugar?.direccion) {
            val lugarActualizado = Lugar(
                id = lugarId ?: "",
                nombre = nombre,
                tipo = tipo,
                telefono = telefono,
                direccion = direccion,
                paginaWeb = paginaWeb,
                valoracion = valoracion,
                imagen = imagenBase64,
                latitud = lugar?.latitud,  // Mantener coordenadas originales
                longitud = lugar?.longitud // Mantener coordenadas originales
            )

            saveLugarFirestore(lugarActualizado)
        } else {
            // La dirección cambió, obtener nuevas coordenadas
            getCoordenadas(direccion) { nuevaLatLng ->
                if (nuevaLatLng != null) {
                    val lugarActualizado = Lugar(
                        id = lugarId ?: "",
                        nombre = nombre,
                        tipo = tipo,
                        telefono = telefono,
                        direccion = direccion,
                        paginaWeb = paginaWeb,
                        valoracion = valoracion,
                        imagen = imagenBase64,
                        latitud = nuevaLatLng.latitude, // Usar nuevas coordenadas
                        longitud = nuevaLatLng.longitude // Usar nuevas coordenadas
                    )
                    saveLugarFirestore(lugarActualizado)
                } else {
                    Toast.makeText(context, "No se pudo obtener la nueva ubicación.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //Metodo para guardar los lugares en firestore
    private fun saveLugarFirestore(lugar: Lugar) {
        val sqliteDb = SQLite(requireContext())

        Firebase.firestore.collection("lugares").document(lugar.id)
            .set(lugar)
            .addOnSuccessListener {
                //actualizar el lugar en SQLite
                sqliteDb.updateLugar(lugar)
                Toast.makeText(context, "Lugar actualizado correctamente", Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error al actualizar lugar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    //Metodo par abtener las coordenadas del lugar que se ha introducido para mostrarlo en el maap
    private fun getCoordenadas(direccion: String, onResult: (LatLng?) -> Unit) {
        val geocoder = Geocoder(requireContext())
        try {
            val direcciones = geocoder.getFromLocationName(direccion, 1)
            if (!direcciones.isNullOrEmpty()) {
                val location = direcciones[0]
                val latLng = LatLng(location.latitude, location.longitude)
                Log.d("Geocoder", "Nuevas coordenadas: ${latLng.latitude}, ${latLng.longitude}")
                onResult(latLng)
            } else {
                Log.e("Geocoder", "No se encontraron coordenadas para $direccion")
                onResult(null)
            }
        } catch (e: IOException) {
            Log.e("Geocoder", "Error al obtener coordenadas: ${e.message}")
            onResult(null)
        }
    }

    //Metodo para manejar el resultado de la seleccion de imagenes desde la galeria o desde la camara
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                //Si se hace una foto con la camara
                cameraRequestCode -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    // Guarda la imagen en la galería y obtiene su URI
                    imagenUri = saveImageToMediaStore(imageBitmap, requireContext())
                    // Muestra la imagen capturada en el ImageView
                    imagen.setImageBitmap(imageBitmap)
                }
                //Si se selecciona una imagen desde la galeria
                galleryRequestCode -> {
                    val selectedImageUri = data?.data
                    //Muestra la img en el ImageView
                    imagen.setImageURI(selectedImageUri)
                    //guarda la URI de la img seleccionada
                    imagenUri = selectedImageUri
                }
            }
        }
    }

    //Metodo para guardar la captura de imagen en la galeria del telefono
    private fun saveImageToMediaStore(bitmap: Bitmap, context: Context): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "imagen_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")  // Asegúrate que sea JPEG o PNG
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyAmazingPlaces")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                Log.d("SaveImage", "Imagen guardada en MediaStore correctamente")
            }
        } ?: Log.e("SaveImage", "No se pudo insertar la imagen en MediaStore")

        return uri
    }

    //Metodo para comprobar si todos los permisos estan concedidos
    private fun requestPermissionsIfNecessary() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(), permissions.toTypedArray(), cameraRequestCode)
        }
    }

    interface OnLugarUpdatedListener {
        fun onLugarUpdated(lugar: Lugar, position: Int)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnLugarUpdatedListener) {
            callback = context
        } else {
            throw RuntimeException("El contexto debe implementar OnLugarUpdatedListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    private fun navigateToFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // Indica que este fragmento tiene un menú
    }

    //Configura el menú de opciones del fragmento
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear() // Limpia cualquier menú previo
        inflater.inflate(R.menu.menu, menu) // Infla el menú actual
        super.onCreateOptionsMenu(menu, inflater)
    }

    //Maneja las opciones seleccionadas en el menú
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_edit_places -> {
                navigateToFragment(FragmentListaLugares())
                true
            }

            R.id.menu_add_place -> {
                navigateToFragment(FragmentAnadirLugar())
                true
            }


            R.id.menu_main_menu -> {
                navigateToFragment(FragmentMenuPrincipal()) // Navegar a la pantalla principal
                true
            }

            else -> super.onOptionsItemSelected(item)

        }
    }

    //Prepara el menú antes de mostrarse
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val menuEditPlacesItem = menu.findItem(R.id.menu_edit_places)
        menuEditPlacesItem?.isVisible = false // Oculta "Editar lugares"
    }

}
