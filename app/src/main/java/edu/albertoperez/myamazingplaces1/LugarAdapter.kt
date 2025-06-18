package edu.albertoperez.myamazingplaces1

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.albertoperez.myamazingplaces1.model.Lugar
/** Adaptador para controlar la lista de lugares del RecyclerView */
class LugarAdapter(
    private var lugares: List<Lugar>, //Lista de lugares
    private val context: Context,     //Contexto de la app
    private var isEditMode: Boolean = false, //Si entramos en modo edicion
    private val showDetailAndMapButtons: Boolean = true, //Mostrar botones detalles y mapas
    private val onMapClicked: (Lugar) -> Unit, //llamada al bton mapas
    private val onEditClicked: (Lugar) -> Unit,
    private val onDetailClicked: (Lugar) -> Unit, //llamada al boton detalles
    private val onItemClicked: (Lugar) -> Unit //llamada para cuando se selcciona un item
) : RecyclerView.Adapter<LugarAdapter.LugarViewHolder>() {

    private val selectedItems = mutableSetOf<String>() // IDs seleccionados cuando estamos en modo edicion

    // ViewHolder que representa un ítem de la lista
    inner class LugarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivLugar: ImageView = view.findViewById(R.id.ivLugar)  //Img del lugar
        val tvNombre: TextView = view.findViewById(R.id.tvNombre) //Nombre del lugar
        val tvTipoLugar: TextView = view.findViewById(R.id.tvTipoLugar) //Tipo de lugar
        val checkBox: CheckBox = view.findViewById(R.id.lugar_checkbox) //Checkbox para seleccion de lugar
        val btnDetalles: Button = view.findViewById(R.id.btnDetalles) //Boton para ver los detalles del lugar
        val btnVerMapa: Button = view.findViewById(R.id.btnVerMapa) //Boton para ver la ubicacion del lugar

        // // Metodo para asignar los datos al ViewHolder
        fun bind(lugar: Lugar) {
            tvNombre.text = lugar.nombre
            tvTipoLugar.text = lugar.tipo

            // Mostrar u ocultar botones según la configuracion
            btnDetalles.visibility = if (showDetailAndMapButtons) View.VISIBLE else View.GONE
            btnVerMapa.visibility = if (showDetailAndMapButtons) View.VISIBLE else View.GONE

            // Configurar el estado del checkbox dependiendo del modo edición
            if (isEditMode) {
                checkBox.visibility = View.VISIBLE
                checkBox.isChecked = selectedItems.contains(lugar.id)
            } else {
                checkBox.visibility = View.GONE
            }
            // Manejar la selección de checkbox
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedItems.add(lugar.id)
                } else {
                    selectedItems.remove(lugar.id)
                }
            }

            // Decodificar imagen en Base64 y asignarla a la ImageView
            if (!lugar.imagen.isNullOrEmpty()) {
                val bitmap = base64ToBitmap(lugar.imagen!!)
                if (bitmap != null) {
                    ivLugar.setImageBitmap(bitmap)
                } else {
                    ivLugar.setImageResource(R.drawable.icon) // Imagen predeterminada si falla la decodificación
                }
            } else {
                ivLugar.setImageResource(R.drawable.icon) // Imagen predeterminada si no hay imagen
            }

            // Configurar clics en el botónes
            btnDetalles.setOnClickListener { onDetailClicked(lugar) }
            btnVerMapa.setOnClickListener { onMapClicked(lugar) }

            // Configurar clic en el ítem
            itemView.setOnClickListener {
                if (isEditMode) {
                    toggleSelection(lugar.id)
                } else {
                    onItemClicked(lugar) // Llamar al callback con el lugar seleccionado
                }
            }
        }
    }

    //Metodo para inflar el layout de cada ítem en el RecyclerView
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LugarViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_lugar, parent, false)
        return LugarViewHolder(view)
    }

    // Vincula los datos de un lugar a un ViewHolder
    override fun onBindViewHolder(holder: LugarViewHolder, position: Int) {
        val lugar = lugares[position]
        holder.tvNombre.text = lugar.nombre
        holder.tvTipoLugar.text = lugar.tipo
        holder.bind(lugar) // Llamada a bind() para completar la configuración del ViewHolder
    }

    // Devuelve el número total de elementos en la lista
    override fun getItemCount(): Int = lugares.size

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

    // Obtener los IDs seleccionados en el modo edicion
    fun getSelectedItems(): List<String> = selectedItems.toList()
    // Alternar la selección de un lugar en eel modo edicion
    private fun toggleSelection(id: String) {
        if (selectedItems.contains(id)) {
            selectedItems.remove(id)
        } else {
            selectedItems.add(id)
        }
        notifyDataSetChanged()
    }

    // Actualizar la lista de lugares y lo notifica al adaptador
    fun updateLugares(newLugares: List<Lugar>) {
        lugares = newLugares
        notifyDataSetChanged()
    }
}
