package com.attendly.mobile

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
import java.net.URL

class MainActivity : AppCompatActivity() {

    // ================== CHANGE THIS IP ==================
    private val TARGET_HOST = "192.168.1.100"  // <-- put your PC IP here e.g. 192.168.1.5
    private val TARGET_PORT = "5173"
    private val TARGET_URL get() = "http://$TARGET_HOST:$TARGET_PORT"
    // ====================================================

    private lateinit var webView: WebView
    private lateinit var statusText: TextView
    private lateinit var retryButton: Button
    private lateinit var detailText: TextView

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
        showLoading("Checking WiFi...")
        if (!isWifiConnected()) {
            showError("Not connected to WiFi", "Please connect to a WiFi network to access\n$TARGET_URL")
            return
        }

        showLoading("WiFi connected. Checking\n$TARGET_URL...")

        // Check server in background
        CoroutineScope(Dispatchers.IO).launch {
            val reachable = isServerReachable(TARGET_URL)
            withContext(Dispatchers.Main) {
                if (reachable) {
                    showWebView()
                    webView.loadUrl(TARGET_URL)
                } else {
                    showError(
                        "Cannot reach server",
                        "Connected to WiFi, but cannot access:\n$TARGET_URL\n\n" +
                        "1. Make sure your PC/server is running on port 5173\n" +
                        "2. Make sure phone & PC are on SAME WiFi\n" +
                        "3. Check firewall allows port 5173\n" +
                        "4. Current IP set: $TARGET_HOST"
                    )
                }
            }
        }
    }

    private fun isWifiConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && 
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isServerReachable(urlString: String): Boolean {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "HEAD"
            conn.connect()
            val code = conn.responseCode
            conn.disconnect()
            code in 200..399
        } catch (e: Exception) {
            // try GET if HEAD fails (Vite dev server sometimes blocks HEAD)
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.connect()
                val code = conn.responseCode
                conn.disconnect()
                code in 200..399
            } catch (e2: Exception) {
                false
            }
        }
    }

    private fun showLoading(msg: String) {
        webView.visibility = View.GONE
        statusText.visibility = View.VISIBLE
        detailText.visibility = View.VISIBLE
        retryButton.visibility = View.GONE
        statusText.text = msg
        detailText.text = ""
    }

    private fun showError(title: String, detail: String) {
        webView.visibility = View.GONE
        statusText.visibility = View.VISIBLE
        detailText.visibility = View.VISIBLE
        retryButton.visibility = View.VISIBLE
        statusText.text = title
        detailText.text = detail
    }

    private fun showWebView() {
        statusText.visibility = View.GONE
        detailText.visibility = View.GONE
        retryButton.visibility = View.GONE
        webView.visibility = View.VISIBLE
    }
}