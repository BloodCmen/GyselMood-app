package com.ada.gyselmode.account

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.ada.gyselmode.R
import com.ada.gyselmode.helper.NavigationHelper
import com.ada.gyselmode.ui.pedidos.MisPedidosActivity

class AccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        NavigationHelper.setupBottomNavigation(this)
        NavigationHelper.setupBackButton(this)

        // Botón MIS PEDIDOS
        findViewById<Button>(R.id.btnPedidos).setOnClickListener {
            val intent = Intent(
                this,
                MisPedidosActivity::class.java
            )
            startActivity(intent)
        }
    }
}