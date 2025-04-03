package com.example.tv_app

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private lateinit var ipAddressEditText: EditText
    private lateinit var portEditText: EditText
    private lateinit var connectButton: Button
    private lateinit var connectionStatusTextView: TextView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ipAddressEditText = findViewById(R.id.ipAddressEditText)
        portEditText = findViewById(R.id.portEditText)
        connectButton = findViewById(R.id.connectButton)
        connectionStatusTextView = findViewById(R.id.connectionStatusTextView)

        connectButton.setOnClickListener {
            val ip = ipAddressEditText.text.toString()
            val portString = portEditText.text.toString()

            if (ip.isNotEmpty() && portString.isNotEmpty()) {
                val port = portString.toInt()
                connectToTv(ip, port, "MyMobileDevice")
            } else {
                connectionStatusTextView.text = "Please enter IP and Port."
            }
        }
    }

    private fun connectToTv(ip: String, port: Int, deviceName: String) {
        connectionStatusTextView.text = "Connecting..."
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = Socket(ip, port)
                val output = OutputStreamWriter(socket.getOutputStream())

                // Send device name to TV
                output.write("$deviceName\n")
                output.flush()

                Log.d("MobileApp", "Device name sent: $deviceName")

                runOnUiThread {
                    connectionStatusTextView.text = "Connected!"
                }

                socket.close()
            } catch (e: Exception) {
                Log.e("MobileApp", "Error connecting to TV: ${e.message}")
                runOnUiThread {
                    connectionStatusTextView.text = "Connection failed: ${e.message}"
                }
            }
        }
    }
}