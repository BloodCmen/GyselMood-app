package com.ada.gyselmode.ui.user.pedidos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ada.gyselmode.ui.user.ProductosActivity
import com.ada.gyselmode.R
import com.ada.gyselmode.data.carrito.CarritoRepository
import com.ada.gyselmode.helper.NavigationHelper
import com.ada.gyselmode.model.carrito.ItemCarrito
import com.ada.gyselmode.ui.user.pedidos.adapter.CarritoAdapter
import kotlinx.coroutines.launch
import java.util.Locale

class CartActivity : AppCompatActivity() {

    private lateinit var contenedorVacio: LinearLayout
    private lateinit var recyclerCarrito: RecyclerView
    private lateinit var panelResumen: LinearLayout

    private lateinit var txtSubtotal: TextView
    private lateinit var txtContador: TextView
    private lateinit var txtCantidadResumen: TextView

    private lateinit var adapter: CarritoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_cart)

        // Navegación existente
        NavigationHelper.setupBottomNavigation(this)
        NavigationHelper.setupBackButton(this)

        inicializarVistas()
        configurarCarrito()
        configurarBotones()
        observarCarrito()
    }

    private fun inicializarVistas() {

        contenedorVacio =
            findViewById(R.id.contenedorVacio)

        recyclerCarrito =
            findViewById(R.id.recyclerCarrito)

        panelResumen =
            findViewById(R.id.panelResumen)

        txtSubtotal =
            findViewById(R.id.txtSubtotalCarrito)

        txtContador =
            findViewById(R.id.txtContadorCarrito)

        txtCantidadResumen =
            findViewById(R.id.txtCantidadResumenCarrito)
    }

    private fun configurarCarrito() {

        adapter = CarritoAdapter(

            onSumar = { item ->

                // Mantenemos tu lógica del repositorio.
                CarritoRepository.cambiarCantidad(
                    item.productoId,
                    item.tallaId,
                    item.cantidad + 1
                )
            },


            onRestar = { item ->

                // La cantidad mínima permitida es 1.
                // Para quitar el producto se utiliza Eliminar.
                if (item.cantidad > 1) {

                    CarritoRepository.cambiarCantidad(
                        item.productoId,
                        item.tallaId,
                        item.cantidad - 1
                    )
                }
            },


            onEliminar = { item ->

                CarritoRepository.eliminar(
                    item.productoId,
                    item.tallaId
                )
            }
        )

        recyclerCarrito.layoutManager =
            LinearLayoutManager(this)

        recyclerCarrito.adapter = adapter

        recyclerCarrito.setHasFixedSize(false)
    }

    private fun configurarBotones() {

        // Estado vacío: explorar el catálogo.
        findViewById<Button>(R.id.btnExplorarProductos)
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        ProductosActivity::class.java
                    )
                )
            }

        // Continuar con el checkout existente.
        findViewById<Button>(R.id.btnContinuarPedido)
            .setOnClickListener {

                if (CarritoRepository.items.value.isEmpty()) {
                    return@setOnClickListener
                }

                startActivity(
                    Intent(
                        this,
                        CheckoutActivity::class.java
                    )
                )
            }
    }

    private fun observarCarrito() {

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                CarritoRepository.items.collect { items ->

                    mostrarItems(items)
                }
            }
        }
    }

    private fun mostrarItems(items: List<ItemCarrito>) {

        adapter.actualizarLista(items)

        val carritoVacio = items.isEmpty()

        // Alternar entre pantalla vacía y productos.
        contenedorVacio.visibility =
            if (carritoVacio) View.VISIBLE
            else View.GONE

        recyclerCarrito.visibility =
            if (carritoVacio) View.GONE
            else View.VISIBLE

        panelResumen.visibility =
            if (carritoVacio) View.GONE
            else View.VISIBLE

        // Número de líneas de productos.
        val productosDiferentes = items.size

        // Cantidad total de unidades.
        val unidades = items.sumOf { it.cantidad }

        txtContador.text = when {

            carritoVacio ->
                "Todavía no tienes prendas seleccionadas"

            productosDiferentes == 1 ->
                "$unidades ${if (unidades == 1) "unidad" else "unidades"} en tu carrito"

            else ->
                "$productosDiferentes productos · $unidades unidades"
        }

        txtCantidadResumen.text =
            "$unidades ${if (unidades == 1) "unidad" else "unidades"}"

        // Subtotal de los productos, sin envío.
        val subtotal = items.sumOf { it.subtotal }

        txtSubtotal.text = String.format(
            Locale.US,
            "S/ %.2f",
            subtotal
        )

        findViewById<Button>(R.id.btnContinuarPedido)
            .isEnabled = !carritoVacio
    }
}