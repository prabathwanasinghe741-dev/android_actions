package com.example.wifiwebchecker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.Socket
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val TARGET_PORT = 5173
    private lateinit var webView: WebView
    private lateinit var statusText: TextView
    private lateinit var detailText: TextView
    private lateinit var retryButton: Button

    private var scanJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        statusText = findViewById(R.id.statusText)
        detailText = findViewById(R.id.detailText)
        retryButton = findViewById(R.id.retryButton)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()

        retryButton.setOnClickListener { checkAndLoad() }
        checkAndLoad()
    }

    private fun checkAndLoad() {
        scanJob?.cancel()
        
        if (!isWifiConnected()) {
            showError("Not connected to WiFi", "Please connect to a WiFi network.\nPhone and server must be on same WiFi.")
            return
        }

        val myIp = getDeviceIp()
        if (myIp == null) {
            showError("No IP found", "Could not get WiFi IP address. Please check WiFi.")
            return
        }

        val subnet = myIp.substringBeforeLast(".")
        showLoading("Connected to WiFi\nYour IP: $myIp\nScanning $subnet.0/24 for :$TARGET_PORT...")

        scanJob = CoroutineScope(Dispatchers.Main).launch {
            val foundUrl = withContext(Dispatchers.IO) { scanNetworkForPort(subnet, TARGET_PORT) }
            if (foundUrl != null) {
                detailText.text = "Found server at $foundUrl"
                showWebView()
                webView.loadUrl(foundUrl)
            } else {
                showError(
                    "Server not found",
                    "Scanned $subnet.1 - $subnet.254 on port $TARGET_PORT\n" +
                    "No device responded.\n\n" +
                    "Make sure:\n" +
                    "1. Your PC runs: npm run dev -- --host\n" +
                    "2. PC firewall allows port $TARGET_PORT\n" +
                    "3. Both phone & PC on SAME WiFi\n\n" +
                    "Your IP was: $myIp"
                )
            }
        }
    }

    // Auto-detect: scan subnet for open port 5173
    private suspend fun scanNetworkForPort(subnet: String, port: Int): String? = coroutineScope {
        // Scan .1 to .254 in parallel batches for speed
        val deferreds = (1..254).map { i ->
            async(Dispatchers.IO) {
                val host = "$subnet.$i"
                if (isPortOpen(host, port, 400)) {
                    // double check HTTP actually responds
                    val url = "http://$host:$port"
                    if (isHttpOk(url)) host else null
                } else null
            }
        }
        
        // Return first found, cancel others
        for (d in deferreds) {
            val result = d.await()
            if (result != null) {
                // cancel remaining
                deferreds.forEach { it.cancel() }
                return@coroutineScope "http://$result:$port"
            }
        }
        null
    }

    private fun isPortOpen(host: String, port: Int, timeout: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(host, port), timeout)
                true
            }
        } catch (e: Exception) { false }
    }

    private fun isHttpOk(urlString: String): Boolean {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 1000
            conn.readTimeout = 1000
            conn.requestMethod = "GET"
            conn.connect()
            val ok = conn.responseCode in 200..399
            conn.disconnect()
            ok
        } catch (e: Exception) { false }
    }

    private fun isWifiConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun getDeviceIp(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                if (!intf.name.contains("wlan")) continue
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
            // fallback: any interface
            for (intf in NetworkInterface.getNetworkInterfaces()) {
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address && addr.hostAddress?.startsWith("192.") == true) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {}
        return null
    }

    private fun showLoading(msg: String) {
        webView.visibility = View.GONE
        statusText.visibility = View.VISIBLE
        detailText.visibility = View.VISIBLE
        retryButton.visibility = View.GONE
        statusText.text = "Searching..."
        detailText.text = msg
    }

    private fun showError(title: String, detail: String) {
        webView.visibility = View.GONE
        statusText.visibility = View.VISIBLE
        detailText.visibility = View.VISIBLE
        retryButton.visibility = View.VISIBLE
        statusText.text = title
        detailText.text = detail
        retryButton.text = "Scan Again"
    }

    private fun showWebView() {
        statusText.visibility = View.GONE
        detailText.visibility = View.GONE
        retryButton.visibility = View.GONE
        webView.visibility = View.VISIBLE
    }
}