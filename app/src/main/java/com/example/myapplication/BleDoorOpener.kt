package com.example.myapplication

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.myapplication.Controller.IdentifyRequest
import com.example.myapplication.Controller.SetState
import com.example.myapplication.Controller.States
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BleDoorOpener(private val context: Context) {
    companion object {
        private const val TAG = "BleDoorOpener"
        private const val DEVICE_ADDRESS = "C0:BB:CC:DD:EE:15"
        private const val TOKEN = "hqggZ4O1WNjEqXOY"
        private val SERVICE_UUID_EXPECTED =
            java.util.UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb")
        private val AUTH_CHAR_UUID_EXPECTED =
            java.util.UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb")
        private val COMMAND_CHAR_UUID_EXPECTED =
            java.util.UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb")
    }

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var gatt: BluetoothGatt? = null
    private var authChar: BluetoothGattCharacteristic? = null
    private var cmdChar: BluetoothGattCharacteristic? = null

    @SuppressLint("MissingPermission")
    suspend fun sendState(targetState: States): Boolean = suspendCancellableCoroutine { cont ->
        val device = adapter?.getRemoteDevice(DEVICE_ADDRESS)
        if (device == null) {
            Log.e(TAG, "Device not found: $DEVICE_ADDRESS")
            cont.resume(false)
            return@suspendCancellableCoroutine
        }

        var result: Boolean? = null
        var commanded = false

        gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                Log.d(TAG, "onConnectionStateChange status=$status newState=$newState")
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    g.requestMtu(517)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    val finalResult = result ?: commanded
                    Log.d(TAG, "Disconnected → resume($finalResult)")
                    cont.resume(finalResult)
                    g.close()
                }
            }

            override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
                Log.d(TAG, "onMtuChanged mtu=$mtu status=$status")
                g.discoverServices()
            }

            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                Log.d(TAG, "onServicesDiscovered status=$status")
                val svcList = g.services
                var svc = g.getService(SERVICE_UUID_EXPECTED)
                if (svc == null) {
                    svc = svcList.firstOrNull {
                        it.uuid.toString().lowercase().contains("ff00") || it.uuid.toString().lowercase().contains("00ff")
                    }
                    Log.w(TAG, "Expected service not found, fallback to $svc")
                }
                if (svc == null) {
                    Log.e(TAG, "No matching service → abort")
                    result = false
                    g.disconnect()
                    return
                }
                authChar = svc.characteristics.firstOrNull {
                    it.uuid == AUTH_CHAR_UUID_EXPECTED || it.uuid.toString().contains("ff02", true)
                }
                cmdChar = svc.characteristics.firstOrNull {
                    it.uuid == COMMAND_CHAR_UUID_EXPECTED || it.uuid.toString().contains("ff01", true)
                }
                if (authChar == null || cmdChar == null) {
                    Log.e(TAG, "Missing auth or cmd char → abort")
                    result = false
                    g.disconnect()
                    return
                }
                val authReq = IdentifyRequest.newBuilder().setToken(TOKEN).build()
                authChar!!.apply {
                    writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    value = authReq.toByteArray()
                }
                g.writeCharacteristic(authChar)
            }

            override fun onCharacteristicWrite(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                Log.d(TAG, "onCharacteristicWrite uuid=${characteristic.uuid} status=$status")
                if (characteristic == authChar) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            val cmd = SetState.newBuilder().setState(targetState).build()
                            cmdChar!!.apply {
                                writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                                value = cmd.toByteArray()
                            }
                            g.writeCharacteristic(cmdChar)
                        }, 500)
                    } else {
                        Log.e(TAG, "AUTH write failed: $status")
                        result = false
                        g.disconnect()
                    }
                } else if (characteristic == cmdChar) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        commanded = true
                    } else {
                        Log.e(TAG, "COMMAND write failed: $status")
                        result = false
                    }
                    g.disconnect()
                }
            }
        })
        cont.invokeOnCancellation { gatt?.disconnect() }
    }
}