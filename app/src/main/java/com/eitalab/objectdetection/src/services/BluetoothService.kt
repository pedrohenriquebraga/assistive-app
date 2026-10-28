package com.eitalab.objectdetection.src.services

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import java.util.UUID

class BluetoothService(private val context: Context) {

    interface OnDeviceFoundListener {
        fun onDeviceFound(device: BluetoothDevice)
        fun onScanFinished()
    }

    interface ConnectionListener {
        fun onConnected()
        fun onDisconnected()
    }

    private var isScanning = false
    private var isConnected = false
    private val handler = Handler(Looper.getMainLooper())

    private var currentScanCallback: ScanCallback? = null

    private val bluetoothManager by lazy { context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    private val bluetoothAdapter by lazy { bluetoothManager.adapter }
    private var bluetoothGatt: BluetoothGatt? = null

    private val SERVICE_UUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val CHAR_UUID: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

    private val SCAN_PERIOD: Long = 5000
    private val foundDeviceAddresses = mutableSetOf<String>()

    private var connectionListener: ConnectionListener? = null

    fun setConnectionListener(listener: ConnectionListener) {
        this.connectionListener = listener
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d("BLE", "Conectado. Descobrindo serviços...")
                    isConnected = true
                    handler.post { connectionListener?.onConnected() }
                    gatt?.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d("BLE", "Desconectado.")
                    isConnected = false
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                    handler.post { connectionListener?.onDisconnected() }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "Serviços prontos.")
                sendMessage("Conexão estabelecida e pronto para enviar novas mensagens!\n")
            } else {
                Log.w("BLE", "Falha na descoberta de serviços: $status")
            }
        }
    }

    init {
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth não disponível neste dispositivo", Toast.LENGTH_LONG)
                .show()
        }
    }

    @SuppressLint("MissingPermission")
    fun scanLeDevice(listener: OnDeviceFoundListener) {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return

        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return

        if (isScanning) {
            stopScan(listener)
            return
        }

        foundDeviceAddresses.clear()

        currentScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val address = device.address
                if (address != null && !foundDeviceAddresses.contains(address)) {
                    foundDeviceAddresses.add(address)
                    listener.onDeviceFound(device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("BLE", "Scan falhou: $errorCode")
                stopScan(listener)
            }
        }

        handler.postDelayed({ stopScan(listener) }, SCAN_PERIOD)

        isScanning = true
        scanner.startScan(currentScanCallback)
        Log.d("BLE", "Scan iniciado")
    }

    @SuppressLint("MissingPermission")
    private fun stopScan(listener: OnDeviceFoundListener? = null) {
        if (!isScanning) return

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (currentScanCallback != null && scanner != null) {
            scanner.stopScan(currentScanCallback)
        }

        isScanning = false
        currentScanCallback = null
        listener?.onScanFinished()
        Log.d("BLE", "Scan finalizado")
    }

    @SuppressLint("MissingPermission")
    fun connectAssistiveDevice(newMacAddress: String = "") {
        if (isConnected || bluetoothGatt != null) return
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return

        val storage = StorageService(context as android.app.Activity)
        val savedMac = storage.getData("assistive_device_mac")

        val targetMac = if (newMacAddress.isNotEmpty()) {
            storage.saveData("assistive_device_mac", newMacAddress)
            newMacAddress
        } else {
            savedMac
        }

        if (targetMac.isNotEmpty()) {
            try {
                val device = bluetoothAdapter?.getRemoteDevice(targetMac)
                bluetoothGatt = device?.connectGatt(context, true, gattCallback)
                Log.d("BLE", "Tentando conectar em: $targetMac")
            } catch (e: IllegalArgumentException) {
                Log.e("BLE", "Endereço MAC inválido: $targetMac")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun sendMessage(message: String) {
        if (!isConnected || bluetoothGatt == null) {
            Log.w("BLE", "Não conectado. Mensagem ignorada.")
            return
        }

        val service = bluetoothGatt?.getService(SERVICE_UUID)
        val characteristic = service?.getCharacteristic(CHAR_UUID)

        if (characteristic != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bluetoothGatt?.writeCharacteristic(
                    characteristic,
                    message.toByteArray(),
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = message.toByteArray()
                @Suppress("DEPRECATION")
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                bluetoothGatt?.writeCharacteristic(characteristic)
            }
        } else {
            Log.e("BLE", "Característica de escrita não encontrada.")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun turnOnBluetooth() {
        if (bluetoothAdapter?.isEnabled == false) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
            ) {
                return
            }
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(enableBtIntent)
        }
    }

    @SuppressLint("MissingPermission")
    fun close() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        isConnected = false
        stopScan()
    }

    private fun hasPermission(permission: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true

        return ActivityCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}