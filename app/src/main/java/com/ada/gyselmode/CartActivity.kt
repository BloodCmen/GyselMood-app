
package com.ada.gyselmode

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
import com.ada.gyselmode.data.carrito.CarritoRepository
import com.ada.gyselmode.helper.NavigationHelper
import com.ada.gyselmode.model.carrito.ItemCarrito
import com.ada.gyselmode.ui.pedidos.adapter.CarritoAdapter
import kotlinx.coroutines.launch
import java.util.Locale

class CartActivity : AppCompatActivity() {

    private lateinit var contenedorVacio: LinearLayout
    private lateinit var recyclerCarrito: RecyclerView
    private lateinit var panelResumen: LinearLayout
    private lateinit var txtSubtotal: TextView
    private lateinit var adapter: CarritoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        NavigationHelper.setupBottomNavigation(this)
        NavigationHelper.setupBackButton(this)

        contenedorVacio = findViewById(R.id.contenedorVacio)
        recyclerCarrito = findViewById(R.id.recyclerCarrito)
        panelResumen = findViewById(R.id.panelResumen)
        txtSubtotal = findViewById(R.id.txtSubtotalCarrito)

        adapter = CarritoAdapter(
            onSumar = { item ->
                CarritoRepository.cambiarCantidad(
                    item.productoId,
                    item.tallaId,
                    item.cantidad + 1
                )
            },
            onRestar = { item ->
                CarritoRepository.cambiarCantidad(
                    item.productoId,
                    item.tallaId,
                    item.cantidad - 1
                )
            },
            onEliminar = { item ->
                CarritoRepository.eliminar(
                    item.productoId,
                    item.tallaId
                )
            }
        )

        recyclerCarrito.layoutManager = LinearLayoutManager(this)
        recyclerCarrito.adapter = adapter

        findViewById<Button>(R.id.btnContinuarPedido).setOnClickListener {
            if (CarritoRepository.items.value.isEmpty()) return@setOnClickListener

            startActivity(
                Intent(this, CheckoutActivity::class.java)
            )
        }

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

        val vacio = items.isEmpty()

        contenedorVacio.visibility =
            if (vacio) View.VISIBLE else View.GONE

        recyclerCarrito.visibility =
            if (vacio) View.GONE else View.VISIBLE

        panelResumen.visibility =
            if (vacio) View.GONE else View.VISIBLE

        val subtotal = items.sumOf { it.subtotal }

        txtSubtotal.text =
            "Subtotal: S/ ${String.format(Locale.US, "%.2f", subtotal)}"
    }
}