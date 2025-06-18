package edu.albertoperez.myamazingplaces1.fragments

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import edu.albertoperez.myamazingplaces1.model.Lugar
import edu.albertoperez.myamazingplaces1.R
import edu.albertoperez.myamazingplaces1.utils.SQLite
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Locale
/** Fragmento para añadir un nuevo lugar a la lista */
class FragmentAnadirLugar : Fragment(R.layout.fragment_anadir_lugar) {
    //Variables
    private lateinit var spinnerTipo: Spinner
    private lateinit var nombreEditText: EditText
    private lateinit var telefonoEditText: EditText
    private lateinit var direccionEditText: EditText
    private lateinit var paginaWebEditText: EditText
    private lateinit var valoracionBar: RatingBar
    private lateinit var imagenButton: Button
    private lateinit var btnAnadirLugar: Button
    private lateinit var btnCancelar: Button
    private lateinit var imagen: ImageView
    private var callback: OnLugarAddedListener? = null
    private var imagenUri: Uri? = null
    //Codigos para identificar la camara y la galeria
    private val cameraRequestCode = 100
    private val galleryRequestCode = 101
    // Lista de tipos de lugares para el Spinner
    private val tiposLugar = listOf("Restaurante", "Museo", "Parque", "Monumento", "Pais")
    // Interfaz para notificar que se ha añadido un nuevo lugar
    interface OnLugarAddedListener {
        fun onLugarAdded(lugar: Lugar)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnLugarAddedListener) {
            callback = context
        } else {
            throw RuntimeException("El contexto debe implementar OnLugarAddedListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        saveDefaultImagesToGallery()
        setHasOptionsMenu(true) // Habilitar el menú en este fragmento
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_anadir_lugar, container, false)
        // Inicializar vistas
        spinnerTipo = view.findViewById(R.id.spinnerTipoLugar)
        nombreEditText = view.findViewById(R.id.nombre)
        telefonoEditText = view.findViewById(R.id.telefono)
        direccionEditText = view.findViewById(R.id.direccion)
        paginaWebEditText = view.findViewById(R.id.paginaWeb)
        valoracionBar = view.findViewById(R.id.ratingBar)
        imagenButton = view.findViewById(R.id.btnAddImagen)
        btnAnadirLugar = view.findViewById(R.id.btnAñadirLugar)
        btnCancelar = view.findViewById(R.id.btnCancelar)
        imagen = view.findViewById(R.id.imagen)

        // Configurar Spinner
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tiposLugar)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipo.adapter = adapter

        // Listeners
        imagenButton.setOnClickListener { showImagePickerDialog() }
        btnAnadirLugar.setOnClickListener {
            btnAnadirLugar.isEnabled = false // Deshabilitar botón para evitar múltiples clics
            saveLugar()
        }
        btnCancelar.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }

        requestPermissionsIfNecessary()

        return view
    }
    // Metodo para abrir la cámara
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, cameraRequestCode)
    }
    // Metodo para abrir la galeria
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        startActivityForResult(intent, galleryRequestCode)
    }

    // Metodo para guardar un nuevo lugar
    private fun saveLugar() {
        val nombre = nombreEditText.text.toString()
        val tipo = spinnerTipo.selectedItem.toString()
        val telefono = telefonoEditText.text.toString()
        val direccion = direccionEditText.text.toString()
        val paginaWeb = paginaWebEditText.text.toString()
        val valoracion = valoracionBar.rating.toInt()

        if (nombre.isEmpty()) {
            Toast.makeText(context, "El nombre y la dirección no pueden estar vacíos", Toast.LENGTH_SHORT).show()
            return
        }
        btnAnadirLugar.isEnabled = false // Deshabilitar botón mientras se procesa

        // Obtener coordenadas primero
        getCoordenadas(direccion) { latLng ->
            if (latLng != null) {
                // Convertir la imagen en un hilo separado
                Thread {
                    val base64Image = imagenUri?.let { uri ->
                        val inputStream = requireContext().contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        bitmap?.let { bitmapToBase64(it) } ?: ""
                    } ?: ""

                    requireActivity().runOnUiThread {
                        // Una vez convertida la imagen, guarda el lugar en Firestore
                        val lugar = Lugar(
                            id = "",
                            nombre = nombre,
                            tipo = tipo,
                            telefono = telefono,
                            direccion = direccion,
                            paginaWeb = paginaWeb,
                            valoracion = valoracion,
                            imagen = base64Image,
                            latitud = latLng.latitude,
                            longitud = latLng.longitude
                        )
                        saveLugarToFirestore(lugar) // Guardar el lugar en Firestore y SQLite
                    }
                }.start()
            } else {
                btnAnadirLugar.isEnabled = true // Reactivar botón si hay error
            }
        }
    }

    // Metodo para convertir una URI en Base64
    private fun bitmapToBase64(bitmap: Bitmap?): String {
        if (bitmap == null) {
            return ""
        }

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 800, 800, true) // Reducir tamaño
        val byteArrayOutputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream) // Comprimir
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    // Metodo para obtener coordenadas a partir de una dirección
    private fun getCoordenadas(direccion: String, onResult: (LatLng?) -> Unit) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        try {
            val direcciones = geocoder.getFromLocationName(direccion, 1)
            if (!direcciones.isNullOrEmpty()) {
                val location = direcciones[0]
                val latLng = LatLng(location.latitude, location.longitude)
                onResult(latLng)
            } else {
                onResult(null)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            onResult(null)
        }
    }

    //Metodo para guardar en Firestore
    private fun saveLugarToFirestore(lugar: Lugar) {
        Firebase.firestore.collection("lugares")
            .add(lugar)
            .addOnSuccessListener {
                val db = SQLite(requireContext())
                db.insertLugar(lugar)
                Toast.makeText(context, "Lugar añadido correctamente", Toast.LENGTH_SHORT).show()

                // Navegar de vuelta a la pantalla principal
                navigateToFragment(FragmentMenuPrincipal())
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error al guardar lugar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    //Mostrar un cuadro de dialogo con las opciones para insertar la imagen
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                cameraRequestCode -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    imagenUri = saveImageToMediaStore(imageBitmap, requireContext())
                    imagen.setImageBitmap(imageBitmap)
                }
                galleryRequestCode -> {
                    imagenUri = data?.data
                    imagenUri?.let { uri ->
                        val inputStream = requireContext().contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        imagen.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }

    //Metodos para guardar imagenes en la galeria
    private fun saveDefaultImagesToGallery() {
        val sharedPreferences = requireContext().getSharedPreferences("MyAmazingPlacesPrefs", Context.MODE_PRIVATE)
        val alreadySaved = sharedPreferences.getBoolean("images_saved", false)

        // Si ya se han guardado las imágenes antes, salir del metodo para evitar duplicados.
        if (alreadySaved) return

        // Lista de imágenes predefinidas en res/drawable
        val images = listOf(
            Pair(R.drawable.castillo_de_conwy, "castillo_de_conwy"),
            Pair(R.drawable.sagrada_familia, "sagrada_familia"),
            Pair(R.drawable.praga, "praga"),
            Pair(R.drawable.img1, "imagen1"),
            Pair(R.drawable.steirereck, "steirereck"),
            Pair(R.drawable.guggenheim, "guggenheim")
        )

        val resolver = requireContext().contentResolver

        // Guardar cada imagen en la galería
        for ((resourceId, fileName) in images) {
            val drawable = ContextCompat.getDrawable(requireContext(), resourceId) ?: continue
            val bitmap = (drawable as BitmapDrawable).bitmap

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyAmazingPlaces")
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
            }
        }

        // Guardar el estado en SharedPreferences para evitar guardar las imágenes múltiples veces
        sharedPreferences.edit().putBoolean("images_saved", true).apply()

        Snackbar.make(requireView(), "Imágenes predeterminadas guardadas en la galería", Snackbar.LENGTH_LONG).show()
    }

    //Metodo para guardar imagenes en la galeria
    private fun saveImageToMediaStore(bitmap: Bitmap, context: Context): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "imagen_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        }

        return uri
    }

    //Metodo para pedir permisos si son necesarios
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

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    // Metodo para navegar entre fragmentos
    private fun navigateToFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
    //Infla el menú de opciones en el fragmento y evita duplicados eliminando cualquier menú anterior
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear() // Limpia cualquier menú previo
        inflater.inflate(R.menu.menu_anadir_lugar, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    //Controla la seleccion de opciones del menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_main_menu -> {
                navigateToFragment(FragmentMenuPrincipal())
                true
            }
            R.id.menu_edit_places -> {
                navigateToFragment(FragmentListaLugares()) // Reutiliza FragmentListaLugares para modo edición
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
