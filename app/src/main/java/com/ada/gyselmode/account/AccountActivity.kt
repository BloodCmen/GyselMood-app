package com.ada.gyselmode.account

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ada.gyselmode.R
import com.ada.gyselmode.helper.NavigationHelper

class AccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        NavigationHelper.setupBottomNavigation(this)
        NavigationHelper.setupBackButton(this)
    }
}