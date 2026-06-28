package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.GameViewModel
import com.example.ui.theme.SgkaraFamily

@Composable
fun LoginScreen(viewModel: GameViewModel, modifier: Modifier = Modifier) {
    val isLoggingIn by viewModel.isLoggingIn.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()

    val green = Color(0xFF39FF14)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "تمرکز",
                color = green,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SgkaraFamily
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "برای ذخیره‌ی پیشرفتت وارد شو",
                color = Color.LightGray,
                fontSize = 14.sp,
                fontFamily = SgkaraFamily,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (isLoggingIn) {
                CircularProgressIndicator(color = green)
            } else {
                Button(
                    onClick = { viewModel.guestLogin() },
                    colors = ButtonDefaults.buttonColors(containerColor = green),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = "ورود",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SgkaraFamily
                    )
                }

                if (loginError != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = loginError ?: "",
                        color = Color(0xFFFF4D6D),
                        fontSize = 13.sp,
                        fontFamily = SgkaraFamily,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
