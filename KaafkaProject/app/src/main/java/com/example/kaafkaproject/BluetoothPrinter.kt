package com.example.kaafkaproject

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.OutputStream
import java.util.*

class BluetoothPrinter(private val context: Context, private val handler: Handler) {
    private val btAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var btSocket: BluetoothSocket
    private lateinit var outputStream: OutputStream

    private val REQUEST_ENABLE_BT = 1

    fun connectPrinter(deviceName: String) {
        // Check if Bluetooth is supported on the device
        if (btAdapter == null) {
            sendMessageToMainThread("BluetoothNotSupported")
            return
        }

        // Check and request Bluetooth permissions
        checkBluetoothPermissions()

        val device = btAdapter.bondedDevices.find { it.name == deviceName }

        device?.let {
            try {
                btSocket = it.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))

                // Check if the Bluetooth device is still available (not turned off)
                if (it.bondState == BluetoothDevice.BOND_BONDED) {
                    btSocket.connect()
                    outputStream = btSocket.outputStream

                    // Send a message to the main thread indicating successful connection
                    sendMessageToMainThread("PrinterConnected")

                } else {
                    // Send a message to the main thread indicating that the printer is not available
                    sendMessageToMainThread("PrinterNotAvailable")
                }
            } catch (e: Exception) {
                e.printStackTrace()

                // Send a message to the main thread indicating an error connecting to the printer
                sendMessageToMainThread("ErrorConnectingPrinter")
            }
        } ?: run {
            // Send a message to the main thread indicating that the printer is not found
            sendMessageToMainThread("PrinterNotFound")
        }
    }

    fun printText(text: String) {
        try {
            outputStream.write(text.toByteArray())
            outputStream.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun closeConnection() {
        try {
            btSocket.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun sendMessageToMainThread(messageText: String) {
        val message = handler.obtainMessage()
        val bundle = Bundle()
        bundle.putString("message", messageText)
        message.data = bundle
        handler.sendMessage(message)
    }

    private fun checkBluetoothPermissions() {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            // Permission not granted, request it
            ActivityCompat.requestPermissions(
                context as AppCompatActivity,
                arrayOf(
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ), REQUEST_ENABLE_BT
            )
        } else {
            // Permissions are granted, proceed with your Bluetooth operations
            // Note: If you are targeting Android 12 (API level 31) or higher, you need to request the LOCATION permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    context as AppCompatActivity,
                    arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT),
                    REQUEST_ENABLE_BT
                )
            }
        }
    }
    fun getOutputStream(): OutputStream {
        return outputStream
    }
}
