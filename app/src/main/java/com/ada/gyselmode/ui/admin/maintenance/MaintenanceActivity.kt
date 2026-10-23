package com.ada.gyselmode.ui.admin.maintenance
import android.content.Intent
import android.os.Bundle
import androidx.cardview.widget.CardView
import com.ada.gyselmode.R
import com.ada.gyselmode.helper.NavigationHelper
import com.ada.gyselmode.security.AdminGuard
import com.ada.gyselmode.security.BaseSecureActivity

class MaintenanceActivity : BaseSecureActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AdminGuard.verificarAdmin(this) {
            if (!isFinishing && !isDestroyed) {
                inicializarPanel()
            }
        }
    }

    private fun inicializarPanel() {
        setContentView(R.layout.activity_maintenance)

        NavigationHelper.setupBottomNavigation(this)

        // CREAR PRODUCTO
        findViewById<CardView>(R.id.btnNuevoProducto)
            .setOnClickListener {
                abrir(CrearProductoActivity::class.java)
            }

        // GESTIONAR PRODUCTOS
        findViewById<CardView>(R.id.btnGestionarProductos)
            .setOnClickListener {
                abrir(GestionarProductosActivity::class.java)
            }

        // CATEGORÍAS: LISTAR, CREAR Y ELIMINAR
        findViewById<CardView>(R.id.btnNuevaCategoria)
            .setOnClickListener {
                abrir(GestionarCategoriasActivity::class.java)
            }

        // TALLAS: LISTAR, CREAR Y ELIMINAR
        findViewById<CardView>(R.id.btnNuevaTalla)
            .setOnClickListener {
                abrir(GestionarTallasActivity::class.java)
            }



        // GESTIONAR PEDIDOS
        findViewById<CardView>(R.id.btnGestionarPedidos)
            .setOnClickListener {
                abrir(GestionPedidosActivity::class.java)
            }
    }

    private fun abrir(destino: Class<*>) {
        startActivity(Intent(this, destino))
    }
}
