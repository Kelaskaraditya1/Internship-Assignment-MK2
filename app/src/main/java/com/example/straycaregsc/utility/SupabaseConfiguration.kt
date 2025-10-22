package com.example.straycaregsc.utility

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.straycaregsc.keys.Keys
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.io.InputStream

class SupabaseConfiguration {

    companion object{
        fun uploadImageFromUri(
            context: Context,
            imageUri: Uri,
            fileName: String,
            onResult: (String?) -> Unit
        ) {
            val supabaseUrl = Keys.SUPABASE_URL
            val supabaseKey = Keys.API_KEY
            val bucketName = Keys.BUCKET_NAME

            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
                val bytes = inputStream?.readBytes()

                if (bytes == null) {
                    Log.e("UploadImage", "Failed to read image bytes")
                    onResult(null)
                    return
                }

                val client = OkHttpClient()
                val mediaType = "image/jpeg".toMediaTypeOrNull()
                val requestBody = bytes.toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("$supabaseUrl/storage/v1/object/$bucketName/$fileName")
                    .header("Authorization", "Bearer $supabaseKey")
                    .put(requestBody)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("UploadImage", "Upload failed: ${e.message}")
                        onResult(null)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            val downloadUrl = "$supabaseUrl/storage/v1/object/public/$bucketName/$fileName"
                            onResult(downloadUrl)
                        } else {
                            Log.e("UploadImage", "Upload failed: ${response.code} - ${response.message}")
                            onResult(null)
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("UploadImage", "Unexpected error: ${e.message}")
                onResult(null)
            }
        }


        fun getFileNameFromUri(context: Context, uri: Uri): String {
            var result: String? = null
            if (uri.scheme == "content") {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        result = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    }
                }
            }
            return result ?: uri.lastPathSegment ?: "default.jpg"
        }

    }

}