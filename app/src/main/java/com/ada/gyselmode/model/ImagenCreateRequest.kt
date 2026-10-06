package com.ada.gyselmode.model

data class ImagenCreateRequest(
    val url: String,
    val publicId: String,
    val esPrincipal: Boolean
)