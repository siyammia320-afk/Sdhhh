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
    
    fun u1(): String = String(Base64.decode("aHR0cHM6Ly9wYXN0ZWJpbi5jb20vcmF3L2hrZ2YzYjI0", Base64.DEFAULT), Charsets.UTF_8).trim()
    
    fun p(): String = String(Base64.decode("ZmIudG9vLnByby54", Base64.DEFAULT), Charsets.UTF_8).trim() 
    
    fun n(): String = String(Base64.decode("RkIgVE9PTCBQUk8=", Base64.DEFAULT), Charsets.UTF_8).trim() 

    fun c(context: Context) {
        if (context.packageName != p() || context.getString(R.string.app_name) != n()) {
            exitProcess(0)
        }
    }

    suspend fun s1(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = URL(u1()).readText().trim()
                if (response.isBlank()) return@withContext false
                
                val json = JSONObject(response)
                json.optString("status") == "ON"
            } catch (e: Exception) {
                false
            }
        }
    }
}
