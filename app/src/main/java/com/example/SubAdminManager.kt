package com.example

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

data class SubAdminItem(
    val email: String,
    val expire: String,
    val apiKey: String,
    val isBlocked: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubAdminManagerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var subAdmins by remember { mutableStateOf<List<SubAdminItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var inputEmail by remember { mutableStateOf("") }
    var inputDays by remember { mutableStateOf("30") }
    var inputApiKey by remember { mutableStateOf("") }

    val dbBaseUrl = "https://my-original-apk-default-rtdb.firebaseio.com"

    fun loadSubAdmins() {
        isLoading = true
        scope.launch {
            try {
                val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token ?: return@launch
                val client = OkHttpClient()
                val url = "$dbBaseUrl/sub_admins.json?auth=$token"
                val request = Request.Builder().url(url).build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                val body = response.body?.string()?.trim() ?: ""
                response.close()

                val list = mutableListOf<SubAdminItem>()
                if (body.isNotEmpty() && body != "null") {
                    val json = JSONObject(body)
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val encodedEmail = keys.next()
                        val email = encodedEmail.replace(",", ".")
                        val obj = json.optJSONObject(encodedEmail)
                        if (obj != null) {
                            list.add(
                                SubAdminItem(
                                    email = email,
                                    expire = obj.optString("expire", ""),
                                    apiKey = obj.optString("apiKey", ""),
                                    isBlocked = obj.optBoolean("blocked", false)
                                )
                            )
                        }
                    }
                }
                subAdmins = list.sortedBy { it.email }
            } catch (e: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadSubAdmins()
    }

    fun saveSubAdmin(email: String, days: Int, apiKey: String, isBlocked: Boolean = false) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty() || apiKey.trim().isEmpty()) {
            Toast.makeText(context, "Fill all fields", Toast.LENGTH_SHORT).show()
            return
        }
        val encodedEmail = cleanEmail.replace(".", ",")
        
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, days)
        val sdf = SimpleDateFormat("dd:MM:yyyy", Locale.US)
        val expireStr = sdf.format(cal.time)

        scope.launch {
            try {
                val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token ?: return@launch
                val client = OkHttpClient()
                val url = "$dbBaseUrl/sub_admins/$encodedEmail.json?auth=$token"
                
                val payload = JSONObject().apply {
                    put("expire", expireStr)
                    put("apiKey", apiKey.trim())
                    put("blocked", isBlocked)
                }.toString()

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val request = Request.Builder().url(url).put(payload.toRequestBody(mediaType)).build()
                withContext(Dispatchers.IO) { client.newCall(request).execute() }.close()

                Toast.makeText(context, "Sub-Admin Saved", Toast.LENGTH_SHORT).show()
                inputEmail = ""
                inputApiKey = ""
                loadSubAdmins()
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteSubAdmin(email: String) {
        scope.launch {
            try {
                val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token ?: return@launch
                val client = OkHttpClient()
                val encodedEmail = email.replace(".", ",")
                val url = "$dbBaseUrl/sub_admins/$encodedEmail.json?auth=$token"
                val request = Request.Builder().url(url).delete().build()
                withContext(Dispatchers.IO) { client.newCall(request).execute() }.close()
                loadSubAdmins()
            } catch (e: Exception) { }
        }
    }
    
    fun toggleBlock(admin: SubAdminItem) {
        scope.launch {
            try {
                val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token ?: return@launch
                val client = OkHttpClient()
                val encodedEmail = admin.email.replace(".", ",")
                val url = "$dbBaseUrl/sub_admins/$encodedEmail/blocked.json?auth=$token"
                val payload = (!admin.isBlocked).toString()
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val request = Request.Builder().url(url).put(payload.toRequestBody(mediaType)).build()
                withContext(Dispatchers.IO) { client.newCall(request).execute() }.close()
                loadSubAdmins()
            } catch (e: Exception) { }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B)).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Manage Sub-Admins", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Add New Sub-Admin", color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = inputEmail,
                                    onValueChange = { inputEmail = it },
                                    label = { Text("Email") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = inputApiKey,
                                    onValueChange = { inputApiKey = it },
                                    label = { Text("API Key") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = inputDays,
                                    onValueChange = { inputDays = it },
                                    label = { Text("Validity Days") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { saveSubAdmin(inputEmail, inputDays.toIntOrNull() ?: 30, inputApiKey) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Text("Add Account")
                                }
                            }
                        }
                    }

                    if (isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF38BDF8))
                            }
                        }
                    } else if (subAdmins.isEmpty()) {
                        item {
                            Text("No Sub-Admins found.", color = Color.Gray, modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    } else {
                        items(subAdmins) { admin ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(admin.email, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("API: ${admin.apiKey}", color = Color.LightGray, fontSize = 12.sp)
                                        Text("Expires: ${admin.expire}", color = if(admin.isBlocked) Color.Red else Color(0xFF38BDF8), fontSize = 12.sp)
                                    }
                                    IconButton(onClick = { toggleBlock(admin) }) {
                                        Icon(if (admin.isBlocked) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = "Block", tint = if (admin.isBlocked) Color.Red else Color.Green)
                                    }
                                    IconButton(onClick = { deleteSubAdmin(admin.email) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
