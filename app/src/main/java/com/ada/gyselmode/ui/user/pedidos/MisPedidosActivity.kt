package com.ada.gyselmode.ui.user.pedidos

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ada.gyselmode.R
import com.ada.gyselmode.databinding.ActivityMisPedidosBinding
import com.ada.gyselmode.model.pedido.PedidoResponseDto
import com.ada.gyselmode.network.RetrofitClient
import com.ada.gyselmode.security.BaseSecureActivity
import com.ada.gyselmode.ui.user.pago.PagoSimuladoActivity
import com.ada.gyselmode.ui.user.pedidos.adapter.MisPedidosAdapter
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale

class MisPedidosActivity : BaseSecureActivity() {

    private lateinit var binding: ActivityMisPedidosBinding
    private lateinit var adapter: MisPedidosAdapter

    private var pedidosOriginales = emptyList<PedidoResponseDto>()
    private var estadoSeleccionado: String? = null

    private var cargando = false
    private var pantallaLista = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMisPedidosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVolverPedidos.setOnClickListener {
            finish()
        }

        adapter = MisPedidosAdapter(emptyList()) { pedido ->
            mostrarDetalle(pedido)
        }

        binding.recyclerMisPedidos.layoutManager =
            LinearLayoutManager(this)

        binding.recyclerMisPedidos.adapter = adapter

        binding.edtBuscarPedido.addTextChangedListener {
            aplicarFiltros()
        }

        pantallaLista = true
        crearFiltros()
    }

    override fun onResume() {
        super.onResume()
        cargarPedidos()
    }

    private fun cargarPedidos() {
        if (cargando || !pantallaLista) return

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Debes iniciar sesión",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        lifecycleScope.launch {
            cargando = true

            binding.progressMisPedidos.visibility = View.VISIBLE
            binding.txtPedidosVacios.visibility = View.GONE

            try {
                val pedidos =
                    RetrofitClient.apiService.listarPedidosUsuario(uid)

                // Evitar conservar pedidos de una sesión anterior.
                if (FirebaseAuth.getInstance().currentUser?.uid != uid) {
                    pedidosOriginales = emptyList()
                    adapter.actualizarPedidos(emptyList())
                    return@launch
                }

                pedidosOriginales = pedidos

                crearFiltros()
                aplicarFiltros()

            } catch (e: Exception) {
                binding.txtCantidadPedidos.text =
                    "No se pudieron actualizar los pedidos"

                if (pedidosOriginales.isEmpty()) {
                    binding.txtPedidosVacios.text =
                        "No se pudieron cargar tus pedidos.\n" +
                                "Comprueba tu conexión."

                    binding.txtPedidosVacios.visibility = View.VISIBLE
                    adapter.actualizarPedidos(emptyList())
                }

                Toast.makeText(
                    this@MisPedidosActivity,
                    e.message ?: "Error de conexión",
                    Toast.LENGTH_LONG
                ).show()

            } finally {
                cargando = false
                binding.progressMisPedidos.visibility = View.GONE
            }
        }
    }

    private fun aplicarFiltros() {
        if (!pantallaLista) return

        val busqueda =
            binding.edtBuscarPedido.text.toString().trim()

        val filtrados = pedidosOriginales.filter { pedido ->

            val coincideEstado =
                estadoSeleccionado == null ||
                        pedido.estado.equals(
                            estadoSeleccionado,
                            ignoreCase = true
                        )

            val coincideBusqueda =
                pedido.id.toString().contains(busqueda, true) ||
                        pedido.productos.any {
                            it.producto.contains(busqueda, ignoreCase = true)
                        }

            coincideEstado && coincideBusqueda

        }.sortedByDescending { it.id }

        binding.txtCantidadPedidos.text =
            "${filtrados.size} de ${pedidosOriginales.size} pedidos"

        adapter.actualizarPedidos(filtrados)

        binding.txtPedidosVacios.visibility =
            if (filtrados.isEmpty() && !cargando) {
                View.VISIBLE
            } else {
                View.GONE
            }

        if (filtrados.isEmpty()) {
            binding.txtPedidosVacios.text =
                if (pedidosOriginales.isEmpty()) {
                    "Todavía no tienes pedidos.\n" +
                            "¡Explora el catálogo y encuentra tu próxima prenda!"
                } else {
                    "No encontramos pedidos con esos filtros."
                }
        }
    }

    private fun crearFiltros() {
        if (!pantallaLista) return

        binding.contenedorFiltrosPedidos.removeAllViews()

        agregarFiltro("Todos", null)

        // Mostrar estados que realmente existen en los pedidos.
        val estados = pedidosOriginales
            .map { it.estado.uppercase(Locale.ROOT) }
            .distinct()
            .sorted()

        if (estadoSeleccionado != null &&
            estadoSeleccionado !in estados
        ) {
            estadoSeleccionado = null
        }

        estados.forEach { estado ->
            agregarFiltro(estado, estado)
        }
    }

    private fun agregarFiltro(nombre: String, estado: String?) {
        val seleccionado = estadoSeleccionado == estado

        val boton = MaterialButton(
            this,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {

            text = nombre
            textSize = 12f
            isAllCaps = false

            cornerRadius = dp(25)
            insetTop = 0
            insetBottom = 0
            minHeight = 0
            minimumHeight = 0
            strokeWidth = 0

            setTextColor(Color.WHITE)

            backgroundTintList = ColorStateList.valueOf(
                if (seleccionado) {
                    Color.parseColor("#B93241")
                } else {
                    Color.parseColor("#303037")
                }
            )

            setOnClickListener {
                estadoSeleccionado = estado
                crearFiltros()
                aplicarFiltros()
            }
        }

        binding.contenedorFiltrosPedidos.addView(
            boton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(40)
            ).apply {
                marginEnd = dp(8)
            }
        )
    }

    private fun mostrarDetalle(pedido: PedidoResponseDto) {
        val vista = LayoutInflater.from(this).inflate(
            R.layout.dialog_detalle_mi_pedido,
            null
        )

        val dialogo = AlertDialog.Builder(this)
            .setView(vista)
            .create()

        vista.findViewById<TextView>(R.id.txtDetalleNumero).text =
            "Pedido #${pedido.id}"

        vista.findViewById<TextView>(R.id.txtDetalleFecha).text =
            pedido.fechaCreacion
                ?.replace("T", " · ")
                ?.take(19)
                ?: "Sin fecha"

        val estado = pedido.estado.uppercase(Locale.ROOT)

        val txtEstado =
            vista.findViewById<TextView>(R.id.txtDetalleEstado)

        txtEstado.text = estado

        txtEstado.setTextColor(
            when (estado) {
                "PAGADO", "ENTREGADO" ->
                    Color.parseColor("#86DBA8")

                "CONFIRMADO", "EN_PROCESO" ->
                    Color.parseColor("#91BBFF")

                "CANCELADO", "RECHAZADO" ->
                    Color.parseColor("#FF898F")

                else ->
                    Color.parseColor("#FFD18C")
            }
        )

        vista.findViewById<TextView>(R.id.txtDescripcionEstado).text =
            descripcionEstado(estado)

        val productos = pedido.productos.joinToString("\n\n") {
            "${it.cantidad} × ${it.producto}\n" +
                    "Talla: ${it.talla}\n" +
                    "Subtotal: ${dinero(it.subtotal)}"
        }

        vista.findViewById<TextView>(R.id.txtDetalleProductos).text =
            productos.ifBlank { "Sin productos registrados" }

        vista.findViewById<TextView>(R.id.txtDetalleSubtotal).text =
            "Subtotal: ${dinero(pedido.subtotal)}"

        vista.findViewById<TextView>(R.id.txtDetalleEnvio).text =
            "Envío: ${dinero(pedido.costoEnvio)}"

        vista.findViewById<TextView>(R.id.txtDetalleTotal).text =
            "TOTAL: ${dinero(pedido.total)}"

        vista.findViewById<TextView>(R.id.txtDetalleDireccion).text =
            pedido.direccionEntrega
                .takeIf { it.isNotBlank() }
                ?: "No registrada"

        vista.findViewById<TextView>(R.id.txtDetalleReferencia).text =
            pedido.referencia
                ?.takeIf { it.isNotBlank() }
                ?: "Sin referencia"

        // Seguimiento disponible cuando el repartidor está en camino.
        val btnSeguimiento =
            vista.findViewById<Button>(R.id.btnSeguimiento)

        val seguimientoDisponible = estado == "EN_CAMINO"
        btnSeguimiento.isEnabled = seguimientoDisponible
        btnSeguimiento.alpha = if (seguimientoDisponible) 1f else 0.55f

        btnSeguimiento.setOnClickListener {
            if (seguimientoDisponible) {
                dialogo.dismiss()
                val intentSeguimiento = Intent(
                    this,
                    SeguimientoPedidoActivity::class.java
                ).apply {
                    putExtra(
                        SeguimientoPedidoActivity.EXTRA_PEDIDO_ID,
                        pedido.id
                    )
                }
                startActivity(intentSeguimiento)
            }
        }

        // Conservar el pago real con Mercado Pago.
        val btnPagar =
            vista.findViewById<Button>(R.id.btnCompletarPago)

        if (estado == "PENDIENTE") {
            btnPagar.visibility = View.VISIBLE

            btnPagar.setOnClickListener {
                dialogo.dismiss()

                val intentPago = Intent(
                    this,
                    PagoSimuladoActivity::class.java
                )

                intentPago.putExtra("PEDIDO_ID", pedido.id)
                startActivity(intentPago)
            }
        } else {
            btnPagar.visibility = View.GONE
        }

        vista.findViewById<Button>(R.id.btnCerrarDetalle)
            .setOnClickListener {
                dialogo.dismiss()
            }

        dialogo.setOnShowListener {
            dialogo.window?.setBackgroundDrawableResource(
                android.R.color.transparent
            )

            dialogo.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.92f).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            dialogo.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )
        }

        dialogo.show()
    }

    private fun descripcionEstado(estado: String): String {
        return when (estado) {
            "PENDIENTE" ->
                "Tu pedido está registrado y pendiente de pago."

            "PAGADO" ->
                "Tu pago fue registrado. Estamos a la espera " +
                        "de la confirmación del pedido."

            "CONFIRMADO" ->
                "Tu pedido ha sido confirmado por la tienda."

            "EN_PROCESO", "PREPARANDO" ->
                "Tu pedido se encuentra en preparación."

            "ENVIADO", "EN_REPARTO" ->
                "Tu pedido ha sido enviado y espera el inicio del recorrido."

            "EN_CAMINO" ->
                "Tu repartidor está en camino. Pulsa Ver seguimiento " +
                        "para consultar su ubicación en el mapa."

            "ENTREGADO" ->
                "Tu pedido figura como entregado."

            "CANCELADO" ->
                "Este pedido fue cancelado."

            else ->
                "Estado reportado por la tienda: $estado"
        }
    }

    private fun dinero(valor: Double): String =
        "S/ ${String.format(Locale.US, "%.2f", valor)}"

    private fun dp(valor: Int): Int =
        (valor * resources.displayMetrics.density).toInt()
}
