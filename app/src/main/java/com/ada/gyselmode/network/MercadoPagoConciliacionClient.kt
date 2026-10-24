
package com.ada.gyselmode.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object MercadoPagoConciliacionClient {

    private val retrofit = Retrofit.Builder()
        .baseUrl(
            "https://api-rest-gym.onrender.com/"
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: MercadoPagoConciliacionApi =
        retrofit.create(MercadoPagoConciliacionApi::class.java)
}
