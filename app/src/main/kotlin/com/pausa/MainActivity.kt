package com.pausa

import android.content.Intent
import android.os.Bundle
import androidx.activity.*
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pausa.presentation.*

class MainActivity:ComponentActivity() {
    private var reminderRequest by mutableIntStateOf(0)
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if(intent.action=="reminder-start")reminderRequest++
        setContent {val vm:AppViewModel=viewModel();PausaApp(vm,reminderRequest,skipStartup=savedInstanceState!=null)}
    }
    override fun onNewIntent(intent:Intent) {super.onNewIntent(intent);setIntent(intent);if(intent.action=="reminder-start")reminderRequest++}
}
