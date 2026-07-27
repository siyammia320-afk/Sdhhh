package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class DeviceItem(
    val id: String,
    val name: String,
    val expire: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanel() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    
    var appStatus by remember { mutableStateOf("ON") }
    var devicesList by remember { mutableStateOf<List<DeviceItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Add Device Form States
    var inputName by remember { mutableStateOf("") }
    var inputDeviceId by remember { mutableStateOf("") }
    var inputExpire by remember { mutableStateOf("31:12:2026") }
    
    var isLoadingStatus by remember { mutableStateOf(false) }
    var isLoadingDevices by remember { mutableStateOf(false) }
    var isSavingDevice by remember { mutableStateOf(false) }

    val dbBaseUrl = "https://my-original-apk-default-rtdb.firebaseio.com"

    // Helper to get ID Token
    suspend fun getIdToken(): String {
        return try {
            auth.currentUser?.getIdToken(false)?.await()?.token ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    // Load App Status
    fun loadAppStatus() {
        isLoadingStatus = true
        scope.launch {
            try {
                val token = getIdToken()
                val client = OkHttpClient()
                val url = "$dbBaseUrl/status.json?auth=$token"
                val request = Request.Builder().url(url).build()
                
                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }
                val body = response.body?.string()?.trim() ?: ""
                response.close()
                
                if (body.isNotEmpty() && body != "null") {
                    try {
                        val json = JSONObject(body)
                        appStatus = json.optString("status", "ON").uppercase()
                    } catch (e: Exception) {
                        appStatus = body.replace("\"", "").trim().uppercase()
                    }
                } else {
                    appStatus = "ON"
                }
            } catch (e: Exception) {
                // Ignore or log
            } finally {
                isLoadingStatus = false
            }
        }
    }

    // Update App Status
    fun updateAppStatus(newStatus: String) {
        isLoadingStatus = true
        scope.launch {
            try {
                val token = getIdToken()
                val client = OkHttpClient()
                val url = "$dbBaseUrl/status.json?auth=$token"
                
                val jsonPayload = JSONObject().apply {
                    put("status", newStatus)
                }.toString()
                
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonPayload.toRequestBody(mediaType)
                val request = Request.Builder().url(url).put(requestBody).build()
                
                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }
                response.close()
                appStatus = newStatus
                Toast.makeText(context, "অ্যাপ স্ট্যাটাস $newStatus করা হয়েছে!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "ত্রুটি: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoadingStatus = false
            }
        }
    }

    // Load Devices List
    fun loadDevices() {
        isLoadingDevices = true
        scope.launch {
            try {
                val token = getIdToken()
                val client = OkHttpClient()
                val url = "$dbBaseUrl/devices.json?auth=$token"
                val request = Request.Builder().url(url).build()
                
                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }
                val body = response.body?.string()?.trim() ?: ""
                response.close()
                
                val newList = mutableListOf<DeviceItem>()
                if (body.isNotEmpty() && body != "null") {
                    val json = JSONObject(body)
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val valueObj = json.opt(key)
                        var dName = ""
                        var dExpire = ""
                        if (valueObj is JSONObject) {
                            dName = valueObj.optString("name", "")
                            dExpire = valueObj.optString("expire", "")
                        } else {
                            dExpire = valueObj?.toString() ?: ""
                        }
                        newList.add(DeviceItem(id = key, name = dName, expire = dExpire))
                    }
                }
                devicesList = newList.sortedBy { it.name.lowercase() }
            } catch (e: Exception) {
                // Ignore or show
            } finally {
                isLoadingDevices = false
            }
        }
    }

    // Add or Update Device
    fun saveDevice() {
        val devId = inputDeviceId.trim()
        val name = inputName.trim()
        val expire = inputExpire.trim()
        
        if (devId.isEmpty() || name.isEmpty() || expire.isEmpty()) {
            Toast.makeText(context, "সবগুলো ইনপুট ফিল্ড পূরণ করুন", Toast.LENGTH_SHORT).show()
            return
        }
        
        isSavingDevice = true
        scope.launch {
            try {
                val token = getIdToken()
                val client = OkHttpClient()
                val url = "$dbBaseUrl/devices/$devId.json?auth=$token"
                
                val jsonPayload = JSONObject().apply {
                    put("name", name)
                    put("expire", expire)
                }.toString()
                
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonPayload.toRequestBody(mediaType)
                val request = Request.Builder().url(url).put(requestBody).build()
                
                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }
                response.close()
                
                Toast.makeText(context, "ডিভাইস সফলভাবে অ্যাক্টিভেট করা হয়েছে!", Toast.LENGTH_SHORT).show()
                inputDeviceId = ""
                inputName = ""
                loadDevices()
            } catch (e: Exception) {
                Toast.makeText(context, "ত্রুটি: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isSavingDevice = false
            }
        }
    }

    // Delete Device
    fun deleteDevice(deviceIdToDelete: String) {
        scope.launch {
            try {
                val token = getIdToken()
                val client = OkHttpClient()
                val url = "$dbBaseUrl/devices/$deviceIdToDelete.json?auth=$token"
                val request = Request.Builder().url(url).delete().build()
                
                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }
                response.close()
                
                Toast.makeText(context, "ডিভাইস রিমুভ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                loadDevices()
            } catch (e: Exception) {
                Toast.makeText(context, "ত্রুটি: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Initial Load
    LaunchedEffect(Unit) {
        loadAppStatus()
        loadDevices()
    }

    val filteredDevices = devicesList.filter {
        it.id.lowercase().contains(searchQuery.lowercase()) ||
        it.name.lowercase().contains(searchQuery.lowercase())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF090D1A), // Dark slate blue
                        Color(0xFF020617)  // Deep rich black
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "অ্যাডমিন প্যানেল",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ডিভাইস ও অ্যাপ কন্ট্রোল সেন্টার",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                
                IconButton(
                    onClick = {
                        auth.signOut()
                    },
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Logout",
                        tint = Color(0xFFEF4444)
                    )
                }
            }

            // Scrollable Layout
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. App Power Status Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PowerSettingsNew,
                                        contentDescription = "Power",
                                        tint = if (appStatus == "ON") Color(0xFF10B981) else Color(0xFFEF4444),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "APK অন/অফ কন্ট্রোল",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                if (isLoadingStatus) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { updateAppStatus("ON") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (appStatus == "ON") Color(0xFF10B981) else Color(0xFF1E293B),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    border = if (appStatus == "ON") null else BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Text(
                                        text = "চালু (ON)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Button(
                                    onClick = { updateAppStatus("OFF") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (appStatus == "OFF") Color(0xFFEF4444) else Color(0xFF1E293B),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    border = if (appStatus == "OFF") null else BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Text(
                                        text = "বন্ধ (OFF)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Add New Device Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "নতুন ডিভাইস অ্যাক্টিভেট করুন",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            OutlinedTextField(
                                value = inputName,
                                onValueChange = { inputName = it },
                                label = { Text("ব্যবহারকারীর নাম (User Name)") },
                                placeholder = { Text("যেমন: Rahat Ahmed") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155)
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            OutlinedTextField(
                                value = inputDeviceId,
                                onValueChange = { inputDeviceId = it },
                                label = { Text("ডিভাইস আইডি (Device ID)") },
                                placeholder = { Text("যেমন: device_id_1") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155)
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            OutlinedTextField(
                                value = inputExpire,
                                onValueChange = { inputExpire = it },
                                label = { Text("মেয়াদ শেষ হওয়ার তারিখ") },
                                placeholder = { Text("যেমন: 31:12:2026") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155)
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { saveDevice() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                enabled = !isSavingDevice
                            ) {
                                if (isSavingDevice) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "ডিভাইস অ্যাক্টিভেট করুন",
                                        color = Color.Black,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Search & List Section
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "অ্যাক্টিভেটেড ডিভাইস তালিকা (${devicesList.size})",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            IconButton(onClick = { loadDevices() }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Devices",
                                    tint = Color.Gray
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("নাম বা ডিভাইস আইডি দিয়ে খুঁজুন...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.Gray
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF334155),
                                unfocusedBorderColor = Color(0xFF1E293B)
                            )
                        )
                    }
                }

                if (isLoadingDevices) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF38BDF8))
                        }
                    }
                } else if (filteredDevices.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "কোন ডিভাইস পাওয়া যায়নি।",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(filteredDevices) { device ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = device.name,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "ID: ${device.id}",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 12.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "মেয়াদ: ${device.expire}",
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                }
                                
                                IconButton(
                                    onClick = { deleteDevice(device.id) },
                                    modifier = Modifier
                                        .background(Color(0xFF3F1A1A), RoundedCornerShape(8.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Device",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
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
