package com.example

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.system.exitProcess
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

object a {
    fun b1(): String = String(Base64.decode("aHR0cHM6Ly9saW1pdGVkLmZhY2Vib29rLmNvbS8=", Base64.DEFAULT), Charsets.UTF_8).trim()
    
    fun p(): String = String(Base64.decode("ZmIudG9vLnByby54", Base64.DEFAULT), Charsets.UTF_8).trim() 
    
    fun n(): String = String(Base64.decode("RkIgVE9PTCBQUk8=", Base64.DEFAULT), Charsets.UTF_8).trim() 

    fun c(context: Context) {
        // Bypassed
    }

    suspend fun checkStatusAndDevice(deviceId: String): String = withContext(Dispatchers.IO) {
        try {
            val client = okhttp3.OkHttpClient()
            // 1. Check Global Status
            val request = okhttp3.Request.Builder().url(z.STATUS_LINK).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()?.trim() ?: ""
            response.close()
            
            if (body.isEmpty()) return@withContext "GLOBAL_OFF"
            val json = JSONObject(body)
            if (json.optString("status", "OFF") != "ON") {
                return@withContext "GLOBAL_OFF"
            }

            // 2. Check Device Approval
            val dbUrl = z.STATUS_LINK.replace("app_status.json", "")
            val reqDevice = okhttp3.Request.Builder().url("${dbUrl}approved_devices/$deviceId.json").build()
            val resDevice = client.newCall(reqDevice).execute()
            val bodyDevice = resDevice.body?.string()?.trim() ?: "null"
            resDevice.close()

            if (bodyDevice == "true") {
                return@withContext "APPROVED"
            } else {
                // Add to pending_devices
                try {
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val postBody = "true".toRequestBody(mediaType)
                    val pendingReq = okhttp3.Request.Builder()
                        .url("${dbUrl}pending_devices/$deviceId.json")
                        .put(postBody)
                        .build()
                    client.newCall(pendingReq).execute().close()
                } catch (e: Exception) {}
                return@withContext "DEVICE_NOT_APPROVED"
            }
        } catch (e: Exception) {
            "GLOBAL_OFF"
        }
    }
}
