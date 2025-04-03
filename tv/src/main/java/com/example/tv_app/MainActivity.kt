package com.example.tv_app

import android.content.Context
import android.graphics.Bitmap
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.tv_app.ui.theme.Tv_AppTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.util.*
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    private val port = 12345 // Port for communication

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tv_AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape,
                    color = Color.Black // Set the background color to black
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val qrCodeBitmapState = remember { mutableStateOf<Bitmap?>(null) }
                        val ipAddress = getLocalIpAddress(this@MainActivity)
                        var connectedDeviceName by remember { mutableStateOf("No device connected") }

                        // Start the server in a background thread
                        LaunchedEffect(Unit) {
                            startServer { deviceName ->
                                connectedDeviceName = deviceName // Update UI with connected device name
                            }
                        }

                        if (ipAddress != null) {
                            val qrCodeContent = "tv_ip:$ipAddress"
                            thread {
                                val bitmap = generateQRCode(qrCodeContent)
                                qrCodeBitmapState.value = bitmap
                            }

                            Text(
                                text = "Scan QR Code to Connect",
                                modifier = Modifier.padding(16.dp),
                                color = Color.White // Set the text color to white
                            )

                            qrCodeBitmapState.value?.let { bitmap ->
                                AndroidView(
                                    factory = { context ->
                                        ImageView(context).apply {
                                            setImageBitmap(bitmap)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            }

                            Text(
                                text = "IP Address: $ipAddress",
                                modifier = Modifier.padding(16.dp),
                                color = Color.White // Set the text color to white
                            )

                            Text(
                                text = "Connected Device: $connectedDeviceName",
                                modifier = Modifier.padding(16.dp),
                                color = Color.White // Set the text color to white
                            )
                        } else {
                            Text(
                                text = "Failed to retrieve IP Address",
                                modifier = Modifier.padding(16.dp),
                                color = Color.Red // Set the text color to red
                            )
                        }
                    }
                }
            }
        }
    }

    // Function to get the local IP address
    private fun getLocalIpAddress(context: Context): String? {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (wifiManager.isWifiEnabled) {
                val wifiInfo = wifiManager.connectionInfo
                val ipAddress = wifiInfo.ipAddress
                val ipString = String.format(
                    "%d.%d.%d.%d",
                    ipAddress and 0xff,
                    ipAddress shr 8 and 0xff,
                    ipAddress shr 16 and 0xff,
                    ipAddress shr 24 and 0xff
                )
                return ipString
            } else {
                // Fallback to get IP address from network interfaces if Wi-Fi is not enabled
                val en = NetworkInterface.getNetworkInterfaces()
                while (en.hasMoreElements()) {
                    val intf = en.nextElement()
                    val enumIpAddr = intf.inetAddresses
                    while (enumIpAddr.hasMoreElements()) {
                        val inetAddress = enumIpAddr.nextElement()
                        if (!inetAddress.isLoopbackAddress) {
                            return inetAddress.hostAddress
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    // Function to generate QR code
    private fun generateQRCode(content: String): Bitmap? {
        try {
            val hints = Hashtable<EncodeHintType, Any>()
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            return bmp
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // Function to start the server
    private fun startServer(onDeviceConnected: (String) -> Unit) {
        Thread {
            try {
                val serverSocket = ServerSocket(port, 0, InetAddress.getByName("0.0.0.0"))
                Log.d("TVApp", "Server started on port $port")
                Log.d("TVApp", "Server local socket address: ${serverSocket.localSocketAddress}")

                while (true) {
                    val socket = serverSocket.accept() // Accept connection from mobile app
                    val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val output = PrintWriter(socket.getOutputStream(), true)

                    // Read device name sent by mobile app
                    val deviceName = input.readLine()
                    Log.d("TVApp", "Connected to: $deviceName")

                    // Notify UI about the connected device
                    onDeviceConnected(deviceName)

                    // Send acknowledgment back to the mobile app
                    output.println("Connected to TV")
                }
            } catch (e: Exception) {
                Log.e("TVApp", "Error in server: ${e.message}")
            }
        }.start()
    }
}

@Composable
fun AndroidView(
    factory: (Context) -> ImageView,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = remember { factory(context) }
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { view },
        modifier = modifier
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Tv_AppTheme {
        Greeting("Android")
    }
}