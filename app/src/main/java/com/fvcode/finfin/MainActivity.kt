package com.fvcode.finfin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.fvcode.finfin.navigation.FinfinNavGraph
import com.fvcode.finfin.ui.session.SessaoViewModel
import com.fvcode.finfin.ui.theme.FinfinTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: SessaoViewModel = hiltViewModel()
            val tema by vm.tema.collectAsState()
            FinfinTheme(tema = tema) {
                FinfinNavGraph(vm)
            }
        }
    }
}
