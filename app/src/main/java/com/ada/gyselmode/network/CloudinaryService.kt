package com.ada.gyselmode.network

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object CloudinaryService {

    private const val CLOUDINARY_NAME = "imgapi"
    private const val UPLOAD_PRESET = "gym_preset"
    private const val FOLDER = "gym_products"

    private val client = OkHttpClient()

    suspend fun subirImagen(
        contentResolver: ContentResolver,
        uri: Uri
    ): Pair<String, String> = withContext(Dispatchers.IO) {

        val bytes = contentResolver.openInputStream(uri)?.use {
            it.readBytes()
        } ?: throw Exception("No se pudo leer la imagen")

        val mimeType = contentResolver.getType(uri) ?: "image/jpeg"

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "imagen.jpg",
                bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            )
            .addFormDataPart(
                "upload_preset",
                UPLOAD_PRESET
            )
            .addFormDataPart(
                "folder",
                FOLDER
            )
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$CLOUDINARY_NAME/image/upload")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->

            val raw = response.body?.string()
                ?: throw Exception("Respuesta vacía de Cloudinary")

            if (!response.isSuccessful) {
                throw Exception("Cloudinary rechazó la imagen")
            }

            val json = JSONObject(raw)

            val url = json.getString("secure_url")
            val publicId = json.getString("public_id")

            Pair(url, publicId)
        }
    }
}