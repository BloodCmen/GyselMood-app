package com.ada.gyselmode.network

import com.ada.gyselmode.model.Categoria
import com.ada.gyselmode.model.Producto
import com.ada.gyselmode.model.ProductoCreateRequest
import com.ada.gyselmode.model.ProductoUpdateRequest
import com.ada.gyselmode.model.CategoriaCreateRequest
import com.ada.gyselmode.model.Talla
import com.ada.gyselmode.model.TallaCreateRequest
import com.ada.gyselmode.model.entrega.AsignacionEntregaResponse
import com.ada.gyselmode.model.entrega.AsignarEntregaRequest
import com.ada.gyselmode.model.entrega.SeguimientoEntregaResponse
import com.ada.gyselmode.model.entrega.UbicacionEntregaRequest
import com.ada.gyselmode.model.pago.CrearPagoRequest
import com.ada.gyselmode.model.pago.PreferenciaMercadoPagoResponse
import com.ada.gyselmode.model.pedido.ActualizarEstadoPedidoRequest
import com.ada.gyselmode.model.pedido.CrearPedidoRequest
import com.ada.gyselmode.model.pedido.HistorialEstadoResponseDto
import com.ada.gyselmode.model.pedido.PedidoResponseDto
import com.ada.gyselmode.model.usuario.AvatarUploadResponse
import okhttp3.MultipartBody


import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.Path

import retrofit2.Response
import okhttp3.ResponseBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.Part

interface ApiService {

    // LISTAR PRODUCTOS ACTIVOS
    @GET("api/productos/listarActivos")
    suspend fun listarProductosActivos(): List<Producto>

    // LISTAR TODOS (conservamos tu método anterior)
    @GET("api/productos/listarProductoData")
    suspend fun listarProductos(): List<Producto>

    // TALLAS
    @GET("api/tallas/listarTallas")
    suspend fun listarTallas(): List<Talla>

    // CREAR TALLA
    @POST("api/tallas/guardarTalla")
    suspend fun crearTalla(
        @Body talla: TallaCreateRequest
    ): Talla

    @DELETE("api/tallas/eliminarTalla/{id}")
    suspend fun eliminarTalla(
        @Path("id") id: Long
    )


    // CREAR PRODUCTO
    @POST("api/productos/crear")
    suspend fun crearProducto(
        @Body producto: ProductoCreateRequest
    ): Producto

    // EDITAR PRODUCTO
    @PATCH("api/productos/{id}")
    suspend fun actualizarProducto(
        @Path("id") id: Long,
        @Body producto: ProductoUpdateRequest
    ): Producto

    // ELIMINAR LÓGICAMENTE
    @DELETE("api/productos/{id}")
    suspend fun eliminarProducto(
        @Path("id") id: Long
    )


// CATEGORÍAS

    @GET("api/categorias/listarCategorias")
    suspend fun listarCategorias(): List<Categoria>

    @POST("api/categorias/crearCategoria")
    suspend fun crearCategoria(
        @Body categoria: CategoriaCreateRequest
    ): Categoria

    @DELETE("api/categorias/eliminarCategoria/{id}")
    suspend fun eliminarCategoria(
        @Path("id") id: Long
    )


// PEDIDOS

    @POST("api/pedidos")
    suspend fun crearPedido(
        @Body pedido: CrearPedidoRequest
    ): PedidoResponseDto


    // Listar pedidos de un usuario
    @GET("api/pedidos/usuario/{firebaseUid}")
    suspend fun listarPedidosUsuario(
        @Path("firebaseUid") firebaseUid: String
    ): List<PedidoResponseDto>

    // Obtener un pedido concreto
    @GET("api/pedidos/{id}/usuario/{firebaseUid}")
    suspend fun obtenerPedido(
        @Path("id") id: Long,
        @Path("firebaseUid") firebaseUid: String
    ): PedidoResponseDto

    // Cancelar pedido
    @PUT("api/pedidos/{id}/cancelar")
    suspend fun cancelarPedido(
        @Path("id") id: Long,
        @retrofit2.http.Query("firebaseUid") firebaseUid: String
    ): Unit

    // Historial de estados
    @GET("api/pedidos/{id}/usuario/{firebaseUid}/historial")
    suspend fun obtenerHistorialPedido(
        @Path("id") id: Long,
        @Path("firebaseUid") firebaseUid: String
    ): List<HistorialEstadoResponseDto>

    @PUT("api/pedidos/{id}/estado")
    suspend fun actualizarEstadoPedido(
        @Path("id") id: Long,
        @Body request: ActualizarEstadoPedidoRequest
    )


    @POST("api/pagos/pedido/{pedidoId}")
    suspend fun registrarPago(
        @Path("pedidoId") pedidoId: Long,
        @Body request: CrearPagoRequest
    ): Response<ResponseBody>

    @GET("api/pagos/pedido/{pedidoId}")
    suspend fun obtenerPago(
        @Path("pedidoId") pedidoId: Long
    ): Response<ResponseBody>


    // ADMIN - LISTAR TODOS LOS PEDIDOS
    @GET("api/pedidos/admin/listar")
    suspend fun listarTodosPedidos(): List<PedidoResponseDto>

    @PUT("api/pedidos/{id}/estado")
    suspend fun actualizarEstadoPedidoAdmin(
        @Path("id") id: Long,
        @Body request: ActualizarEstadoPedidoRequest
    ): Response<Unit>




    // MERCADO PAGO - GENERAR PREFERENCIA
    @POST("api/mercadopago/preferencia/{pedidoId}")
    suspend fun crearPreferenciaMercadoPago(
        @Path("pedidoId") pedidoId: Long
    ): PreferenciaMercadoPagoResponse



    @Multipart
    @POST("perfil/avatar")
    suspend fun subirAvatar(
        @Header("Authorization") token: String,
        @Part archivo: MultipartBody.Part
    ): AvatarUploadResponse

    //REPARTO

    @POST("api/entregas/pedido/{pedidoId}/asignar")
    suspend fun asignarRepartidor(
        @Header("Authorization") token: String,
        @Path("pedidoId") pedidoId: Long,
        @Body request: AsignarEntregaRequest
    ): AsignacionEntregaResponse

    @GET("api/entregas/pedido/{pedidoId}")
    suspend fun obtenerAsignacionEntrega(
        @Header("Authorization") token: String,
        @Path("pedidoId") pedidoId: Long
    ): AsignacionEntregaResponse

    @GET("api/entregas/mis")
    suspend fun listarMisEntregas(
        @Header("Authorization") token: String
    ): List<AsignacionEntregaResponse>


    //ENTREGA ENDPOINT




    @PUT("api/entregas/pedido/{pedidoId}/iniciar")
    suspend fun iniciarEntrega(
        @Header("Authorization") token: String,
        @Path("pedidoId") pedidoId: Long
    ): retrofit2.Response<Unit>

    @PUT("api/entregas/pedido/{pedidoId}/finalizar")
    suspend fun finalizarEntrega(
        @Header("Authorization") token: String,
        @Path("pedidoId") pedidoId: Long
    ): retrofit2.Response<Unit>

    @PUT("api/entregas/pedido/{pedidoId}/ubicacion")
    suspend fun actualizarUbicacionEntrega(
        @Header("Authorization") token: String,
        @Path("pedidoId") pedidoId: Long,
        @Body request: UbicacionEntregaRequest
    ): retrofit2.Response<Unit>

    @GET("api/entregas/pedido/{pedidoId}/seguimiento")
    suspend fun obtenerSeguimientoEntrega(
        @Header("Authorization") token: String,
        @Path("pedidoId") pedidoId: Long
    ): SeguimientoEntregaResponse


}