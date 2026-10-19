
package com.ada.gyselmode.helper

import android.app.Activity
import android.content.Intent
import android.widget.TextView
import com.ada.gyselmode.CartActivity
import com.ada.gyselmode.MainActivity
import com.ada.gyselmode.MaintenanceActivity
import com.ada.gyselmode.ProductosActivity
import com.ada.gyselmode.R
import com.ada.gyselmode.account.AccountActivity
import com.ada.gyselmode.maintenance.EditarProductoActivity
import com.ada.gyselmode.security.AdminGuard
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

object NavigationHelper {

    // Caché temporal del rol ADMIN.
    // Existe únicamente mientras viva el proceso de la app.
    private var uidConsultado: String? = null
    private var esAdminCache: Boolean? = null

    // Evita iniciar varias consultas a Firestore
    // cuando se abren Activities rápidamente.
    private var consultaPendiente:
            com.google.android.gms.tasks.Task<
                    com.google.firebase.firestore.DocumentSnapshot
                    >? = null


    fun setupBottomNavigation(activity: Activity) {

        val navigation =
            activity.findViewById<BottomNavigationView?>(
                R.id.includeBottomNavigation
            ) ?: activity.findViewById<BottomNavigationView?>(
                R.id.bottomNavigation
            ) ?: return

        val itemAdmin = navigation.menu.findItem(
            R.id.navMantenimiento
        )

        

        val seccionActual = obtenerSeccionActual(activity)

        // Seleccionamos inmediatamente la sección correcta.
        // Para ADMIN esperaremos la validación de Firestore.
        if (seccionActual != R.id.navMantenimiento) {
            navigation.selectedItemId = seccionActual
        }

        navigation.setOnItemSelectedListener { item ->

            // Si ya estamos en esa sección, no hacer nada.
            if (item.itemId == seccionActual) {
                return@setOnItemSelectedListener true
            }

            when (item.itemId) {

                R.id.navInicio -> {
                    abrirSeccion(
                        activity,
                        MainActivity::class.java
                    )
                    false
                }

                R.id.navProductos -> {
                    abrirSeccion(
                        activity,
                        ProductosActivity::class.java
                    )
                    false
                }

                R.id.navCarrito -> {
                    abrirSeccion(
                        activity,
                        CartActivity::class.java
                    )
                    false
                }

                R.id.navCuenta -> {
                    abrirSeccion(
                        activity,
                        AccountActivity::class.java
                    )
                    false
                }

                R.id.navMantenimiento -> {

                    AdminGuard.verificarAdmin(activity) {

                        if (!activity.isFinishing &&
                            !activity.isDestroyed
                        ) {
                            abrirSeccion(
                                activity,
                                MaintenanceActivity::class.java
                            )
                        }
                    }

                    false
                }

                else -> false
            }
        }

        navigation.setOnItemReselectedListener {
            // No recargar la pantalla actual.
        }

        validarAdministrador(
            activity,
            navigation,
            seccionActual
        )
    }

    private fun obtenerSeccionActual(
        activity: Activity
    ): Int {

        return when (activity) {

            is MainActivity ->
                R.id.navInicio

            is ProductosActivity ->
                R.id.navProductos

            is CartActivity ->
                R.id.navCarrito

            is AccountActivity ->
                R.id.navCuenta

            is MaintenanceActivity,
            is EditarProductoActivity ->
                R.id.navMantenimiento

            else ->
                R.id.navInicio
        }
    }


    private fun validarAdministrador(
        activity: Activity,
        navigation: BottomNavigationView,
        seccionActual: Int
    ) {

        val itemAdmin = navigation.menu.findItem(
            R.id.navMantenimiento
        ) ?: return

        val usuario = FirebaseAuth.getInstance()
            .currentUser

        // Sin usuario autenticado: ocultar ADMIN.
        if (usuario == null) {

            uidConsultado = null
            esAdminCache = null
            consultaPendiente = null

            itemAdmin.isVisible = false
            return
        }

        val uidActual = usuario.uid

        // Si cambió la cuenta, invalidar el caché anterior.
        if (uidConsultado != uidActual) {

            uidConsultado = uidActual
            esAdminCache = null
            consultaPendiente = null
        }

        // CASO 1: Ya verificamos el rol en esta sesión.
        // Aplicarlo inmediatamente SIN consultar Firestore.
        val rolGuardado = esAdminCache

        if (rolGuardado != null) {

            itemAdmin.isVisible = rolGuardado

            if (rolGuardado &&
                seccionActual == R.id.navMantenimiento
            ) {
                navigation.selectedItemId =
                    R.id.navMantenimiento
            }

            return
        }

        // CASO 2: Aún no tenemos el rol.
        // Ocultar la navbar durante la primera verificación,
        // evitando que se vea pasar de 4 a 5 opciones.
        navigation.visibility = android.view.View.INVISIBLE

        // Si otra Activity ya inició la consulta,
        // reutilizar esa misma tarea.
        val tarea = consultaPendiente
            ?: FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uidActual)
                .get(Source.SERVER)
                .also {
                    consultaPendiente = it
                }

        tarea.addOnCompleteListener { resultado ->

            // Ignorar respuestas de una cuenta anterior.
            if (uidConsultado != uidActual) {
                return@addOnCompleteListener
            }

            // Guardar el resultado solo una vez.
            if (esAdminCache == null) {

                val esAdmin = resultado.isSuccessful &&
                        resultado.result
                            ?.getString("rol")
                            ?.equals(
                                "ADMIN",
                                ignoreCase = true
                            ) == true

                esAdminCache = esAdmin
                consultaPendiente = null
            }

            // No modificar una pantalla que ya se cerró.
            if (activity.isFinishing ||
                activity.isDestroyed
            ) {
                return@addOnCompleteListener
            }

            val admin = esAdminCache == true

            itemAdmin.isVisible = admin

            if (admin &&
                seccionActual == R.id.navMantenimiento
            ) {
                navigation.selectedItemId =
                    R.id.navMantenimiento
            }

            // Mostrar la navbar ya con su distribución final.
            navigation.alpha = 0f
            navigation.visibility = android.view.View.VISIBLE

            navigation.animate()
                .alpha(1f)
                .setDuration(180)
                .start()
        }
    }


    private fun abrirSeccion(
        activity: Activity,
        destino: Class<out Activity>
    ) {

        if (activity.javaClass == destino) {
            return
        }

        val intent = Intent(
            activity,
            destino
        )

        activity.startActivity(intent)

        // Animación muy corta para evitar el
        // efecto brusco de cambiar de Activity.
        @Suppress("DEPRECATION")
        activity.overridePendingTransition(
            R.anim.nav_slide_in,
            R.anim.nav_slide_out
        )

        // Cerramos la pestaña anterior para
        // evitar múltiples copias de Activities.
        activity.finish()
    }

    fun setupBackButton(activity: Activity) {

        val btnVolver =
            activity.findViewById<TextView?>(
                R.id.btnVolver
            )

        btnVolver?.setOnClickListener {
            activity.finish()
        }
    }
}
