package com.blogspot.vadimaz.cameraapp.ui.camera

import com.blogspot.vadimaz.cameraapp.ui.compass.HorizontalCompass

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CameraScreen(
  modifier: Modifier = Modifier,
  viewModel: CameraViewModel = viewModel()
) {
  val context = LocalContext.current
  val view = LocalView.current

  // State to track permissions
  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }

  var hasStoragePermission by remember {
    mutableStateOf(
      if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
          PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
    onResult = { permissions ->
      hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
      hasStoragePermission = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
      } else {
        true
      }
    }
  )

  // Request permissions if not granted
  LaunchedEffect(Unit) {
    val permissions = mutableListOf(Manifest.permission.CAMERA)
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
      permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
    permissionLauncher.launch(permissions.toTypedArray())
  }

  // Hide the Status Bar dynamically for an immersive full-screen experience
  LaunchedEffect(view) {
    val window = (view.context as? Activity)?.window
    if (window != null) {
      val windowInsetsController = WindowCompat.getInsetsController(window, view)
      // Hide the status bar
      windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
      // Make it transient so the user can still swipe to see it if needed
      windowInsetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
  }

  val azimuth by viewModel.azimuth.collectAsStateWithLifecycle()

  Box(
    modifier = modifier.fillMaxSize().background(Color.Black),
    contentAlignment = Alignment.Center
  ) {
    if (hasCameraPermission && hasStoragePermission) {
      CameraPreviewAndCapture(context = context, azimuth = azimuth)
    } else {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
      ) {
        Text("Camera and Storage permissions are required to use this feature.", color = Color.White, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
          val permissions = mutableListOf(Manifest.permission.CAMERA)
          if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
          }
          permissionLauncher.launch(permissions.toTypedArray())
        }) {
          Text("Grant Permissions")
        }
      }
    }
  }
}

@Composable
fun CameraPreviewAndCapture(
  context: Context,
  azimuth: Float
) {
  val lifecycleOwner = LocalLifecycleOwner.current
  var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
  val coordinateTransformer = remember { MutableCoordinateTransformer() }

  val preview = remember {
    Preview.Builder().build().apply {
      setSurfaceProvider { request -> surfaceRequest = request }
    }
  }

  val imageCapture = remember {
    ImageCapture.Builder()
      .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
      .build()
  }

  val cameraSelector = remember {
    CameraSelector.Builder()
      .requireLensFacing(CameraSelector.LENS_FACING_BACK)
      .build()
  }

  var isCapturing by remember { mutableStateOf(false) }

  // Bind Camera Use Cases to Lifecycle
  LaunchedEffect(context, lifecycleOwner) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
      val cameraProvider = cameraProviderFuture.get()
      try {
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
          lifecycleOwner,
          cameraSelector,
          preview,
          imageCapture
        )
      } catch (e: Exception) {
        Log.e("CameraScreen", "Use case binding failed", e)
      }
    }, ContextCompat.getMainExecutor(context))
  }

  Box(modifier = Modifier.fillMaxSize()) {
    // 1. Viewfinder (Draws edge-to-edge behind the controls)
    surfaceRequest?.let { request ->
      CameraXViewfinder(
        surfaceRequest = request,
        coordinateTransformer = coordinateTransformer,
        modifier = Modifier.fillMaxSize()
      )
    } ?: Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator(color = Color.White)
    }

    // 2. Overlay UI Controls
    Column(
      modifier = Modifier.fillMaxSize()
    ) {
      // HorizontalCompass takes the full width and sits at the very top (status bar space)
      HorizontalCompass(azimuth = azimuth)

      // The rest of the UI controls are padded and aligned to the bottom
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .safeDrawingPadding() // Handles bottom navigation bar space safely
          .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Bottom control panel with Capture button
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = {
              if (isCapturing) return@Button
              isCapturing = true

              // Configure MediaStore output options
              val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                  put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/CameraApp")
                }
              }

              val outputOptions = ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
              ).build()

              imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                  override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    isCapturing = false
                    Toast.makeText(context, "Photo saved successfully to Gallery!", Toast.LENGTH_LONG).show()
                  }

                  override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                    Toast.makeText(context, "Failed to save photo: ${exception.message}", Toast.LENGTH_LONG).show()
                    Log.e("CameraScreen", "Photo capture failed", exception)
                  }
                }
              )
            },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = if (isCapturing) Color.Gray else Color.White),
            modifier = Modifier.size(80.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
          ) {
            Box(
              modifier = Modifier.size(68.dp).background(Color.Transparent, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              if (isCapturing) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(36.dp))
              } else {
                // Inner white circle to resemble camera shutter button
                Box(
                  modifier = Modifier
                    .size(60.dp)
                    .background(Color.White, CircleShape)
                )
              }
            }
          }
        }
      }
    }
  }
}
