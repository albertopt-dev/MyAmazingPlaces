package edu.albertoperez.myamazingplaces1.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import edu.albertoperez.myamazingplaces1.model.Lugar
import edu.albertoperez.myamazingplaces1.LugarAdapter
import edu.albertoperez.myamazingplaces1.R
import edu.albertoperez.myamazingplaces1.utils.SQLite

/** Fragmento que muestra la lista de lugares */
class FragmentListaLugares : Fragment(R.layout.fragment_lista_lugares) {
    //Variables
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LugarAdapter
    private var lugarList = mutableListOf<Lugar>()
    private val db = Firebase.firestore
    private lateinit var sqliteDb: SQLite
    //Botones
    private lateinit var btnEditarLugar: Button
    private lateinit var btnBorrarTodo: Button
    private lateinit var btnBorrarSeleccionados: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // Indicar que este fragmento tiene un menú
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_lista_lugares, container, false)

        // Iniciar SQLite
        sqliteDb = SQLite(requireContext())

        // Configurar RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = LugarAdapter(
            lugarList,
            requireContext(),
            isEditMode = true, // Modo edición
            showDetailAndMapButtons = false, // Ocultar botones
            onEditClicked = { lugar -> navigateToEditFragment(lugar.id) },
            onDetailClicked = {}, // No hacer nada
            onMapClicked = {}, // No hacer nada
            onItemClicked = {} // No hacer nada
        )


        recyclerView.adapter = adapter

        // Configurar botones
        btnBorrarTodo = view.findViewById(R.id.btnBorrarTodo)
        btnBorrarSeleccionados = view.findViewById(R.id.btnBorrarSeleccionados)
        btnEditarLugar = view.findViewById(R.id.btnEditar)
        btnBorrarTodo.setOnClickListener { deleteAllLugares() }
        btnBorrarSeleccionados.setOnClickListener { deleteSelectedLugares() }
        btnEditarLugar.setOnClickListener { editSelectedLugar() }

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadLugaresFromFirestore()
    }

    //Infla el menú de opciones en el fragmento y evita duplicados eliminando cualquier menú anterior
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear() // Evitar duplicados
        inflater.inflate(R.menu.menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    //Controla la seleccion de opciones del menu
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
        adapter.notifyDataSetChanged()

        Snackbar.make(requireView(), "Lista ordenada correctamente", Snackbar.LENGTH_LONG).show()
        return true
    }

    // Metodo para cargar lugares desde Firestore y SQLite
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
                    sqliteDb.insertLugar(nuevaLista.first()) // Sincronizar con SQLite
                    requireActivity().invalidateOptionsMenu() // Refresca el menú después de cargar los datos
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error al cargar lugares: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Metodo para editar el lugar seleccionado
    private fun editSelectedLugar() {
        val selectedIds = adapter.getSelectedItems()

        if (selectedIds.size == 1) {
            // Navegar al FragmentEditarLugar con el ID seleccionado
            val lugarId = selectedIds.first()
            navigateToEditFragment(lugarId)
        } else if (selectedIds.isEmpty()) {
            Toast.makeText(context, "Por favor, selecciona un lugar para editar.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Por favor, selecciona solo un lugar para editar.", Toast.LENGTH_SHORT).show()
        }
    }

    // Metodo para eliminar todos los lugares de SQLite y Firestore
    private fun deleteAllLugares() {
        val sqliteDb = SQLite(requireContext())

        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar todos los lugares")
            .setMessage("¿Estás seguro de que deseas eliminar todos los lugares?")
            .setPositiveButton("Sí") { _, _ ->
                db.collection("lugares").get()
                    .addOnSuccessListener { result ->
                        for (document in result) {
                            db.collection("lugares").document(document.id).delete()
                        }
                        sqliteDb.writableDatabase.execSQL("DELETE FROM lugares") // Borra todos los datos en SQLite
                        lugarList.clear()
                        adapter.notifyDataSetChanged()
                        Toast.makeText(context, "Todos los lugares han sido eliminados.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error al eliminar lugares: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Metodo para eliminar solo los lugares seleccionados de SQLite y Firestore
    private fun deleteSelectedLugares() {
        val sqliteDb = SQLite(requireContext())
        val selectedIds = adapter.getSelectedItems()

        if (selectedIds.isEmpty()) {
            Toast.makeText(context, "No hay lugares seleccionados.", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar lugares seleccionados")
            .setMessage("¿Estás seguro de que deseas eliminar los lugares seleccionados?")
            .setPositiveButton("Sí") { _, _ ->
                selectedIds.forEach { id ->
                    db.collection("lugares").document(id).delete()
                    sqliteDb.deleteLugar(id)
                }
                lugarList.removeAll { it.id in selectedIds }
                adapter.notifyDataSetChanged()
                Toast.makeText(context, "Lugares seleccionados eliminados.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    //Metodo para ocultar la opcion "Editar lugares"
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val menuEditPlacesItem = menu.findItem(R.id.menu_edit_places)
        menuEditPlacesItem?.isVisible = false // Oculta "Editar lugares" en este fragmento
    }

    //Metodo para poder navegar al fragmento para editar el lugar
    private fun navigateToEditFragment(lugarId: String) {
        val fragmentEditarLugar = FragmentEditarLugar().apply {
            arguments = Bundle().apply {
                putString("lugarId", lugarId)
            }
        }
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragmentEditarLugar)
            .addToBackStack(null)
            .commit()
    }
}
