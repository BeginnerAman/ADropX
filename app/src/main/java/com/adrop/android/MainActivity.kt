package com.adrop.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.os.PowerManager
import android.provider.OpenableColumns
import android.view.View
import android.webkit.*
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.zxing.integration.android.IntentIntegrator
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var connectionLayout: LinearLayout
    private lateinit var tvStatusNotice: TextView
    private lateinit var etIpAddress: EditText
    private lateinit var btnScanQr: AppCompatButton
    private lateinit var btnManualConnect: AppCompatButton
    private lateinit var btnReconnectLast: AppCompatButton

    private var wakeLock: PowerManager.WakeLock? = null
    private val pendingShareUris = mutableListOf<Uri>()
    private var currentConnectedUrl: String? = null
    private var isContentAtTop: Boolean = true

    companion object {
        private const val PREFS_NAME = "ADropPrefs"
        private const val KEY_LAST_URL = "last_connected_url"
        // Static callback survives Activity lifecycle/pause/recreation
        var staticFileUploadCallback: ValueCallback<Array<Uri>>? = null
    }

    // Direct Native File/Gallery Picker Launcher (100% reliable, zero WebView dependency)
    private val nativeFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        swipeRefresh.isEnabled = true
        if (result.resultCode == RESULT_OK && result.data != null) {
            val data = result.data!!
            val uris = mutableListOf<Uri>()

            val clipData = data.clipData
            if (clipData != null && clipData.itemCount > 0) {
                for (i in 0 until clipData.itemCount) {
                    val itemUri = clipData.getItemAt(i).uri
                    if (itemUri != null) uris.add(itemUri)
                }
            } else if (data.data != null) {
                uris.add(data.data!!)
            }

            if (uris.isNotEmpty()) {
                val targetUrl = currentConnectedUrl ?: prefs.getString(KEY_LAST_URL, null)
                if (!targetUrl.isNullOrEmpty()) {
                    uploadSharedFiles(uris, targetUrl)
                } else {
                    pendingShareUris.addAll(uris)
                    Toast.makeText(this, "Connect to PC to share ${uris.size} selected file(s)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Fallback standard HTML file picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        swipeRefresh.isEnabled = true
        if (result.resultCode == RESULT_OK && result.data != null) {
            val data = result.data!!
            val uris = mutableListOf<Uri>()

            val clipData = data.clipData
            if (clipData != null && clipData.itemCount > 0) {
                for (i in 0 until clipData.itemCount) {
                    val itemUri = clipData.getItemAt(i).uri
                    if (itemUri != null) uris.add(itemUri)
                }
            } else if (data.data != null) {
                uris.add(data.data!!)
            }

            if (uris.isNotEmpty()) {
                staticFileUploadCallback?.onReceiveValue(uris.toTypedArray())
            } else {
                val parsed = WebChromeClient.FileChooserParams.parseResult(result.resultCode, data)
                staticFileUploadCallback?.onReceiveValue(parsed)
            }
        } else {
            staticFileUploadCallback?.onReceiveValue(null)
        }
        staticFileUploadCallback = null
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
        tvStatusNotice = findViewById(R.id.tvStatusNotice)
        etIpAddress = findViewById(R.id.etIpAddress)
        btnScanQr = findViewById(R.id.btnScanQr)
        btnManualConnect = findViewById(R.id.btnManualConnect)
        btnReconnectLast = findViewById(R.id.btnReconnectLast)

        setupWebView()
        setupWakeLock()

        // Smart scroll conflict resolution: Only allow pull-to-refresh when web content is at the very top
        swipeRefresh.setOnChildScrollUpCallback { _, _ ->
            !isContentAtTop || webView.scrollY > 0 || webView.canScrollVertically(-1)
        }

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

        btnReconnectLast.setOnClickListener {
            val lastUrl = prefs.getString(KEY_LAST_URL, null)
            if (!lastUrl.isNullOrEmpty()) {
                connectToUrl(lastUrl)
            }
        }

        swipeRefresh.setOnRefreshListener {
            webView.reload()
        }

        // Handle shared files if launched from Android Share Sheet
        handleShareIntent(intent)

        // Auto-Connect on App Launch:
        // If user connected previously, immediately connect to last PC without requiring re-scan!
        val lastUrl = prefs.getString(KEY_LAST_URL, null)
        if (!lastUrl.isNullOrEmpty()) {
            connectToUrl(lastUrl, isAutoConnect = true)
        } else {
            showConnectionScreen()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return

        val sharedUris = mutableListOf<Uri>()
        if (Intent.ACTION_SEND == action) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let { sharedUris.add(it) }
            } else {
                @Suppress("DEPRECATION")
                (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let { sharedUris.add(it) }
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)?.forEach {
                    if (it != null) sharedUris.add(it)
                }
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.forEach {
                    (it as? Uri)?.let { uri -> sharedUris.add(uri) }
                }
            }
        }

        if (sharedUris.isNotEmpty()) {
            if (currentConnectedUrl != null) {
                uploadSharedFiles(sharedUris, currentConnectedUrl!!)
            } else {
                val lastUrl = prefs.getString(KEY_LAST_URL, null)
                if (!lastUrl.isNullOrEmpty()) {
                    pendingShareUris.addAll(sharedUris)
                    connectToUrl(lastUrl, isAutoConnect = true)
                } else {
                    pendingShareUris.addAll(sharedUris)
                    showConnectionScreen(noticeMessage = "Connect to PC to share ${sharedUris.size} selected file(s)")
                }
            }
        }
    }

    private fun uploadSharedFiles(uris: List<Uri>, serverBaseUrl: String) {
        val total = uris.size
        Toast.makeText(this, "Uploading $total file(s) to ADrop PC...", Toast.LENGTH_SHORT).show()
        wakeLock?.acquire(10 * 60 * 1000L /* 10 minutes */)

        thread {
            var successCount = 0
            for (uri in uris) {
                val filename = getFileName(uri) ?: "shared_${System.currentTimeMillis()}"
                try {
                    val stream: InputStream? = contentResolver.openInputStream(uri)
                    if (stream != null) {
                        val endpoint = if (serverBaseUrl.endsWith("/")) "${serverBaseUrl}upload/raw" else "$serverBaseUrl/upload/raw"
                        val url = URL(endpoint)
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.doOutput = true
                        conn.connectTimeout = 8000
                        conn.readTimeout = 60000
                        conn.setRequestProperty("X-Filename", filename)
                        conn.setChunkedStreamingMode(65536)

                        stream.use { input ->
                            conn.outputStream.use { output ->
                                input.copyTo(output)
                            }
                        }

                        if (conn.responseCode in 200..299) {
                            successCount++
                        }
                        conn.disconnect()
                    }
                } catch (e: Exception) {
                    // Skip or retry next
                }
            }

            runOnUiThread {
                if (successCount > 0) {
                    Toast.makeText(this@MainActivity, "$successCount file(s) shared successfully!", Toast.LENGTH_SHORT).show()
                    webView.reload()
                } else {
                    Toast.makeText(this@MainActivity, "Failed to share files. Check PC connection.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
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

        // Identify as Native Android APK Client
        settings.userAgentString = "${settings.userAgentString} ADropAndroid/3.2.2"

        // JavaScript Bridge for native interactions
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun showConnectionScreen() {
                runOnUiThread {
                    this@MainActivity.showConnectionScreen()
                }
            }

            @JavascriptInterface
            fun openNativeFilePicker() {
                runOnUiThread {
                    swipeRefresh.isEnabled = false
                    wakeLock?.acquire(10 * 60 * 1000L)
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    nativeFilePickerLauncher.launch(Intent.createChooser(intent, "Select Files to Send via ADrop"))
                }
            }

            @JavascriptInterface
            fun setSwipeRefreshEnabled(enabled: Boolean) {
                runOnUiThread {
                    isContentAtTop = enabled
                    swipeRefresh.isEnabled = enabled
                }
            }

            @JavascriptInterface
            fun getVersion(): String {
                return "3.2.2"
            }
        }, "ADropNative")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                swipeRefresh.isRefreshing = false

                // If there are pending files shared from another app, upload them now
                if (pendingShareUris.isNotEmpty() && currentConnectedUrl != null) {
                    val urisToUpload = ArrayList(pendingShareUris)
                    pendingShareUris.clear()
                    uploadSharedFiles(urisToUpload, currentConnectedUrl!!)
                }
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                swipeRefresh.isRefreshing = false

                // If connection failed, show clean retry screen with switch option
                val displayUrl = failingUrl ?: currentConnectedUrl ?: "PC"
                showConnectionScreen(
                    errorMessage = "Cannot reach $displayUrl\nMake sure your PC and phone are on the same Wi-Fi / Hotspot and ADrop is open on PC."
                )
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                swipeRefresh.isEnabled = false
                staticFileUploadCallback?.onReceiveValue(null)
                staticFileUploadCallback = filePathCallback

                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }

                try {
                    filePickerLauncher.launch(Intent.createChooser(intent, "Select Files to Send via ADrop"))
                } catch (e: Exception) {
                    staticFileUploadCallback = null
                    swipeRefresh.isEnabled = true
                    return false
                }
                return true
            }
        }

        // Native Download Listener using Android DownloadManager
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
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
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }
    }

    private fun connectToUrl(url: String, isAutoConnect: Boolean = false) {
        currentConnectedUrl = url
        prefs.edit().putString(KEY_LAST_URL, url).apply()

        connectionLayout.visibility = View.GONE
        swipeRefresh.visibility = View.VISIBLE

        swipeRefresh.isRefreshing = true
        webView.loadUrl(url)

        if (isAutoConnect) {
            Toast.makeText(this, "Connecting to PC...", Toast.LENGTH_SHORT).show()
        }
    }

    fun showConnectionScreen(errorMessage: String? = null, noticeMessage: String? = null) {
        currentConnectedUrl = null
        swipeRefresh.visibility = View.GONE
        connectionLayout.visibility = View.VISIBLE

        val lastUrl = prefs.getString(KEY_LAST_URL, null)
        if (!lastUrl.isNullOrEmpty()) {
            btnReconnectLast.visibility = View.VISIBLE
            btnReconnectLast.text = "Reconnect: $lastUrl"
        } else {
            btnReconnectLast.visibility = View.GONE
        }

        if (!errorMessage.isNullOrEmpty()) {
            tvStatusNotice.visibility = View.VISIBLE
            tvStatusNotice.text = errorMessage
            tvStatusNotice.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
        } else if (!noticeMessage.isNullOrEmpty()) {
            tvStatusNotice.visibility = View.VISIBLE
            tvStatusNotice.text = noticeMessage
            tvStatusNotice.setTextColor(ContextCompat.getColor(this, R.color.accent_primary))
        } else {
            tvStatusNotice.visibility = View.GONE
        }

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
