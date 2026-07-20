package com.example.ui.theme.font

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
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

@Composable
fun ActivationBarrier(
    onGranted: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val deviceId = remember { TypeResolver.getDeviceIdentifier(context) }
    val prefs = remember { context.getSharedPreferences("fb_creator_prefs", Context.MODE_PRIVATE) }
    var userName by remember { mutableStateOf(prefs.getString("user_activation_name", "") ?: "") }
    
    var isAuthorized by remember { mutableStateOf<Boolean?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Check authorization on launch
    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        
        // Direct safe integrity check
        if (!ThemeResources.checkIntegrity()) {
            kotlin.system.exitProcess(0)
        }

        val data = ThemeResources.fetchRawData()
        if (data != null) {
            val authorized = TypeResolver.isDeviceAuthorized(deviceId, data)
            isAuthorized = authorized
            if (!authorized) {
                errorMessage = "আপনার ডিভাইসটি এক্টিভেট করা নেই! অনুগ্রহ করে ডিভাইস আইডিটি কপি করে এডমিনের সাথে যোগাযোগ করুন।"
            }
        } else {
            isAuthorized = false
            errorMessage = "সার্ভারের সাথে যোগাযোগ করা যাচ্ছে না। ইন্টারনেট কানেকশন চেক করুন।"
        }
        isLoading = false
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
                            Color(0xFF0F172A), // Slate 900
                            Color(0xFF020617)  // Slate 950
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .widthIn(max = 480.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Lock Icon inside glowing circle
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFF1E293B), RoundedCornerShape(40.dp))
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(40.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "ডিভাইস ভেরিফিকেশন প্রয়োজন",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Device Verification Required",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error Message Section
                errorMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF451A03)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = msg,
                            color = Color(0xFFFDBA74),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Device ID Display Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "আপনার ডিভাইস আইডি (Device ID):",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Styled ID text container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = deviceId,
                                color = Color(0xFF38BDF8),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Copy Button
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Device ID", deviceId)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "ডিভাইস আইডি কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ডিভাইস আইডি কপি করুন",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Admin Contact Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "অ্যাডমিন যোগাযোগ (Admin Contact)",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // WhatsApp Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801912953085"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "WhatsApp ওপেন করা যাচ্ছে না", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💬 WhatsApp:",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "+8801912953085",
                                color = Color(0xFF4ADE80), // Green
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Telegram Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Arafat_bhai1"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Telegram ওপেন করা যাচ্ছে না", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✈️ Telegram:",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "@Arafat_bhai1",
                                color = Color(0xFF38BDF8), // Blue
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Verify / Retry Button
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                
                                // Direct safe integrity check
                                if (!ThemeResources.checkIntegrity()) {
                                    kotlin.system.exitProcess(0)
                                }

                                val data = ThemeResources.fetchRawData()
                                if (data != null) {
                                    val authorized = TypeResolver.isDeviceAuthorized(deviceId, data)
                                    isAuthorized = authorized
                                    if (authorized) {
                                        Toast.makeText(context, "ভেরিফিকেশন সফল হয়েছে!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        errorMessage = "ডিভাইস আইডিটি এখনও এক্টিভেট করা হয়নি। অনুগ্রহ করে অ্যাডমিনের সাথে যোগাযোগ করুন।"
                                    }
                                } else {
                                    errorMessage = "সার্ভারের সাথে সংযোগ ব্যর্থ হয়েছে। আবার চেষ্টা করুন।"
                                }
                                isLoading = false
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ভেরিফাই করুন (Verify Status)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
