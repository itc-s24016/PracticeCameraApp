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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.Icon
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PracticeCameraAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ConfirmPermission(modifier = Modifier.padding(innerPadding)) {
                        Main(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ConfirmPermission(
    modifier: Modifier,
    content: @Composable () -> Unit
){
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted){
            cameraPermission.launchPermissionRequest()
        }
    }
    if (cameraPermission.status.isGranted){
        content()
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ){
            Text("カメラの使用を許可してください")
        }
    }
}

@Composable
fun Main(modifier: Modifier = Modifier) {
    val nav = rememberNavController()
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    NavHost(navController = nav, startDestination = "preview", modifier = modifier){
        composable("preview") {
            CameraPreview(Modifier.fillMaxSize()) { bmp ->
                capturedBitmap = bmp
                nav.navigate("recognize")
            }
        }
        composable("recognize"){
            RecognizeView(
                modifier = Modifier.fillMaxSize(),
                bitmap = capturedBitmap!!
            ) {
                nav.popBackStack()
            }
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier,
    onCapture: (Bitmap) -> Unit
){
    val context = LocalContext.current
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(LifecycleCameraController.IMAGE_CAPTURE)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = {context ->
                PreviewView(context).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    controller = cameraController
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        Button(
            onClick = {
                cameraController.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback(){
                        override fun onCaptureSuccess(image: ImageProxy){
                            val bitmap = image.use {
                                val bmp = imageProxyToBitmap(it)
                                rotateBitmapIfNeeded(bmp, it.imageInfo.rotationDegrees)
                            }
                            onCapture(bitmap)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            onCapture(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Icon(imageVector = Icons.Filled.Camera, contentDescription = "撮影")
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buf = image.planes.firstOrNull()?.buffer
        ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val bytes = ByteArray(buf.remaining()).also { buf.get(it) }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

private fun rotateBitmapIfNeeded(src: Bitmap, rotationDegrees: Int): Bitmap {
    if (rotationDegrees == 0) return src
    val m = Matrix().apply {postRotate(rotationDegrees.toFloat())}
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true).also {
        if (it != src) src.recycle()
    }
}

@Composable
fun RecognizeView(
    modifier: Modifier,
    bitmap: Bitmap,
    onBack: () -> Unit
){
    var editBitmap by remember { mutableStateOf(bitmap) }
    Box(modifier = modifier.fillMaxSize()){
        Image(
            bitmap = editBitmap.asImageBitmap(),
            contentDescription = "撮影画像",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ){
            Button(onClick = onBack){
                Icon(imageVector = Icons.Filled.CameraAlt, contentDescription = "カメラに戻る")
            }
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