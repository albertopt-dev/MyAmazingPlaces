package edu.albertoperez.myamazingplaces1

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import edu.albertoperez.myamazingplaces1.fragments.FragmentAnadirLugar
import edu.albertoperez.myamazingplaces1.fragments.FragmentEditarLugar
import edu.albertoperez.myamazingplaces1.fragments.FragmentListaLugares
import edu.albertoperez.myamazingplaces1.fragments.FragmentMenuPrincipal
import edu.albertoperez.myamazingplaces1.model.Lugar

/** Actividad Principal de la aplicación */
class MainActivity : AppCompatActivity(),
    FragmentAnadirLugar.OnLugarAddedListener,
    FragmentEditarLugar.OnLugarUpdatedListener {

    private lateinit var fragmentMenuPrincipal: FragmentMenuPrincipal
    //Permisos para la camara y el almacenamiento
    private val cameraPermissionRequestCode = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Solicitar permisos al iniciar la app
        requestPermissionsIfNecessary()

        // Barra de herramientas
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        //Si no hay un estado guardado, carga el fragmento del menu principal
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FragmentMenuPrincipal())
                .commit()
        }
    }

    //Imfla el menu de opciones de la barra de herramientas
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }
    //Controla las opciones seleccionadas del menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_add_place -> {    //Opcion para añadir un lugar
                replaceFragment(FragmentAnadirLugar())
                true
            }
            R.id.menu_edit_places -> {  //Opcion para editar un lugar
                replaceFragment(FragmentListaLugares())
                true
            }
            R.id.menu_main_menu -> {   //Opcion par avolver al menu principal
                replaceFragment(FragmentMenuPrincipal())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Metodo para reemplazar el fragmento actual con otro
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    // Implementación de OnLugarAddedListener para añadir un nuevo lugar
    override fun onLugarAdded(lugar: Lugar) {
        Firebase.firestore.collection("lugares")      //Accedemos a la coleccion lugares de fireBase
            .add(lugar)
            .addOnSuccessListener { documentReference ->
                lugar.id = documentReference.id                   // Asigna el ID generado por Firebase
                fragmentMenuPrincipal.loadLugaresFromFirestore() // Recarga la lista desde Firestore
                Toast.makeText(this, "Lugar añadido correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al añadir lugar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Implementación de OnLugarUpdatedListener para actualizar el lugar existente
    override fun onLugarUpdated(lugar: Lugar, position: Int) {
        Firebase.firestore.collection("lugares")
            .document(lugar.id)
            .set(lugar)
            .addOnSuccessListener {
                fragmentMenuPrincipal.loadLugaresFromFirestore() // Recargar la lista desde Firestore
                Toast.makeText(this, "Lugar actualizado correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar lugar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Solicitar permisos necesarios al inicio de abrir la app
    private fun requestPermissionsIfNecessary() {
        val permissionsToRequest = mutableListOf<String>()

        // Verificamos si el permiso de la cámara no está concedido
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        // Verificamo si el permiso de almacenamiento no está concedido
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        // Si hay permisos que solicitar, los pide al usuario y si no, muestra que ya estan concedidos
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), cameraPermissionRequestCode)
        } else {
            Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show()
        }
    }

    // Maneja el resultado de la solicitud de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == cameraPermissionRequestCode) {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }
            // Muestra un mensaje indicando si los permisos fueron concedidos o no
            if (deniedPermissions.isEmpty()) {
                Toast.makeText(this, "Permisos concedidos correctamente", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No se concedieron algunos permisos necesarios", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
