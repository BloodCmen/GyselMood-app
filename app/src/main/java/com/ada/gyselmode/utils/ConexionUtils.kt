package com.ada.gyselmode.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast

object ConexionUtils {

    fun hayInternet(context: Context): Boolean {

        val connectivityManager =
            context.getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager

        val network =
            connectivityManager.activeNetwork ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(network)
                ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) && capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        )
    }

    fun verificarConexion(context: Context): Boolean {

        if (!hayInternet(context)) {

            Toast.makeText(
                context,
                "Sin conexión a Internet. Revisa tu Wi-Fi o datos móviles.",
                Toast.LENGTH_LONG
            ).show()

            return false
        }

        return true
    }
}
