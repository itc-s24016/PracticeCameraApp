package com.example.practicecameraapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.practicecameraapp.ui.theme.PracticeCameraAppTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PracticeCameraAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Main(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun Main(modifier: Modifier = Modifier) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "preview", modifier = modifier){
        composable("preview") {
            CameraPreview(Modifier.fillMaxSize()) {
                nav.navigate("recognize")
            }
        }
        composable("recognize"){
            RecognizeView(
                modifier = Modifier.fillMaxSize()
            ) {
                nav.popBackStack()
            }
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier,
    onCapture: () -> Unit
){
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Green)
    ){
        Button(onClick = {
            onCapture()
        }){
            Text("画像認識画面へ移動")
        }
    }
}

@Composable
fun RecognizeView(
    modifier: Modifier,
    onBack: () -> Unit
){
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Blue)
    ){
        Button(onClick = {
            onBack()
        }){
            Text("カメラプレビューに戻る")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    PracticeCameraAppTheme {
        Main()
    }
}