package com.adrop.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.os.PowerManager
import android.os.Process
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.View
import android.view.ViewGroup
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
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import org.json.JSONObject

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
    private var customVideoView: View? = null
    private var customVideoViewCallback: WebChromeClient.CustomViewCallback? = null

    private fun hideCustomVideoView(): Boolean {
        if (customVideoView != null) {
            (window.decorView as? ViewGroup)?.removeView(customVideoView)
            customVideoView = null
            customVideoViewCallback?.onCustomViewHidden()
            customVideoViewCallback = null
            swipeRefresh.visibility = View.VISIBLE
            return true
        }
        return false
    }

    data class ActiveDownloadTask(
        val filename: String,
        val isCancelled: AtomicBoolean = AtomicBoolean(false),
        @Volatile var connection: HttpURLConnection? = null
    )
    private val activeDownloadTasks = ConcurrentHashMap<String, ActiveDownloadTask>()

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
        swipeRefresh.isEnabled = isContentAtTop
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
        swipeRefresh.isEnabled = isContentAtTop
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
        swipeRefresh.setProgressBackgroundColorSchemeColor(android.graphics.Color.parseColor("#16162a"))
        swipeRefresh.setColorSchemeColors(
            android.graphics.Color.parseColor("#7c83ff"),
            android.graphics.Color.parseColor("#a78bfa")
        )
        connectionLayout = findViewById(R.id.connectionLayout)
        tvStatusNotice = findViewById(R.id.tvStatusNotice)
        etIpAddress = findViewById(R.id.etIpAddress)
        btnScanQr = findViewById(R.id.btnScanQr)
        btnManualConnect = findViewById(R.id.btnManualConnect)
        btnReconnectLast = findViewById(R.id.btnReconnectLast)

        setupWebView()
        setupWakeLock()

        // Smart scroll conflict resolution: Only allow pull-to-refresh when web content is at the very top
        swipeRefresh.setDistanceToTriggerSync((resources.displayMetrics.density * 130).toInt())
        swipeRefresh.setOnChildScrollUpCallback { _, _ ->
            !isContentAtTop || webView.scrollY > 0 || webView.canScrollVertically(-1)
        }

        // Handle Back Press natively
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hideCustomVideoView()) {
                    return
                }
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
                        conn.setRequestProperty("X-Device-Type", "mobile")
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
        // Prevent white screen flash on load/connect by anchoring dark canvas immediately
        webView.setBackgroundColor(Color.parseColor("#0a0a1a"))
        swipeRefresh.setBackgroundColor(Color.parseColor("#0a0a1a"))

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
        settings.mediaPlaybackRequiresUserGesture = false

        // Identify as Native Android APK Client
        settings.userAgentString = "${settings.userAgentString} ADropXAndroid/3.3.0"

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
            fun downloadFile(url: String, filename: String) {
                runOnUiThread {
                    startHighSpeedDownload(url, filename)
                }
            }

            @JavascriptInterface
            fun cancelDownload(filename: String) {
                val task = activeDownloadTasks[filename]
                if (task != null) {
                    task.isCancelled.set(true)
                    thread {
                        try {
                            task.connection?.disconnect()
                        } catch (e: Exception) {}
                    }
                }
            }

            @JavascriptInterface
            fun openExternalMedia(url: String) {
                runOnUiThread {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(Uri.parse(url), "video/*")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(Intent.createChooser(intent, "Play with"))
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "No external video player found", Toast.LENGTH_SHORT).show()
                    }
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
                return "3.3.0"
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
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customVideoView != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                customVideoView = view
                customVideoViewCallback = callback
                swipeRefresh.visibility = View.GONE
                (window.decorView as? ViewGroup)?.addView(
                    view,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            }

            override fun onHideCustomView() {
                hideCustomVideoView()
            }

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
                    swipeRefresh.isEnabled = isContentAtTop
                    return false
                }
                return true
            }
        }

        // High-Speed Direct Native Download Engine (Bypasses throttled Android DownloadManager)
        webView.setDownloadListener { url, _, contentDisposition, mimeType, _ ->
            val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
            startHighSpeedDownload(url, filename, mimeType)
        }
    }

    private fun startHighSpeedDownload(downloadUrl: String, filename: String, mimeTypeHint: String? = null) {
        if (activeDownloadTasks.containsKey(filename)) {
            Toast.makeText(this, "Download already in progress for $filename", Toast.LENGTH_SHORT).show()
            return
        }

        var fullUrl = downloadUrl
        if (!fullUrl.startsWith("http://") && !fullUrl.startsWith("https://")) {
            val base = currentConnectedUrl ?: prefs.getString(KEY_LAST_URL, "http://127.0.0.1:8080") ?: "http://127.0.0.1:8080"
            fullUrl = if (base.endsWith("/")) "$base${fullUrl.removePrefix("/")}" else "$base/$fullUrl"
        }

        val task = ActiveDownloadTask(filename)
        activeDownloadTasks[filename] = task

        val qName = JSONObject.quote(filename)
        webView.evaluateJavascript("window.ADropDownloadTracker && window.ADropDownloadTracker.onStart($qName);", null)

        wakeLock?.acquire(30 * 60 * 1000L)

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val wifiLock = wifiManager?.createWifiLock(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            else
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
            "ADrop:HighSpeedDownload"
        )
        wifiLock?.acquire()

        thread {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE)
            } catch (e: Exception) {}

            var totalBytesRead = 0L
            var totalExpectedSize = 0L
            var lastReportTime = 0L
            var lastBytesReported = 0L
            val startTime = System.currentTimeMillis()
            var errorOccurred: String? = null
            var isUserCancelled = false
            var insertedUri: Uri? = null
            var legacyTargetFile: File? = null

            try {
                val urlObj = URL(fullUrl)
                val conn = urlObj.openConnection() as HttpURLConnection
                task.connection = conn
                conn.requestMethod = "GET"
                conn.connectTimeout = 8000
                conn.readTimeout = 120000
                conn.setRequestProperty("User-Agent", "ADropXAndroid/3.3.0")
                conn.setRequestProperty("Accept-Encoding", "identity")
                conn.doInput = true

                val contentLength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    conn.contentLengthLong
                } else {
                    conn.contentLength.toLong()
                }
                if (contentLength > 0) {
                    totalExpectedSize = contentLength
                }

                val contentType = conn.contentType ?: mimeTypeHint ?: "application/octet-stream"
                val buffer = ByteArray(131072) // 128 KB buffer for wire speed

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, contentType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val resolver = contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        insertedUri = uri
                        resolver.openOutputStream(uri)?.use { outStream ->
                            val bufferedOut = BufferedOutputStream(outStream, 131072)
                            val bufferedIn = BufferedInputStream(conn.inputStream, 131072)
                            var bytes: Int
                            while (bufferedIn.read(buffer).also { bytes = it } != -1) {
                                if (task.isCancelled.get()) {
                                    isUserCancelled = true
                                    throw InterruptedIOException("Download cancelled by user")
                                }
                                bufferedOut.write(buffer, 0, bytes)
                                totalBytesRead += bytes

                                val now = System.currentTimeMillis()
                                if (lastReportTime == 0L) {
                                    lastReportTime = now
                                    lastBytesReported = totalBytesRead
                                } else if (now - lastReportTime >= 1500) {
                                    val dtSec = (now - lastReportTime) / 1000.0
                                    val dBytes = totalBytesRead - lastBytesReported
                                    val instantSpeedMBs = if (dtSec > 0) (dBytes / (1024.0 * 1024.0)) / dtSec else 0.0
                                    val percent = if (totalExpectedSize > 0) {
                                        ((totalBytesRead * 100.0) / totalExpectedSize).coerceIn(0.0, 100.0)
                                    } else {
                                        -1.0
                                    }
                                    val pStr = String.format(Locale.US, "%.1f", percent)
                                    val sStr = String.format(Locale.US, "%.1f", instantSpeedMBs)
                                    val js = "window.ADropDownloadTracker && window.ADropDownloadTracker.onProgress($qName, $totalBytesRead, $totalExpectedSize, $pStr, $sStr);"
                                    runOnUiThread {
                                        webView.evaluateJavascript(js, null)
                                    }
                                    lastReportTime = now
                                    lastBytesReported = totalBytesRead
                                }
                            }
                            bufferedOut.flush()
                        }
                        if (!isUserCancelled) {
                            contentValues.clear()
                            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                            resolver.update(uri, contentValues, null, null)
                        }
                    } else {
                        errorOccurred = "Could not create file in Downloads"
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val targetDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    targetDir.mkdirs()
                    val targetFile = File(targetDir, filename)
                    legacyTargetFile = targetFile
                    FileOutputStream(targetFile).use { outStream ->
                        val bufferedOut = BufferedOutputStream(outStream, 131072)
                        val bufferedIn = BufferedInputStream(conn.inputStream, 131072)
                        var bytes: Int
                        while (bufferedIn.read(buffer).also { bytes = it } != -1) {
                            if (task.isCancelled.get()) {
                                isUserCancelled = true
                                throw InterruptedIOException("Download cancelled by user")
                            }
                            bufferedOut.write(buffer, 0, bytes)
                            totalBytesRead += bytes

                            val now = System.currentTimeMillis()
                            if (lastReportTime == 0L) {
                                lastReportTime = now
                                lastBytesReported = totalBytesRead
                            } else if (now - lastReportTime >= 1500) {
                                val dtSec = (now - lastReportTime) / 1000.0
                                val dBytes = totalBytesRead - lastBytesReported
                                val instantSpeedMBs = if (dtSec > 0) (dBytes / (1024.0 * 1024.0)) / dtSec else 0.0
                                val percent = if (totalExpectedSize > 0) {
                                    ((totalBytesRead * 100.0) / totalExpectedSize).coerceIn(0.0, 100.0)
                                } else {
                                    -1.0
                                }
                                val pStr = String.format(Locale.US, "%.1f", percent)
                                val sStr = String.format(Locale.US, "%.1f", instantSpeedMBs)
                                val js = "window.ADropDownloadTracker && window.ADropDownloadTracker.onProgress($qName, $totalBytesRead, $totalExpectedSize, $pStr, $sStr);"
                                runOnUiThread {
                                    webView.evaluateJavascript(js, null)
                                }
                                lastReportTime = now
                                lastBytesReported = totalBytesRead
                            }
                        }
                        bufferedOut.flush()
                    }
                    if (!isUserCancelled) {
                        MediaScannerConnection.scanFile(this@MainActivity, arrayOf(targetFile.absolutePath), null, null)
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                if (task.isCancelled.get() || e is InterruptedIOException) {
                    isUserCancelled = true
                } else {
                    errorOccurred = e.message ?: "Download failed"
                }
            } finally {
                activeDownloadTasks.remove(filename)
                if (wifiLock != null && wifiLock.isHeld) {
                    wifiLock.release()
                }
                if (wakeLock != null && wakeLock?.isHeld == true) {
                    try { wakeLock?.release() } catch (e: Exception) {}
                }
                if (isUserCancelled) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && insertedUri != null) {
                            contentResolver.delete(insertedUri!!, null, null)
                        } else if (legacyTargetFile?.exists() == true) {
                            legacyTargetFile?.delete()
                        }
                    } catch (e: Exception) {}
                }
            }

            val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
            val avgSpeedMBs = if (elapsedSec > 0) (totalBytesRead / (1024.0 * 1024.0)) / elapsedSec else 0.0

            runOnUiThread {
                if (isUserCancelled) {
                    webView.evaluateJavascript("window.ADropDownloadTracker && window.ADropDownloadTracker.onCancelled($qName);", null)
                    Toast.makeText(this@MainActivity, "Download cancelled: $filename", Toast.LENGTH_SHORT).show()
                } else if (errorOccurred == null) {
                    val avgStr = String.format(Locale.US, "%.1f", avgSpeedMBs)
                    val completeJs = "window.ADropDownloadTracker && window.ADropDownloadTracker.onComplete($qName, $totalBytesRead, $avgStr);"
                    webView.evaluateJavascript(completeJs, null)
                    Toast.makeText(
                        this@MainActivity,
                        "$filename downloaded at ${String.format(Locale.US, "%.1f MB/s", avgSpeedMBs)} to Downloads!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val qErr = JSONObject.quote(errorOccurred)
                    webView.evaluateJavascript("window.ADropDownloadTracker && window.ADropDownloadTracker.onError($qName, $qErr);", null)
                    Toast.makeText(this@MainActivity, "Download failed: $errorOccurred", Toast.LENGTH_LONG).show()
                }
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
        integrator.setBeepEnabled(false)
        integrator.setOrientationLocked(true)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.initiateScan()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                // Crisp 50ms haptic feedback strictly on successful QR scan connect
                try {
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(50)
                    }
                } catch (e: Exception) {}

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
