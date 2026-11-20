package com.adrop.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.view.View
import android.webkit.*
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.integration.android.IntentIntegrator

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var connectionLayout: LinearLayout
    private lateinit var fabSwitchPc: FloatingActionButton
    private lateinit var etIpAddress: EditText
    private lateinit var btnReconnectLast: MaterialButton

    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val PREFS_NAME = "ADropPrefs"
        private const val KEY_LAST_URL = "last_connected_url"
    }

    // Modern file picker launcher for WebChromeClient
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, data)
            fileUploadCallback?.onReceiveValue(uris)
        } else {
            fileUploadCallback?.onReceiveValue(null)
        }
        fileUploadCallback = null
    }

    // Camera permission request launcher
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startQrScanner()
        } else {
            Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Initialize UI Elements
        webView = findViewById(R.id.webView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        connectionLayout = findViewById(R.id.connectionLayout)
        fabSwitchPc = findViewById(R.id.fabSwitchPc)
        etIpAddress = findViewById(R.id.etIpAddress)
        btnReconnectLast = findViewById(R.id.btnReconnectLast)

        val btnScanQr = findViewById<MaterialButton>(R.id.btnScanQr)
        val btnManualConnect = findViewById<MaterialButton>(R.id.btnManualConnect)

        setupWebView()
        setupWakeLock()

        // Handle Back Press natively
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (swipeRefresh.visibility == View.VISIBLE && webView.canGoBack()) {
                    webView.goBack()
                } else if (swipeRefresh.visibility == View.VISIBLE) {
                    showConnectionScreen()
                } else {
                    finish()
                }
            }
        })

        // Check for last connected URL
        val lastUrl = prefs.getString(KEY_LAST_URL, null)
        if (!lastUrl.isNullOrEmpty()) {
            btnReconnectLast.visibility = View.VISIBLE
            btnReconnectLast.text = "Reconnect: $lastUrl"
            btnReconnectLast.setOnClickListener {
                connectToUrl(lastUrl)
            }
        }

        btnScanQr.setOnClickListener {
            checkCameraPermissionAndScan()
        }

        btnManualConnect.setOnClickListener {
            var input = etIpAddress.text.toString().trim()
            if (input.isNotEmpty()) {
                if (!input.startsWith("http://") && !input.startsWith("https://")) {
                    input = "http://$input"
                }
                connectToUrl(input)
            } else {
                Toast.makeText(this, getString(R.string.invalid_url), Toast.LENGTH_SHORT).show()
            }
        }

        fabSwitchPc.setOnClickListener {
            showConnectionScreen()
        }

        swipeRefresh.setOnRefreshListener {
            webView.reload()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                swipeRefresh.isRefreshing = false
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                swipeRefresh.isRefreshing = false
                Toast.makeText(this@MainActivity, "Connection error: $description", Toast.LENGTH_SHORT).show()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileUploadCallback?.onReceiveValue(null)
                fileUploadCallback = filePathCallback

                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }

                try {
                    filePickerLauncher.launch(intent)
                } catch (e: Exception) {
                    fileUploadCallback = null
                    return false
                }
                return true
            }
        }

        // Native Download Listener using Android DownloadManager
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)

                request.setMimeType(mimeType)
                request.addRequestHeader("User-Agent", userAgent)
                request.setDescription("Downloading file via ADrop...")
                request.setTitle(filename)
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)

                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(this, "Downloading $filename...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Fallback: open via external browser/downloader
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }
    }

    private fun connectToUrl(url: String) {
        prefs.edit().putString(KEY_LAST_URL, url).apply()

        connectionLayout.visibility = View.GONE
        swipeRefresh.visibility = View.VISIBLE
        fabSwitchPc.visibility = View.VISIBLE

        swipeRefresh.isRefreshing = true
        webView.loadUrl(url)
    }

    private fun showConnectionScreen() {
        swipeRefresh.visibility = View.GONE
        fabSwitchPc.visibility = View.GONE
        connectionLayout.visibility = View.VISIBLE

        webView.loadUrl("about:blank")
    }

    private fun checkCameraPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startQrScanner()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startQrScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setPrompt("Align with the QR code on your PC screen")
        integrator.setBeepEnabled(true)
        integrator.setOrientationLocked(true)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.initiateScan()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                val scannedUrl = result.contents
                connectToUrl(scannedUrl)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun setupWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ADrop::TransferWakeLock")
        } catch (e: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        wakeLock?.acquire(10 * 60 * 1000L /* 10 minutes */)
    }

    override fun onPause() {
        super.onPause()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }
}
