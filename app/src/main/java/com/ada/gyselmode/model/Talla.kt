package com.ada.gyselmode.model

import java.io.Serializable

data class Talla(
    val id: Int,
    val nombre: String,
    val stock: Int
) : Serializable