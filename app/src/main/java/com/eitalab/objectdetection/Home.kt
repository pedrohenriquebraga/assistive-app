package com.eitalab.objectdetection

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.eitalab.objectdetection.databinding.ActivityHomeBinding
import com.eitalab.objectdetection.src.services.BluetoothService
import com.eitalab.objectdetection.ui.main.HomeDashboardFragment
import com.eitalab.objectdetection.ui.main.SectionsPagerAdapter
import com.eitalab.objectdetection.ui.main.SettingsFragment

class Home : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    var bluetoothService: BluetoothService? = null
    var isDeviceConnected = false
        private set
    var isScanning = false
        private set

    private val defaultAssistiveDeviceName = "Assistive Device"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isBluetoothInitialized = false

    private val colorActiveDark = Color.parseColor("#121620")
    private val colorInactiveWhite = Color.parseColor("#FFFFFF")

    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                initBluetoothService()
            } else {
                Toast.makeText(this, "Permissões necessárias não concedidas.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViewPager()
        setupBottomTabBar()

        checkAndRequestPermissions()
    }

    private fun setupViewPager() {
        val adapter = SectionsPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 3
        binding.viewPager.isUserInputEnabled = false

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateTabBarVisuals(position)
            }
        })
    }

    private fun setupBottomTabBar() {
        val tabEntries = listOf(
            Pair(binding.tabHome, binding.tabHomePill) to 0,
            Pair(binding.tabCamera, binding.tabCameraPill) to 1,
            Pair(binding.tabHistory, binding.tabHistoryPill) to 2,
            Pair(binding.tabSettings, binding.tabSettingsPill) to 3
        )

        tabEntries.forEach { (views, pos) ->
            views.first.setOnClickListener { selectTab(pos) }
            views.second.setOnClickListener { selectTab(pos) }
        }

        // Inicia na Home (tab 0)
        selectTab(0)
    }

    fun selectTab(position: Int) {
        binding.viewPager.setCurrentItem(position, false)
        updateTabBarVisuals(position)
    }

    private fun updateTabBarVisuals(position: Int) {
        // Tab 0: Início
        if (position == 0) {
            binding.tabHomePill.setBackgroundResource(R.drawable.bg_tab_active)
            binding.tabHomeIcon.imageTintList = ColorStateList.valueOf(colorActiveDark)
            binding.tabHomeLabel.setTextColor(colorActiveDark)
        } else {
            binding.tabHomePill.background = null
            binding.tabHomeIcon.imageTintList = ColorStateList.valueOf(colorInactiveWhite)
            binding.tabHomeLabel.setTextColor(colorInactiveWhite)
        }

        // Tab 1: Câmera
        if (position == 1) {
            binding.tabCameraPill.setBackgroundResource(R.drawable.bg_tab_active)
            binding.tabCameraIcon.imageTintList = ColorStateList.valueOf(colorActiveDark)
            binding.tabCameraLabel.setTextColor(colorActiveDark)
        } else {
            binding.tabCameraPill.background = null
            binding.tabCameraIcon.imageTintList = ColorStateList.valueOf(colorInactiveWhite)
            binding.tabCameraLabel.setTextColor(colorInactiveWhite)
        }

        // Tab 2: Histórico
        if (position == 2) {
            binding.tabHistoryPill.setBackgroundResource(R.drawable.bg_tab_active)
            binding.tabHistoryIcon.imageTintList = ColorStateList.valueOf(colorActiveDark)
            binding.tabHistoryLabel.setTextColor(colorActiveDark)
        } else {
            binding.tabHistoryPill.background = null
            binding.tabHistoryIcon.imageTintList = ColorStateList.valueOf(colorInactiveWhite)
            binding.tabHistoryLabel.setTextColor(colorInactiveWhite)
        }

        // Tab 3: Ajustes
        if (position == 3) {
            binding.tabSettingsPill.setBackgroundResource(R.drawable.bg_tab_active)
            binding.tabSettingsIcon.imageTintList = ColorStateList.valueOf(colorActiveDark)
            binding.tabSettingsLabel.setTextColor(colorActiveDark)
        } else {
            binding.tabSettingsPill.background = null
            binding.tabSettingsIcon.imageTintList = ColorStateList.valueOf(colorInactiveWhite)
            binding.tabSettingsLabel.setTextColor(colorInactiveWhite)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = getRequiredPermissions()
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            initBluetoothService()
        } else {
            requestMultiplePermissionsLauncher.launch(permissions)
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.VIBRATE
        )
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
    private fun initBluetoothService() {
        if (isBluetoothInitialized) return

        try {
            bluetoothService = BluetoothService(this)
            bluetoothService?.turnOnBluetooth()

            bluetoothService?.setConnectionListener(object : BluetoothService.ConnectionListener {
                override fun onConnected() {
                    runOnUiThread {
                        isDeviceConnected = true
                        isScanning = false
                        Toast.makeText(this@Home, "Assistive Device Conectado!", Toast.LENGTH_SHORT).show()
                        notifyFragmentsBleStatus()
                    }
                }

                override fun onDisconnected() {
                    runOnUiThread {
                        isDeviceConnected = false
                        Toast.makeText(this@Home, "Conexão perdida. Reconectando...", Toast.LENGTH_SHORT).show()
                        notifyFragmentsBleStatus()
                        startBleScanLoop()
                    }
                }
            })

            isBluetoothInitialized = true
            startBleScanLoop()
        } catch (e: Exception) {
            Log.e("Home", "Erro ao inicializar Bluetooth: ${e.message}")
        }
    }

    fun triggerBleScan() {
        if (isDeviceConnected) return
        startBleScanLoop()
        Toast.makeText(this, "Buscando Assistive Device...", Toast.LENGTH_SHORT).show()
    }

    fun disconnectBle() {
        try {
            bluetoothService?.close()
            isDeviceConnected = false
            notifyFragmentsBleStatus()
            Toast.makeText(this, "Dispositivo desconectado.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startBleScanLoop() {
        val service = bluetoothService ?: return
        if (isFinishing || isDestroyed || isDeviceConnected || isScanning) return

        isScanning = true
        Log.d("Home_BLE", "Iniciando scan de dispositivo...")

        service.scanLeDevice(object : BluetoothService.OnDeviceFoundListener {
            @SuppressLint("MissingPermission")
            override fun onDeviceFound(device: BluetoothDevice) {
                try {
                    if (device.name == defaultAssistiveDeviceName) {
                        Log.d("Home_BLE", "Dispositivo encontrado! Conectando...")
                        service.connectAssistiveDevice(device.address)
                    }
                } catch (e: SecurityException) {
                    Log.e("Home_BLE", "Erro de permissão no BLE: ${e.message}")
                }
            }

            override fun onScanFinished() {
                isScanning = false
                if (!isDeviceConnected && !isFinishing) {
                    mainHandler.postDelayed({
                        startBleScanLoop()
                    }, 2000)
                }
            }
        })
    }

    private fun notifyFragmentsBleStatus() {
        supportFragmentManager.fragments.forEach { fragment ->
            when (fragment) {
                is HomeDashboardFragment -> fragment.updateBleStatus()
                is SettingsFragment -> fragment.updateBleStatus()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
        bluetoothService?.close()
    }
}
