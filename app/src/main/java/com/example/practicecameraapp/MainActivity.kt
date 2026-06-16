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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.clickable
import androidx.compose.material3.CircularProgressIndicator
import com.google.mlkit.vision.barcode.BarcodeScanning
import androidx.compose.material.icons.filled.QrCodeScanner

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecognizeView(
    modifier: Modifier,
    bitmap: Bitmap,
    onBack: () -> Unit
){
    var editBitmap by remember { mutableStateOf(bitmap) }
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }
    var textList by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

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
            Button(onClick = {
                isLoading = true
                recognizeText(bitmap){resultBitmap, result ->
                    editBitmap = resultBitmap
                    textList = result
                    showSheet = true
                    isLoading = false
                }
            }){
                Icon(imageVector = Icons.Filled.TextFields, contentDescription = "文字読み取り")
            }
            Button(onClick = {
                isLoading = true
                recognizeBarcode(bitmap){ resultBitmap, result ->
                    editBitmap = resultBitmap
                    textList = result
                    showSheet = true
                    isLoading = false
                }
            }){
                Icon(imageVector = Icons.Filled.QrCodeScanner, contentDescription = "バーコード読み取り")
            }
            Button(onClick = {showSheet = true}){
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = "再表示")
            }
            Button(onClick = onBack){
                Icon(imageVector = Icons.Filled.CameraAlt, contentDescription = "カメラに戻る")
            }
        }
    }
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {showSheet = false},
            sheetState = sheetState
        ){
            Card(modifier = Modifier.align(Alignment.CenterHorizontally)
            ){
                Text(
                    text = "*** 認識結果 ***",
                    modifier = Modifier.padding(8.dp)
                )
            }
            Text(
                text = textList.joinToString("\n"),
                modifier = Modifier.padding(8.dp)
                    .verticalScroll(rememberScrollState())
            )
        }

        if (isLoading) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .clickable{/*なにもしない*/}
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ){
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

fun recognizeText(
    bitmap: Bitmap,
    onResult: (Bitmap, List<String>) -> Unit
){
    val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    val image = InputImage.fromBitmap(bitmap, 0)

    recognizer.process(image).addOnSuccessListener {result ->
        val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(resultBitmap)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLUE
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 4f
        }

        val results = mutableListOf<String>()

        result.textBlocks.forEach { block ->
            block.boundingBox?.let {canvas.drawRect(it, paint)}
            block.lines.forEach {
                it.boundingBox?.let { box -> canvas.drawRect(box, paint) }
                results.add(it.text)
            }
        }
        onResult(resultBitmap, results)
    }.addOnFailureListener { e ->
        onResult(bitmap, listOf("読み取り失敗: ${e.message}"))
    }
}

fun recognizeBarcode(bitmap: Bitmap, onResult: (Bitmap, List<String>) -> Unit){
    val scanner = BarcodeScanning.getClient()
    val image = InputImage.fromBitmap(bitmap, 0)

    scanner.process(image).addOnSuccessListener {barcodes ->
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(mutableBitmap)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.RED
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 4f
        }

        val results = mutableListOf<String>()
        barcodes.forEach{ barcode ->
            barcode.boundingBox?.let{canvas.drawRect(it, paint)}
            barcode.rawValue?.let {
                results.add(it)
            }
        }
        onResult(mutableBitmap, results)
    }.addOnFailureListener { e ->
        onResult(bitmap, listOf("読み取り失敗: ${e.message}"))
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    PracticeCameraAppTheme {
        Main()
    }
}