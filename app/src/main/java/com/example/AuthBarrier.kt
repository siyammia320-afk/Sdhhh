package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthBarrier(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var initError by remember { mutableStateOf<String?>(null) }
    val auth = remember {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            try {
                if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                    val options = com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId("1:171803187901:android:06f962b35043d369bcf5d4")
                        .setApiKey("AIzaSyDoegs-mez3YrhxM_uzx6Q6vKqifR5FXYQ")
                        .setDatabaseUrl("https://my-original-apk-default-rtdb.firebaseio.com")
                        .setProjectId("my-original-apk")
                        .setStorageBucket("my-original-apk.firebasestorage.app")
                        .build()
                    com.google.firebase.FirebaseApp.initializeApp(context, options)
                }
                FirebaseAuth.getInstance()
            } catch (innerEx: Exception) {
                initError = innerEx.localizedMessage ?: innerEx.toString()
                null
            }
        }
    }

    if (auth == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090D1A)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "ফায়ারবেস অথেনটিকেশন ইনিশিয়ালাইজ করতে ব্যর্থ হয়েছে।",
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (initError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Error: $initError",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "দয়া করে গুগল প্লে সার্ভিস সচল আছে কিনা তা চেক করুন।",
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }
        }
        return
    }

    var currentUser by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser != null) {
        val isPasswordProvider = currentUser?.providerData?.any { it.providerId == "password" } == true
        val isEmailVerified = currentUser?.isEmailVerified == true

        if (isPasswordProvider && !isEmailVerified) {
            var isReloading by remember { mutableStateOf(false) }
            var isResending by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF090D1A), // Ultra Deep Slate
                                Color(0xFF030712)  // Deep Black
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .widthIn(max = 420.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFF3B82F6).copy(alpha = 0.15f), RoundedCornerShape(40.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Verification Email",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "ইমেইল ভেরিফিকেশন প্রয়োজন",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "একটি ভেরিফিকেশন লিঙ্ক আপনার নিম্নোক্ত ইমেইলে পাঠানো হয়েছে। দয়া করে আপনার ইনবক্স অথবা স্প্যাম ফোল্ডার চেক করুন।",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser?.email ?: "",
                                color = Color(0xFF38BDF8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            isReloading = true
                            scope.launch {
                                try {
                                    auth.currentUser?.reload()?.await()
                                    currentUser = auth.currentUser
                                    if (currentUser?.isEmailVerified == true) {
                                        Toast.makeText(context, "সফলভাবে ইমেইল ভেরিফাই করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "আপনার ইমেইল এখনও ভেরিফাই করা হয়নি। দয়া করে ইনবক্স চেক করে লিঙ্কটিতে ক্লিক করুন।", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "ত্রুটি: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isReloading = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isReloading
                    ) {
                        if (isReloading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = "Check", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("আমি ভেরিফাই করেছি", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            isResending = true
                            scope.launch {
                                try {
                                    auth.currentUser?.sendEmailVerification()?.await()
                                    Toast.makeText(context, "ভেরিফিকেশন ইমেইল পুনরায় পাঠানো হয়েছে!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "ত্রুটি: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isResending = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isResending
                    ) {
                        if (isResending) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Email, contentDescription = "Resend", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("আবার লিঙ্ক পাঠান (Resend)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    TextButton(
                        onClick = {
                            auth.signOut()
                            Toast.makeText(context, "লগআউট করা হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("লগইন পেজে ফিরে যান (Log Out)", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        } else {
            if (currentUser?.email?.lowercase() == "siyammia320@gmail.com") {
                AdminPanel()
            } else {
                content()
            }
        }
    } else {
        AuthScreen(auth)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(auth: FirebaseAuth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoginTab by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var isSendingResetEmail by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF090D1A), // Ultra Deep Slate
                        Color(0xFF030712)  // Deep Black
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
                .widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo or Auth Header Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(40.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Auth Logo",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "নিরাপদ সাইন আপ ও লগইন",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "অ্যাপটি ব্যবহার করতে প্রথমে অ্যাকাউন্ট তৈরি অথবা লগইন করুন",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Switch Tab Indicator (Login vs Register)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { isLoginTab = true; errorMessage = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLoginTab) Color(0xFF10B981) else Color.Transparent,
                        contentColor = if (isLoginTab) Color.White else Color.LightGray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(text = "লগইন (Sign In)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { isLoginTab = false; errorMessage = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isLoginTab) Color(0xFF10B981) else Color.Transparent,
                        contentColor = if (!isLoginTab) Color.White else Color.LightGray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(text = "রেজিস্ট্রেশন (Sign Up)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Error Display Card
            errorMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3F1A1A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = msg,
                        color = Color(0xFFFCA5A5),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Input Fields
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("ইমেইল অ্যাড্রেস (Email)") },
                placeholder = { Text("example@gmail.com") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = Color.LightGray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF10B981),
                    unfocusedBorderColor = Color(0xFF475569),
                    focusedLabelColor = Color(0xFF10B981),
                    unfocusedLabelColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("পাসওয়ার্ড (Password)") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = Color.LightGray) },
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = icon, contentDescription = "Toggle password visibility", tint = Color.LightGray)
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF10B981),
                    unfocusedBorderColor = Color(0xFF475569),
                    focusedLabelColor = Color(0xFF10B981),
                    unfocusedLabelColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Confirm Password for registration
            AnimatedVisibility(
                visible = !isLoginTab,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("পাসওয়ার্ড নিশ্চিত করুন (Confirm Password)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password", tint = Color.LightGray) },
                        trailingIcon = {
                            val icon = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(imageVector = icon, contentDescription = "Toggle password visibility", tint = Color.LightGray)
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedLabelColor = Color(0xFF10B981),
                            unfocusedLabelColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Forgot Password Link (Only visible on Login Screen)
            if (isLoginTab) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(onClick = { showForgotPasswordDialog = true }) {
                        Text(
                            text = "পাসওয়ার্ড ভুলে গেছেন? (Forgot Password)",
                            color = Color(0xFF10B981),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Primary Auth Button
            Button(
                onClick = {
                    if (email.trim().isEmpty() || password.trim().isEmpty()) {
                        errorMessage = "দয়া করে ইমেইল এবং পাসওয়ার্ড দুটিই পূরণ করুন।"
                        return@Button
                    }
                    if (password.length < 6) {
                        errorMessage = "পাসওয়ার্ড অবশ্যই কমপক্ষে ৬ অক্ষরের হতে হবে।"
                        return@Button
                    }
                    if (!isLoginTab && password != confirmPassword) {
                        errorMessage = "পাসওয়ার্ড দুটি মেলেনি।"
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    scope.launch {
                        try {
                            if (isLoginTab) {
                                // Sign In
                                auth.signInWithEmailAndPassword(email.trim(), password).await()
                                Toast.makeText(context, "সফলভাবে লগইন করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            } else {
                                // Sign Up
                                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                                result.user?.sendEmailVerification()?.await()
                                Toast.makeText(context, "অ্যাকাউন্ট তৈরি সম্পূর্ণ হয়েছে! ভেরিফিকেশন ইমেইল পাঠানো হয়েছে।", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            errorMessage = when {
                                e.localizedMessage?.contains("ALREADY_IN_USE") == true -> "এই ইমেইলটি ইতিমধ্যেই নিবন্ধিত রয়েছে।"
                                e.localizedMessage?.contains("INVALID_EMAIL") == true -> "দয়া করে একটি সঠিক ইমেইল ঠিকানা প্রদান করুন।"
                                e.localizedMessage?.contains("WRONG_PASSWORD") == true -> "ভুল পাসওয়ার্ড। আবার চেষ্টা করুন।"
                                e.localizedMessage?.contains("USER_NOT_FOUND") == true -> "এই ইমেইলের কোনো অ্যাকাউন্ট পাওয়া যায়নি।"
                                else -> e.localizedMessage ?: "অনাকাঙ্ক্ষিত ত্রুটি ঘটেছে। পুনরায় চেষ্টা করুন।"
                            }
                        } finally {
                            isLoading = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isLoginTab) "লগইন করুন" else "রেজিস্ট্রেশন করুন",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1.0f),
                    color = Color.Gray.copy(alpha = 0.3f)
                )
                Text(
                    text = "অথবা",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1.0f),
                    color = Color.Gray.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Sign-In Button
            var isGoogleLoading by remember { mutableStateOf(false) }

            Button(
                onClick = {
                    isGoogleLoading = true
                    errorMessage = null
                    val credentialManager = CredentialManager.create(context)
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId("171803187901-jac8d7m1bv2er6n1lruvnrfuijaohurg.apps.googleusercontent.com")
                        .setAutoSelectEnabled(true)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    scope.launch {
                        try {
                            val result = credentialManager.getCredential(context, request)
                            val credential = result.credential
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            val idToken = googleIdTokenCredential.idToken
                            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                            auth.signInWithCredential(firebaseCredential).await()
                            Toast.makeText(context, "গুগল দিয়ে সফলভাবে লগইন করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            errorMessage = "গুগল সাইন-ইন ব্যর্থ হয়েছে: ${e.localizedMessage ?: e.toString()}"
                        } finally {
                            isGoogleLoading = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isGoogleLoading && !isLoading
            ) {
                if (isGoogleLoading) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "G  ",
                            color = Color(0xFFEA4335),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "গুগল দিয়ে লগইন করুন (Google)",
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = {
                Text(
                    text = "পাসওয়ার্ড রিসেট করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = "আপনার একাউন্টের ইমেইল ঠিকানাটি নিচে প্রবেশ করুন। আমরা আপনাকে পাসওয়ার্ড পরিবর্তন করার একটি লিঙ্ক পাঠাবো।",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = forgotPasswordEmail,
                        onValueChange = { forgotPasswordEmail = it },
                        label = { Text("ইমেইল অ্যাড্রেস") },
                        placeholder = { Text("example@gmail.com") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (forgotPasswordEmail.trim().isEmpty()) {
                            Toast.makeText(context, "দয়া করে ইমেইল লিখুন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSendingResetEmail = true
                        scope.launch {
                            try {
                                auth.sendPasswordResetEmail(forgotPasswordEmail.trim()).await()
                                Toast.makeText(context, "রিসেট ইমেইল পাঠানো হয়েছে! দয়া করে ইনবক্স চেক করুন।", Toast.LENGTH_LONG).show()
                                showForgotPasswordDialog = false
                            } catch (e: Exception) {
                                Toast.makeText(context, "ত্রুটি: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSendingResetEmail = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    enabled = !isSendingResetEmail
                ) {
                    if (isSendingResetEmail) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("রিসেট লিঙ্ক পাঠান")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForgotPasswordDialog = false }
                ) {
                    Text("বন্ধ করুন", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp)
        )
    }
}
