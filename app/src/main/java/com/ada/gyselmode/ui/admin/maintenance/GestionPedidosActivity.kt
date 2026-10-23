package com.ada.gyselmode.ui.admin.maintenance

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ada.gyselmode.R
import com.ada.gyselmode.databinding.ActivityGestionPedidosBinding
import com.ada.gyselmode.ui.admin.maintenance.adapter.GestionPedidosAdminAdapter
import com.ada.gyselmode.model.pedido.ActualizarEstadoPedidoRequest
import com.ada.gyselmode.model.pedido.PedidoResponseDto
import com.ada.gyselmode.network.RetrofitClient
import com.ada.gyselmode.security.AdminGuard
import com.ada.gyselmode.security.BaseSecureActivity
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.Locale

import com.ada.gyselmode.model.entrega.AsignarEntregaRequest
import com.google.firebase.auth.FirebaseAuth
import retrofit2.HttpException
import android.widget.ProgressBar
import com.ada.gyselmode.model.entrega.Repartidor
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
class GestionPedidosActivity : BaseSecureActivity() {

    private lateinit var binding: ActivityGestionPedidosBinding
    private lateinit var adapter: GestionPedidosAdminAdapter

    private var pedidosOriginales = emptyList<PedidoResponseDto>()
    private var estadoSeleccionado: String? = null

    private var procesando = false
    private var pantallaLista = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AdminGuard.verificarAdmin(this) {
            if (!isFinishing && !isDestroyed) {
                prepararPantalla()
            }
        }
    }

    private fun prepararPantalla() {
        binding = ActivityGestionPedidosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVolverPedidos.setOnClickListener {
            finish()
        }

        adapter = GestionPedidosAdminAdapter(emptyList()) { pedido ->
            mostrarDetalle(pedido)
        }

        binding.recyclerGestionPedidos.layoutManager =
            LinearLayoutManager(this)

        binding.recyclerGestionPedidos.adapter = adapter

        binding.edtBuscarPedido.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    aplicarFiltros()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) = Unit
            }
        )

        pantallaLista = true
        crearFiltros()
        cargarPedidos()
    }

    override fun onResume() {
        super.onResume()
        // La primera carga se hace en prepararPantalla().
        // Evitamos duplicar peticiones al iniciar.
    }

    private fun cargarPedidos() {
        if (procesando || !pantallaLista) return

        lifecycleScope.launch {
            procesando = true
            binding.progressGestion.visibility = View.VISIBLE
            binding.txtSinPedidos.visibility = View.GONE

            try {
                val pedidos =
                    RetrofitClient.apiService.listarTodosPedidos()

                pedidosOriginales = pedidos

                binding.txtTotalPedidos.text =
                    pedidos.size.toString()

                binding.txtPedidosPagados.text =
                    pedidos.count {
                        it.estado.equals("PAGADO", ignoreCase = true)
                    }.toString()

                crearFiltros()
                aplicarFiltros()

            } catch (e: Exception) {
                binding.txtResultadosPedidos.text =
                    "Error al cargar los pedidos"

                if (pedidosOriginales.isEmpty()) {
                    binding.txtSinPedidos.text =
                        "No se pudieron obtener los pedidos.\n" +
                                "Comprueba tu conexión."

                    binding.txtSinPedidos.visibility = View.VISIBLE
                }

                Toast.makeText(
                    this@GestionPedidosActivity,
                    e.message ?: "Error de conexión",
                    Toast.LENGTH_LONG
                ).show()

            } finally {
                procesando = false
                binding.progressGestion.visibility = View.GONE
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
                        pedido.firebaseUid.contains(busqueda, true) ||
                        pedido.productos.any {
                            it.producto.contains(busqueda, true)
                        }

            coincideEstado && coincideBusqueda
        }.sortedByDescending {
            it.id
        }

        binding.txtResultadosPedidos.text =
            "${filtrados.size} de ${pedidosOriginales.size} pedidos"

        adapter.actualizarPedidos(filtrados)

        binding.txtSinPedidos.visibility =
            if (filtrados.isEmpty()) View.VISIBLE else View.GONE

        if (filtrados.isEmpty()) {
            binding.txtSinPedidos.text =
                if (pedidosOriginales.isEmpty()) {
                    "Todavía no hay pedidos registrados."
                } else {
                    "No encontramos pedidos con esos filtros."
                }
        }
    }

    private fun crearFiltros() {
        if (!pantallaLista) return

        val contenedor = binding.contenedorFiltrosPedidos
        contenedor.removeAllViews()

        agregarFiltro("Todos", null)

        // Usamos los estados que realmente devuelve la API.
        val estados = pedidosOriginales
            .map { it.estado.uppercase(Locale.ROOT) }
            .distinct()
            .sorted()

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
            minimumHeight = 0
            minHeight = 0
            strokeWidth = 0

            setTextColor(Color.WHITE)

            backgroundTintList = ColorStateList.valueOf(
                if (seleccionado) getColor(R.color.gysel_red)
                else Color.rgb(44, 44, 50)
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
                rightMargin = dp(8)
            }
        )
    }

    private fun mostrarDetalle(pedido: PedidoResponseDto) {
        val vista = LayoutInflater.from(this).inflate(
            R.layout.dialog_detalle_pedido_admin,
            null
        )

        val dialogo = AlertDialog.Builder(this)
            .setView(vista)
            .create()

        vista.findViewById<TextView>(R.id.txtDetalleNumero).text =
            "Pedido #${pedido.id}"

        val txtEstado =
            vista.findViewById<TextView>(R.id.txtDetalleEstado)

        txtEstado.text = pedido.estado.uppercase(Locale.ROOT)

        txtEstado.setTextColor(
            when (pedido.estado.uppercase(Locale.ROOT)) {
                "PAGADO", "ENTREGADO" ->
                    Color.rgb(119, 217, 160)

                "CANCELADO", "RECHAZADO" ->
                    Color.rgb(255, 128, 135)

                else ->
                    Color.rgb(255, 193, 109)
            }
        )

        vista.findViewById<TextView>(R.id.txtDetalleFecha).text =
            pedido.fechaCreacion
                ?.replace("T", " · ")
                ?.take(19)
                ?: "Sin fecha"

        val productos = pedido.productos.joinToString("\n\n") {
            "${it.cantidad} × ${it.producto}\n" +
                    "Talla: ${it.talla}\n" +
                    "Subtotal: ${dinero(it.subtotal)}"
        }

        vista.findViewById<TextView>(R.id.txtDetalleProductos).text =
            productos.ifBlank { "Sin productos registrados" }

        vista.findViewById<TextView>(R.id.txtDetalleTotal).text =
            dinero(pedido.total)

        val txtCliente = vista.findViewById<TextView>(R.id.txtDetalleCliente)
        txtCliente.text = "Cliente: consultando nombre..."

        vista.findViewById<TextView>(R.id.txtDetalleDireccion).text =
            pedido.direccionEntrega
                .takeIf { it.isNotBlank() }
                ?: "No registrada"

        vista.findViewById<TextView>(R.id.txtDetalleReferencia).text =
            pedido.referencia
                ?.takeIf { it.isNotBlank() }
                ?: "Sin referencia"

        // Se habilitará cuando exista asignación de repartidores
        // y un endpoint seguro de seguimiento GPS.
        val btnSeguimiento =
            vista.findViewById<Button>(R.id.btnSeguimientoPedido)

        btnSeguimiento.isEnabled = false
        btnSeguimiento.alpha = 0.55f

        val btnConfirmar =
            vista.findViewById<Button>(R.id.btnConfirmarPedido)

        if (pedido.estado.equals("PAGADO", ignoreCase = true)) {
            btnConfirmar.visibility = View.VISIBLE

            btnConfirmar.setOnClickListener {
                dialogo.dismiss()
                confirmarCambio(pedido)
            }
        } else {
            btnConfirmar.visibility = View.GONE
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
        }


        val btnAsignar = vista.findViewById<Button>(
            R.id.btnAsignarRepartidor
        )

        val permiteAsignar =
            pedido.estado.equals("CONFIRMADO", true) ||
                    pedido.estado.equals("PREPARANDO", true)

        btnAsignar.visibility =
            if (permiteAsignar) View.VISIBLE else View.GONE

        btnAsignar.setOnClickListener {
            dialogo.dismiss()
            mostrarDialogoAsignacion(pedido.id)
        }


        dialogo.show()
        consultarNombreCliente(pedido.firebaseUid, txtCliente, dialogo)
        consultarEntregaDelPedido(pedido, vista, dialogo)
    }

    private fun confirmarCambio(pedido: PedidoResponseDto) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar pedido #${pedido.id}")
            .setMessage(
                "¿Deseas cambiar el estado de PAGADO a CONFIRMADO?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ ->
                actualizarEstado(pedido.id)
            }
            .show()
    }

    private fun actualizarEstado(pedidoId: Long) {
        if (procesando) return

        lifecycleScope.launch {
            procesando = true
            binding.progressGestion.visibility = View.VISIBLE

            try {
                val request = ActualizarEstadoPedidoRequest(
                    estado = "CONFIRMADO",
                    comentario = "Pedido confirmado por administración"
                )

                val response =
                    RetrofitClient.apiService.actualizarEstadoPedidoAdmin(
                        pedidoId,
                        request
                    )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@GestionPedidosActivity,
                        "Pedido confirmado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@GestionPedidosActivity,
                        "Error HTTP ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@GestionPedidosActivity,
                    e.message ?: "Error de conexión",
                    Toast.LENGTH_LONG
                ).show()

            } finally {
                procesando = false
                binding.progressGestion.visibility = View.GONE
                cargarPedidos()
            }
        }
    }


    private fun mostrarDialogoAsignacion(pedidoId: Long) {

        val usuario = FirebaseAuth.getInstance().currentUser

        if (usuario == null) {
            Toast.makeText(
                this,
                "Debes iniciar sesión",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val dialogoCarga = AlertDialog.Builder(this)
            .setTitle("Buscando repartidores")
            .setMessage("Consultando trabajadores disponibles...")
            .setCancelable(false)
            .create()

        dialogoCarga.show()

        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .whereEqualTo("rol", "REPARTIDOR")
            .get(Source.SERVER)
            .addOnSuccessListener { documentos ->

                dialogoCarga.dismiss()

                if (isFinishing || isDestroyed) {
                    return@addOnSuccessListener
                }

                val repartidores = documentos.map { documento ->
                    Repartidor(
                        uid = documento.id,
                        nombre = documento.getString("nombre")
                            ?.takeIf { it.isNotBlank() }
                            ?: "Repartidor sin nombre"
                    )
                }.sortedBy { it.nombre.lowercase() }

                if (repartidores.isEmpty()) {
                    AlertDialog.Builder(this)
                        .setTitle("Sin repartidores")
                        .setMessage(
                            "No hay cuentas con el rol REPARTIDOR " +
                                    "registradas en Firestore."
                        )
                        .setPositiveButton("Entendido", null)
                        .show()

                    return@addOnSuccessListener
                }

                val nombres = repartidores.map {
                    it.nombre
                }.toTypedArray()

                var seleccionado = 0

                val dialogo = AlertDialog.Builder(this)
                    .setTitle("Asignar pedido #$pedidoId")
                    .setSingleChoiceItems(
                        nombres,
                        seleccionado
                    ) { _, posicion ->
                        seleccionado = posicion
                    }
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Asignar", null)
                    .create()

                dialogo.setOnShowListener {
                    dialogo.getButton(
                        AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener {

                        val repartidor = repartidores[seleccionado]

                        dialogo.dismiss()

                        // Reutilizamos tu función existente.
                        asignarRepartidor(
                            pedidoId,
                            repartidor.uid
                        )
                    }
                }

                dialogo.show()
            }
            .addOnFailureListener { error ->

                dialogoCarga.dismiss()

                if (!isFinishing && !isDestroyed) {
                    AlertDialog.Builder(this)
                        .setTitle("No se pudo cargar repartidores")
                        .setMessage(
                            error.localizedMessage
                                ?: "Revisa tu conexión o los permisos de Firestore."
                        )
                        .setPositiveButton("Cerrar", null)
                        .show()
                }
            }
    }




    private fun asignarRepartidor(
        pedidoId: Long,
        repartidorUid: String
    ) {
        if (procesando) return

        val usuario = FirebaseAuth.getInstance().currentUser

        if (usuario == null) {
            Toast.makeText(
                this,
                "Debes iniciar sesión",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        usuario.getIdToken(false)
            .addOnSuccessListener { resultado ->

                val token = resultado.token

                if (token.isNullOrBlank()) {
                    Toast.makeText(
                        this,
                        "No se pudo obtener el token Firebase",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                lifecycleScope.launch {
                    procesando = true
                    binding.progressGestion.visibility = View.VISIBLE

                    try {
                        val respuesta =
                            RetrofitClient.apiService.asignarRepartidor(
                                "Bearer $token",
                                pedidoId,
                                AsignarEntregaRequest(repartidorUid)
                            )

                        Toast.makeText(
                            this@GestionPedidosActivity,
                            "Pedido #${respuesta.pedidoId} " +
                                    "asignado correctamente",
                            Toast.LENGTH_LONG
                        ).show()

                    } catch (e: HttpException) {

                        val mensaje = when (e.code()) {
                            401 -> "Sesión Firebase no válida"
                            403 -> "Tu UID no tiene permiso ADMIN"
                            404 -> "No se encontró el endpoint o pedido"
                            409 -> "El pedido ya tiene repartidor"
                            500 -> "Error del servidor; revisar logs"
                            else -> "Error HTTP ${e.code()}"
                        }

                        Toast.makeText(
                            this@GestionPedidosActivity,
                            mensaje,
                            Toast.LENGTH_LONG
                        ).show()

                    } catch (e: Exception) {
                        Toast.makeText(
                            this@GestionPedidosActivity,
                            e.message ?: "Error de conexión",
                            Toast.LENGTH_LONG
                        ).show()

                    } finally {
                        procesando = false
                        binding.progressGestion.visibility = View.GONE
                        cargarPedidos()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "No se pudo validar la sesión Firebase",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    private fun consultarEntregaDelPedido(
        pedido: PedidoResponseDto,
        vista: View,
        dialogo: AlertDialog
    ) {
        val txtEstado = vista.findViewById<TextView>(
            R.id.txtEstadoEntregaAdmin
        )

        val txtRepartidor = vista.findViewById<TextView>(
            R.id.txtRepartidorAsignado
        )

        val progress = vista.findViewById<ProgressBar>(
            R.id.progressConsultaEntrega
        )

        val btnAsignar = vista.findViewById<Button>(
            R.id.btnAsignarRepartidor
        )

        val permiteAsignar =
            pedido.estado.equals("CONFIRMADO", true) ||
                    pedido.estado.equals("PREPARANDO", true)

        btnAsignar.visibility = View.GONE
        progress.visibility = View.VISIBLE
        txtEstado.text = "Consultando asignación..."
        txtRepartidor.text = ""

        val usuario = FirebaseAuth.getInstance().currentUser

        if (usuario == null) {
            progress.visibility = View.GONE
            txtEstado.text = "Sesión no disponible"
            return
        }

        usuario.getIdToken(false)
            .addOnSuccessListener { resultado ->

                val token = resultado.token

                if (token.isNullOrBlank()) {
                    progress.visibility = View.GONE
                    txtEstado.text = "No se pudo validar la sesión"
                    return@addOnSuccessListener
                }

                lifecycleScope.launch {
                    try {
                        val entrega =
                            RetrofitClient.apiService
                                .obtenerAsignacionEntrega(
                                    "Bearer $token",
                                    pedido.id
                                )

                        if (!dialogo.isShowing) return@launch

                        txtEstado.text =
                            "Estado: ${entrega.estado}"

                        txtRepartidor.text = "Repartidor: consultando nombre..."
                        consultarNombreRepartidor(
                            entrega.repartidorUid,
                            txtRepartidor,
                            dialogo
                        )

                        // Ya existe una asignación.
                        // No permitir crear otra.
                        btnAsignar.visibility = View.GONE

                    } catch (e: HttpException) {
                        if (!dialogo.isShowing) return@launch

                        when (e.code()) {
                            404 -> {
                                // La API usa 404 cuando no existe
                                // una asignación para este pedido.
                                txtEstado.text =
                                    "Sin repartidor asignado"

                                txtRepartidor.text =
                                    "Todavía no hay una entrega registrada."

                                btnAsignar.visibility =
                                    if (permiteAsignar) {
                                        View.VISIBLE
                                    } else {
                                        View.GONE
                                    }
                            }

                            401, 403 -> {
                                txtEstado.text =
                                    "No tienes acceso a esta información"
                            }

                            else -> {
                                txtEstado.text =
                                    "No se pudo consultar la entrega"
                            }
                        }

                    } catch (e: Exception) {
                        if (!dialogo.isShowing) return@launch

                        txtEstado.text =
                            "Error al consultar la entrega"

                        txtRepartidor.text =
                            e.localizedMessage ?: "Error de conexión"

                    } finally {
                        if (dialogo.isShowing) {
                            progress.visibility = View.GONE
                        }
                    }
                }
            }
            .addOnFailureListener {
                if (dialogo.isShowing) {
                    progress.visibility = View.GONE
                    txtEstado.text =
                        "No se pudo validar la sesión Firebase"
                }
            }

        btnAsignar.setOnClickListener {
            dialogo.dismiss()
            mostrarDialogoAsignacion(pedido.id)
        }
    }



    // Los nombres son datos de presentación: nunca sustituyen al UID
    // que sigue utilizando el backend para identificar al usuario.
    private fun consultarNombreCliente(
        uid: String,
        vistaTexto: TextView,
        dialogo: AlertDialog
    ) {
        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(uid)
            .get(Source.SERVER)
            .addOnSuccessListener { documento ->
                if (!isFinishing && !isDestroyed && dialogo.isShowing) {
                    val nombre = documento.getString("nombre")
                        ?.trim()?.takeIf { it.isNotEmpty() }
                    vistaTexto.text = "Cliente: ${nombre ?: "Nombre no registrado"}"
                }
            }
            .addOnFailureListener {
                if (!isFinishing && !isDestroyed && dialogo.isShowing) {
                    vistaTexto.text = "Cliente: nombre no disponible"
                }
            }
    }

    private fun consultarNombreRepartidor(
        uid: String,
        vistaTexto: TextView,
        dialogo: AlertDialog
    ) {
        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(uid)
            .get(Source.SERVER)
            .addOnSuccessListener { documento ->
                if (!isFinishing && !isDestroyed && dialogo.isShowing) {
                    val nombre = documento.getString("nombre")
                        ?.trim()?.takeIf { it.isNotEmpty() }
                    vistaTexto.text = "Repartidor: ${nombre ?: "Nombre no registrado"}"
                }
            }
            .addOnFailureListener {
                if (!isFinishing && !isDestroyed && dialogo.isShowing) {
                    vistaTexto.text = "Repartidor: nombre no disponible"
                }
            }
    }

    private fun dinero(valor: Double): String =
        "S/ ${String.format(Locale.US, "%.2f", valor)}"

    private fun dp(valor: Int): Int =
        (valor * resources.displayMetrics.density).toInt()
}
