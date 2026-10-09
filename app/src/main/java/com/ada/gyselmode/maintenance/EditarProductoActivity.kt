package com.ada.gyselmode.maintenance

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.ada.gyselmode.R
import com.ada.gyselmode.helper.NavigationHelper
import com.ada.gyselmode.model.Categoria
import com.ada.gyselmode.model.Imagen
import com.ada.gyselmode.model.ImagenUpdateRequest
import com.ada.gyselmode.model.Producto
import com.ada.gyselmode.model.ProductoTallaRequest
import com.ada.gyselmode.model.ProductoUpdateRequest
import com.ada.gyselmode.model.Talla
import com.ada.gyselmode.network.CloudinaryService
import com.ada.gyselmode.network.RetrofitClient

import kotlinx.coroutines.launch

class EditarProductoActivity : AppCompatActivity() {

    private lateinit var producto: Producto

    private lateinit var edtTitulo: EditText
    private lateinit var edtDescripcion: EditText
    private lateinit var edtAdicional: EditText
    private lateinit var edtPrecioUnidad: EditText
    private lateinit var edtPrecioTotal: EditText

    private lateinit var spinnerCategoria: Spinner
    private lateinit var contenedorTallas: LinearLayout

    private lateinit var recyclerImagenes: RecyclerView
    private lateinit var btnAgregarImagenes: Button
    private lateinit var btnGuardarCambios: Button // CORREGIDO: Se agregó el tipo de dato : Button

    private lateinit var txtImagenPrincipal: TextView

    private var categorias: List<Categoria> = emptyList()
    private var tallas: List<Talla> = emptyList()

    // Imágenes que ya existen en el producto
    private val imagenesEditables = mutableListOf<Imagen>()

    // Nuevas imágenes seleccionadas desde el teléfono
    private val nuevasImagenes = mutableListOf<Uri>()

    /*
     * Selector de nuevas imágenes.
     *
     * El máximo total del producto será de 3 imágenes.
     */
    private val selectorNuevasImagenes =
        registerForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(3)
        ) { uris ->

            if (uris.isEmpty()) {
                return@registerForActivityResult
            }

            val totalActual =
                imagenesEditables.size + nuevasImagenes.size

            val disponibles = 3 - totalActual

            if (disponibles <= 0) {

                Toast.makeText(
                    this,
                    getString(R.string.form_max_images),
                    Toast.LENGTH_SHORT
                ).show()

                return@registerForActivityResult
            }

            val nuevasSeleccionadas =
                uris.take(disponibles)

            nuevasImagenes.addAll(
                nuevasSeleccionadas
            )

            Toast.makeText(
                this,
                getString(R.string.form_new_images_selected, nuevasSeleccionadas.size),
                Toast.LENGTH_SHORT
            ).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_edit_producto)

        NavigationHelper.setupBottomNavigation(this)

        @Suppress("DEPRECATION")
        val recibido =
            intent.getSerializableExtra("producto") as? Producto

        if (recibido == null) {

            Toast.makeText(
                this,
                getString(R.string.form_product_not_found),
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        producto = recibido

        inicializarVistas()
        cargarDatosProducto()
        cargarCategorias()
        cargarTallas()
    }

    private fun inicializarVistas() {

        edtTitulo =
            findViewById(R.id.edtTituloEditar)

        edtDescripcion =
            findViewById(R.id.edtDescripcionEditar)

        edtAdicional =
            findViewById(R.id.edtAdicionalEditar)

        edtPrecioUnidad =
            findViewById(R.id.edtPrecioUnidadEditar)

        edtPrecioTotal =
            findViewById(R.id.edtPrecioTotalEditar)

        spinnerCategoria =
            findViewById(R.id.spinnerCategoriaEditar)

        contenedorTallas =
            findViewById(R.id.contenedorTallasEditar)

        recyclerImagenes =
            findViewById(R.id.recyclerImagenesEditar)

        btnAgregarImagenes =
            findViewById(R.id.btnAgregarImagenes)

        btnGuardarCambios =
            findViewById(R.id.btnGuardarCambios)

        txtImagenPrincipal =
            findViewById(R.id.txtImagenPrincipalEditar)

        recyclerImagenes.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        /*
         * Guardar cambios
         */
        btnGuardarCambios.setOnClickListener {
            guardarCambios()
        }

        /*
         * Agregar nuevas imágenes
         */
        btnAgregarImagenes.setOnClickListener {

            val totalActual =
                imagenesEditables.size + nuevasImagenes.size

            if (totalActual >= 3) {

                Toast.makeText(
                    this,
                    getString(R.string.form_max_images),
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            selectorNuevasImagenes.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }
    }

    private fun cargarDatosProducto() {

        edtTitulo.setText(producto.titulo)

        edtDescripcion.setText(producto.descripcion)

        edtAdicional.setText(producto.adicional)

        edtPrecioUnidad.setText(
            producto.precioUnidad.toString()
        )

        edtPrecioTotal.setText(
            producto.precioTotal.toString()
        )

        /*
         * Copiamos las imágenes existentes
         * para poder modificarlas.
         */
        imagenesEditables.clear()

        imagenesEditables.addAll(
            producto.imagenes
        )

        configurarImagenes()
    }

    private fun configurarImagenes() {

        actualizarTextoPrincipal()

        if (imagenesEditables.isEmpty()) {

            recyclerImagenes.adapter = null

            Toast.makeText(
                this,
                getString(R.string.form_product_no_images),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        recyclerImagenes.adapter =
            ImagenEditAdapter(
                imagenesEditables,

                onPrincipalSeleccionada = { posicion ->

                    seleccionarImagenPrincipal(
                        posicion
                    )
                },

                onEliminar = { posicion ->

                    eliminarImagen(
                        posicion
                    )
                }
            )
    }

    private fun seleccionarImagenPrincipal(
        posicion: Int
    ) {

        if (posicion !in imagenesEditables.indices) {
            return
        }

        /*
         * Solo una imagen puede ser principal.
         */
        for (i in imagenesEditables.indices) {

            imagenesEditables[i] =
                imagenesEditables[i].copy(
                    esPrincipal = i == posicion
                )
        }

        actualizarTextoPrincipal()

        recyclerImagenes.adapter?.notifyDataSetChanged()
    }

    private fun eliminarImagen(
        posicion: Int
    ) {

        if (posicion !in imagenesEditables.indices) {
            return
        }

        val eraPrincipal =
            imagenesEditables[posicion].esPrincipal

        imagenesEditables.removeAt(posicion)

        /*
         * Si eliminamos la principal,
         * elegimos automáticamente la primera
         * imagen restante.
         */
        if (
            eraPrincipal &&
            imagenesEditables.isNotEmpty()
        ) {

            for (i in imagenesEditables.indices) {

                imagenesEditables[i] =
                    imagenesEditables[i].copy(
                        esPrincipal = i == 0
                    )
            }
        }

        configurarImagenes()
    }

    private fun actualizarTextoPrincipal() {

        val principal =
            imagenesEditables.firstOrNull {
                it.esPrincipal
            }

        if (principal != null) {

            txtImagenPrincipal.text =
                getString(R.string.form_main_image_selected)

        } else {

            txtImagenPrincipal.text =
                getString(R.string.form_no_main_image)
        }
    }

    private fun cargarCategorias() {

        lifecycleScope.launch {

            try {

                categorias =
                    RetrofitClient.apiService.listarCategorias()

                val nombres =
                    categorias.map {
                        it.nombre
                    }

                val adapter =
                    ArrayAdapter(
                        this@EditarProductoActivity,
                        android.R.layout.simple_spinner_item,
                        nombres
                    )

                adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )

                spinnerCategoria.adapter =
                    adapter

                val posicion =
                    categorias.indexOfFirst {
                        it.id == producto.categoriaId
                    }

                if (posicion >= 0) {

                    spinnerCategoria.setSelection(
                        posicion
                    )
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@EditarProductoActivity,
                    getString(R.string.categories_load_error, e.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun cargarTallas() {

        lifecycleScope.launch {

            try {

                tallas =
                    RetrofitClient.apiService.listarTallas()

                mostrarTallas()

            } catch (e: Exception) {

                Toast.makeText(
                    this@EditarProductoActivity,
                    getString(R.string.sizes_load_error, e.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun mostrarTallas() {

        contenedorTallas.removeAllViews()

        tallas.forEach { talla ->

            val checkBox = CheckBox(this)

            checkBox.text =
                talla.nombre

            checkBox.textSize =
                16f

            checkBox.setTextColor(
                android.graphics.Color.WHITE
            )

            val seleccionada =
                producto.tallas.any {
                    it.id == talla.id
                }

            checkBox.isChecked =
                seleccionada

            checkBox.tag =
                talla.id

            contenedorTallas.addView(
                checkBox
            )
        }
    }

    private fun guardarCambios() {

        val titulo =
            edtTitulo.text.toString().trim()

        val descripcion =
            edtDescripcion.text.toString().trim()

        val adicional =
            edtAdicional.text.toString().trim()

        val precioUnidad =
            edtPrecioUnidad.text.toString()
                .trim()
                .toDoubleOrNull()

        val precioTotal =
            edtPrecioTotal.text.toString()
                .trim()
                .toDoubleOrNull()

        /*
         * Validaciones
         */

        if (titulo.isEmpty()) {

            edtTitulo.error =
                getString(R.string.form_name_required_edit)

            edtTitulo.requestFocus()

            return
        }

        if (precioUnidad == null) {

            edtPrecioUnidad.error =
                getString(R.string.form_price_enter_valid)

            edtPrecioUnidad.requestFocus()

            return
        }

        if (precioTotal == null) {

            edtPrecioTotal.error =
                getString(R.string.form_price_enter_valid)

            edtPrecioTotal.requestFocus()

            return
        }

        /*
         * Categoría
         */

        if (categorias.isEmpty()) {

            Toast.makeText(
                this,
                getString(R.string.form_categories_not_loaded_edit),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val categoriaSeleccionada =
            categorias[
                spinnerCategoria.selectedItemPosition
            ]

        /*
         * Tallas
         */

        val tallasSeleccionadas =
            mutableListOf<ProductoTallaRequest>()

        for (
        i in 0 until contenedorTallas.childCount
        ) {

            val vista =
                contenedorTallas.getChildAt(i)

            if (
                vista is CheckBox &&
                vista.isChecked
            ) {

                val tallaId =
                    vista.tag as? Int

                if (tallaId != null) {

                    tallasSeleccionadas.add(
                        ProductoTallaRequest(
                            tallaId = tallaId,
                            stock = 0
                        )
                    )
                }
            }
        }

        /*
         * Verificar imagen principal.
         *
         * Si tenemos imágenes existentes,
         * debe existir una principal.
         */
        if (imagenesEditables.isNotEmpty()) {

            val tienePrincipal =
                imagenesEditables.any {
                    it.esPrincipal
                }

            if (!tienePrincipal) {

                Toast.makeText(
                    this,
                    getString(R.string.form_select_main_image),
                    Toast.LENGTH_SHORT
                ).show()

                return
            }
        }

        /*
         * Evitar múltiples guardados.
         */
        btnGuardarCambios.isEnabled = false

        lifecycleScope.launch {

            try {

                /*
                 * =========================================
                 * 1. IMÁGENES EXISTENTES
                 * =========================================
                 */

                val imagenesRequest =
                    imagenesEditables.map {

                        ImagenUpdateRequest(
                            id = it.id,
                            url = it.url,
                            publicId = it.publicId,
                            esPrincipal = it.esPrincipal
                        )

                    }.toMutableList()

                /*
                 * =========================================
                 * 2. SUBIR NUEVAS IMÁGENES
                 * =========================================
                 */

                if (nuevasImagenes.isNotEmpty()) {

                    btnGuardarCambios.text =
                        getString(R.string.form_uploading_images)

                    for (uri in nuevasImagenes) {

                        val resultado =
                            CloudinaryService.subirImagen(
                                contentResolver,
                                uri
                            )

                        imagenesRequest.add(
                            ImagenUpdateRequest(
                                id = null,
                                url = resultado.first,
                                publicId = resultado.second,
                                esPrincipal = false
                            )
                        )
                    }
                }

                /*
                 * =========================================
                 * 3. VERIFICAR IMAGEN PRINCIPAL
                 * =========================================
                 */

                if (
                    imagenesRequest.isNotEmpty() &&
                    imagenesRequest.none {
                        it.esPrincipal
                    }
                ) {

                    imagenesRequest[0] =
                        imagenesRequest[0].copy(
                            esPrincipal = true
                        )
                }

                /*
                 * =========================================
                 * 4. CREAR REQUEST
                 * =========================================
                 */

                btnGuardarCambios.text =
                    getString(R.string.form_saving_changes)

                val request =
                    ProductoUpdateRequest(
                        titulo = titulo,
                        descripcion = descripcion,
                        adicional = adicional,
                        precioUnidad = precioUnidad,
                        precioTotal = precioTotal,
                        categoriaId = categoriaSeleccionada.id,
                        tallas = tallasSeleccionadas,
                        imagenes = imagenesRequest
                    )

                /*
                 * =========================================
                 * 5. PATCH
                 * =========================================
                 */

                RetrofitClient.apiService.actualizarProducto(
                    producto.id.toLong(),
                    request
                )

                Toast.makeText(
                    this@EditarProductoActivity,
                    getString(R.string.form_product_updated),
                    Toast.LENGTH_LONG
                ).show()

                /*
                 * Regresamos a la lista.
                 */
                finish()

            } catch (e: Exception) {

                e.printStackTrace()

                btnGuardarCambios.isEnabled =
                    true

                btnGuardarCambios.text =
                    getString(R.string.form_save_changes)

                Toast.makeText(
                    this@EditarProductoActivity,
                    getString(R.string.form_update_error, e.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}