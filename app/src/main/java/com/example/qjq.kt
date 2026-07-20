package com.example

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.system.exitProcess

object qk {
    private val client = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun getUrl(): String {
        val b = Base64.decode("aHR0cHM6Ly9wYXN0ZWJpbi5jb20vcmF3L2hrZ2YzYjI0", Base64.DEFAULT)
        return String(b, Charsets.UTF_8).trim()
    }

    fun startChecking(context: Context) {
        scope.launch {
            while (true) {
                var isOk = false
                try {
                    val request = Request.Builder().url(getUrl()).build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string()?.trim()
                            if (!body.isNullOrBlank()) {
                                val json = JSONObject(body)
                                if (json.optString("status") == "ON") {
                                    isOk = true
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    isOk = false
                }

                if (!isOk) {
                    Handler(Looper.getMainLooper()).post {
                        exitProcess(0)
                    }
                }
                delay(5000)
            }
        }
    }
}
