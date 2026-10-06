package com.ada.gyselmode

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ada.gyselmode.helper.NavigationHelper

class CartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        NavigationHelper.setupBottomNavigation(this)
        NavigationHelper.setupBackButton(this)
    }
}