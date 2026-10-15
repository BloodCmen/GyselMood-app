package com.ada.gyselmode.helper

import android.app.Activity
import android.content.Intent
import android.widget.TextView
import com.ada.gyselmode.CartActivity
import com.ada.gyselmode.FavoritesActivity
import com.ada.gyselmode.MainActivity
import com.ada.gyselmode.MaintenanceActivity
import com.ada.gyselmode.ProductosActivity
import com.ada.gyselmode.R
import com.ada.gyselmode.account.AccountActivity
import com.ada.gyselmode.maintenance.EditarProductoActivity
import android.view.View


import com.ada.gyselmode.security.AdminGuard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
object NavigationHelper {

    fun setupBottomNavigation(activity: Activity) {

        val navInicio = activity.findViewById<TextView?>(R.id.navInicio)
        val navProductos = activity.findViewById<TextView?>(R.id.navProductos)
        val navCarrito = activity.findViewById<TextView?>(R.id.navCarrito)
        val navCuenta = activity.findViewById<TextView?>(R.id.navCuenta)
        val navMantenimiento = activity.findViewById<TextView?>(R.id.navMantenimiento)
        // Ocultar hasta comprobar que es ADMIN
        navMantenimiento?.visibility = View.GONE


        val usuario = FirebaseAuth.getInstance().currentUser

        if (usuario != null && navMantenimiento != null) {

            FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(usuario.uid)
                .get(Source.SERVER)
                .addOnSuccessListener { documento ->

                    if (!activity.isFinishing && !activity.isDestroyed) {

                        val esAdmin = documento.getString("rol") == "ADMIN"

                        navMantenimiento.visibility =
                            if (esAdmin) View.VISIBLE else View.GONE
                    }
                }
                .addOnFailureListener {
                    // Si no podemos comprobar el rol, ocultamos el acceso.
                    navMantenimiento.visibility = View.GONE
                }
        }


        // Si la barra no está en la pantalla o faltan los IDs, salimos sin romper la app
        if (navInicio == null && navProductos == null && navCarrito == null && navCuenta == null && navMantenimiento == null) {
            return
        }

        // Todos en blanco primero (usando el operador safe-call '?.')
        navInicio?.setTextColor(activity.getColor(R.color.gysel_white))
        navProductos?.setTextColor(activity.getColor(R.color.gysel_white))
        navCarrito?.setTextColor(activity.getColor(R.color.gysel_white))
        navCuenta?.setTextColor(activity.getColor(R.color.gysel_white))
        navMantenimiento?.setTextColor(activity.getColor(R.color.gysel_white))

        // Activar la sección actual
        when (activity) {
            is MainActivity -> {
                navInicio?.setTextColor(activity.getColor(R.color.gysel_red))
            }
            is ProductosActivity -> {
                navProductos?.setTextColor(activity.getColor(R.color.gysel_red))
            }
            is CartActivity -> {
                navCarrito?.setTextColor(activity.getColor(R.color.gysel_red))
            }
            is AccountActivity -> {
                navCuenta?.setTextColor(activity.getColor(R.color.gysel_red))
            }
            // Si estás en Mantenimiento O en EditarProducto, se resalta 'Mantenimiento'
            is MaintenanceActivity, is EditarProductoActivity -> {
                navMantenimiento?.setTextColor(activity.getColor(R.color.gysel_red))
            }
        }

        // LISTENERS CON NAVEGACIÓN SEGURA

        navInicio?.setOnClickListener {
            if (activity !is MainActivity) {
                activity.startActivity(Intent(activity, MainActivity::class.java))
            }
        }

        navProductos?.setOnClickListener {
            if (activity !is ProductosActivity) {
                activity.startActivity(Intent(activity, ProductosActivity::class.java))
            }
        }

        navCarrito?.setOnClickListener {
            if (activity !is CartActivity) {
                activity.startActivity(Intent(activity, CartActivity::class.java))
            }
        }

        navCuenta?.setOnClickListener {
            if (activity !is AccountActivity) {
                activity.startActivity(Intent(activity, AccountActivity::class.java))
            }
        }



        navMantenimiento?.setOnClickListener {

            if (activity !is MaintenanceActivity) {

                AdminGuard.verificarAdmin(activity) {

                    activity.startActivity(
                        Intent(
                            activity,
                            MaintenanceActivity::class.java
                        )
                    )
                }
            }
        }
    }

    fun setupBackButton(activity: Activity) {
        val btnVolver = activity.findViewById<TextView?>(R.id.btnVolver)
        btnVolver?.setOnClickListener {
            activity.finish()
        }
    }
}