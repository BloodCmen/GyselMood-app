package com.ada.gyselmode.network

import com.ada.gyselmode.model.Categoria
import com.ada.gyselmode.model.Producto
import com.ada.gyselmode.model.ProductoCreateRequest
import com.ada.gyselmode.model.ProductoUpdateRequest
import com.ada.gyselmode.model.CategoriaCreateRequest
import com.ada.gyselmode.model.Talla
import com.ada.gyselmode.model.TallaCreateRequest

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.DELETE
import retrofit2.http.Path

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




}