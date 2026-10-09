
package com.ada.gyselmode.maintenance

import android.graphics.Color
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
import com.ada.gyselmode.adapter.ImagenPreviewAdapter
import com.ada.gyselmode.helper.NavigationHelper
import com.ada.gyselmode.model.Categoria
import com.ada.gyselmode.model.Talla
import com.ada.gyselmode.model.ProductoCreateRequest
import com.ada.gyselmode.model.ProductoTallaRequest
import com.ada.gyselmode.model.ImagenCreateRequest
import com.ada.gyselmode.network.RetrofitClient

import kotlinx.coroutines.launch

import com.ada.gyselmode.network.CloudinaryService

class CrearProductoActivity : AppCompatActivity() {



    // =========================================
    // CAMPOS DEL FORMULARIO
    // =========================================

    private lateinit var edtTitulo: EditText
    private lateinit var edtDescripcion: EditText
    private lateinit var edtAdicional: EditText
    private lateinit var edtPrecioUnidad: EditText
    private lateinit var edtPrecioTotal: EditText

    // =========================================
    // CATEGORÍAS Y TALLAS
    // =========================================

    private lateinit var spinnerCategoria: Spinner
    private lateinit var contenedorTallas: LinearLayout

    private var listaCategorias = listOf<Categoria>()
    private var listaTallas = listOf<Talla>()

    private val tallasSeleccionadas = mutableSetOf<Int>()

    // =========================================
    // IMÁGENES
    // =========================================

    private lateinit var recyclerImagenes: RecyclerView
    private lateinit var imagenAdapter: ImagenPreviewAdapter
    private lateinit var txtImagenPrincipal: TextView

    private val imagenesSeleccionadas = mutableListOf<Uri>()

    private var posicionImagenPrincipal = 0

    // =========================================
    // BOTONES
    // =========================================

    private lateinit var btnSeleccionarImagenes: Button
    private lateinit var btnGuardarProducto: Button

    // =========================================
    // SELECTOR DE IMÁGENES
    // =========================================

    private val selectorImagenes =
        registerForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(3)
        ) { uris ->

            if (uris.isNotEmpty()) {

                imagenesSeleccionadas.clear()
                imagenesSeleccionadas.addAll(uris.take(3))

                posicionImagenPrincipal = 0

                imagenAdapter.posicionPrincipal = 0
                imagenAdapter.notifyDataSetChanged()

                actualizarTextoImagenPrincipal()
            }
        }

    // =========================================
    // ON CREATE
    // =========================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_new_product)

        inicializarVistas()

        configurarCarrusel()

        NavigationHelper.setupBottomNavigation(this)

        configurarEventos()

        cargarCategorias()
        cargarTallas()
    }

    // =========================================
    // INICIALIZAR VISTAS
    // =========================================

    private fun inicializarVistas() {

        edtTitulo = findViewById(R.id.edtTitulo)
        edtDescripcion = findViewById(R.id.edtDescripcion)
        edtAdicional = findViewById(R.id.edtAdicional)

        edtPrecioUnidad = findViewById(R.id.edtPrecioUnidad)
        edtPrecioTotal = findViewById(R.id.edtPrecioTotal)

        spinnerCategoria = findViewById(R.id.spinnerCategoria)
        contenedorTallas = findViewById(R.id.contenedorTallas)

        btnSeleccionarImagenes =
            findViewById(R.id.btnSeleccionarImagenes)

        btnGuardarProducto =
            findViewById(R.id.btnGuardarProducto)

        txtImagenPrincipal =
            findViewById(R.id.txtImagenPrincipal)

        recyclerImagenes =
            findViewById(R.id.recyclerImagenes)
    }

    // =========================================
    // CONFIGURAR CARRUSEL
    // =========================================

    private fun configurarCarrusel() {

        recyclerImagenes.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        imagenAdapter = ImagenPreviewAdapter(
            imagenesSeleccionadas
        ) { posicion ->

            posicionImagenPrincipal = posicion

            actualizarTextoImagenPrincipal()
        }

        recyclerImagenes.adapter = imagenAdapter
    }

    private fun actualizarTextoImagenPrincipal() {

        txtImagenPrincipal.text =
            if (imagenesSeleccionadas.isEmpty()) {
                getString(R.string.form_select_main_image)
            } else {
                getString(R.string.form_main_image_position, posicionImagenPrincipal + 1, imagenesSeleccionadas.size)
            }
    }

    // =========================================
    // EVENTOS
    // =========================================

    private fun configurarEventos() {

        btnSeleccionarImagenes.setOnClickListener {

            selectorImagenes.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }

        btnGuardarProducto.setOnClickListener {

            validarFormulario()
        }
    }

    // =========================================
    // CARGAR CATEGORÍAS
    // =========================================

    private fun cargarCategorias() {

        lifecycleScope.launch {

            try {

                listaCategorias =
                    RetrofitClient.apiService.listarCategorias()

                if (listaCategorias.isEmpty()) {

                    mostrarMensaje(getString(R.string.categories_empty))
                    return@launch
                }

                val nombresCategorias =
                    listaCategorias.map { it.nombre }

                val adapter = ArrayAdapter(
                    this@CrearProductoActivity,
                    android.R.layout.simple_spinner_item,
                    nombresCategorias
                )

                adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )

                spinnerCategoria.adapter = adapter

            } catch (e: Exception) {

                mostrarMensaje(
                    getString(R.string.categories_load_error, e.message)
                )
            }
        }
    }

    // =========================================
    // CARGAR TALLAS
    // =========================================

    private fun cargarTallas() {

        lifecycleScope.launch {

            try {

                listaTallas =
                    RetrofitClient.apiService.listarTallas()

                mostrarTallas()

            } catch (e: Exception) {

                mostrarMensaje(
                    getString(R.string.sizes_load_error, e.message)
                )
            }
        }
    }

    // =========================================
    // MOSTRAR TALLAS
    // =========================================

    private fun mostrarTallas() {

        contenedorTallas.removeAllViews()
        tallasSeleccionadas.clear()

        for (talla in listaTallas) {

            val checkBox = CheckBox(this)

            checkBox.text = talla.nombre
            checkBox.textSize = 14f
            checkBox.setTextColor(Color.WHITE)

            checkBox.buttonTintList =
                android.content.res.ColorStateList.valueOf(
                    Color.parseColor("#E01C1C")
                )

            checkBox.layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

            checkBox.setOnCheckedChangeListener { _, checked ->

                if (checked) {
                    tallasSeleccionadas.add(talla.id)
                } else {
                    tallasSeleccionadas.remove(talla.id)
                }
            }

            contenedorTallas.addView(checkBox)
        }
    }

    // =========================================
    // VALIDAR FORMULARIO
    // =========================================

    private fun validarFormulario() {

        val titulo = edtTitulo.text.toString().trim()
        val descripcion = edtDescripcion.text.toString().trim()
        val adicional = edtAdicional.text.toString().trim()

        val precioUnidad =
            edtPrecioUnidad.text.toString().trim().toDoubleOrNull()

        val precioTotal =
            edtPrecioTotal.text.toString().trim().toDoubleOrNull()

        if (titulo.isEmpty()) {
            edtTitulo.error = getString(R.string.form_name_required_new)
            return
        }

        if (descripcion.isEmpty()) {
            edtDescripcion.error = getString(R.string.form_description_required)
            return
        }

        if (precioUnidad == null || precioUnidad < 0) {
            edtPrecioUnidad.error = getString(R.string.form_price_invalid)
            return
        }

        if (precioTotal == null || precioTotal < 0) {
            edtPrecioTotal.error = getString(R.string.form_price_invalid)
            return
        }

        if (listaCategorias.isEmpty()) {
            mostrarMensaje(getString(R.string.form_categories_not_loaded_new))
            return
        }

        if (spinnerCategoria.selectedItemPosition !in listaCategorias.indices) {
            mostrarMensaje(getString(R.string.form_select_category))
            return
        }

        if (tallasSeleccionadas.isEmpty()) {
            mostrarMensaje(getString(R.string.form_select_size))
            return
        }

        if (imagenesSeleccionadas.isEmpty()) {
            mostrarMensaje(getString(R.string.form_select_image))
            return
        }

        val categoriaSeleccionada =
            listaCategorias[spinnerCategoria.selectedItemPosition]

        val tallasRequest =
            tallasSeleccionadas.map { tallaId ->
                ProductoTallaRequest(
                    tallaId = tallaId,
                    stock = 0
                )
            }

        val imagenesLocales =
            imagenesSeleccionadas.toList()

        btnGuardarProducto.isEnabled = false
        btnGuardarProducto.text = getString(R.string.form_uploading_images)

        lifecycleScope.launch {

            try {

                // Subir las imágenes a Cloudinary

                val imagenesCloudinary = mutableListOf<ImagenCreateRequest>()

                for ((index, uri) in imagenesLocales.withIndex()) {

                    val resultado = CloudinaryService.subirImagen(
                        contentResolver,
                        uri
                    )

                    imagenesCloudinary.add(
                        ImagenCreateRequest(
                            url = resultado.first,
                            publicId = resultado.second,
                            esPrincipal = index == posicionImagenPrincipal
                        )
                    )
                }

                btnGuardarProducto.text = getString(R.string.form_saving_product)

                // Construir el JSON que espera Spring Boot

                val productoRequest = ProductoCreateRequest(
                    titulo = titulo,
                    descripcion = descripcion,
                    adicional = adicional,
                    precioUnidad = precioUnidad,
                    precioTotal = precioTotal,
                    categoriaId = categoriaSeleccionada.id,
                    tallas = tallasRequest,
                    imagenes = imagenesCloudinary
                )

                // Enviar a la API REST

                val productoCreado =
                    RetrofitClient.apiService.crearProducto(
                        productoRequest
                    )

                mostrarMensaje(
                    getString(R.string.form_product_created, productoCreado.titulo)
                )

                limpiarFormulario()

            } catch (e: Exception) {

                e.printStackTrace()

                mostrarMensaje(
                    getString(R.string.form_save_error, e.message)
                )

            } finally {

                btnGuardarProducto.isEnabled = true
                btnGuardarProducto.text = getString(R.string.form_save_product)
            }
        }
    }




    // =========================================
    // LIMPIAR FORMULARIO
    // =========================================

    private fun limpiarFormulario() {

        edtTitulo.text.clear()
        edtDescripcion.text.clear()
        edtAdicional.text.clear()

        edtPrecioUnidad.text.clear()
        edtPrecioTotal.text.clear()

        tallasSeleccionadas.clear()

        for (i in 0 until contenedorTallas.childCount) {

            val vista = contenedorTallas.getChildAt(i)

            if (vista is CheckBox) {
                vista.isChecked = false
            }
        }

        imagenesSeleccionadas.clear()

        posicionImagenPrincipal = 0

        imagenAdapter.notifyDataSetChanged()

        actualizarTextoImagenPrincipal()

        spinnerCategoria.setSelection(0)
    }

    // =========================================
    // MENSAJES
    // =========================================

    private fun mostrarMensaje(mensaje: String) {

        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_LONG
        ).show()
    }
}