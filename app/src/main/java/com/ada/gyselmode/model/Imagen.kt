package com.ada.gyselmode.model

import java.io.Serializable

data class Imagen(
    val id: Int,
    val url: String,
    val publicId: String,
    val esPrincipal: Boolean
) : Serializable