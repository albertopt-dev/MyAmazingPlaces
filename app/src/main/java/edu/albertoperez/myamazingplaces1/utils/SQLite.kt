package edu.albertoperez.myamazingplaces1.utils

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import edu.albertoperez.myamazingplaces1.model.Lugar
/** Clase para gestionar la base de datos SQLite, cuando no tenga conexion a internet */
class SQLite(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "lugares.db" //Nombre de la BBDD
        private const val DATABASE_VERSION = 1         //Version de la BBDD
        //Nombre de la tabla y las columnas
        private const val TABLE_LUGARES = "lugares"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NOMBRE = "nombre"
        private const val COLUMN_TIPO = "tipo"
        private const val COLUMN_TELEFONO = "telefono"
        private const val COLUMN_DIRECCION = "direccion"
        private const val COLUMN_PAGINA_WEB = "pagina_web"
        private const val COLUMN_VALORACION = "valoracion"
        private const val COLUMN_IMAGEN = "imagen"
        private const val COLUMN_LATITUD = "latitud"
        private const val COLUMN_LONGITUD = "longitud"
    }

    //Metodo par crear la base de datos
    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_LUGARES (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_NOMBRE TEXT,
                $COLUMN_TIPO TEXT,
                $COLUMN_TELEFONO TEXT,
                $COLUMN_DIRECCION TEXT,
                $COLUMN_PAGINA_WEB TEXT,
                $COLUMN_VALORACION INTEGER,
                $COLUMN_IMAGEN TEXT,
                $COLUMN_LATITUD REAL,
                $COLUMN_LONGITUD REAL
            )
        """
        db.execSQL(createTableQuery)
    }
    //Metodo para actualizar la BBDD
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LUGARES")
        onCreate(db)
    }

    // Insertar un lugar
    fun insertLugar(lugar: Lugar): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, lugar.id)
            put(COLUMN_NOMBRE, lugar.nombre)
            put(COLUMN_TIPO, lugar.tipo)
            put(COLUMN_TELEFONO, lugar.telefono)
            put(COLUMN_DIRECCION, lugar.direccion)
            put(COLUMN_PAGINA_WEB, lugar.paginaWeb)
            put(COLUMN_VALORACION, lugar.valoracion)
            put(COLUMN_IMAGEN, lugar.imagen)
            put(COLUMN_LATITUD, lugar.latitud)
            put(COLUMN_LONGITUD, lugar.longitud)
        }
        return db.insert(TABLE_LUGARES, null, values)
    }

    // Leer todos los lugares
    fun getAllLugares(): List<Lugar> {
        val lugares = mutableListOf<Lugar>()
        val db = readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM $TABLE_LUGARES", null)

        if (cursor.moveToFirst()) {
            do {
                val lugar = Lugar(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRE)),
                    tipo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIPO)),
                    telefono = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TELEFONO)),
                    direccion = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DIRECCION)),
                    paginaWeb = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAGINA_WEB)),
                    valoracion = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_VALORACION)),
                    imagen = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGEN)),
                    latitud = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUD)),
                    longitud = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUD))
                )
                lugares.add(lugar)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lugares
    }

    // Actualizar un lugar
    fun updateLugar(lugar: Lugar): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NOMBRE, lugar.nombre)
            put(COLUMN_TIPO, lugar.tipo)
            put(COLUMN_TELEFONO, lugar.telefono)
            put(COLUMN_DIRECCION, lugar.direccion)
            put(COLUMN_PAGINA_WEB, lugar.paginaWeb)
            put(COLUMN_VALORACION, lugar.valoracion)
            put(COLUMN_IMAGEN, lugar.imagen)
            put(COLUMN_LATITUD, lugar.latitud)
            put(COLUMN_LONGITUD, lugar.longitud)
        }

        // Actualizar en SQLite
        val rowsUpdated = db.update(TABLE_LUGARES, values, "$COLUMN_ID = ?", arrayOf(lugar.id))

        //Actualizar en Firestore
        Firebase.firestore.collection("lugares")
            .document(lugar.id)
            .set(lugar)
            .addOnSuccessListener {
                Log.d("SQLite", "Lugar actualizado en Firestore")
            }
            .addOnFailureListener { e ->
                Log.e("SQLite", "Error al actualizar en Firestore: ${e.message}")
            }

        return rowsUpdated // Devuelve cuántas filas fueron actualizadas en SQLite
    }


    // Eliminar un lugar
    fun deleteLugar(id: String): Int {
        val db = writableDatabase

        // Eliminar de SQLite
        val rowsDeleted = db.delete(TABLE_LUGARES, "$COLUMN_ID = ?", arrayOf(id))

        // **Eliminar de Firestore**
        Firebase.firestore.collection("lugares")
            .document(id)
            .delete()
            .addOnSuccessListener {
                Log.d("SQLite", "Lugar eliminado en Firestore")
            }
            .addOnFailureListener { e ->
                Log.e("SQLite", "Error al eliminar en Firestore: ${e.message}")
            }

        return rowsDeleted // Devuelve cuántas filas fueron eliminadas en SQLite
    }

    //Metodo para obtener el lugar por el ID
    fun getLugarById(id: String): Lugar? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_LUGARES WHERE $COLUMN_ID = ?", arrayOf(id))

        return if (cursor.moveToFirst()) {
            val lugar = Lugar(
                id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRE)),
                tipo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIPO)),
                telefono = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TELEFONO)),
                direccion = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DIRECCION)),
                paginaWeb = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAGINA_WEB)),
                valoracion = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_VALORACION)),
                imagen = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGEN)),
                latitud = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUD)),
                longitud = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUD))
            )
            cursor.close()
            lugar
        } else {
            cursor.close()
            null
        }
    }

}
