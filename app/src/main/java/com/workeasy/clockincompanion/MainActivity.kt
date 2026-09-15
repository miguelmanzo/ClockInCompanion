package com.workeasy.clockincompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.workeasy.clockincompanion.presentation.clockin.ClockInScreen
import com.workeasy.clockincompanion.presentation.theme.ClockInCompanionTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClockInCompanionTheme {
                ClockInScreen()
            }
        }
    }
}
