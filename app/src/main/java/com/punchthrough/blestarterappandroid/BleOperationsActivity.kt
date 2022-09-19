
package com.punchthrough.blestarterappandroid

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.punchthrough.blestarterappandroid.ble.ConnectionEventListener
import com.punchthrough.blestarterappandroid.ble.ConnectionManager
import com.punchthrough.blestarterappandroid.ble.isIndicatable
import com.punchthrough.blestarterappandroid.ble.isNotifiable
import com.punchthrough.blestarterappandroid.ble.isReadable
import com.punchthrough.blestarterappandroid.ble.isWritable
import com.punchthrough.blestarterappandroid.ble.isWritableWithoutResponse
import com.punchthrough.blestarterappandroid.ble.toHexString
import kotlinx.android.synthetic.main.activity_ble_operations.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.selector
import org.jetbrains.anko.yesButton
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

class BleOperationsActivity : AppCompatActivity() {

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var device: BluetoothDevice
    private val dateFormatter = SimpleDateFormat("MMM d, HH:mm:ss", Locale.US)
    private val characteristics by lazy {
        ConnectionManager.servicesOnDevice(device)?.flatMap { service ->
            service.characteristics ?: listOf()
        } ?: listOf()
    }
    private val characteristicProperties by lazy {
        characteristics.map { characteristic ->
            characteristic to mutableListOf<CharacteristicProperty>().apply {
                if (characteristic.isNotifiable()) add(CharacteristicProperty.Notifiable)
                if (characteristic.isIndicatable()) add(CharacteristicProperty.Indicatable)
                if (characteristic.isReadable()) add(CharacteristicProperty.Readable)
                if (characteristic.isWritable()) add(CharacteristicProperty.Writable)
                if (characteristic.isWritableWithoutResponse()) {
                    add(CharacteristicProperty.WritableWithoutResponse)
                }
            }.toList()
        }.toMap()
    }
    private val characteristicAdapter: CharacteristicAdapter by lazy {
        CharacteristicAdapter(characteristics) { characteristic ->
            showCharacteristicOptions(characteristic)
        }
    }
    private var notifyingCharacteristics = mutableListOf<UUID>()

    override fun onCreate(savedInstanceState: Bundle?) {
        ConnectionManager.registerListener(connectionEventListener)
        super.onCreate(savedInstanceState)
        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            ?: error("Missing BluetoothDevice from MainActivity!")

        setContentView(R.layout.activity_ble_operations)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            title = device.name ?: "Unnamed" //getString(R.string.ble_playground)
        }
        setupRecyclerView()

        swipeRefreshLayout.setOnRefreshListener {
            characteristicAdapter.notifyDataSetChanged()
            swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onDestroy() {
        ConnectionManager.unregisterListener(connectionEventListener)
        ConnectionManager.teardownConnection(device)
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        characteristics_recycler_view.apply {
            adapter = characteristicAdapter
            layoutManager = LinearLayoutManager(
                this@BleOperationsActivity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
        }

        val animator = characteristics_recycler_view.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    @SuppressLint("SetTextI18n")
    private fun log(message: String) {
        val formattedMessage = String.format("%s -> %s", dateFormatter.format(Date()), message)
        runOnUiThread {
            val current = log_text_view.text
            /*
            val currentLogText = if (log_text_view.text.isEmpty()) {
                "Seleccione una característica"
            } else {
                log_text_view.text
            } */
            log_text_view.text = "$current$formattedMessage\n\n"
            log_scroll_view.post { log_scroll_view.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun showCharacteristicOptions(characteristic: BluetoothGattCharacteristic) {
        characteristicProperties[characteristic]?.let { properties ->
            selector("Select an action to perform", properties.map { it.action }) { _, i ->
                when (properties[i]) {
                    CharacteristicProperty.Readable -> {
                        ConnectionManager.readCharacteristic(device, characteristic)
                    }
                    CharacteristicProperty.Writable, CharacteristicProperty.WritableWithoutResponse -> {
                        showWritePayloadDialog(characteristic)
                    }
                    CharacteristicProperty.Notifiable, CharacteristicProperty.Indicatable -> {
                        if (notifyingCharacteristics.contains(characteristic.uuid)) {
                            log("Disabling notifications on ${characteristic.uuid}")
                            ConnectionManager.disableNotifications(device, characteristic)
                        } else {
                            log("Enabling notifications on ${characteristic.uuid}")
                            ConnectionManager.enableNotifications(device, characteristic)
                        }
                    }
                }
            }
        }
        //characteristicAdapter.notifyDataSetChanged()
    }

    @SuppressLint("InflateParams")
    private fun showWritePayloadDialog(characteristic: BluetoothGattCharacteristic) {
        val hexField = layoutInflater.inflate(R.layout.edittext_hex_payload, null) as EditText
        alert {
            customView = hexField
            isCancelable = false
            positiveButton("Enviar") {
                with(hexField.text.toString()) {
                    if (isNotBlank() && isNotEmpty()) {
                        val bytes = formato()
                        //val bytes = toByteArray()
                        //val bytes = hexToBytes()
                        //log("Writing to ${characteristic.uuid}: ${hexField.text.toString()}")
                        ConnectionManager.writeCharacteristic(device, characteristic, bytes)
                    } else {
                        log("Escriba un mensaje en ${characteristic.uuid}")
                    }
                }
            }
            negativeButton("Cancelar") { }
            neutralPressed("Más") {
                alert {
                    isCancelable = false
                    negativeButton("Cancelar") {}
                    positiveButton("GPS") {
                        fusedLocationProviderClient =
                            LocationServices.getFusedLocationProviderClient(this@BleOperationsActivity)

                        val task = if (ActivityCompat.checkSelfPermission(
                                this@BleOperationsActivity,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                this@BleOperationsActivity,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                this@BleOperationsActivity,
                                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                                101
                            )
                            return@positiveButton
                        } else {
                            fusedLocationProviderClient.lastLocation
                        }

                        task.addOnSuccessListener {
                            if (it != null) {
                                val coordenadas = "${it.latitude},${it.longitude}"
                                val indice =
                                    layoutInflater.inflate(R.layout.indice, null) as EditText
                                alert {
                                    customView = indice
                                    isCancelable = false
                                    yesButton {
                                        val bytes =
                                            (indice.text.toString() + "," + coordenadas).formato()
                                        ConnectionManager.writeCharacteristic(
                                            device,
                                            characteristic,
                                            bytes
                                        )
                                    }
                                    noButton { }
                                }.show()
                                indice.showKeyboard()
                                //ConnectionManager.writeCharacteristic(device, characteristic, coordenadas.toByteArray())
                                Toast.makeText(
                                    applicationContext,
                                    "${it.latitude}, ${it.longitude}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    }

                    neutralPressed("Limpiar") {
                        for (i in (0..9).toList()) {
                            var bytes = "0$i, 0, 0".formato()
                            ConnectionManager.writeCharacteristic(device, characteristic, bytes)
                        }
                    }
                }.show()
            }
        }.show()
        hexField.showKeyboard()
    }

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onDisconnect = {
                runOnUiThread {
                    alert {
                        title = "Disconnected"
                        message = "Disconnected from device."
                        positiveButton("OK") { onBackPressed() }
                    }.show()
                }
            }

            onCharacteristicRead = { _, characteristic ->
                log("Se leyó: ${String(characteristic.value)}")
            }

            onCharacteristicWrite = { _, characteristic ->
                log("Se escribió: ${characteristic.value}")
            }

            onMtuChanged = { _, mtu ->
                log("MTU updated to $mtu")
            }

            onCharacteristicChanged = { _, characteristic ->
                log("Value changed on ${characteristic.uuid}: ${characteristic.value.toHexString()}")
            }

            onNotificationsEnabled = { _, characteristic ->
                log("Enabled notifications on ${characteristic.uuid}")
                notifyingCharacteristics.add(characteristic.uuid)
            }

            onNotificationsDisabled = { _, characteristic ->
                log("Disabled notifications on ${characteristic.uuid}")
                notifyingCharacteristics.remove(characteristic.uuid)
            }
        }
    }

    private enum class CharacteristicProperty {
        Readable,
        Writable,
        WritableWithoutResponse,
        Notifiable,
        Indicatable;

        val action
            get() = when (this) {
                Readable -> "Leer"
                Writable -> "Escribir"
                WritableWithoutResponse -> "Escribir (no habrá respuesta)"
                Notifiable -> "Toggle Notifications"
                Indicatable -> "Toggle Indications"
            }
    }

    private fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun EditText.showKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        requestFocus()
        inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun String.hexToBytes() =
        this.chunked(2).map { it.toUpperCase(Locale.US).toInt(16).toByte() }.toByteArray()

    private fun String.deDoubleAHex():String {
        if (this.toDouble() == 0.0) return "0000000000000000"
        val b = this.split(".")
        val exponente = floor(ln(abs(this.toDouble())) / ln(2.0))
        var auxi = 2.0.pow(exponente)
        var exponente_bi = (1023.0 + exponente).toString().split(".")[0].toInt().toString(2)
        while (exponente_bi.length < 11){
            exponente_bi = "0$exponente_bi"
        }

        var bits = if (this.toDouble() < 0){"1" + exponente_bi} else{"0"+exponente_bi}
        var ent_bytes = bits.chunked(4).map{it.toInt(2).toString(16)}.joinToString("")
        var mant = (abs(this.toDouble()) /auxi - 1)		//.toString().split(".")[1].chunked(1).toMutableList()
        var j = 0
        var mant_byte = ""
        while (j<13){
            mant *= 16
            var ent1 = mant.toString().split(".")[0].toInt()
            mant = ("0." + mant.toString().split(".")[1]).toDouble()
            mant_byte += ent1.toString(16)
            j += 1
        }

        var bytes = ent_bytes + mant_byte
        var reversed_bytes = bytes.chunked(2).reversed().joinToString("")
        return reversed_bytes
    }

    private fun String.formato():ByteArray {
        val arreglo = this.split(",")
        val latitud_hex = arreglo[1].deDoubleAHex()
        val longitud_hex = arreglo[2].deDoubleAHex()
        val hex = arreglo[0] + latitud_hex + longitud_hex
        return hex.hexToBytes()
    }

}




