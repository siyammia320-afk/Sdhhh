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
    onGranted()
}
