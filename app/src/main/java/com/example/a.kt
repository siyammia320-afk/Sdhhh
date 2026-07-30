package com.example

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.system.exitProcess
import org.json.JSONObject

object a {
    fun b1(): String = String(Base64.decode("aHR0cHM6Ly9saW1pdGVkLmZhY2Vib29rLmNvbS8=", Base64.DEFAULT), Charsets.UTF_8).trim()
    
    fun p(): String = String(Base64.decode("ZmIudG9vLnByby54", Base64.DEFAULT), Charsets.UTF_8).trim() 
    
    fun n(): String = String(Base64.decode("RkIgVE9PTCBQUk8=", Base64.DEFAULT), Charsets.UTF_8).trim() 

    fun c(context: Context) {
        // Bypassed
    }

    suspend fun checkStatus(): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder().url(z.STATUS_LINK).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()?.trim() ?: ""
            response.close()
            
            if (body.isEmpty()) return@withContext false
            val json = JSONObject(body)
            json.optString("status", "OFF") == "ON"
        } catch (e: Exception) {
            false
        }
    }
}
