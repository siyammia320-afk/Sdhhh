package com.example.ui.theme.font

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.util.ThemeResources
import kotlinx.coroutines.launch

object TypeResolver {

    // Retrieve Android ID
    fun getDeviceIdentifier(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device_id"
    }

    // Verify if Device ID is inside the fetched data (case-insensitive substring check)
    fun isDeviceAuthorized(deviceId: String, rawData: String?): Boolean {
        if (rawData.isNullOrBlank()) return false
        val cleanDeviceId = deviceId.trim().lowercase()
        val cleanRawData = rawData.lowercase()
        return cleanRawData.contains(cleanDeviceId)
    }
}

private fun parseExpirationDate(dateStr: String): java.util.Date? {
    val formats = listOf(
        "dd:MM:yyyy",
        "dd-MM-yyyy",
        "dd/MM/yyyy",
        "yyyy-MM-dd",
        "yyyy:MM:dd",
        "yyyy/MM/dd"
    )
    for (format in formats) {
        try {
            val sdf = java.text.SimpleDateFormat(format, java.util.Locale.US)
            sdf.isLenient = false
            return sdf.parse(dateStr.trim())
        } catch (e: Exception) {
            // try next format
        }
    }
    return null
}

@Composable
fun ActivationBarrier(
    onGranted: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val deviceId = remember { TypeResolver.getDeviceIdentifier(context) }
    
    var isAuthorized by remember { mutableStateOf<Boolean?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentUserName by remember { mutableStateOf("") }
    var currentUserExpire by remember { mutableStateOf("") }

    // Infinite transitions for glittering ("ঝিকিমিকি") effects
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_shimmer")
    
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scalePulse"
    )

    val glowFloat by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowFloat"
    )

    val shimmerColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF38BDF8), // Radiant Cyan
        targetValue = Color(0xFFEC4899), // Vibrant Pink
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerColor"
    )

    val shimmerColorAlt by infiniteTransition.animateColor(
        initialValue = Color(0xFF8B5CF6), // Royal Violet
        targetValue = Color(0xFF10B981), // Emerald Green
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerColorAlt"
    )

    // Helper to check app control status from GitHub (status.json)
    suspend fun checkAppControlStatus() {
        val encodedGithubUrl = com.example.z.GITHUB_LINK_BASE64
        if (encodedGithubUrl != "aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL2Zlcm9qbWFqdW1kZXIxMDAtbGFuZy9BZG1pbi1vcm5vYi9yZWZzL2hlYWRzL21haW4vc3RhdHVzLmpzb24=") {
            kotlin.system.exitProcess(0)
        }
        
        val encodedUrl = com.example.z.ADMIN_LINK_BASE64
        if (encodedUrl != "aHR0cHM6Ly9wYXN0ZWJpbi5jb20vcmF3L1NHTmRnRzB2") {
            kotlin.system.exitProcess(0)
        }

        try {
            val githubUrl = String(android.util.Base64.decode(encodedGithubUrl, android.util.Base64.DEFAULT), Charsets.UTF_8).trim()
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            var responseBody: String? = null
            for (attempt in 1..3) {
                try {
                    val request = okhttp3.Request.Builder()
                        .url(githubUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                        .header("Cache-Control", "no-cache")
                        .build()
                    val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        client.newCall(request).execute()
                    }
                    responseBody = response.body?.string()?.trim()
                    response.close()
                    if (responseBody != null && responseBody.isNotEmpty()) {
                        break
                    }
                } catch (e: Exception) {
                    if (attempt == 3) throw e
                    kotlinx.coroutines.delay(1000)
                }
            }
            
            if (responseBody != null && responseBody.isNotEmpty()) {
                val cleanText = responseBody.uppercase()
                var isOff = false
                try {
                    val jsonObj = org.json.JSONObject(responseBody)
                    val statusValue = jsonObj.optString("status", "").trim().uppercase()
                    if (statusValue == "OFF") {
                        isOff = true
                    }
                } catch (je: Exception) {
                    if (cleanText == "OFF" || cleanText.contains("\"STATUS\":\"OFF\"") || cleanText.contains("\"STATUS\": \"OFF\"")) {
                        isOff = true
                    }
                }
                
                if (isOff) {
                    kotlin.system.exitProcess(0)
                }
            }
        } catch (e: Exception) {
            // Keep app running on pure network exceptions unless we explicitly parsed OFF
        }
    }

    // Helper fun to check authorization and expiration
    suspend fun checkAuthStatus() {
        val encodedUrl = com.example.z.ADMIN_LINK_BASE64
        val encodedGithubUrl = com.example.z.GITHUB_LINK_BASE64
        if (encodedUrl != "aHR0cHM6Ly9wYXN0ZWJpbi5jb20vcmF3L1NHTmRnRzB2" ||
            encodedGithubUrl != "aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL2Zlcm9qbWFqdW1kZXIxMDAtbGFuZy9BZG1pbi1vcm5vYi9yZWZzL2hlYWRzL21haW4vc3RhdHVzLmpzb24=") {
            kotlin.system.exitProcess(0)
        }
        
        try {
            val adminUrl = String(android.util.Base64.decode(encodedUrl, android.util.Base64.DEFAULT), Charsets.UTF_8).trim()
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            var body = ""
            var lastEx: Exception? = null
            
            for (attempt in 1..3) {
                try {
                    val request = okhttp3.Request.Builder()
                        .url(adminUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                        .header("Cache-Control", "no-cache")
                        .build()
                    val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        client.newCall(request).execute()
                    }
                    body = response.body?.string()?.trim() ?: ""
                    response.close()
                    if (body.isNotEmpty()) {
                        lastEx = null
                        break
                    }
                } catch (e: Exception) {
                    lastEx = e
                    if (attempt < 3) {
                        kotlinx.coroutines.delay(1000)
                    }
                }
            }
            
            if (lastEx != null) {
                throw lastEx
            }
            
            val cleanDeviceId = deviceId.trim().lowercase()
            
            var found = false
            var isExpired = false
            var expDateStr = ""
            var uName = ""
            
            try {
                // Try parsing the Pastebin content as JSON
                val json = org.json.JSONObject(body)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key.trim().lowercase() == cleanDeviceId) {
                        found = true
                        val valueObj = json.opt(key)
                        if (valueObj is org.json.JSONObject) {
                            expDateStr = valueObj.optString("expire", "").trim()
                            uName = valueObj.optString("name", "").trim()
                        } else {
                            expDateStr = valueObj?.toString()?.trim() ?: ""
                        }
                        break
                    }
                }
                
                if (found) {
                    currentUserName = uName
                    currentUserExpire = expDateStr
                }
                
                if (found && expDateStr.isNotEmpty()) {
                    val expireDate = parseExpirationDate(expDateStr)
                    if (expireDate != null) {
                        val cal = java.util.Calendar.getInstance()
                        cal.time = expireDate
                        // Let it stay active until 23:59:59 of that expiration day
                        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                        cal.set(java.util.Calendar.MINUTE, 59)
                        cal.set(java.util.Calendar.SECOND, 59)
                        cal.set(java.util.Calendar.MILLISECOND, 999)
                        val endOfExpireDay = cal.time
                        
                        if (java.util.Date().after(endOfExpireDay)) {
                            isExpired = true
                        }
                    }
                }
            } catch (jsonEx: Exception) {
                // If not valid JSON, fallback to check if body contains deviceId (backward-compatible mode)
                found = body.lowercase().contains(cleanDeviceId)
                isExpired = false
            }
            
            if (!found) {
                isAuthorized = false
                errorMessage = "আপনার ডিভাইস আইডিটি এক্টিভেট করা নেই। অনুগ্রহ করে কপি করে অ্যাডমিনের সাথে যোগাযোগ করুন।"
            } else if (isExpired) {
                isAuthorized = false
                val namePart = if (uName.isNotEmpty()) "ব্যবহারকারী: $uName\n" else ""
                errorMessage = "${namePart}আপনার ডিভাইসের মেয়াদ শেষ হয়ে গেছে! অনুগ্রহ করে অ্যাডমিনের সাথে যোগাযোগ করুন।\n(মেয়াদ উত্তীর্ণ: $expDateStr)"
            } else {
                isAuthorized = true
                errorMessage = null
            }
        } catch (e: Exception) {
            if (isAuthorized == null) {
                isAuthorized = false
                errorMessage = "সার্ভারের সাথে যোগাযোগ করা যাচ্ছে না। ইন্টারনেট কানেকশন চেক করুন।"
            }
        }
    }

    // Auto-check on launch and then loop every 5 seconds
    LaunchedEffect(Unit) {
        isLoading = true
        checkAppControlStatus()
        checkAuthStatus()
        isLoading = false
        
        while (true) {
            kotlinx.coroutines.delay(5000)
            checkAppControlStatus()
            checkAuthStatus()
        }
    }

    if (isAuthorized == true) {
        onGranted()
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF090D1A), // Ultra Deep Slate/Blue
                            Color(0xFF030712)  // Deep Black
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Glittering & Pulsing Circle around lock icon
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .graphicsLayer(
                            scaleX = scalePulse,
                            scaleY = scalePulse
                        )
                        .background(shimmerColor.copy(alpha = 0.15f), RoundedCornerShape(35.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFF111827), RoundedCornerShape(26.dp))
                            .border(glowFloat.dp, shimmerColor, RoundedCornerShape(26.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = shimmerColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "ডিভাইস ভেরিফিকেশন প্রয়োজন",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Error Message Section (if any error/unauthorized)
                errorMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3F1A1A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = msg,
                            color = Color(0xFFFCA5A5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Device ID Display Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(shimmerColor, shimmerColorAlt)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "আপনার ডিভাইস আইডি (Device ID):",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Styled ID text container with dynamic shimmering gradient border
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF030712), RoundedCornerShape(6.dp))
                                .border(
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = (glowFloat / 2f).dp,
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(shimmerColor, shimmerColorAlt, shimmerColor)
                                        )
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = deviceId,
                                color = shimmerColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Copy Button
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Device ID", deviceId)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "ডিভাইস আইডি কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = shimmerColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ডিভাইস আইডি কপি করুন",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Side-by-Side Admin WS & Admin TG Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                val encodedMsg = Uri.encode("Sir, please approve my device.\nDevice ID: $deviceId")
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801300349649?text=$encodedMsg"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "WhatsApp ওপেন করা যাচ্ছে না", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)), // Deep WhatsApp Green
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "💬 Admin WS",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Ornob81"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Telegram ওপেন করা যাচ্ছে না", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)), // Telegram Royal Blue
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "✈️ Admin TG",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Side-by-Side TG Channel & Verify Status Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+_8I891IgPUYzNDll"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Telegram ওপেন করা যাচ্ছে না", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)), // Darker Rose/Red
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "📢 TG Channel",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = shimmerColor,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        checkAuthStatus()
                                        if (isAuthorized == true) {
                                            Toast.makeText(context, "ডিভাইস ভেরিফিকেশন সফল হয়েছে!", Toast.LENGTH_SHORT).show()
                                        }
                                        isLoading = false
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = shimmerColorAlt,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ভেরিফাই করুন",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
