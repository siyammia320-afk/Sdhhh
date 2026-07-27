package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

data class ChatMessage(
    val id: String = "",
    val text: String,
    val senderId: String,
    val senderName: String,
    val isAdmin: Boolean,
    val timestamp: Long
)

suspend fun fetchMessages(userId: String): List<ChatMessage> {
    return try {
        val auth = FirebaseAuth.getInstance()
        val token = auth.currentUser?.getIdToken(false)?.await()?.token ?: return emptyList()
        val client = OkHttpClient()
        val url = "https://my-original-apk-default-rtdb.firebaseio.com/support_messages/$userId.json?auth=$token"
        val request = Request.Builder().url(url).build()
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        if (!response.isSuccessful) {
            withContext(Dispatchers.Main) {
                // Not ideal to use context here without passing it, but let's just log it or return empty
            }
        }
        val body = response.body?.string()?.trim() ?: ""
        response.close()
        
        val list = mutableListOf<ChatMessage>()
        if (body.isNotEmpty() && body != "null") {
            val json = JSONObject(body)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val msgObj = json.optJSONObject(key)
                if (msgObj != null) {
                    list.add(
                        ChatMessage(
                            id = key,
                            text = msgObj.optString("text", ""),
                            senderId = msgObj.optString("senderId", ""),
                            senderName = msgObj.optString("senderName", "User"),
                            isAdmin = msgObj.optBoolean("isAdmin", false),
                            timestamp = msgObj.optLong("timestamp", 0L)
                        )
                    )
                }
            }
        }
        list.sortedBy { it.timestamp }
    } catch (e: Exception) {
        emptyList()
    }
}

suspend fun fetchAllConversations(): List<String> {
    return try {
        val auth = FirebaseAuth.getInstance()
        val token = auth.currentUser?.getIdToken(false)?.await()?.token ?: return emptyList()
        val client = OkHttpClient()
        val url = "https://my-original-apk-default-rtdb.firebaseio.com/support_messages.json?shallow=true&auth=$token"
        val request = Request.Builder().url(url).build()
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        val body = response.body?.string()?.trim() ?: ""
        response.close()
        
        val list = mutableListOf<String>()
        if (body.isNotEmpty() && body != "null") {
            val json = JSONObject(body)
            val keys = json.keys()
            while (keys.hasNext()) {
                list.add(keys.next())
            }
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

suspend fun sendMessage(userId: String, text: String, isAdmin: Boolean, senderName: String) {
    try {
        val auth = FirebaseAuth.getInstance()
        val token = auth.currentUser?.getIdToken(false)?.await()?.token ?: return
        val currentUserId = auth.currentUser?.uid ?: return
        val client = OkHttpClient()
        
        val newMsgRef = "https://my-original-apk-default-rtdb.firebaseio.com/support_messages/$userId.json?auth=$token"
        // Generate simple ID by posting
        val jsonPayload = JSONObject().apply {
            put("text", text)
            put("senderId", currentUserId)
            put("senderName", if (isAdmin) "Admin" else senderName)
            put("isAdmin", isAdmin)
            put("timestamp", System.currentTimeMillis())
        }.toString()
        
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder().url(newMsgRef).post(jsonPayload.toRequestBody(mediaType)).build()
        withContext(Dispatchers.IO) { client.newCall(request).execute() }.close()
    } catch (e: Exception) { }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportChatDialog(userId: String, onDismiss: () -> Unit, isAdminMode: Boolean = false, senderName: String = "User") {
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }

    fun refresh() {
        scope.launch {
            messages = fetchMessages(userId)
            isLoading = false
        }
    }

    LaunchedEffect(userId) {
        refresh()
        while (true) {
            kotlinx.coroutines.delay(3000)
            messages = fetchMessages(userId)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
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
                    Text("Support Chat", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                
                if (isLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF38BDF8))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                        reverseLayout = true
                    ) {
                        items(messages.reversed()) { msg ->
                            val isMe = if (isAdminMode) msg.isAdmin else !msg.isAdmin
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                            ) {
                                Column(
                                    modifier = Modifier
                                        .background(if (isMe) Color(0xFF38BDF8) else Color(0xFF334155), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                        .fillMaxWidth(0.8f)
                                ) {
                                    Text(msg.senderName, color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.LightGray, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(msg.text, color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val textToSend = inputText
                                inputText = ""
                                scope.launch {
                                    sendMessage(userId, textToSend, isAdminMode, senderName)
                                    refresh()
                                }
                            }
                        },
                        modifier = Modifier.background(Color(0xFF38BDF8), RoundedCornerShape(24.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSupportConversationsDialog(onDismiss: () -> Unit, allowedDevicesMap: Map<String, String>, isOwner: Boolean) {
    var conversations by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val allConvos = fetchAllConversations()
        if (isOwner) {
            conversations = allConvos
        } else {
            conversations = allConvos.filter { allowedDevicesMap.containsKey(it) }
        }
        isLoading = false
    }

    if (selectedUserId != null) {
        SupportChatDialog(
            userId = selectedUserId!!,
            onDismiss = { selectedUserId = null },
            isAdminMode = true
        )
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
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
                        Text("User Support Inboxes", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF38BDF8))
                        }
                    } else if (conversations.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No messages from users yet.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            items(conversations) { uid ->
                                val displayName = allowedDevicesMap[uid] ?: "Unknown User ($uid)"
                                Card(
                                    onClick = { selectedUserId = uid },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                                ) {
                                    Text(displayName, color = Color.White, modifier = Modifier.padding(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
