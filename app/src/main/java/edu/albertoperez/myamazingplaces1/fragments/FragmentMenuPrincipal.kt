package edu.albertoperez.myamazingplaces1.fragments

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import edu.albertoperez.myamazingplaces1.model.Lugar
import edu.albertoperez.myamazingplaces1.LugarAdapter
import edu.albertoperez.myamazingplaces1.MapsActivity
import edu.albertoperez.myamazingplaces1.R
import edu.albertoperez.myamazingplaces1.utils.SQLite
/** Fragmento para representar la pantalla principal de la aplicacion */
class FragmentMenuPrincipal : Fragment(R.layout.fragment_menu_principal) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LugarAdapter
    private var lugarList = mutableListOf<Lugar>()
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // Indicar que este fragmento tiene un menú
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_menu_principal, container, false)

        // Configurar RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = LugarAdapter(
            lugarList,
            requireContext(),
            isEditMode = false,
            showDetailAndMapButtons = true, // Mostrar botones
            onEditClicked = { lugar -> navigateToEditFragment(lugar.id) },
            onDetailClicked = { lugar -> navigateToDetailFragment(lugar.id) },
            onMapClicked = { lugar -> navigateToMapActivity(lugar) },
            onItemClicked = {}
        )
        recyclerView.adapter = adapter

        return view
    }
    //Navega al fragmento de edicion del lugar
    private fun navigateToEditFragment(lugarId: String) {
        val fragmentEditarLugar = FragmentEditarLugar().apply {
            arguments = Bundle().apply { putString("lugarId", lugarId) }
        }
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragmentEditarLugar)
            .addToBackStack(null)
            .commit()
    }

    //Navega al fragmento de detalles de un lugar
    private fun navigateToDetailFragment(lugarId: String) {
        val fragmentDetalles = FragmentDetallesLugar().apply {
            arguments = Bundle().apply {
                putString("lugarId", lugarId)
            }
        }
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragmentDetalles)
            .addToBackStack(null)
            .commit()
    }

    //Abre el mapa para mostrar la ubicacion del lugar
    private fun navigateToMapActivity(lugar: Lugar) {
        val intent = Intent(context, MapsActivity::class.java)
        intent.putExtra("latitud", lugar.latitud)
        intent.putExtra("longitud", lugar.longitud)
        intent.putExtra("nombre", lugar.nombre)
        requireContext().startActivity(intent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isConnectedToInternet()) {
            // Cargar los datos desde Firebase Firestore
            loadLugaresFromFirestore()
        } else {
            // Cargar los datos desde SQLite en modo offline
            loadLugares()
        }
    }

    //Infla el menú de opciones en el fragmento y evita duplicados eliminando cualquier menú anterior
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear() // Evitar duplicaciones
        inflater.inflate(R.menu.menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    //Controla las opciones seleccionadas del menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (lugarList.isEmpty()) {
            Toast.makeText(context, "Esperando datos... Intenta de nuevo.", Toast.LENGTH_SHORT).show()
            return true
        }

        when (item.itemId) {
            R.id.menu_order_alphabetically -> {
                lugarList = lugarList.sortedBy { it.nombre.lowercase() }.toMutableList()
            }
            R.id.menu_order_by_rating -> {
                lugarList = lugarList.sortedByDescending { it.valoracion }.toMutableList()
            }
            else -> return super.onOptionsItemSelected(item)
        }

        adapter.updateLugares(lugarList) // Actualizar adaptador con la nueva lista ordenada
        adapter.notifyDataSetChanged() // Forzar actualización UI

        Snackbar.make(requireView(), "Lista ordenada correctamente", Snackbar.LENGTH_LONG).show()
        return true
    }

    //Carga los lugares desde SQLite si no hay conexión a internet.
    private fun loadLugares() {
        val db = SQLite(requireContext())
        val lugares = db.getAllLugares()
        adapter.updateLugares(lugares)
    }

    //Carga los lugares desde Firebase Firestore y los almacena en la lista.
    fun loadLugaresFromFirestore() {
        db.collection("lugares")
            .get()
            .addOnSuccessListener { result ->
                val nuevaLista = mutableListOf<Lugar>()
                for (document in result) {
                    val lugar = document.toObject(Lugar::class.java)
                    lugar.id = document.id
                    nuevaLista.add(lugar)
                }

                if (nuevaLista.isNotEmpty()) {
                    lugarList.clear()
                    lugarList.addAll(nuevaLista)
                    adapter.updateLugares(lugarList)
                    requireActivity().invalidateOptionsMenu() // Refresca el menú después de cargar los datos
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error al cargar lugares: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    //Metodo para cargar los lugares si esta conectado a internet
    private fun isConnectedToInternet(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    //Metodo para ocultar la opcion "Editar lugares"
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val menuPrincipalItem = menu.findItem(R.id.menu_main_menu)
        menuPrincipalItem?.isVisible = false // Oculta "Pantalla principal" solo en este fragmento
    }

}
