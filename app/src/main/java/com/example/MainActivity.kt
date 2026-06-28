package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.GameViewModel
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainGameScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: GameViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        // کل برنامه راست‌چین (RTL) نمایش داده می‌شود
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

          if (!isLoggedIn) {
            LoginScreen(
              viewModel = viewModel,
              modifier = Modifier.fillMaxSize()
            )
          } else {
            // هنگام ورود/شروع، آخرین پیشرفت را از سرور بخوان
            LaunchedEffect(Unit) { viewModel.pullProfile() }
            MainGameScreen(
              viewModel = viewModel,
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }
    }
  }

  // هنگام خروج/رفتن به پس‌زمینه، پیشرفت را روی سرور ذخیره کن
  override fun onStop() {
    super.onStop()
    viewModel.pushProfile()
  }
}
