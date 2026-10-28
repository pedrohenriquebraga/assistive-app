package com.eitalab.objectdetection

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.view.ScaleGestureDetector
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.eitalab.objectdetection.databinding.MainActivityBinding
import com.eitalab.objectdetection.src.services.BluetoothService
import com.eitalab.objectdetection.src.tensorflow.TensorflowController
import com.eitalab.objectdetection.src.tensorflow.Utils
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var tf: TensorflowController
    private lateinit var bluetoothService: BluetoothService
    private val utils = Utils()
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private lateinit var binding: MainActivityBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var modelFile: File
    private lateinit var capturedImage: Bitmap
    private lateinit var camera: Camera

    private var isBluetoothInitialized = false
    private var isDeviceConnected = false
    private var isScanning = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val defaultAssistiveDeviceName = "Assistive Device"

    @SuppressLint("MissingPermission")
    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                startAppFeatures()
            } else {
                Toast.makeText(this, "Permissões necessárias não concedidas.", Toast.LENGTH_LONG)
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = getRequiredPermissions()
        val allPermissionsGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allPermissionsGranted) {
            startAppFeatures()
        } else {
            requestMultiplePermissionsLauncher.launch(permissionsToRequest)
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        return permissions.toTypedArray()
    }

    @SuppressLint("MissingPermission")
    private fun startAppFeatures() {
        initCamera()
        initBluetoothService()
        setupDetectButton()
    }

    private fun setupDetectButton() {
        binding.btDetect.setOnClickListener {
            if (::capturedImage.isInitialized) {
                val detections = tf.detect(capturedImage)
                if (detections != null) {
                    runOnUiThread {
                        binding.detectionOverlay.setResults(detections)
                    }

                    if (::bluetoothService.isInitialized && isDeviceConnected) {
                        detections.forEach { detect ->
                            bluetoothService.sendMessage("Objeto ${detect.label} detectado com ${(detect.confidence * 100).toInt()}%\n")
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Aguarde a câmera inicializar...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initCamera() {
        modelFile = utils.getModelFileFromAssets("mobile_net_v3_small_300x300.tflite", filesDir, assets)
        tf = TensorflowController(modelFile)
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindPreview()
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("MissingPermission")
    private fun initBluetoothService() {
        if (isBluetoothInitialized) return

        bluetoothService = BluetoothService(this)
        bluetoothService.turnOnBluetooth()

        bluetoothService.setConnectionListener(object : BluetoothService.ConnectionListener {
            override fun onConnected() {
                runOnUiThread {
                    isDeviceConnected = true
                    isScanning = false
                    Toast.makeText(this@MainActivity, "Dispositivo Conectado!", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onDisconnected() {
                runOnUiThread {
                    isDeviceConnected = false
                    Toast.makeText(
                        this@MainActivity,
                        "Conexão perdida. Reconectando...",
                        Toast.LENGTH_LONG
                    ).show()
                    startBleScanLoop()
                }
            }
        })

        isBluetoothInitialized = true
        startBleScanLoop()
    }

    private fun startBleScanLoop() {
        if (isFinishing || isDestroyed || isDeviceConnected || isScanning) return

        if (!hasBluetoothPermissions()) return

        isScanning = true
        Log.d("BLE_LOOP", "Iniciando ciclo de scan...")

        bluetoothService.scanLeDevice(object : BluetoothService.OnDeviceFoundListener {
            @SuppressLint("MissingPermission")
            override fun onDeviceFound(device: BluetoothDevice) {
                try {
                    if (device.name == defaultAssistiveDeviceName) {
                        Log.d("BLE_LOOP", "BT05 Encontrado! Tentando conectar...")
                        bluetoothService.connectAssistiveDevice(device.address)
                    }
                } catch (e: SecurityException) {
                    Log.e("BLE_LOOP", "Erro de permissão: ${e.message}")
                }
            }

            override fun onScanFinished() {
                isScanning = false
                if (!isDeviceConnected && !isFinishing) {
                    Log.d("BLE_LOOP", "Scan finalizado sem conexão. Reiniciando em 1s...")
                    mainHandler.postDelayed({
                        startBleScanLoop()
                    }, 500)
                }
            }
        })
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun bindPreview() {
        val preview: Preview = Preview.Builder().build()
        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.surfaceProvider = binding.cameraPreview.surfaceProvider

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    try {
                        capturedImage = utils.capturedImageToBitmap(imageProxy, tf)
                    } catch (e: Exception) {
                        Log.e("ImageAnalyzer", "Erro ao capturar frame: ${e.message}")
                    } finally {
                        imageProxy.close()
                    }
                }
            }

        cameraProvider.unbindAll()
        camera = cameraProvider.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            preview,
            imageAnalyzer
        )

        setupPinchToZoom()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPinchToZoom() {
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentZoomRatio = camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                val delta = detector.scaleFactor
                camera.cameraControl.setZoomRatio(currentZoomRatio * delta)
                return true
            }
        }

        val scaleGestureDetector = ScaleGestureDetector(this, listener)

        binding.cameraPreview.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
        if (::bluetoothService.isInitialized) {
            bluetoothService.close()
        }
        cameraExecutor.shutdown()
    }
}