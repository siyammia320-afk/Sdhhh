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
import androidx.compose.material.icons.automirrored.filled.*
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DeviceItem(
    val id: String,
    val name: String,
    val expire: String,
    val isBanned: Boolean = false,
    val approvedBy: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanel(onSwitchToUser: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember {
        try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
    }
    
    var appStatus by remember { mutableStateOf("ON") }
    var devicesList by remember { mutableStateOf<List<DeviceItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    
    var inputName by remember { mutableStateOf("") }
    var inputDeviceId by remember { mutableStateOf("") }
    var inputDays by remember { mutableFloatStateOf(30f) }
    
    var noticeText by remember { mutableStateOf("") }
    var isSendingNotice by remember { mutableStateOf(false) }

    var isLoadingStatus by remember { mutableStateOf(false) }
    var isLoadingDevices by remember { mutableStateOf(false) }
    var isSavingDevice by remember { mutableStateOf(false) }

    var showEditDialog by remember { mutableStateOf(false) }
    var editDeviceItem by remember { mutableStateOf<DeviceItem?>(null) }
    var editInputName by remember { mutableStateOf("") }
    var editInputDays by remember { mutableFloatStateOf(30f) }

    val dbBaseUrl = "https://my-original-apk-default-rtdb.firebaseio.com"

    suspend fun getIdToken(): String {
        return try {
            auth?.currentUser?.getIdToken(false)?.await()?.token ?: ""
        } catch (e: Exception) { "" }
    }

    fun loadAppStatus() {
        isLoadingStatus = true
        scope.launch {
            try {
                val token = getIdToken()
                val client = OkHttpClient()
                val url = "$dbBaseUrl/status.json?auth=$token"
                val request = Request.Builder().url(url).build()
                
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
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
            } catch (e: Exception) { } finally { isLoadingStatus = false }
        }
    }

    fun updateAppStatus(newStatus: String) {
        isLoadingStatus = true
        scope.launch {
            try {
                val token = getIdToken()
                val client = OkHttpClient()
                val url = "$dbBaseUrl/status.json?auth=$token"
                
                val jsonPayload = JSONObject().apply { put("status", newStatus) }.toString()
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val request = Request.Builder().url(url).put(jsonPayload.toRequestBody(mediaType)).build()
                
                withContext(Dispatchers.IO) { client.newCall(request).execute() }.close()
                appStatus = newStatus
                Toast.makeText(context, "App Status changed to $newStatus!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally { isLoadingStatus = false }
        }
    }

    fun sendNotice() {
        if (noticeText.trim().isEmpty()) {
            Toast.makeText(context, "Enter a notice", Toast.LENGTH_SHORT).show()
            return
        }
        isSendingNotice = true
        scope.launch {
            try {
                val token = getIdToken()
                val client = OkHttpClient()
                val url = "$dbBaseUrl/notice.json?auth=$token"
                
                val jsonPayload = JSONObject().apply { 
                    put("text", noticeText.trim())
                    put("timestamp", System.currentTimeMillis())
                }.toString()
                
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val request = Request.Builder().url(url).put(jsonPayload.toRequestBody(mediaType)).build()
                
                withContext(Dispatchers.IO) { client.newCall(request).execute() }.close()
                Toast.makeText(context, "Notice Sent!", Toast.LENGTH_SHORT).show()
                noticeText = ""
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally { isSendingNotice = false }
        }
    }

    fun loadDevices() {
        isLoadingDevices = true
        scope.launch {
            try {
                val token = getIdToken()
                val client = OkHttpClient()
                val url = "$dbBaseUrl/devices.json?auth=$token"
                val request = Request.Builder().url(url).build()
                
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
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
                        var dBanned = false
                        var dApprovedBy = ""
                        if (valueObj is JSONObject) {
                            dName = valueObj.optString("name", "")
                            dExpire = valueObj.optString("expire", "")
                            dBanned = valueObj.optBoolean("banned", false)
                            dApprovedBy = valueObj.optString("approvedBy", "")
                        } else {
                            dExpire = valueObj?.toString() ?: ""
                        }
                        newList.add(DeviceItem(id = key, name = dName, expire = dExpire, isBanned = dBanned, approvedBy = dApprovedBy))
                    }
                }
                devicesList = newList.sortedBy { it.name.lowercase() }
            } catch (e: Exception) { } finally { isLoadingDevices = false }
        }
    }

    fun saveDevice(devId: String, name: String, days: Int, isBanned: Boolean = false, isEdit: Boolean = false) {
        val finalId = devId.trim()
        val finalName = name.trim()
        if (finalId.isEmpty() || finalName.isEmpty()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, days)
        val sdf = SimpleDateFormat("dd:MM:yyyy", Locale.US)
        val expireDate = sdf.format(calendar.time)

        isSavingDevice = true
        scope.launch {
            try {
                val token = getIdToken()
                val client = OkHttpClient()
                val url = "$dbBaseUrl/devices/$finalId.json?auth=$token"
                
                val currentUserEmail = auth?.currentUser?.email?.lowercase() ?: ""
                val jsonPayload = JSONObject().apply {
                    put("name", finalName)
                    put("expire", expireDate)
                    put("banned", isBanned)
                    if (!isEdit) {
                        put("approvedBy", currentUserEmail)
                    } else {
                        // Preserve original approvedBy during edit, or update it
                        put("approvedBy", editDeviceItem?.approvedBy ?: currentUserEmail)
                    }
                }.toString()
                
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val request = Request.Builder().url(url).put(jsonPayload.toRequestBody(mediaType)).build()
                
                withContext(Dispatchers.IO) { client.newCall(request).execute() }.close()
                
                Toast.makeText(context, if(isEdit) "Device Updated!" else "Device Activated!", Toast.LENGTH_SHORT).show()
                if (!isEdit) {
                    inputDeviceId = ""
                    inputName = ""
                    inputDays = 30f
                } else {
                    showEditDialog = false
                }
                loadDevices()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally { isSavingDevice = false }
        }
    }

    fun deleteDevice(deviceIdToDelete: String) {
        scope.launch {
            try {
                val token = getIdToken()
                val client = OkHttpClient()
                val url = "$dbBaseUrl/devices/$deviceIdToDelete.json?auth=$token"
                val request = Request.Builder().url(url).delete().build()
                withContext(Dispatchers.IO) { client.newCall(request).execute() }.close()
                Toast.makeText(context, "Device Removed!", Toast.LENGTH_SHORT).show()
                loadDevices()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun toggleBanDevice(device: DeviceItem) {
        scope.launch {
            try {
                val token = getIdToken()
                val client = OkHttpClient()
                val url = "$dbBaseUrl/devices/${device.id}.json?auth=$token"
                
                val jsonPayload = JSONObject().apply {
                    put("name", device.name)
                    put("expire", device.expire)
                    put("banned", !device.isBanned)
                }.toString()
                
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val request = Request.Builder().url(url).put(jsonPayload.toRequestBody(mediaType)).build()
                withContext(Dispatchers.IO) { client.newCall(request).execute() }.close()
                Toast.makeText(context, if(!device.isBanned) "Device Banned!" else "Device Unbanned!", Toast.LENGTH_SHORT).show()
                loadDevices()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        loadAppStatus()
        loadDevices()
    }

    val adminEmail = org.slf4j.z.getAdmin()
    val isOwner = auth?.currentUser?.email?.lowercase() == adminEmail
    val myEmail = auth?.currentUser?.email?.lowercase() ?: ""

    val filteredDevices = devicesList.filter { device ->
        (isOwner || device.approvedBy.lowercase() == myEmail) &&
        (device.id.lowercase().contains(searchQuery.lowercase()) ||
        device.name.lowercase().contains(searchQuery.lowercase()))
    }

    val visibleDevicesCount = devicesList.count { isOwner || it.approvedBy.lowercase() == myEmail }
    val totalApproved = devicesList.count { (!it.isBanned) && (isOwner || it.approvedBy.lowercase() == myEmail) }
    val totalBanned = devicesList.count { it.isBanned && (isOwner || it.approvedBy.lowercase() == myEmail) }

    var showSupportDialog by remember { mutableStateOf(false) }
    var showSubAdminsDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF090D1A), Color(0xFF020617))))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Admin Panel", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Device & App Control Center", color = Color.Gray, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isOwner) {
                        IconButton(
                            onClick = { showSubAdminsDialog = true },
                            modifier = Modifier.background(Color(0xFFEAB308).copy(alpha = 0.2f), RoundedCornerShape(10.dp)).border(1.dp, Color(0xFFEAB308).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        ) {
                            Icon(Icons.Default.ManageAccounts, contentDescription = "Manage Sub-Admins", tint = Color(0xFFEAB308))
                        }
                    }
                    IconButton(
                        onClick = { showSupportDialog = true },
                        modifier = Modifier.background(Color(0xFF8B5CF6).copy(alpha = 0.2f), RoundedCornerShape(10.dp)).border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = "Support Inbox", tint = Color(0xFF8B5CF6))
                    }
                    IconButton(
                        onClick = { onSwitchToUser() },
                        modifier = Modifier.background(Color(0xFF38BDF8).copy(alpha = 0.2f), RoundedCornerShape(10.dp)).border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = "User Mode", tint = Color(0xFF38BDF8))
                    }
                    IconButton(
                        onClick = { auth?.signOut() },
                        modifier = Modifier.background(Color(0xFF1E293B), RoundedCornerShape(10.dp)).border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = Color(0xFFEF4444))
                    }
                }
            }
            
            // Dashboard Stats
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Approved Users", color = Color(0xFF10B981), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$totalApproved", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Banned Users", color = Color(0xFFEF4444), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$totalBanned", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Notice System
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = "Notice", tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Global Notice System", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = noticeText,
                                onValueChange = { noticeText = it },
                                placeholder = { Text("Write a notice to show to all users...") },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFF59E0B), unfocusedBorderColor = Color(0xFF334155)
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { sendNotice() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                enabled = !isSendingNotice
                            ) {
                                if (isSendingNotice) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                else Text("Send Notice", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 2. Add Device
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AddModerator, contentDescription = "Add", tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Approve / Add Device", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = inputName, onValueChange = { inputName = it },
                                label = { Text("User Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155))
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = inputDeviceId, onValueChange = { inputDeviceId = it },
                                label = { Text("Device ID") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155))
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Activation Duration: ${inputDays.toInt()} Days", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Slider(
                                value = inputDays,
                                onValueChange = { inputDays = it },
                                valueRange = 1f..365f,
                                steps = 364,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { saveDevice(inputDeviceId, inputName, inputDays.toInt(), false, false) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                enabled = !isSavingDevice
                            ) {
                                if (isSavingDevice) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                else Text("Activate Device", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                // 3. App Power Status
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PowerSettingsNew, contentDescription = "Power", tint = if (appStatus == "ON") Color(0xFF10B981) else Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("APK Power Control", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                if (isLoadingStatus) CircularProgressIndicator(color = Color(0xFF10B981), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { updateAppStatus("ON") },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (appStatus == "ON") Color(0xFF10B981) else Color(0xFF1E293B), contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(48.dp),
                                    border = if (appStatus == "ON") null else BorderStroke(1.dp, Color(0xFF334155))
                                ) { Text("ON", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = { updateAppStatus("OFF") },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (appStatus == "OFF") Color(0xFFEF4444) else Color(0xFF1E293B), contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(48.dp),
                                    border = if (appStatus == "OFF") null else BorderStroke(1.dp, Color(0xFF334155))
                                ) { Text("OFF", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }

                // 4. Search & List Section
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Activated Devices (${devicesList.size})", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { loadDevices() }) { Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.Gray) }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = searchQuery, onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by name or device ID...") },
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF334155), unfocusedBorderColor = Color(0xFF1E293B))
                        )
                    }
                }

                if (isLoadingDevices) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF38BDF8))
                        }
                    }
                } else if (filteredDevices.isEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()) {
                            Text("No devices found.", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    items(filteredDevices) { device ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (device.isBanned) Color(0xFF3F1A1A) else Color(0xFF0F172A)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, if (device.isBanned) Color(0xFFEF4444) else Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(device.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        if (device.isBanned) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(modifier = Modifier.background(Color(0xFFEF4444), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                Text("BANNED", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("ID: ${device.id}", color = Color(0xFF38BDF8), fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Expire: ${device.expire}", color = Color.LightGray, fontSize = 11.sp)
                                }
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Edit
                                    IconButton(
                                        onClick = { 
                                            editDeviceItem = device
                                            editInputName = device.name
                                            editInputDays = 30f // Default add days
                                            showEditDialog = true
                                        },
                                        modifier = Modifier.background(Color(0xFF1E293B), RoundedCornerShape(8.dp)).size(36.dp)
                                    ) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp)) }
                                    
                                    // Ban/Unban
                                    IconButton(
                                        onClick = { toggleBanDevice(device) },
                                        modifier = Modifier.background(if (device.isBanned) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f), RoundedCornerShape(8.dp)).size(36.dp)
                                    ) { Icon(if (device.isBanned) Icons.Default.CheckCircle else Icons.Default.Block, contentDescription = "Ban", tint = if (device.isBanned) Color(0xFF10B981) else Color(0xFFF59E0B), modifier = Modifier.size(18.dp)) }

                                    // Delete
                                    IconButton(
                                        onClick = { deleteDevice(device.id) },
                                        modifier = Modifier.background(Color(0xFF3F1A1A), RoundedCornerShape(8.dp)).size(36.dp)
                                    ) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && editDeviceItem != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Device", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editInputName, onValueChange = { editInputName = it },
                        label = { Text("User Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Add more days from today: ${editInputDays.toInt()} Days", color = Color.White, fontSize = 14.sp)
                    Slider(
                        value = editInputDays,
                        onValueChange = { editInputDays = it },
                        valueRange = 1f..365f,
                        steps = 364
                    )
                }
            },
            confirmButton = {
                Button(onClick = { 
                    saveDevice(editDeviceItem!!.id, editInputName, editInputDays.toInt(), editDeviceItem!!.isBanned, true) 
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel", color = Color.LightGray) }
            },
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White
        )
    }

    if (showSupportDialog) {
        val myAllowedDevicesMap = devicesList.associate { it.id to it.name }
        AdminSupportConversationsDialog(
            onDismiss = { showSupportDialog = false },
            allowedDevicesMap = myAllowedDevicesMap,
            isOwner = isOwner
        )
    }

    if (showSubAdminsDialog) {
        SubAdminManagerDialog(onDismiss = { showSubAdminsDialog = false })
    }
}
