package com.example

import com.example.ui.theme.font.LiveCkDialog
import com.example.ui.theme.font.MenuDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.tasks.await
import android.app.Activity
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.net.Uri
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme
import com.example.z
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

data class HistoryItem(
  val phone: String,
  val uid: String,
  val cookies: String,
  val password: String,
  val otp: String,
  val timestamp: Long
)

data class ButtonNames(
  val btn_bot_creator: String = "Bot Creator",
  val btn_range: String = "Range",
  val btn_copy_cookie: String = "Copy Cookie",
  val btn_copy_uid: String = "Copy UID",
  val btn_history: String = "History",
  val btn_cookie_login: String = "Cookie Login",
  val btn_gmail_copy: String = "Gmail Copy",
  val btn_fb_login: String = "FB Login",
  val btn_whoer_ip: String = "Whoer IP",
  val btn_proxy_config: String = "Proxy Config",
  val btn_admin_ws: String = "Admin WS",
  val btn_admin_tg: String = "Admin TG",
  val btn_tg_channel: String = "TG Channel",
  val btn_verify: String = "ভেরিফাই করুন",
  val btn_live_console: String = "Live Console",
  val btn_settings: String = "Settings"
)

private fun generateRandomGmail(): String {
  val chars = "abcdefghijklmnopqrstuvwxyz"
  val prefix = (1..8).map { chars.random() }.joinToString("")
  val number = (100..999).random()
  return "$prefix$number@gmail.com"
}

// OkHttp client setup
private val okHttpClient = OkHttpClient.Builder()
  .connectTimeout(15, TimeUnit.SECONDS)
  .readTimeout(15, TimeUnit.SECONDS)
  .build()

private fun getTodayOtpCount(context: Context): Int {
  val prefs = context.getSharedPreferences("otp_counter_prefs", Context.MODE_PRIVATE)
  val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
  val todayStr = sdf.format(Date())
  val savedDate = prefs.getString("last_date", "")
  if (savedDate != todayStr) {
    prefs.edit().putString("last_date", todayStr).putInt("otp_count", 0).apply()
    return 0
  }
  return prefs.getInt("otp_count", 0)
}

private fun incrementTodayOtpCount(context: Context) {
  val prefs = context.getSharedPreferences("otp_counter_prefs", Context.MODE_PRIVATE)
  val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
  val todayStr = sdf.format(Date())
  val savedDate = prefs.getString("last_date", "")
  val currentCount = if (savedDate == todayStr) {
    prefs.getInt("otp_count", 0)
  } else {
    0
  }
  prefs.edit()
    .putString("last_date", todayStr)
    .putInt("otp_count", currentCount + 1)
    .apply()
}

fun applyWebViewProxy(context: Context, enabled: Boolean, host: String, port: String) {
  if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
    if (enabled && host.isNotBlank() && port.isNotBlank()) {
      val proxyConfig = ProxyConfig.Builder()
        .addProxyRule("http://$host:$port", "http")
        .addProxyRule("http://$host:$port", "https")
        .addDirect()
        .build()
      try {
        val executor = java.util.concurrent.Executor { command -> 
          android.os.Handler(android.os.Looper.getMainLooper()).post(command) 
        }
        ProxyController.getInstance().setProxyOverride(proxyConfig, executor, Runnable {
          // Success callback
        })
      } catch (e: Exception) {
        e.printStackTrace()
      }
    } else {
      try {
        val executor = java.util.concurrent.Executor { command -> 
          android.os.Handler(android.os.Looper.getMainLooper()).post(command) 
        }
        ProxyController.getInstance().clearProxyOverride(executor, Runnable {
          // Cleared callback
        })
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }
}

// Save to SharedPreferences history
private fun saveAccountToHistory(context: Context, phone: String, uid: String, cookies: String, password: String, otp: String) {
  val prefs = context.getSharedPreferences("fb_creator_prefs", Context.MODE_PRIVATE)
  val history = prefs.getStringSet("creation_history", emptySet())?.toMutableSet() ?: mutableSetOf()
  val timestamp = System.currentTimeMillis()
  val encodedCookies = Base64.encodeToString(cookies.toByteArray(), Base64.NO_WRAP)
  val entry = "$phone|$uid|$encodedCookies|$password|$otp|$timestamp"
  history.add(entry)
  prefs.edit().putStringSet("creation_history", history).apply()
}

// Reset and delete history after midnight (12:00 AM)
private fun checkAndResetHistoryAtMidnight(context: Context) {
  val prefs = context.getSharedPreferences("fb_creator_prefs", Context.MODE_PRIVATE)
  val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
  val todayStr = sdf.format(Date())
  val savedDate = prefs.getString("last_history_date", "") ?: ""
  if (savedDate.isNotEmpty() && savedDate != todayStr) {
    prefs.edit()
      .remove("creation_history")
      .putString("last_history_date", todayStr)
      .apply()
  } else if (savedDate.isEmpty()) {
    prefs.edit().putString("last_history_date", todayStr).apply()
  }
}

// Get SharedPreferences history
private fun getHistory(context: Context): List<HistoryItem> {
  checkAndResetHistoryAtMidnight(context)
  val prefs = context.getSharedPreferences("fb_creator_prefs", Context.MODE_PRIVATE)
  val history = prefs.getStringSet("creation_history", emptySet()) ?: emptySet()
  return history.mapNotNull { entry ->
    val parts = entry.split("|")
    if (parts.size >= 5) {
      val phone = parts[0]
      val uid = parts[1]
      val rawCookies = try {
        String(Base64.decode(parts[2], Base64.NO_WRAP))
      } catch (e: Exception) {
        parts[2]
      }
      val password = parts[3]
      val otp = parts[4]
      val timestamp = parts.getOrNull(5)?.toLongOrNull() ?: 0L
      HistoryItem(phone, uid, rawCookies, password, otp, timestamp)
    } else null
  }.sortedByDescending { it.timestamp }
}

// Update OTP in history
private fun updateOtpInHistory(context: Context, phone: String, otp: String) {
  val prefs = context.getSharedPreferences("fb_creator_prefs", Context.MODE_PRIVATE)
  val history = prefs.getStringSet("creation_history", emptySet())?.toMutableSet() ?: mutableSetOf()
  val updatedHistory = mutableSetOf<String>()
  for (entry in history) {
    val parts = entry.split("|")
    if (parts.isNotEmpty() && parts[0] == phone) {
      val uid = parts.getOrNull(1) ?: ""
      val cookies = parts.getOrNull(2) ?: ""
      val password = parts.getOrNull(3) ?: ""
      val timestamp = parts.getOrNull(5) ?: System.currentTimeMillis().toString()
      updatedHistory.add("$phone|$uid|$cookies|$password|$otp|$timestamp")
    } else {
      updatedHistory.add(entry)
    }
  }
  prefs.edit().putStringSet("creation_history", updatedHistory).apply()
}

// Clear all history
private fun clearHistory(context: Context) {
  val prefs = context.getSharedPreferences("fb_creator_prefs", Context.MODE_PRIVATE)
  prefs.edit().remove("creation_history").apply()
}

// Delete individual item from history
private fun deleteItemFromHistory(context: Context, item: HistoryItem) {
  val prefs = context.getSharedPreferences("fb_creator_prefs", Context.MODE_PRIVATE)
  val history = prefs.getStringSet("creation_history", emptySet())?.toMutableSet() ?: mutableSetOf()
  val updatedHistory = mutableSetOf<String>()
  for (entry in history) {
    val parts = entry.split("|")
    if (parts.size >= 5) {
      val phone = parts[0]
      val timestamp = parts.getOrNull(5)?.toLongOrNull() ?: 0L
      if (phone == item.phone && timestamp == item.timestamp) {
        // Skip this entry to delete it
      } else {
        updatedHistory.add(entry)
      }
    } else {
      updatedHistory.add(entry)
    }
  }
  prefs.edit().putStringSet("creation_history", updatedHistory).apply()
}

// Fetch Dynamic Button Names
fun fetchDynamicButtonNames(onSuccess: (ButtonNames) -> Unit) {
  val dbUrl = "https://fb-lite-pro-vr-default-rtdb.firebaseio.com/app_config/buttons.json"
  val request = okhttp3.Request.Builder().url(dbUrl).build()

  okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
    override fun onFailure(call: okhttp3.Call, e: IOException) {
      // do nothing on failure, keep existing
    }
    override fun onResponse(call: okhttp3.Call, response: Response) {
      response.use { res ->
        val bodyStr = res.body?.string() ?: return
        if (bodyStr == "null") return
        try {
          val json = JSONObject(bodyStr)
          val names = ButtonNames(
            btn_bot_creator = json.optString("btn_bot_creator", "Bot Creator"),
            btn_range = json.optString("btn_range", "Range"),
            btn_copy_cookie = json.optString("btn_copy_cookie", "Copy Cookie"),
            btn_copy_uid = json.optString("btn_copy_uid", "Copy UID"),
            btn_history = json.optString("btn_history", "History"),
            btn_cookie_login = json.optString("btn_cookie_login", "Cookie Login"),
            btn_gmail_copy = json.optString("btn_gmail_copy", "Gmail Copy"),
            btn_fb_login = json.optString("btn_fb_login", "FB Login"),
            btn_whoer_ip = json.optString("btn_whoer_ip", "Whoer IP"),
            btn_proxy_config = json.optString("btn_proxy_config", "Proxy Config"),
            btn_admin_ws = json.optString("btn_admin_ws", "Admin WS"),
            btn_admin_tg = json.optString("btn_admin_tg", "Admin TG"),
            btn_tg_channel = json.optString("btn_tg_channel", "TG Channel"),
            btn_verify = json.optString("btn_verify", "ভেরিফাই করুন"),
            btn_live_console = json.optString("btn_live_console", "Live Console"),
            btn_settings = json.optString("btn_settings", "Settings")
          )
          onSuccess(names)
        } catch (e: Exception) {
          // ignore
        }
      }
    }
  })
}

// Fetch live Facebook ranges
fun fetchFacebookRanges(onSuccess: (List<String>) -> Unit, onFailure: (String) -> Unit) {
  val request = Request.Builder()
    .url("${z.API_BASE_URL}/liveaccess")
    .addHeader("mauthapi", z.API_KEY)
    .addHeader("Content-Type", "application/json")
    .build()

  okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
    override fun onFailure(call: okhttp3.Call, e: IOException) {
      onFailure(e.message ?: "Unknown network error")
    }

    override fun onResponse(call: okhttp3.Call, response: Response) {
      response.use { res ->
        if (!res.isSuccessful) {
          onFailure("HTTP error ${res.code}")
          return
        }
        val bodyString = res.body?.string() ?: ""
        try {
          val json = JSONObject(bodyString)
          val meta = json.optJSONObject("meta")
          if (meta?.optInt("code") == 200) {
            val dataObj = json.optJSONObject("data")
            val services = dataObj?.optJSONArray("services")
            if (services != null) {
              for (i in 0 until services.length()) {
                val sObj = services.getJSONObject(i)
                val sid = sObj.optString("sid", "")
                if (sid.equals("Facebook", ignoreCase = true)) {
                  val rangesArray = sObj.optJSONArray("ranges")
                  val ranges = mutableListOf<String>()
                  if (rangesArray != null) {
                    for (j in 0 until rangesArray.length()) {
                      ranges.add(rangesArray.getString(j))
                    }
                  }
                  onSuccess(ranges)
                  return
                }
              }
            }
          }
          onFailure("Facebook service not found in API")
        } catch (e: Exception) {
          onFailure("Parsing error: ${e.message}")
        }
      }
    }
  })
}

// Request phone number for selected range
fun fetchNumber(rangeCode: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
  val rid = rangeCode.replace("XXX", "").replace("X", "").replace(" ", "").trim()
  val cleanRid = if (rid.isEmpty()) "8801" else rid

  val payload = JSONObject().put("rid", cleanRid).toString()
  val requestBody = payload.toRequestBody("application/json".toMediaTypeOrNull())

  val request = Request.Builder()
    .url("${z.API_BASE_URL}/getnum")
    .addHeader("mauthapi", z.API_KEY)
    .addHeader("Content-Type", "application/json")
    .post(requestBody)
    .build()

  okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
    override fun onFailure(call: okhttp3.Call, e: IOException) {
      onFailure(e.message ?: "Unknown network error")
    }

    override fun onResponse(call: okhttp3.Call, response: Response) {
      response.use { res ->
        if (!res.isSuccessful) {
          onFailure("HTTP error ${res.code}")
          return
        }
        val bodyString = res.body?.string() ?: ""
        try {
          val json = JSONObject(bodyString)
          val meta = json.optJSONObject("meta")
          if (meta?.optInt("code") == 200) {
            val dataObj = json.optJSONObject("data")
            if (dataObj != null) {
              val fullNumber = dataObj.optString("full_number", "")
                .ifEmpty { dataObj.optString("no_plus_number", "") }
              val cleanNumber = fullNumber.replace("+", "").replace(" ", "").trim()
              if (cleanNumber.isNotEmpty()) {
                onSuccess(cleanNumber)
                return
              }
            }
          }
          onFailure("রানিং কোনো নাম্বার পাওয়া যায়নি!")
        } catch (e: Exception) {
          onFailure("Parsing error: ${e.message}")
        }
      }
    }
  })
}

// Create actual Facebook account via submit reg
fun createFacebookAccount(
  context: Context,
  phone: String,
  passwordInput: String,
  country: String,
  onSuccess: (uid: String, name: String, cookies: String) -> Unit,
  onFailure: (String) -> Unit
) {
  val prefs = context.getSharedPreferences("fb_creator_prefs", Context.MODE_PRIVATE)
  val proxyHost = prefs.getString("proxy_host", "") ?: ""
  val proxyPort = prefs.getString("proxy_port", "") ?: ""
  val proxyUser = prefs.getString("proxy_user", "") ?: ""
  val proxyPass = prefs.getString("proxy_pass", "") ?: ""

  val clientBuilder = okHttpClient.newBuilder()
  
  // Automatically use proxy if host/port are provided
  if (proxyHost.isNotBlank() && proxyPort.isNotBlank()) {
    try {
      val proxy = java.net.Proxy(
        java.net.Proxy.Type.HTTP,
        java.net.InetSocketAddress(proxyHost, proxyPort.toInt())
      )
      clientBuilder.proxy(proxy)
      
      // Direct Authentication for Proxy if credentials exist
      if (proxyUser.isNotBlank() && proxyPass.isNotBlank()) {
          val credential = okhttp3.Credentials.basic(proxyUser, proxyPass)
          clientBuilder.proxyAuthenticator { _, response ->
              response.request.newBuilder()
                  .header("Proxy-Authorization", credential)
                  .build()
          }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  val activeClient = clientBuilder.build()

  val firstNames = when (country) {
    "Bangladesh" -> listOf("রাহিম", "করিম", "সাকিব", "শফিকুল", "আব্দুর", "মাসুদ", "আরিফ", "নাজমুল", "তারেক", "ফারুক", "হাসান", "কামরুল", "মেহেদী", "রনি", "সুমন", "রুবেল", "জসিম", "সোহেল", "ইমরান", "আকবর", "রাশেদ", "রিয়াজ", "মিজান", "আনোয়ার", "মাহমুদ", "হাবিব", "শাহিন", "তৌহিদ", "ফয়সাল", "সাগর", "শরীফ", "জাকির", "আলমগীর", "নূর", "তাজুল", "মহিউদ্দিন", "সেলিম", "রুহুল", "জালাল", "বাপ্পি", "শামিম", "সাইফুল", "তন্ময়", "পলাশ", "আশরাফ", "মুকুল", "লিটন", "শাহাদাত", "নাসির", "সাজ্জাদ")
    "United States" -> listOf("James", "John", "Robert", "Michael", "William", "David", "Richard", "Joseph", "Thomas", "Charles", "Christopher", "Daniel", "Matthew", "Anthony", "Donald", "Mark", "Paul", "Steven", "Andrew", "Kenneth", "Joshua", "Kevin", "Brian", "George", "Edward", "Ronald", "Timothy", "Jason", "Jeffrey", "Ryan", "Jacob", "Gary", "Nicholas", "Eric", "Jonathan", "Stephen", "Larry", "Justin", "Scott", "Brandon", "Benjamin", "Samuel", "Frank", "Gregory", "Raymond", "Alexander", "Patrick", "Jack", "Dennis", "Jerry")
    "China" -> listOf("雅婷", "雅君", "雅涵", "雅萱", "雅妤", "雅甄", "雅玲", "雅芬", "雅儀", "雅柔", "雅蓉", "雅瑄", "雅穎", "雅琪", "雅晴", "雅雯", "雅潔", "雅瑜", "雅蓁", "雅芸", "雅珊", "雅慈", "雅茹", "雅嫻", "雅薇", "雅彤", "雅恩", "雅榕", "雅媛", "雅寧", "怡婷", "怡君", "怡涵", "怡萱", "怡妤", "怡甄", "怡玲", "怡芬", "怡儀", "怡柔", "怡蓉", "怡瑄", "怡穎", "怡琪", "怡晴", "怡雯", "怡潔", "怡瑜", "怡蓁", "怡芸", "怡珊", "怡慈", "怡茹", "怡嫻", "怡薇", "怡彤", "怡恩", "怡榕", "怡媛", "怡寧", "欣婷", "欣君", "欣涵", "欣萱", "欣妤", "欣甄", "欣玲", "欣芬", "欣儀", "欣柔", "欣蓉", "欣瑄", "欣穎", "欣琪", "欣晴", "欣雯", "欣潔", "欣瑜")
    "Madagascar" -> listOf("Rakoto", "Rasoa", "Rabe", "Jean", "Marie", "Nirina", "Fanja", "Mialy", "Tiana", "Tahina", "Lova", "Andry", "Hery", "Rivo", "Tolotra", "Vola", "Bodo", "Mamy", "Njaka", "Mihaja", "Lanto", "Naivo", "Solo", "Rija", "Mahefa", "Tojo", "Kanto", "Aina", "Sitraka", "Zo", "Faly", "Haja", "Riana", "Tafita", "Mino", "Dina", "Ndriana", "Malala", "Hasina", "Ony", "Noro", "Tovo", "Zoly", "Ihary", "Lina", "Bako", "Nary", "Fidy", "Rado")
    else -> listOf("John", "Emma", "Michael", "Sophia", "David")
  }
  
  val lastNames = when (country) {
    "Bangladesh" -> listOf("তালুকদার", "রহমান", "খান", "চৌধুরী", "আহমেদ", "শেখ", "ইসলাম", "মোল্লা", "মন্ডল", "হাওলাদার", "সরকার", "শিকদার", "ভূঁইয়া", "মির্জা", "খন্দকার", "কাজী", "মজুমদার", "খাঁন", "সৈয়দ", "বিশ্বাস", "মিয়া", "মিয়াঁ", "বেপারী", "খলিফা", "মাহী", "হোসেন", "আকন্দ", "ফকির", "হক", "সানা", "মৃধা", "সৈয়াল", "ফরায়েজী", "মালী", "মুন্সী", "তপাদার", "পাটোয়ারী", "তরফদার", "মহাজন", "দাস", "সাহা", "কৰ্মকার", "দেবনাথ", "ভট্টাচার্য", "শীল", "পাল", "বনিক", "রায়", "মিত্র", "ঘোষ")
    "United States" -> listOf("Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson", "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker", "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores", "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell", "Carter", "Roberts")
    "China" -> listOf("池", "曾", "張", "熊", "謝", "馮", "彭", "鄧", "馬", "葉", "武", "向", "孫", "柯", "黎", "康", "陳", "林", "黃", "李", "王", "吳", "劉", "蔡", "楊", "許", "鄭", "郭", "洪", "邱", "廖", "賴", "徐", "周", "蘇", "莊", "呂", "江", "何", "蕭", "羅", "高", "潘", "簡", "朱", "鍾", "游", "詹", "胡", "施", "沉", "余", "盧", "趙", "梁", "顏", "翁", "魏", "戴", "方", "宋", "范", "杜", "傅", "侯", "曹", "薛", "丁", "卓", "董", "唐", "藍", "蔣", "石", "紀", "姚", "古", "連")
    "Madagascar" -> listOf("Andrianampy", "Ramanana", "Ravalomanana", "Andriamanjato", "Rakotomanga", "Randria", "Razafy", "Rasamimanana", "Ratsiraka", "Razafindrakoto", "Andriamandimby", "Rasoamanana", "Rabearivelo", "Razafimahatratra", "Rajoelina", "Rakotondrazaka", "Randrianasolo", "Ravelomanantsoa", "Rakotomalala", "Rasolofondraibe", "Rakotonirina", "Rahajanirina", "Rakotoniaina", "Rabemananjara", "Rakotondrabe", "Rakotoarisoa", "Andrianirina", "Rasoanaivo", "Razafindrabe", "Andriantsilavo", "Rakotovao", "Razafindratandra", "Randriamampionona", "Rakotondramanana", "Ramaroson", "Rakotoarivelo", "Randriamanantena", "Rasolomanana", "Razafindrakondro", "Andriamasinoro", "Rakotomandimby", "Razafimanantsoa", "Rakotoarimanana", "Rakotondrasoa", "Randriamanjaka", "Rasoarimalala", "Rakotozandriny", "Razafindramamba", "Rakotondravony", "Andriantsalama")
    else -> listOf("Smith", "Johnson", "Williams", "Jones", "Brown")
  }
  
  val fname = firstNames.random()
  val lname = lastNames.random()
  val day = (1..28).random().toString()
  val month = (1..12).random().toString()
  val year = (1985..2003).random().toString()
  
  val uuid1 = UUID.randomUUID().toString()
  val uuid2 = UUID.randomUUID().toString()
  
  val cookieDatr = "3XA5at-YBOFaGHi2xPrg-wka"
  val androidUa = "Mozilla/5.0 (Linux; Android 12; itel S665L Build/SP1A.210812.016) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.91 Mobile Safari/537.36"

  val formBody = FormBody.Builder()
    .add("ccp", "2")
    .add("reg_instance", cookieDatr)
    .add("submission_request", "true")
    .add("helper", "")
    .add("reg_impression_id", uuid1)
    .add("ns", "1")
    .add("zero_header_af_client", "")
    .add("app_id", "103")
    .add("logger_id", uuid2)
    .add("field_names[0]", "firstname")
    .add("firstname", fname)
    .add("lastname", lname)
    .add("field_names[1]", "birthday_wrapper")
    .add("birthday_day", day)
    .add("birthday_month", month)
    .add("birthday_year", year)
    .add("age_step_input", "")
    .add("did_use_age", "false")
    .add("field_names[2]", "reg_email__")
    .add("reg_email__", phone)
    .add("field_names[3]", "sex")
    .add("sex", "2")
    .add("preferred_pronoun", "")
    .add("custom_gender", "")
    .add("reg_passwd__", passwordInput)
    .add("name_suggest_elig", "false")
    .add("was_shown_name_suggestions", "false")
    .add("did_use_suggested_name", "false")
    .add("use_custom_gender", "false")
    .add("guid", "")
    .add("pre_form_step", "")
    .add("submit", "Sign up")
    .add("fb_dtsg", "NAfx5UxG44eai86HC1iwiixBs1mUDFhn3ccN1fj3-SJJc64TeUsEAEg:0:0")
    .add("jazoest", "24748")
    .add("lsd", "AdRCh7SdER7Za5PotUuics5fFt0")
    .add("__dyn", "1Z3pawlEnwm8_Bg9ppoW5UdE4a2i5U4e0C86u7E39x60zU3ex608ewk9E4W0pKq0FE6S0x81vohw73wGwcq1GwqU2YwbK0oi0zE1jU1soG0hi0Lo6-0Co1kU1UU3jwea")
    .add("__csr", "")
    .add("__hsdp", "")
    .add("__hblp", "")
    .add("__sjsp", "")
    .add("__req", "g")
    .add("__fmt", "1")
    .add("__a", "AYzJ_41FhHOHmeaJtz_y-NZ41BrpCkk8MZbenM7ATpRLY9c4d3QLNQW9sph6SN5jNJBH5tH1yvE_P-EybRqM6tZ_nqLEaV4b3ZU")
    .add("__user", "0")
    .build()

  val url = "${a.b1()}reg/submit/?privacy_mutation_token=eyJ0eXBlIjowLCJjcmVhdGlvbl90aW1lIjoxNzgyMTQ5MzY4LCJjYWxsc2l0ZV9pZCI6OTA3OTI0NDAyOTQ4MDU4fQ%3D%3D&app_id=103&multi_step_form=1&skip_suma=0&shouldForceMTouch=1"

  val request = Request.Builder()
    .url(url)
    .header("User-Agent", androidUa)
    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .header("Accept-Language", "fr-FR,fr;q=0.9,en;q=0.8")
    .header("Accept-Encoding", "gzip, deflate, br, zstd")
    .header("Connection", "keep-alive")
    .header("Upgrade-Insecure-Requests", "1")
    .header("sec-ch-ua-platform", "\"Android\"")
    .header("sec-ch-ua", "\"Android WebView\";v=\"149\", \"Chromium\";v=\"149\", \"Not)A;Brand\";v=\"24\"")
    .header("x-response-format", "JSONStream")
    .header("sec-ch-ua-mobile", "?1")
    .header("x-asbd-id", "359341")
    .header("x-fb-lsd", "AdRCh7SdER7Za5PotUuics5fFt0")
    .header("x-requested-with", "XMLHttpRequest")
    .header("origin", a.b1().dropLast(1))
    .header("sec-fetch-site", "same-origin")
    .header("sec-fetch-mode", "cors")
    .header("sec-fetch-dest", "empty")
    .header("referer", "${a.b1()}reg/?is_two_steps_login=0&cid=103&refsrc=deprecated&soft=hjk")
    .header("priority", "u=1, i")
    .header("Cookie", "datr=$cookieDatr")
    .post(formBody)
    .build()

  activeClient.newCall(request).enqueue(object : okhttp3.Callback {
    override fun onFailure(call: okhttp3.Call, e: IOException) {
      onFailure(e.message ?: "Account creation failed due to network error")
    }

    override fun onResponse(call: okhttp3.Call, response: Response) {
      response.use { res ->
        val headers = res.headers
        val cookiesList = headers.values("Set-Cookie")
        
        val cookieMap = mutableMapOf<String, String>()
        cookieMap["datr"] = cookieDatr

        for (cookieStr in cookiesList) {
          val cookieParts = cookieStr.split(";").firstOrNull()?.split("=")
          if (cookieParts != null && cookieParts.size == 2) {
            val key = cookieParts[0].trim()
            val value = cookieParts[1].trim()
            cookieMap[key] = value
          }
        }

        val cUser = cookieMap["c_user"]
        if (cUser != null) {
          val requiredKeys = listOf("datr", "sb", "ps_l", "ps_n", "m_pixel_ratio", "wd", "c_user", "fr", "xs")
          val cookiePartsList = mutableListOf<String>()
          for (k in requiredKeys) {
            val v = cookieMap[k]
            if (v != null) {
              cookiePartsList.add("$k=$v")
            }
          }
          val formattedCookies = cookiePartsList.joinToString("; ")
          onSuccess(cUser, "$fname $lname", formattedCookies)
        } else {
          onFailure("অ্যাকাউন্ট তৈরি করা যায়নি! ফেসবুক লিমিটেড থেকে c_user পাওয়া যায়নি।")
        }
      }
    }
  })
}

// Polling OTP list from API
fun checkOtpForPhone(phone: String, onSuccess: (String, String) -> Unit, onFailure: () -> Unit) {
  val request = Request.Builder()
    .url("${z.API_BASE_URL}/success-otp")
    .addHeader("mauthapi", z.API_KEY)
    .addHeader("Content-Type", "application/json")
    .build()

  okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
    override fun onFailure(call: okhttp3.Call, e: IOException) {
      onFailure()
    }

    override fun onResponse(call: okhttp3.Call, response: Response) {
      response.use { res ->
        if (!res.isSuccessful) {
          onFailure()
          return
        }
        val bodyString = res.body?.string() ?: ""
        try {
          val json = JSONObject(bodyString)
          val meta = json.optJSONObject("meta")
          if (meta?.optInt("code") == 200) {
            val dataObj = json.optJSONObject("data")
            val otps = dataObj?.optJSONArray("otps")
            if (otps != null) {
              val cleanTargetPhone = phone.replace("+", "").replace(" ", "").trim()
              for (i in 0 until otps.length()) {
                val otpItem = otps.getJSONObject(i)
                val otpNumber = otpItem.optString("number", "").replace("+", "").replace(" ", "").trim()
                if (otpNumber == cleanTargetPhone) {
                  val message = otpItem.optString("message", "")
                  if (message.isNotEmpty()) {
                    val otpCode = extractOtpFromText(message)
                    if (otpCode != "N/A") {
                      onSuccess(otpCode, message)
                      return
                    }
                  }
                }
              }
            }
          }
          onFailure()
        } catch (e: Exception) {
          onFailure()
        }
      }
    }
  })
}

private fun extractOtpFromText(text: String): String {
  val cleanText = text.replace("-", "").replace(" ", "")
  val patterns = listOf(
    Regex("\\b(\\d{8})\\b"),
    Regex("\\b(\\d{7})\\b"),
    Regex("\\b(\\d{6})\\b"),
    Regex("\\b(\\d{5})\\b"),
    Regex("\\b(\\d{4})\\b"),
    Regex("\\b(\\d{3})\\b"),
    Regex("code[:\\s]*(\\d+)", RegexOption.IGNORE_CASE),
    Regex("OTP[:\\s]*(\\d+)", RegexOption.IGNORE_CASE),
    Regex("(\\d+)")
  )
  for (pattern in patterns) {
    val match = pattern.find(cleanText)
    if (match != null) {
      val groupVal = match.groupValues.getOrNull(1) ?: match.value
      if (groupVal.length >= 3) {
        return groupVal
      }
    }
  }
  return "N/A"
}

private class AndroidIdInterface(val isEnabled: () -> Boolean, val context: Context) {
    @android.webkit.JavascriptInterface
    fun getAndroidId(): String {
        return if (isEnabled()) {
             Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "null"
        } else {
             "fake-android-id-12345"
        }
    }
}

@Composable
fun TargetedNoticeOverlay() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var noticeText by remember { mutableStateOf<String?>(null) }
    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Get the admin email from SharedPreferences (set in TypeResolver)
    val adminEmail = prefs.getString("my_admin_email", "owner") ?: "owner"
    val adminId = if (adminEmail == "owner") "owner" else adminEmail.lowercase().replace(".", ",")
    
    var lastNoticeTimestamp by remember { mutableStateOf(prefs.getLong("last_notice_ts_$adminId", 0L)) }

    LaunchedEffect(adminId) {
        while (true) {
            try {
                val auth = FirebaseAuth.getInstance()
                val token = try { auth.currentUser?.getIdToken(false)?.await()?.token } catch(e: Exception) { null }
                
                val client = OkHttpClient()
                val url = "https://all-admin-pnal-default-rtdb.firebaseio.com/notices/$adminId.json" + 
                    if (token != null) "?auth=$token" else ""
                
                val request = Request.Builder().url(url).build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                val body = response.body?.string() ?: ""
                response.close()
                
                if (body.isNotEmpty() && body != "null") {
                    val json = JSONObject(body)
                    val text = json.optString("text", "")
                    val ts = json.optLong("timestamp", 0L)
                    
                    if (ts > lastNoticeTimestamp && text.isNotEmpty()) {
                        noticeText = text
                        isVisible = true
                        lastNoticeTimestamp = ts
                        prefs.edit().putLong("last_notice_ts_$adminId", ts).apply()
                        
                        // Hide after 5 seconds
                        kotlinx.coroutines.delay(5000)
                        isVisible = false
                    }
                }
            } catch (e: Exception) { }
            kotlinx.coroutines.delay(10000) // Check every 10 seconds
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp).statusBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.fillMaxWidth().clickable { isVisible = false }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Notice from Admin", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(noticeText ?: "", color = Color.Black, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    try {
      if (FirebaseApp.getApps(this).isEmpty()) {
        try {
          FirebaseApp.initializeApp(this)
        } catch (innerEx: Exception) {
          val options = com.google.firebase.FirebaseOptions.Builder()
            .setApplicationId("1:171803187901:android:06f962b35043d369bcf5d4")
            .setApiKey("AIzaSyDoegs-mez3YrhxM_uzx6Q6vKqifR5FXYQ")
            .setDatabaseUrl("https://all-admin-pnal-default-rtdb.firebaseio.com")
            .setProjectId("my-original-apk")
            .setStorageBucket("my-original-apk.firebasestorage.app")
            .build()
          FirebaseApp.initializeApp(this, options)
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    a.c(this)

    // Set up global authenticator for authenticated proxies
    val prefs = getSharedPreferences("fb_creator_prefs", Context.MODE_PRIVATE)
    java.net.Authenticator.setDefault(object : java.net.Authenticator() {
      override fun getPasswordAuthentication(): java.net.PasswordAuthentication? {
        val proxyUser = prefs.getString("proxy_user", "") ?: ""
        val proxyPass = prefs.getString("proxy_pass", "") ?: ""
        val proxyHost = prefs.getString("proxy_host", "") ?: ""
        val proxyPort = prefs.getString("proxy_port", "") ?: ""
        
        // Auto-authenticate if proxy details are set
        if (proxyUser.isNotBlank() && proxyPass.isNotBlank()) {
          return java.net.PasswordAuthentication(proxyUser, proxyPass.toCharArray())
        }
        return null
      }
    })

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        TargetedNoticeOverlay()
        MainScreen()
      }
    }
  }
}

private fun getCookieValue(cookieString: String?, key: String): String? {
  if (cookieString.isNullOrBlank()) return null
  val cookies = cookieString.split(";")
  for (cookie in cookies) {
    val parts = cookie.trim().split("=", limit = 2)
    if (parts.size == 2 && parts[0].trim() == key) {
      return parts[1].trim()
    }
  }
  return null
}


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MainScreen() {
  val context = LocalContext.current
  val activity = context as? Activity
  
  var dynamicButtons by remember { mutableStateOf(ButtonNames()) }

  LaunchedEffect(Unit) {
    while(true) {
      fetchDynamicButtonNames { names ->
        dynamicButtons = names
      }
      kotlinx.coroutines.delay(2000)
    }
  }

  var isAppAuthorized by remember { mutableStateOf<Boolean?>(null) }

  val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
  var deviceNotApprovedDialog by remember { mutableStateOf(false) }
  var blockReason by remember { mutableStateOf("") }
  var expiryDateText by remember { mutableStateOf("") }
  var userName by remember { mutableStateOf("Unknown") }

  LaunchedEffect(Unit) {
    while(true) {
      val rawResult = a.checkStatusAndDevice(deviceId)
      if (rawResult == "GLOBAL_OFF") {
        activity?.finishAffinity()
        return@LaunchedEffect
      }
      
      val parts = rawResult.split("|")
      val result = parts[0]
      if (parts.size > 1) {
        userName = parts[1]
      }

      when {
        result == "BANNED" -> {
          blockReason = "BANNED"
          deviceNotApprovedDialog = true
          isAppAuthorized = false
        }
        result == "NOT_FOUND" -> {
          blockReason = "NOT_FOUND"
          deviceNotApprovedDialog = true
          isAppAuthorized = false
        }
        result.startsWith("EXPIRED_") -> {
          blockReason = "EXPIRED"
          val timestamp = result.removePrefix("EXPIRED_").toLongOrNull() ?: 0L
          expiryDateText = if (timestamp > 0) java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(timestamp)) else "N/A"
          deviceNotApprovedDialog = true
          isAppAuthorized = false
        }
        result.startsWith("APPROVED_") -> {
          isAppAuthorized = true
          deviceNotApprovedDialog = false
        }
      }
      kotlinx.coroutines.delay(3000)
    }
  }

  if (deviceNotApprovedDialog) {
    val mainTitle = when (blockReason) {
      "BANNED" -> "অ্যাকাউন্ট ব্যান করা হয়েছে"
      "EXPIRED" -> "ডিভাইসের মেয়াদ শেষ"
      else -> "ডিভাইস ভেরিফিকেশন প্রয়োজন"
    }
    val descText = when (blockReason) {
      "BANNED" -> "অ্যাডমিন আপনার ডিভাইসটি ব্লক করে দিয়েছেন।"
      "EXPIRED" -> "আপনার ডিভাইসের মেয়াদ শেষ হয়ে গেছে! অনুগ্রহ করে অ্যাডমিনের সাথে যোগাযোগ করুন।"
      else -> "আপনার ডিভাইসটি এখনো অনুমোদিত নয়। অ্যাডমিনের সাথে যোগাযোগ করুন।"
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF0D1117)) // Dark theme background
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        // Lock Icon
        Surface(
          shape = androidx.compose.foundation.shape.CircleShape,
          color = Color.Transparent,
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF58A6FF)),
          modifier = Modifier.size(64.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = Color(0xFF58A6FF),
              modifier = Modifier.size(32.dp)
            )
          }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(
          text = mainTitle,
          color = Color.White,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
        )
        
        Spacer(Modifier.height(24.dp))
        
        // Warning Box
        Surface(
          color = Color(0xFF1E1111), // Dark red background
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5534B)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              "ব্যবহারকারী: $userName",
              color = Color.White,
              fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
              text = descText,
              color = Color(0xFFE5534B), // Red text
              textAlign = TextAlign.Center,
              fontSize = 14.sp
            )
            if (blockReason == "EXPIRED" && expiryDateText.isNotEmpty()) {
              Spacer(Modifier.height(8.dp))
              Text(
                "(মেয়াদ উত্তীর্ণ: $expiryDateText)",
                color = Color(0xFFE5534B),
                fontSize = 12.sp
              )
            }
          }
        }

        Spacer(Modifier.height(16.dp))

        // Device ID Box
        Surface(
          color = Color(0xFF161B22), // Dark box
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("আপনার ডিভাইস আইডি (Device ID):", color = Color.White, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            
            // ID value area
            Surface(
              color = Color(0xFF0D1117),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF58A6FF)),
              shape = RoundedCornerShape(4.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = deviceId,
                color = Color(0xFF58A6FF),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(12.dp),
                fontSize = 16.sp
              )
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Copy Button
            Surface(
              color = Color(0xFF161B22),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D)),
              shape = RoundedCornerShape(4.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  val clip = ClipData.newPlainText("Device ID", deviceId)
                  clipboard.setPrimaryClip(clip)
                  Toast.makeText(context, "ডিভাইস আইডি কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                }
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.ContentCopy,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("ডিভাইস আইডি কপি করুন", color = Color.White)
              }
            }
          }
        }

        Spacer(Modifier.height(16.dp))

        // Action Buttons Row 1
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = {
              val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://wa.me/8801300349649"))
              context.startActivity(intent)
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)), // Green
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(dynamicButtons.btn_admin_ws)
          }
          
          Button(
            onClick = {
              val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("tg://resolve?domain=ornob24"))
              try { context.startActivity(intent) } catch (e: Exception) {
                val webIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/ornob24"))
                context.startActivity(webIntent)
              }
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F6FEB)), // Blue
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(dynamicButtons.btn_admin_tg)
          }
        }

        Spacer(Modifier.height(8.dp))

        // Action Buttons Row 2
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = {
              val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/+sjTCu5CFkscxNWU1"))
              context.startActivity(intent)
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDA3633)), // Red
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(dynamicButtons.btn_tg_channel)
          }

          Button(
            onClick = { 
              // Refresh action
              activity?.recreate()
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)), // Dark gray
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(dynamicButtons.btn_verify, color = Color.White)
          }
        }
      }
    }
    return
  }

  if (isAppAuthorized == null) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
      }
      return
  }

  val clipboardManager = LocalClipboardManager.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  
  var webView: WebView? by remember { mutableStateOf(null) }
  var isLoading by remember { mutableStateOf(false) }
  var progress by remember { mutableStateOf(0) }
  var currentUrl by remember { mutableStateOf(a.b1()) }
  var canGoBack by remember { mutableStateOf(false) }
  var canGoForward by remember { mutableStateOf(false) }

  // State variables for auto creation dialog
  val prefs = remember { context.getSharedPreferences("fb_creator_prefs", Context.MODE_PRIVATE) }
  var showCreatorDialog by remember { mutableStateOf(false) }
  var showHistoryDialog by remember { mutableStateOf(false) }
  var showCookieLoginDialog by remember { mutableStateOf(false) }
  var showSetRangeDialog by remember { mutableStateOf(false) }
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showLiveConsoleDialog by remember { mutableStateOf(false) }
  var isDesktopMode by remember { mutableStateOf(prefs.getBoolean("is_desktop_mode", false)) }
  var isProxyEnabled by remember { mutableStateOf(prefs.getBoolean("is_proxy_enabled", false)) }
  var dnsServer by remember { mutableStateOf("8.8.8.8") }
  
  var cookieLoginInput by remember { mutableStateOf("") }
  var rangesList by remember { mutableStateOf<List<String>>(emptyList()) }
  var selectedRange by remember { mutableStateOf(prefs.getString("saved_range", "") ?: "") }
  var isFetchingRanges by remember { mutableStateOf(false) }
  var activePhoneChecking by remember { mutableStateOf("") }
  var currentCreationStatus by remember { mutableStateOf("") }
  var isCreatingAccount by remember { mutableStateOf(false) }
  var lastCreatedPhone by remember { mutableStateOf("") }
  var lastCreatedUid by remember { mutableStateOf("") }
  var lastCreatedCookies by remember { mutableStateOf("") }
  var lastCreatedOtp by remember { mutableStateOf("") }
  var lastSuccessCookies by remember { mutableStateOf(prefs.getString("last_success_cookies", "") ?: "") }

  // Proxy state variables
  var proxyHost by remember { mutableStateOf(prefs.getString("proxy_host", "") ?: "") }
  var proxyPort by remember { mutableStateOf(prefs.getString("proxy_port", "") ?: "") }
  var proxyUser by remember { mutableStateOf(prefs.getString("proxy_user", "") ?: "") }
  var proxyPass by remember { mutableStateOf(prefs.getString("proxy_pass", "") ?: "") }
  var showProxyConfigDialog by remember { mutableStateOf(false) }
  var customPassword by remember { mutableStateOf(prefs.getString("saved_password", "") ?: "Pass@" + (1000..9999).random().toString()) }
  var selectedCountry by remember { mutableStateOf(prefs.getString("selected_country", "Bangladesh") ?: "Bangladesh") }

  // Loop for active OTP checking - checks every 2 seconds with a 20-minute timeout

  LaunchedEffect(activePhoneChecking) {
    if (activePhoneChecking.isNotEmpty()) {
      val startTime = System.currentTimeMillis()
      while (activePhoneChecking.isNotEmpty()) {
        // Timeout check: 20 minutes (20 * 60 * 1000 milliseconds)
        if (System.currentTimeMillis() - startTime > 20 * 60 * 1000L) {
          lastCreatedOtp = "number expired"
          updateOtpInHistory(context, activePhoneChecking, "number expired")
          activePhoneChecking = "" // Stop polling
          scope.launch {
            snackbarHostState.showSnackbar("ওটিপি সময়সীমা পার হয়েছে (20 মিনিট)")
          }
          break
        }

        checkOtpForPhone(
          phone = activePhoneChecking,
          onSuccess = { otp, msg ->
            clipboardManager.setText(AnnotatedString(otp))
            updateOtpInHistory(context, activePhoneChecking, otp)
            incrementTodayOtpCount(context)
            lastCreatedOtp = otp
            scope.launch {
              snackbarHostState.showSnackbar("OTP স্বয়ংক্রিয়ভাবে কপি করা হয়েছে: $otp")
            }
            activePhoneChecking = "" // Stop polling on success
          },
          onFailure = {
            // Just wait and continue
          }
        )
        kotlinx.coroutines.delay(2000)
      }
    }
  }

  // Loop for fetching ranges - updates every 10 seconds automatically from startup
  LaunchedEffect(Unit) {
/*
    isFetchingRanges = true
    while (true) {
      // ... (code)
      kotlinx.coroutines.delay(10000)
    }
*/
  }

  // Support system back press navigation inside WebView
  BackHandler(enabled = canGoBack) {
    webView?.goBack()
  }

  if (showLiveConsoleDialog) {
    var liveRanges by remember { mutableStateOf<List<String>>(emptyList()) }
    var isFetchingLiveRanges by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    fun fetchLive() {
       isFetchingLiveRanges = true
       errorMessage = ""
       fetchFacebookRanges(
         onSuccess = { r -> 
            liveRanges = r
            isFetchingLiveRanges = false
         },
         onFailure = { e ->
            errorMessage = e
            isFetchingLiveRanges = false
         }
       )
    }

    LaunchedEffect(Unit) {
       fetchLive()
    }

    AlertDialog(
      onDismissRequest = { showLiveConsoleDialog = false },
      title = {
         Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(dynamicButtons.btn_live_console, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            IconButton(onClick = { fetchLive() }) {
               Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
            }
         }
      },
      text = {
         Column(modifier = Modifier.fillMaxWidth()) {
            if (isFetchingLiveRanges) {
               CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
            } else if (errorMessage.isNotEmpty()) {
               Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
            } else if (liveRanges.isEmpty()) {
               Text("কোনো রেঞ্জ পাওয়া যায়নি।")
            } else {
               Text("Facebook Ranges (Click to copy):", fontSize = 14.sp, color = Color.Gray)
               Spacer(Modifier.height(8.dp))
               LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                  items(liveRanges.size) { index ->
                     val range = liveRanges[index]
                     Card(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(vertical = 4.dp)
                          .clickable {
                             clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(range))
                             selectedRange = range
                             prefs.edit().putString("saved_range", range).apply()
                             Toast.makeText(context, "Range Copied & Auto Setup!", Toast.LENGTH_SHORT).show()
                             showLiveConsoleDialog = false
                          },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                     ) {
                        Text(
                           text = range,
                           modifier = Modifier.padding(12.dp),
                           fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                           fontWeight = FontWeight.Bold,
                           fontSize = 14.sp
                        )
                     }
                  }
               }
            }
         }
      },
      confirmButton = {
         TextButton(onClick = { showLiveConsoleDialog = false }) {
            Text("Close")
         }
      }
    )
  }

  if (showSettingsDialog) {
    AlertDialog(
      onDismissRequest = { showSettingsDialog = false },
      title = { Text("Settings") },
      text = {
        Column {
            OutlinedTextField(
                value = dnsServer,
                onValueChange = { 
                    dnsServer = it 
                    prefs.edit().putString("saved_dns", it).apply()
                },
                label = { Text("DNS Server (Internal Networking)") }
            )
        }
      },
      confirmButton = {
        TextButton(onClick = { showSettingsDialog = false }) { Text("OK") }
      }
    )
  }
  
  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      TopAppBar(
        windowInsets = WindowInsets(0, 0, 0, 0),
        title = {
          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Secure Connection",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "FB Limited",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Text(
              text = if (isLoading) "লোড হচ্ছে... $progress%" else "নিরাপদ সংযোগ",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = if (isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        },
        actions = {
          TextButton(onClick = { showLiveConsoleDialog = true }) {
            Icon(Icons.Default.Terminal, contentDescription = "Live Console", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(dynamicButtons.btn_live_console, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
          IconButton(onClick = { showSettingsDialog = true }) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        )
      )
    },
    bottomBar = {
      Surface(
        tonalElevation = 4.dp,
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp),
          verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
          // Top Row: Navigation on left, Refresh and Clear Data on right
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            // Navigation Group (Back, Forward, Home)
            Row(
              horizontalArrangement = Arrangement.spacedBy(2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              IconButton(
                onClick = { webView?.goBack() },
                enabled = canGoBack,
                modifier = Modifier
                  .size(28.dp)
                  .testTag("back_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "পেছনে",
                  modifier = Modifier.size(16.dp)
                )
              }
              IconButton(
                onClick = { webView?.goForward() },
                enabled = canGoForward,
                modifier = Modifier
                  .size(28.dp)
                  .testTag("forward_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                  contentDescription = "সামনে",
                  modifier = Modifier.size(16.dp)
                )
              }
              IconButton(
                onClick = {
                  webView?.loadUrl(a.b1())
                },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Home,
                  contentDescription = "হোম",
                  modifier = Modifier.size(16.dp),
                  tint = MaterialTheme.colorScheme.onSurface
                )
              }
            }

            // Compact Actions Group (Reload, Clear Data)
            Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Small Reload Button
              Button(
                onClick = { webView?.reload() },
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.secondaryContainer,
                  contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier
                  .height(25.dp)
                  .testTag("reload_button")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "রিলোড",
                    modifier = Modifier.size(12.dp)
                  )
                  Text(
                    text = "রিলোড",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold)
                  )
                }
              }

              // Small Clear Data Button
              Button(
                onClick = {
                  webView?.let { wv ->
                    // 1. Clear cookies
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.removeAllCookies { success ->
                      cookieManager.flush()
                    }
                    cookieManager.removeSessionCookies(null)

                    // 2. Clear storage & cache
                    WebStorage.getInstance().deleteAllData()
                    wv.clearCache(true)
                    wv.clearFormData()
                    wv.clearHistory()

                    // 3. Change useragent based on desktop mode setting
                    if (isDesktopMode) {
                      wv.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    } else {
                      wv.settings.userAgentString = getRandomMobileUserAgent()
                    }
                    wv.loadUrl(a.b1())

                    // 5. Show success info
                    scope.launch {
                      snackbarHostState.showSnackbar(
                        message = "সমস্ত ডাটা ও কুকি মুছে ফেলা হয়েছে!",
                        actionLabel = "ঠিক আছে",
                        duration = SnackbarDuration.Short
                      )
                    }
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.errorContainer,
                  contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier
                  .height(25.dp)
                  .testTag("clear_data_button")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "ডাটা মুছুন",
                    modifier = Modifier.size(12.dp)
                  )
                  Text(
                    text = "ডাটা মুছুন",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold)
                  )
                }
              }
            }
          }

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
          ) {
            // Bottom Rows: Ultra Compact Button Sizes
            val compactBtnPadding = PaddingValues(horizontal = 2.dp, vertical = 1.dp)
            val compactTextStyle = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 9.sp
            )
            val compactIconSize = 12.dp
            val compactBtnHeight = 28.dp

            // Row 0: Start (Main Feature)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Button(
                onClick = {
                  scope.launch {
                    // 1. Clear Data & Initial Load
                    webView?.let { wv ->
                      val cookieManager = CookieManager.getInstance()
                      cookieManager.removeAllCookies { }
                      cookieManager.flush()
                      WebStorage.getInstance().deleteAllData()
                      wv.clearCache(true)
                      wv.loadUrl(a.b1())
                    }
                    
                    kotlinx.coroutines.delay(2000)

                    // 2. Auto Login with last success cookies
                    if (lastSuccessCookies.isNotBlank()) {
                      val cookieManager = CookieManager.getInstance()
                      cookieManager.setAcceptCookie(true)
                      val domains = listOf(
                        "https://.facebook.com", "https://facebook.com",
                        "https://m.facebook.com", "https://limited.facebook.com"
                      )
                      val trimmed = lastSuccessCookies.trim()
                      if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                        try {
                          val jsonArray = JSONArray(trimmed)
                          for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val name = obj.optString("name")
                            val value = obj.optString("value")
                            val domain = obj.optString("domain", ".facebook.com")
                            val path = obj.optString("path", "/")
                            if (name.isNotEmpty()) {
                              val cookieString = "$name=$value; Domain=$domain; Path=$path"
                              domains.forEach { d -> cookieManager.setCookie(d, cookieString) }
                            }
                          }
                        } catch (e: Exception) {}
                      } else {
                        val parts = trimmed.split(";")
                        for (part in parts) {
                          val cleanPart = part.trim()
                          if (cleanPart.isNotEmpty() && cleanPart.contains("=")) {
                            domains.forEach { d -> cookieManager.setCookie(d, "$cleanPart; Domain=.facebook.com; Path=/") }
                          }
                        }
                      }
                      cookieManager.flush()
                    }

                    // 3. Forced Desktop Mode ON -> Load
                    webView?.let { wv ->
                      wv.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                      wv.loadUrl(a.b1())
                    }
                    kotlinx.coroutines.delay(3000)

                    // 4. Reload to final state (Mobile Mode)
                    webView?.let { wv ->
                      isDesktopMode = false
                      prefs.edit().putBoolean("is_desktop_mode", false).apply()
                      wv.settings.userAgentString = getRandomMobileUserAgent()
                      wv.reload()
                    }
                    
                    snackbarHostState.showSnackbar("Process Completed!")
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF6366F1), // Indigo
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(2.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(compactBtnHeight)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Start",
                    modifier = Modifier.size(compactIconSize).padding(end = 4.dp)
                  )
                  Text(text = "Start (Clear, Login, Mode Fix)", style = compactTextStyle)
                }
              }
            }

            // Row 1: Tools
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Button(
                onClick = { showCreatorDialog = true },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFFF97316), // Orange
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(2.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = dynamicButtons.btn_bot_creator, style = compactTextStyle, maxLines = 1)
              }
              Button(
                onClick = { showSetRangeDialog = true },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF8B5CF6), // Violet
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(2.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = if (selectedRange.isEmpty()) dynamicButtons.btn_range else selectedRange, style = compactTextStyle, maxLines = 1)
              }
              Button(
                onClick = {
                  val cookies = CookieManager.getInstance().getCookie(webView?.url ?: a.b1())
                  if (!cookies.isNullOrEmpty()) {
                    clipboardManager.setText(AnnotatedString(cookies))
                    scope.launch { snackbarHostState.showSnackbar("Cookies Copied!") }
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF3B82F6), // Vibrant Blue
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(2.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = dynamicButtons.btn_copy_cookie, style = compactTextStyle, maxLines = 1)
              }
            }

            // Row 2: Account
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Button(
                onClick = {
                  val cookies = CookieManager.getInstance().getCookie(webView?.url ?: a.b1())
                  val uid = cookies?.split(";")?.firstOrNull { it.trim().startsWith("c_user=") }?.substringAfter("=")?.trim()
                  if (!uid.isNullOrEmpty()) {
                    clipboardManager.setText(AnnotatedString(uid))
                    scope.launch { snackbarHostState.showSnackbar("UID ($uid) Copied!") }
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF06B6D4), // Deep Cyan/Teal
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(2.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = dynamicButtons.btn_copy_uid, style = compactTextStyle, maxLines = 1)
              }
              Button(
                onClick = { showHistoryDialog = true },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFFF43F5E), // Rose/Pink
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(2.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = dynamicButtons.btn_history, style = compactTextStyle, maxLines = 1)
              }
              Button(
                onClick = { showCookieLoginDialog = true },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFFD97706), // Amber/Golden
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(2.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = dynamicButtons.btn_cookie_login, style = compactTextStyle, maxLines = 1)
              }
            }

            // Row 3: Others
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Button(
                onClick = {
                  val randomGmail = generateRandomGmail()
                  clipboardManager.setText(AnnotatedString(randomGmail))
                  scope.launch { snackbarHostState.showSnackbar("Gmail Copied!") }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF10B981), // Vibrant Green/Emerald
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(2.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = dynamicButtons.btn_gmail_copy, style = compactTextStyle, maxLines = 1)
              }
              Button(
                onClick = { webView?.loadUrl("https://m.facebook.com/login/") },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF1877F2), // Official Facebook Blue
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(2.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = dynamicButtons.btn_fb_login, style = compactTextStyle, maxLines = 1)
              }
              Button(
                onClick = { webView?.loadUrl("https://whoer.net/") },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFFD946EF), // Fuchsia/Pink-Purple
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(2.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = dynamicButtons.btn_whoer_ip, style = compactTextStyle, maxLines = 1)
              }
            }

            // Row 4: Proxy & Desktop Mode
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Button(
                onClick = { showProxyConfigDialog = true },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF64748B), // Slate Grey
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(2.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = dynamicButtons.btn_proxy_config, style = compactTextStyle, maxLines = 1)
              }
              Button(
                onClick = {
                  isProxyEnabled = !isProxyEnabled
                  prefs.edit().putBoolean("is_proxy_enabled", isProxyEnabled).apply()
                  applyWebViewProxy(context, isProxyEnabled, proxyHost, proxyPort)
                  scope.launch {
                    snackbarHostState.showSnackbar(if (isProxyEnabled) "Proxy Enabled" else "Proxy Disabled")
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isProxyEnabled) Color(0xFF22C55E) else Color(0xFFEF4444), // Vibrant Green or Red
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(2.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = if (isProxyEnabled) "Proxy: ON" else "Proxy: OFF", style = compactTextStyle, maxLines = 1)
              }
              Button(
                onClick = {
                  isDesktopMode = !isDesktopMode
                  prefs.edit().putBoolean("is_desktop_mode", isDesktopMode).apply()
                  webView?.let { wv ->
                    if (isDesktopMode) {
                      wv.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    } else {
                      wv.settings.userAgentString = getRandomMobileUserAgent()
                    }
                    wv.reload()
                  }
                  scope.launch {
                    snackbarHostState.showSnackbar(if (isDesktopMode) "Desktop Mode ON" else "Desktop Mode OFF")
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isDesktopMode) Color(0xFFF59E0B) else Color(0xFF475569), // Bright Amber/Orange or Deep Slate
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(2.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = if (isDesktopMode) "Desktop: ON" else "Desktop: OFF", style = compactTextStyle, maxLines = 1)
              }
            }
          }
        }
      }
    }
  ) { innerPadding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {

      // Elegant slim loading indicator
      AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        LinearProgressIndicator(
          progress = { progress / 100f },
          modifier = Modifier.fillMaxWidth(),
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
      }

      Box(
        modifier = Modifier
          .fillMaxSize()
          .weight(1f)
          .background(MaterialTheme.colorScheme.background)
      ) {
        AndroidView(
          factory = { context ->
            WebView(context).apply {
              layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
              )
              
              // Essential settings for facebook and standard web apps
              settings.javaScriptEnabled = true
              settings.domStorageEnabled = true
              settings.databaseEnabled = true
              settings.useWideViewPort = true
              settings.loadWithOverviewMode = true
              settings.setSupportZoom(true)
              settings.builtInZoomControls = true
              settings.displayZoomControls = false
              
              // Set Initial User Agent (Desktop or Mobile based on configuration)
              if (isDesktopMode) {
                settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
              } else {
                settings.userAgentString = getRandomMobileUserAgent()
              }
              
              // Android ID Interface
              addJavascriptInterface(AndroidIdInterface({ true }, context), "AndroidIDInterface")

              // Accept Third-Party Cookies
              val cookieManager = CookieManager.getInstance()
              cookieManager.setAcceptCookie(true)
              cookieManager.setAcceptThirdPartyCookies(this, true)

              webViewClient = object : WebViewClient() {
                override fun onReceivedHttpAuthRequest(
                  view: WebView?,
                  handler: android.webkit.HttpAuthHandler?,
                  host: String?,
                  realm: String?
                ) {
                  if (isProxyEnabled && proxyHost.isNotBlank() && proxyPort.isNotBlank() && proxyUser.isNotBlank() && proxyPass.isNotBlank()) {
                    handler?.proceed(proxyUser, proxyPass)
                  } else {
                    super.onReceivedHttpAuthRequest(view, handler, host, realm)
                  }
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                  super.onPageStarted(view, url, favicon)
                  isLoading = true
                  currentUrl = url ?: ""
                  canGoBack = view?.canGoBack() ?: false
                  canGoForward = view?.canGoForward() ?: false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                  super.onPageFinished(view, url)
                  isLoading = false
                  currentUrl = url ?: ""
                  canGoBack = view?.canGoBack() ?: false
                  canGoForward = view?.canGoForward() ?: false
                }

                private fun handleCustomUri(view: WebView?, urlString: String): Boolean {
                  if (urlString.startsWith("http://") || urlString.startsWith("https://")) {
                    return false
                  }
                  try {
                    val intent = android.content.Intent.parseUri(urlString, android.content.Intent.URI_INTENT_SCHEME)
                    if (intent != null) {
                      val packageManager = context.packageManager
                      val info = packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                      if (info != null) {
                        context.startActivity(intent)
                      } else {
                        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                        if (fallbackUrl != null) {
                          view?.loadUrl(fallbackUrl)
                        }
                      }
                      return true
                    }
                  } catch (e: Exception) {
                    try {
                      val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(urlString))
                      context.startActivity(intent)
                      return true
                    } catch (ex: Exception) {
                      ex.printStackTrace()
                    }
                  }
                  return true
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                  val urlString = request?.url?.toString() ?: return false
                  return handleCustomUri(view, urlString)
                }

                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                  if (url == null) return false
                  return handleCustomUri(view, url)
                }
              }

              webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                  super.onProgressChanged(view, newProgress)
                  progress = newProgress
                  canGoBack = view?.canGoBack() ?: false
                  canGoForward = view?.canGoForward() ?: false
                }
              }

              loadUrl(a.b1())
              webView = this
            }
          },
          modifier = Modifier
            .fillMaxSize()
            .testTag("webview")
        )
      }
    }
  }

  // Creator Dialog
  if (showCreatorDialog) {
    AlertDialog(
      onDismissRequest = { showCreatorDialog = false },
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Android,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "অটো অ্যাকাউন্ট ক্রিয়েটর",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
          )
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "রেন্জ: $selectedRange",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          // Country Selector
          var countryExpanded by remember { mutableStateOf(false) }
          val countries = listOf("Bangladesh", "United States", "China", "Madagascar")
          
          ExposedDropdownMenuBox(
            expanded = countryExpanded,
            onExpandedChange = { countryExpanded = !countryExpanded }
          ) {
            OutlinedTextField(
              value = selectedCountry,
              onValueChange = { },
              readOnly = true,
              label = { Text("একাউন্ট নেম টাইপ (Country)") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded) },
              colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
              modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
              expanded = countryExpanded,
              onDismissRequest = { countryExpanded = false }
            ) {
              countries.forEach { selectionOption ->
                DropdownMenuItem(
                  text = { Text(selectionOption) },
                  onClick = {
                    selectedCountry = selectionOption
                    prefs.edit().putString("selected_country", selectionOption).apply()
                    countryExpanded = false
                  }
                )
              }
            }
          }

          // Editable password field
          OutlinedTextField(
            value = customPassword,
            onValueChange = { 
              customPassword = it
              prefs.edit().putString("saved_password", it).apply()
            },
            label = { Text("অ্যাকাউন্ট পাসওয়ার্ড") },
            leadingIcon = { Icon(imageVector = Icons.Default.Key, contentDescription = null) },
            trailingIcon = {
              IconButton(onClick = { 
                val newPass = "Pass@" + (1000..9999).random().toString()
                customPassword = newPass
                prefs.edit().putString("saved_password", newPass).apply()
              }) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "নতুন পাসওয়ার্ড জেনারেট করুন")
              }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          // Creator button
          Button(
            onClick = {
              if (selectedRange.isEmpty()) {
                currentCreationStatus = "please enter range"
                return@Button
              }
              isCreatingAccount = true
              
              currentCreationStatus = "account creating..."
              lastCreatedPhone = ""
              lastCreatedUid = ""
              lastCreatedCookies = ""
              lastCreatedOtp = ""

              fetchNumber(
                rangeCode = selectedRange,
                onSuccess = { phoneNumber ->
                  scope.launch {
                    lastCreatedPhone = phoneNumber
                    createFacebookAccount(
                      context = context,
                      phone = phoneNumber,
                      passwordInput = customPassword,
                      country = selectedCountry,
                      onSuccess = { uid, name, cookies ->
                        scope.launch {
                          lastCreatedUid = uid
                          lastCreatedCookies = cookies
                          currentCreationStatus = "create success"
                          saveAccountToHistory(context, phoneNumber, uid, cookies, customPassword, "")
                          lastSuccessCookies = cookies
                          prefs.edit().putString("last_success_cookies", cookies).apply()
                          activePhoneChecking = phoneNumber // Start OTP checking loop
                          isCreatingAccount = false

                          // Automatic Cookie Login on Successful Creation
                          if (cookies.isNotEmpty()) {
                            try {
                              val cookieManager = CookieManager.getInstance()
                              cookieManager.setAcceptCookie(true)
                              cookieManager.removeAllCookies(null)
                              
                              val domains = listOf(
                                "https://.facebook.com",
                                "https://facebook.com",
                                "https://m.facebook.com",
                                "https://limited.facebook.com"
                              )
                              
                              val trimmed = cookies.trim()
                              if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                                val jsonArray = org.json.JSONArray(trimmed)
                                for (i in 0 until jsonArray.length()) {
                                  val obj = jsonArray.getJSONObject(i)
                                  val cookieName = obj.optString("name")
                                  val cookieValue = obj.optString("value")
                                  val domain = obj.optString("domain", ".facebook.com")
                                  val path = obj.optString("path", "/")
                                  if (cookieName.isNotEmpty()) {
                                    val cookieString = "$cookieName=$cookieValue; Domain=$domain; Path=$path"
                                    domains.forEach { d ->
                                      cookieManager.setCookie(d, cookieString)
                                    }
                                  }
                                }
                              } else {
                                val parts = trimmed.split(";")
                                for (part in parts) {
                                  val cleanPart = part.trim()
                                  if (cleanPart.isNotEmpty() && cleanPart.contains("=")) {
                                    domains.forEach { d ->
                                      cookieManager.setCookie(d, "$cleanPart; Domain=.facebook.com; Path=/")
                                    }
                                  }
                                }
                              }
                              cookieManager.flush()
                              // Ensure proxy is applied to WebView during automatic login if settings exist
                              if (isProxyEnabled && proxyHost.isNotBlank() && proxyPort.isNotBlank()) {
                                applyWebViewProxy(context, true, proxyHost, proxyPort)
                              }
                              webView?.loadUrl(a.b1())
                              snackbarHostState.showSnackbar("অ্যাকাউন্ট তৈরি সফল! অটোমেটিক লগইন করা হচ্ছে...")
                            } catch (e: Exception) {
                              e.printStackTrace()
                            }
                          }
                        }
                      },
                      onFailure = { errorMsg ->
                        scope.launch {
                          currentCreationStatus = "create failed"
                          saveAccountToHistory(context, phoneNumber, "N/A", "N/A", customPassword, "তৈরি ব্যর্থ")
                          isCreatingAccount = false
                        }
                      }
                    )
                  }
                },
                onFailure = { errorMsg ->
                  scope.launch {
                    currentCreationStatus = "create failed"
                    isCreatingAccount = false
                  }
                }
              )
            },
            enabled = !isCreatingAccount && selectedRange.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          ) {
            if (isCreatingAccount) {
              CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(8.dp))
            }
            Text("অ্যাকাউন্ট তৈরি করুন")
          }

          // Creation Status / Logs
          if (currentCreationStatus.isNotEmpty()) {
            Card(
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
              ),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = currentCreationStatus,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = if (currentCreationStatus == "create success") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          // OTP received block
          if (lastCreatedPhone.isNotEmpty()) {
            Card(
              colors = CardDefaults.cardColors(
                containerColor = if (lastCreatedOtp.isNotEmpty() && lastCreatedOtp != "Expired" && lastCreatedOtp != "number expired") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
              ),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                if (lastCreatedOtp.isNotEmpty()) {
                  val isExpired = lastCreatedOtp == "Expired" || lastCreatedOtp == "number expired"
                  Text(
                    text = if (isExpired) "number expired" else "ওটিপি: $lastCreatedOtp",
                    style = MaterialTheme.typography.titleLarge.copy(
                      fontWeight = FontWeight.Bold,
                      color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                  )
                } else {
                  CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
              }
            }
          }

          // Display 3 copy buttons (only after successful account creation)
          if (lastCreatedUid.isNotEmpty() && lastCreatedUid != "N/A" && lastCreatedCookies.isNotEmpty() && lastCreatedCookies != "N/A") {
            Row(
              modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Button(
                onClick = {
                  clipboardManager.setText(AnnotatedString(lastCreatedPhone))
                  scope.launch { snackbarHostState.showSnackbar("নাম্বার কপি করা হয়েছে!") }
                },
                contentPadding = PaddingValues(horizontal = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f).height(38.dp),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text("নাম্বার কপি", style = MaterialTheme.typography.labelSmall)
              }

              Button(
                onClick = {
                  clipboardManager.setText(AnnotatedString(lastCreatedUid))
                  scope.launch { snackbarHostState.showSnackbar("UID কপি করা হয়েছে!") }
                },
                contentPadding = PaddingValues(horizontal = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.weight(1f).height(38.dp),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text("UID কপি", style = MaterialTheme.typography.labelSmall)
              }

              Button(
                onClick = {
                  clipboardManager.setText(AnnotatedString(lastCreatedCookies))
                  scope.launch { snackbarHostState.showSnackbar("কুকি কপি করা হয়েছে!") }
                },
                contentPadding = PaddingValues(horizontal = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                modifier = Modifier.weight(1f).height(38.dp),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text("কুকি কপি", style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showCreatorDialog = false }) {
          Text("বন্ধ করুন")
        }
      }
    )
  }

  // Set Range Dialog
  if (showSetRangeDialog) {
    AlertDialog(
      onDismissRequest = { showSetRangeDialog = false },
      title = { Text("রেঞ্জ সেট করুন") },
      text = {
        OutlinedTextField(
          value = selectedRange,
          onValueChange = { 
            selectedRange = it
            prefs.edit().putString("saved_range", it).apply()
          },
          label = { Text("রেঞ্জ লিখুন (যেমন: 22508XXXX)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
      },
      confirmButton = {
        Button(onClick = { 
          prefs.edit().putString("saved_range", selectedRange).apply()
          showSetRangeDialog = false 
        }) {
          Text("সেভ করুন")
        }
      }
    )
  }

  if (showCookieLoginDialog) {
    AlertDialog(
      onDismissRequest = { showCookieLoginDialog = false },
      title = { Text("Login with Cookies") },
      text = {
        OutlinedTextField(
          value = cookieLoginInput,
          onValueChange = { cookieLoginInput = it },
          label = { Text("Paste Cookies Here") },
          modifier = Modifier.fillMaxWidth(),
          minLines = 3
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (cookieLoginInput.isNotEmpty()) {
              lastSuccessCookies = cookieLoginInput
              prefs.edit().putString("last_success_cookies", cookieLoginInput).apply()
              val cookieManager = CookieManager.getInstance()
              cookieManager.setAcceptCookie(true)
              cookieManager.removeAllCookies(null)
              
              val domains = listOf(
                "https://.facebook.com",
                "https://facebook.com",
                "https://m.facebook.com",
                "https://limited.facebook.com"
              )
              
              val trimmed = cookieLoginInput.trim()
              if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                try {
                  val jsonArray = org.json.JSONArray(trimmed)
                  for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val name = obj.optString("name")
                    val value = obj.optString("value")
                    val domain = obj.optString("domain", ".facebook.com")
                    val path = obj.optString("path", "/")
                    if (name.isNotEmpty()) {
                      val cookieString = "$name=$value; Domain=$domain; Path=$path"
                      domains.forEach { d ->
                        cookieManager.setCookie(d, cookieString)
                      }
                    }
                  }
                } catch (e: Exception) {
                  e.printStackTrace()
                }
              } else {
                val parts = trimmed.split(";")
                for (part in parts) {
                  val cleanPart = part.trim()
                  if (cleanPart.isNotEmpty() && cleanPart.contains("=")) {
                    domains.forEach { d ->
                      cookieManager.setCookie(d, "$cleanPart; Domain=.facebook.com; Path=/")
                    }
                  }
                }
              }
              cookieManager.flush()
              webView?.loadUrl(a.b1())
              scope.launch {
                snackbarHostState.showSnackbar("Cookie set and reloading!")
              }
            }
            showCookieLoginDialog = false
            cookieLoginInput = ""
          }
        ) {
          Text("Login")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCookieLoginDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // History Dialog
  if (showHistoryDialog) {
    val historyItems = remember { mutableStateOf(getHistory(context)) }

    AlertDialog(
      onDismissRequest = { showHistoryDialog = false },
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.History,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = "ওটিপি ও অ্যাকাউন্ট ইতিহাস",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1
            )
          }
          
          Button(
            onClick = {
              val textToCopy = historyItems.value
                .filter { it.otp.trim().isNotEmpty() }
                .joinToString("\n") { "${it.phone}\t${it.otp}" }
              if (textToCopy.isNotEmpty()) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Copied OTPs", textToCopy)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "আজকের ওটিপি কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
              } else {
                Toast.makeText(context, "কপি করার মতো কোনো ওটিপি পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
              }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFF10B981), // Emerald/Green
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            modifier = Modifier.height(30.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy OTP",
                modifier = Modifier.size(12.dp)
              )
              Text(
                text = "Copy",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Total OTP today display
          val todayOtpCount = remember { getTodayOtpCount(context) }
          Card(
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Total otp $todayOtpCount",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 330.dp)
          ) {
          if (historyItems.value.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "কোনো অ্যাকাউন্ট বা ওটিপি রেকর্ড পাওয়া যায়নি।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              items(historyItems.value) { item ->
                Card(
                  colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                  ),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      val formattedTime = try {
                        val sdf = SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault())
                        sdf.format(Date(item.timestamp))
                      } catch (e: Exception) {
                        "N/A"
                      }
                      Text(
                        text = "সময়: $formattedTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                      )
                    }

                    Text(
                      text = "নাম্বার: " + item.phone,
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Text(
                        text = "ওটিপি কোড: ",
                        style = MaterialTheme.typography.bodyMedium
                      )
                      if (item.otp.isNotEmpty()) {
                        val isExpired = item.otp == "Expired" || item.otp == "number expired"
                        Text(
                          text = if (isExpired) "number expired" else item.otp,
                          style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                          )
                        )
                        if (!isExpired) {
                          IconButton(
                            onClick = {
                              clipboardManager.setText(AnnotatedString(item.otp))
                              scope.launch { snackbarHostState.showSnackbar("OTP কপি করা হয়েছে!") }
                            },
                            modifier = Modifier.size(24.dp)
                          ) {
                            Icon(
                              imageVector = Icons.Default.ContentCopy,
                              contentDescription = "OTP কপি করুন",
                              modifier = Modifier.size(14.dp)
                            )
                          }
                        }
                      } else {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                          CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.dp)
                          Text(
                            text = "অপেক্ষারত / পাওয়া যায়নি",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                          )
                        }
                      }
                    }

                    // Copy action row
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Button(
                        onClick = {
                          clipboardManager.setText(AnnotatedString(item.phone))
                          scope.launch { snackbarHostState.showSnackbar("নাম্বার কপি করা হয়েছে!") }
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.weight(1f).height(30.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(4.dp)
                      ) {
                        Text("নাম্বার কপি", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                      }

                      Button(
                        onClick = {
                          clipboardManager.setText(AnnotatedString(item.uid))
                          scope.launch { snackbarHostState.showSnackbar("UID কপি করা হয়েছে!") }
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.weight(1f).height(30.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(4.dp)
                      ) {
                        Text("UID কপি", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                      }

                      Button(
                        onClick = {
                          clipboardManager.setText(AnnotatedString(item.cookies))
                          scope.launch { snackbarHostState.showSnackbar("কুকি কপি করা হয়েছে!") }
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.weight(1f).height(30.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        shape = RoundedCornerShape(4.dp)
                      ) {
                        Text("কুকি কপি", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                      }
                    }
                  }
                }
              }
            }
          }
        }
        }
      },
      confirmButton = {
        TextButton(onClick = { showHistoryDialog = false }) {
          Text("বন্ধ করুন")
        }
      }
    )
  }

  // Proxy Config Dialog
  if (showProxyConfigDialog) {
    var hostInput by remember { mutableStateOf(proxyHost) }
    var portInput by remember { mutableStateOf(proxyPort) }
    var userInput by remember { mutableStateOf(proxyUser) }
    var passInput by remember { mutableStateOf(proxyPass) }
    AlertDialog(
      onDismissRequest = { showProxyConfigDialog = false },
      title = { Text("Proxy Settings") },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedTextField(
            value = hostInput,
            onValueChange = { hostInput = it },
            label = { Text("Server Change/Host") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )

          OutlinedTextField(
            value = portInput,
            onValueChange = { portInput = it },
            label = { Text("Port") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )

          OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )

          OutlinedTextField(
            value = passInput,
            onValueChange = { passInput = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            proxyHost = hostInput.trim()
            proxyPort = portInput.trim()
            proxyUser = userInput.trim()
            proxyPass = passInput.trim()

            prefs.edit()
              .putString("proxy_host", proxyHost)
              .putString("proxy_port", proxyPort)
              .putString("proxy_user", proxyUser)
              .putString("proxy_pass", proxyPass)
              .apply()

            // Apply to WebView if enabled
            if (isProxyEnabled && proxyHost.isNotBlank() && proxyPort.isNotBlank()) {
              applyWebViewProxy(context, true, proxyHost, proxyPort)
            }

            showProxyConfigDialog = false
            scope.launch { snackbarHostState.showSnackbar("Proxy settings saved successfully!") }
          }
        ) {
          Text("সংরক্ষণ করুন (Save)")
        }
      },
      dismissButton = {
        TextButton(onClick = { showProxyConfigDialog = false }) {
          Text("বাতিল (Cancel)")
        }
      }
    )
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
  MyApplicationTheme {
    MainScreen()
  }
}

fun getRandomMobileUserAgent(): String {
    val androidVersions = listOf("10", "11", "12", "13", "14")
    val deviceModels = listOf(
        "SM-G998B", "SM-G991B", "SM-S901B", "SM-S908B", "SM-S918B", "SM-A546B", "SM-A536B", 
        "Pixel 6", "Pixel 6 Pro", "Pixel 7", "Pixel 7 Pro", "Pixel 8", "Pixel 8 Pro", 
        "2201117TY", "2201116PG", "2210132G", "23049PCD8G", 
        "CPH2305", "CPH2371", "CPH2437", 
        "V2130", "V2145", "V2227A"
    )
    val chromeVersions = listOf(
        "114.0.0.0", "115.0.0.0", "116.0.0.0", "117.0.0.0", 
        "118.0.0.0", "119.0.0.0", "120.0.0.0", "121.0.0.0", "122.0.0.0", "123.0.0.0"
    )
    
    val androidVersion = androidVersions.random()
    val deviceModel = deviceModels.random()
    val chromeVersion = chromeVersions.random()
    
    return "Mozilla/5.0 (Linux; Android $androidVersion; $deviceModel) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Mobile Safari/537.36"
}
