
package com.ada.gyselmode.network

import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface MercadoPagoConciliacionApi {

    @POST("api/mercadopago/conciliar/{pedidoId}")
    fun conciliarPago(
        @Path("pedidoId") pedidoId: Long,
        @Header("Authorization") authorization: String
    ): Call<ConciliacionResponse>
}
