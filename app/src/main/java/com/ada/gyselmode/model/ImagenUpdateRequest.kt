package com.ada.gyselmode.model

data class ImagenUpdateRequest(
    val id: Int?,
    val url: String,
    val publicId: String,
    val esPrincipal: Boolean
)