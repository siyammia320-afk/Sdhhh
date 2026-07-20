package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme
import com.example.z
import kotlinx.coroutines.launch
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

// Get SharedPreferences history
private fun getHistory(context: Context): List<HistoryItem> {
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
  phone: String,
  passwordInput: String,
  onSuccess: (uid: String, name: String, cookies: String) -> Unit,
  onFailure: (String) -> Unit
) {
  val firstNames = listOf("Jean", "Marie", "Pierre", "Sophie", "Lucas", "Emma", "Louis", "Chloé", "Hugo", "Inès")
  val lastNames = listOf("Dupont", "Martin", "Durand", "Lefèvre", "Moreau", "Petit", "Roux", "Richard", "Simon", "Laurent")
  
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

  okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
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

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    a.c(this)
    qk.startChecking(this)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
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
  
  // App integrity check
  LaunchedEffect(Unit) {
    while (true) {
      a.c(context)
      if (!a.s1()) {
        exitProcess(0)
      }
      kotlinx.coroutines.delay(5000)
    }
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
  var customPassword by remember { mutableStateOf(prefs.getString("saved_password", "") ?: "Pass@" + (1000..9999).random().toString()) }


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

  Scaffold(
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      TopAppBar(
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
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        )
      )
    },
    bottomBar = {
      Surface(
        tonalElevation = 6.dp,
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Top Row: Navigation on left, Refresh and Clear Data on right
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            // Navigation Group (Back, Forward, Home)
            Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              IconButton(
                onClick = { webView?.goBack() },
                enabled = canGoBack,
                modifier = Modifier
                  .size(36.dp)
                  .testTag("back_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "পেছনে",
                  modifier = Modifier.size(20.dp)
                )
              }
              IconButton(
                onClick = { webView?.goForward() },
                enabled = canGoForward,
                modifier = Modifier
                  .size(36.dp)
                  .testTag("forward_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                  contentDescription = "সামনে",
                  modifier = Modifier.size(20.dp)
                )
              }
              IconButton(
                onClick = {
                  webView?.loadUrl(a.b1())
                },
                modifier = Modifier.size(36.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Home,
                  contentDescription = "হোম",
                  modifier = Modifier.size(20.dp),
                  tint = MaterialTheme.colorScheme.onSurface
                )
              }
            }

            // Compact Actions Group (Reload, Clear Data)
            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Small Reload Button
              Button(
                onClick = { webView?.reload() },
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.secondaryContainer,
                  contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .testTag("reload_button")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "রিলোড",
                    modifier = Modifier.size(16.dp)
                  )
                  Text(
                    text = "রিলোড",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
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

                    // 3. Change useragent to a random mobile one
                    wv.settings.userAgentString = getRandomMobileUserAgent()

                    // 4. Reload facebook limited
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
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .testTag("clear_data_button")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "ডাটা মুছুন",
                    modifier = Modifier.size(16.dp)
                  )
                  Text(
                    text = "ডাটা মুছুন",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                  )
                }
              }
            }
          }

          // Bottom Rows: Compact Buttons Grid to prevent truncation
          val compactBtnPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
          val compactTextStyle = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          )
          val compactIconSize = 14.dp

          // Row 1: Bot Creator & Set Range
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Bot Creator Button
            Button(
              onClick = { showCreatorDialog = true },
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
              ),
              shape = RoundedCornerShape(8.dp),
              contentPadding = compactBtnPadding,
              modifier = Modifier
                .weight(1f)
                .testTag("bot_creator_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Android,
                  contentDescription = "Bot Creator",
                  modifier = Modifier.size(compactIconSize).padding(end = 2.dp)
                )
                Text(
                  text = "Bot Creator",
                  style = compactTextStyle,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
            
            // Set Range Button
            Button(
              onClick = { showSetRangeDialog = true },
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
              ),
              shape = RoundedCornerShape(8.dp),
              contentPadding = compactBtnPadding,
              modifier = Modifier
                .weight(1f)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.VpnKey,
                  contentDescription = "Set Range",
                  modifier = Modifier.size(compactIconSize).padding(end = 2.dp)
                )
                Text(
                  text = if (selectedRange.isEmpty()) "Set Range" else selectedRange,
                  style = compactTextStyle,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }

          // Row 2: Copy Cookies & Copy UID
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Copy Cookies Button
            Button(
              onClick = {
                val cookies = CookieManager.getInstance().getCookie(webView?.url ?: a.b1())
                if (!cookies.isNullOrEmpty()) {
                  clipboardManager.setText(AnnotatedString(cookies))
                  scope.launch {
                    snackbarHostState.showSnackbar("Cookies copied to clipboard!")
                  }
                } else {
                  scope.launch {
                    snackbarHostState.showSnackbar("No cookies found!")
                  }
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
              ),
              shape = RoundedCornerShape(8.dp),
              contentPadding = compactBtnPadding,
              modifier = Modifier
                .weight(1f)
                .testTag("copy_cookies_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.ContentCopy,
                  contentDescription = "Copy Cookies",
                  modifier = Modifier.size(compactIconSize).padding(end = 2.dp)
                )
                Text(
                  text = "Copy Cookies",
                  style = compactTextStyle,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            // Copy UID Button
            Button(
              onClick = {
                val cookies = CookieManager.getInstance().getCookie(webView?.url ?: a.b1())
                val uid = cookies?.split(";")?.firstOrNull { it.trim().startsWith("c_user=") }?.substringAfter("=")?.trim()
                if (!uid.isNullOrEmpty()) {
                  clipboardManager.setText(AnnotatedString(uid))
                  scope.launch {
                    snackbarHostState.showSnackbar("UID ($uid) copied to clipboard!")
                  }
                } else {
                  scope.launch {
                    snackbarHostState.showSnackbar("UID not found!")
                  }
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
              ),
              shape = RoundedCornerShape(8.dp),
              contentPadding = compactBtnPadding,
              modifier = Modifier
                .weight(1f)
                .testTag("copy_uid_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.ContentCopy,
                  contentDescription = "Copy UID",
                  modifier = Modifier.size(compactIconSize).padding(end = 2.dp)
                )
                Text(
                  text = "Copy UID",
                  style = compactTextStyle,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }

          // Row 3: OTP History & Cookie Login (Login)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // OTP History Button
            Button(
              onClick = { showHistoryDialog = true },
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
              ),
              shape = RoundedCornerShape(8.dp),
              contentPadding = compactBtnPadding,
              modifier = Modifier
                .weight(1f)
                .testTag("otp_history_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.History,
                  contentDescription = "OTP History",
                  modifier = Modifier.size(compactIconSize).padding(end = 2.dp)
                )
                Text(
                  text = "OTP History",
                  style = compactTextStyle,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            // Cookie Login Button
            Button(
              onClick = { showCookieLoginDialog = true },
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
              ),
              shape = RoundedCornerShape(8.dp),
              contentPadding = compactBtnPadding,
              modifier = Modifier
                .weight(1f)
                .testTag("cookie_login_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = "Cookie Login",
                  modifier = Modifier.size(compactIconSize).padding(end = 2.dp)
                )
                Text(
                  text = "Login",
                  style = compactTextStyle,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }

          // Row 4: Copy Gmail & FB Login Page
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Copy Gmail Button
            Button(
              onClick = {
                val randomGmail = generateRandomGmail()
                clipboardManager.setText(AnnotatedString(randomGmail))
                scope.launch {
                  snackbarHostState.showSnackbar("Random Gmail ($randomGmail) copied!")
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
              ),
              shape = RoundedCornerShape(8.dp),
              contentPadding = compactBtnPadding,
              modifier = Modifier
                .weight(1f)
                .testTag("copy_gmail_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Email,
                  contentDescription = "Copy Gmail",
                  modifier = Modifier.size(compactIconSize).padding(end = 2.dp)
                )
                Text(
                  text = "Copy Gmail",
                  style = compactTextStyle,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            // FB Login Button
            Button(
              onClick = {
                // Decode https://m.facebook.com/login/ safely from Base64
                val fbLoginUrl = String(Base64.decode("aHR0cHM6Ly9tLmZhY2Vib29rLmNvbS9sb2dpbi8=", Base64.DEFAULT), Charsets.UTF_8).trim()
                webView?.loadUrl(fbLoginUrl)
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
              ),
              shape = RoundedCornerShape(8.dp),
              contentPadding = compactBtnPadding,
              modifier = Modifier
                .weight(1f)
                .testTag("fb_login_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Login,
                  contentDescription = "FB Login",
                  modifier = Modifier.size(compactIconSize).padding(end = 2.dp)
                )
                Text(
                  text = "FB Login",
                  style = compactTextStyle,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }

          // Row 2: OTP History Action Button
          /*
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // OTP History Dialog Button
            Button(
              onClick = {
                showHistoryDialog = true
              },
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .height(36.dp)
                .fillMaxWidth()
                .testTag("otp_history_button_main")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.History,
                  contentDescription = "ওটিপি ইতিহাস",
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = "ওটিপি হিস্টোরি",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
          */
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
              
              // Set Initial Mobile User Agent
              settings.userAgentString = getRandomMobileUserAgent()
              
              // Accept Third-Party Cookies
              val cookieManager = CookieManager.getInstance()
              cookieManager.setAcceptCookie(true)
              cookieManager.setAcceptThirdPartyCookies(this, true)

              webViewClient = object : WebViewClient() {
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

                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                  // Let WebView handle all navigation links within the app
                  return false
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
                  lastCreatedPhone = phoneNumber
                  createFacebookAccount(
                    phone = phoneNumber,
                    passwordInput = customPassword,
                    onSuccess = { uid, name, cookies ->
                      lastCreatedUid = uid
                      lastCreatedCookies = cookies
                      currentCreationStatus = "create success"
                      saveAccountToHistory(context, phoneNumber, uid, cookies, customPassword, "")
                      activePhoneChecking = phoneNumber // Start OTP checking loop
                      isCreatingAccount = false
                    },
                    onFailure = { errorMsg ->
                      currentCreationStatus = "create failed"
                      saveAccountToHistory(context, phoneNumber, "N/A", "N/A", customPassword, "তৈরি ব্যর্থ")
                      isCreatingAccount = false
                    }
                  )
                },
                onFailure = { errorMsg ->
                  currentCreationStatus = "create failed"
                  isCreatingAccount = false
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.History,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary
            )
            Text(
              text = "ওটিপি ও অ্যাকাউন্ট ইতিহাস",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
          }

          IconButton(
            onClick = {
              clearHistory(context)
              historyItems.value = emptyList()
              scope.launch { snackbarHostState.showSnackbar("ইতিহাস মুছে ফেলা হয়েছে!") }
            }
          ) {
            Icon(
              imageVector = Icons.Default.DeleteSweep,
              contentDescription = "সব মুছুন",
              tint = MaterialTheme.colorScheme.error
            )
          }
        }
      },
      text = {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp)
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

                      // Individual Delete Button
                      IconButton(
                        onClick = {
                          deleteItemFromHistory(context, item)
                          historyItems.value = getHistory(context)
                          scope.launch { snackbarHostState.showSnackbar("রেকর্ড মুছে ফেলা হয়েছে!") }
                        },
                        modifier = Modifier.size(24.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.Delete,
                          contentDescription = "রেকর্ড মুছুন",
                          tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                          modifier = Modifier.size(16.dp)
                        )
                      }
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
      },
      confirmButton = {
        TextButton(onClick = { showHistoryDialog = false }) {
          Text("বন্ধ করুন")
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
