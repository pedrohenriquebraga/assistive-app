package com.eitalab.objectdetection.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.eitalab.objectdetection.Home
import com.eitalab.objectdetection.R
import com.eitalab.objectdetection.databinding.FragmentCameraBinding
import com.eitalab.objectdetection.src.services.HistoryRepository
import com.eitalab.objectdetection.src.services.StorageService
import com.eitalab.objectdetection.src.tensorflow.TensorflowController
import com.eitalab.objectdetection.src.tensorflow.Utils
import com.google.common.util.concurrent.ListenableFuture
import java.util.Locale
import java.util.concurrent.Executors

class CameraDetectionFragment : Fragment(), TextToSpeech.OnInitListener {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private var tf: TensorflowController? = null
    private val utils = Utils()
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var cameraProvider: ProcessCameraProvider? = null
    private var capturedImage: Bitmap? = null
    private var camera: Camera? = null

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var isTorchOn: Boolean = false

    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val historyRepo by lazy { HistoryRepository.getInstance(requireContext()) }
    private val storage by lazy { StorageService(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textToSpeech = TextToSpeech(requireContext(), this)
        setupDetectButton()
        setupControlButtons()
        initCamera()
    }

    private fun setupControlButtons() {
        binding.btnFlash.setOnClickListener {
            toggleFlash()
        }

        binding.btnSwitchCamera.setOnClickListener {
            toggleCameraLens()
        }
    }

    private fun toggleFlash() {
        val activeCamera = camera
        if (activeCamera == null) {
            Toast.makeText(requireContext(), "Câmera ainda não inicializada", Toast.LENGTH_SHORT).show()
            return
        }

        if (!activeCamera.cameraInfo.hasFlashUnit()) {
            Toast.makeText(requireContext(), "Flash indisponível nesta câmera", Toast.LENGTH_SHORT).show()
            return
        }

        val targetState = !isTorchOn
        activeCamera.cameraControl.enableTorch(targetState)
            .addListener({
                activity?.runOnUiThread {
                    isTorchOn = targetState
                    updateFlashButtonUI()
                }
            }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun toggleCameraLens() {
        val provider = cameraProvider ?: return
        val newLensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }

        val newSelector = CameraSelector.Builder().requireLensFacing(newLensFacing).build()
        if (!provider.hasCamera(newSelector)) {
            Toast.makeText(requireContext(), "Câmera oposta não disponível", Toast.LENGTH_SHORT).show()
            return
        }

        // Se o flash estiver ativado, desligue-o antes de alternar
        if (isTorchOn) {
            camera?.cameraControl?.enableTorch(false)
            isTorchOn = false
            updateFlashButtonUI()
        }

        lensFacing = newLensFacing
        bindPreview()

        val lensName = if (lensFacing == CameraSelector.LENS_FACING_FRONT) "frontal" else "traseira"
        Toast.makeText(requireContext(), "Câmera $lensName ativada", Toast.LENGTH_SHORT).show()
    }

    private fun updateFlashButtonUI() {
        if (_binding == null) return
        if (isTorchOn) {
            binding.ivFlash.setImageResource(R.drawable.ic_flash_on)
            binding.ivFlash.imageTintList = ColorStateList.valueOf(Color.parseColor("#FFCC00"))
            binding.btnFlash.setBackgroundResource(R.drawable.bg_circle_dark)
        } else {
            binding.ivFlash.setImageResource(R.drawable.ic_flash_off)
            binding.ivFlash.imageTintList = ColorStateList.valueOf(Color.WHITE)
            binding.btnFlash.setBackgroundResource(R.drawable.bg_circle_dark)
        }
    }

    private fun updateFlashButtonAvailability() {
        if (_binding == null) return
        val hasFlash = camera?.cameraInfo?.hasFlashUnit() == true
        binding.btnFlash.alpha = if (hasFlash) 1.0f else 0.4f
        binding.btnFlash.isEnabled = hasFlash
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("pt", "BR"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.setLanguage(Locale.US)
            }
            isTtsReady = true
        }
    }

    private fun speakDetected(label: String) {
        val isTtsEnabled = storage.getData("pref_tts_enabled", "true") == "true"
        if (isTtsEnabled && isTtsReady) {
            textToSpeech?.speak(
                "$label detectado",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "detection_tts"
            )
        }
    }

    private fun setupDetectButton() {
        binding.btDetect.setOnClickListener {
            val image = capturedImage
            val controller = tf
            if (image != null && controller != null) {
                val detections = controller.detect(image)
                if (detections != null && detections.isNotEmpty()) {
                    val best = detections.first()
                    val localizedLabel = best.localizedLabel
                    val confidencePct = best.confidencePercent

                    activity?.runOnUiThread {
                        // Atualiza overlay no Canvas
                        binding.detectionOverlay.setResults(detections)

                        // Atualiza card flutuante na tela da câmera
                        binding.cardDetectionResult.visibility = View.VISIBLE
                        binding.tvDetectedObjectTitle.text = localizedLabel
                        binding.tvDetectedObjectConfidence.text = "Detectado com $confidencePct% de precisão"
                        binding.btnSpeakAgain.setOnClickListener {
                            speakDetected(localizedLabel)
                        }
                    }

                    // Salva no repositório de histórico com nome traduzido
                    historyRepo.addDetection(localizedLabel, best.confidence)

                    // Envia para o Bluetooth se conectado
                    val homeActivity = activity as? Home
                    if (homeActivity?.isDeviceConnected == true) {
                        homeActivity.bluetoothService?.sendMessage("Objeto $localizedLabel detectado com $confidencePct%\n")
                    }

                    // Feedback por Voz (TTS)
                    speakDetected(localizedLabel)

                    // Feedback por Vibração
                    val isVibrationEnabled = storage.getData("pref_vibration_enabled", "true") == "true"
                    if (isVibrationEnabled) {
                        triggerVibration()
                    }
                } else {
                    Toast.makeText(requireContext(), "Nenhum objeto identificado com clareza.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Aguarde a câmera inicializar...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun triggerVibration() {
        try {
            val context = context ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(150)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initCamera() {
        try {
            val modelFile = utils.getModelFileFromAssets(
                "mobile_net_v3_small_300x300.tflite",
                requireContext().filesDir,
                requireContext().assets
            )
            tf = TensorflowController(modelFile)

            cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
            cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()
                bindPreview()
            }, ContextCompat.getMainExecutor(requireContext()))
        } catch (e: Exception) {
            Log.e("CameraFragment", "Erro ao inicializar camera/modelo: ${e.message}")
        }
    }

    private fun bindPreview() {
        val provider = cameraProvider ?: return
        if (_binding == null) return

        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        preview.surfaceProvider = binding.cameraPreview.surfaceProvider

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    try {
                        val controller = tf
                        if (controller != null) {
                            capturedImage = utils.capturedImageToBitmap(imageProxy, controller)
                        }
                    } catch (e: Exception) {
                        Log.e("ImageAnalyzer", "Erro ao processar frame: ${e.message}")
                    } finally {
                        imageProxy.close()
                    }
                }
            }

        try {
            provider.unbindAll()
            val boundCamera = provider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
            camera = boundCamera

            boundCamera.cameraInfo.torchState.observe(viewLifecycleOwner) { torchState ->
                isTorchOn = (torchState == TorchState.ON)
                updateFlashButtonUI()
            }

            updateFlashButtonAvailability()
            setupPinchToZoom()
        } catch (e: Exception) {
            Log.e("CameraFragment", "Falha ao vincular lifecycle da câmera: ${e.message}")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPinchToZoom() {
        val activeCamera = camera ?: return
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentZoomRatio = activeCamera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                val delta = detector.scaleFactor
                activeCamera.cameraControl.setZoomRatio(currentZoomRatio * delta)
                return true
            }
        }

        val scaleGestureDetector = ScaleGestureDetector(requireContext(), listener)
        binding.cameraPreview.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraProvider?.unbindAll()
        camera = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        fun newInstance() = CameraDetectionFragment()
    }
}
