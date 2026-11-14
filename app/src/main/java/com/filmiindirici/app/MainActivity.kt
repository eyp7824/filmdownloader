package com.filmiindirici.app

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var browser: WebView
    private lateinit var address: EditText
    private lateinit var status: TextView
    private lateinit var progress: TextView
    private var lastDownloadId: Long = -1L
    private val handler = Handler(Looper.getMainLooper())
    private val poll = object : Runnable {
        override fun run() {
            if (lastDownloadId >= 0) {
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.query(DownloadManager.Query().setFilterById(lastDownloadId))?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val state = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val done = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        progress.text = when (state) {
                            DownloadManager.STATUS_SUCCESSFUL -> "✓ İndirme tamamlandı"
                            DownloadManager.STATUS_FAILED -> "İndirme başarısız. İndirmeleri kontrol edin."
                            DownloadManager.STATUS_PAUSED -> "İndirme beklemede"
                            else -> if (total > 0) "İndiriliyor: %${done * 100 / total}" else "İndiriliyor: ${done / 1024} KB"
                        }
                        if (state == DownloadManager.STATUS_SUCCESSFUL || state == DownloadManager.STATUS_FAILED) lastDownloadId = -1L
                    }
                }
            }
            handler.postDelayed(this, 1200)
        }
    }
    private fun message(s: String) {
        status.text = s
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(10, 17, 28)
        window.navigationBarColor = Color.rgb(10, 17, 28)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(10, 17, 28))
            setPadding(18, 18, 18, 10)
        }
        fun text(s: String, sp: Float = 16f) = TextView(this).apply {
            text = s
            textSize = sp
            setTextColor(Color.WHITE)
        }
        root.addView(text("🎬 FİLM İNDİRİCİ", 26f).apply {
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 10, 0, 12)
        })
        root.addView(text("Türkçe • Koyu tema • Doğrudan video bağlantıları", 12f))
        address = EditText(this).apply {
            hint = "https://video-adresi..."
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
        }
        root.addView(address)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun button(label: String, action: () -> Unit) {
            actions.addView(Button(this).apply {
                text = label
                setOnClickListener { action() }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        button("Siteyi aç") {
            val url = address.text.toString().trim()
            if (valid(url)) browser.loadUrl(url) else message("Geçerli HTTPS bağlantısı girin")
        }
        button("Videoyu indir") { download(address.text.toString().trim()) }
        root.addView(actions)
        val extras = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        extras.addView(Button(this).apply {
            text = "İndirmeler"
            setOnClickListener { startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        extras.addView(Button(this).apply {
            text = "Geri"
            setOnClickListener { if (browser.canGoBack()) browser.goBack() }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(extras)
        progress = text("Henüz indirme yok", 15f).apply { setPadding(0, 10, 0, 10) }
        root.addView(progress)
        status = text("İndirmek için doğrudan video bağlantısı kullanın.", 13f)
        root.addView(status)
        browser = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    address.setText(url ?: "")
                }
            }
            setDownloadListener { url, _, _, _, _ -> download(url) }
        }
        root.addView(browser, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        if (intent?.action == Intent.ACTION_SEND) {
            val shared = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!shared.isNullOrBlank()) address.setText(shared)
        }
        handler.post(poll)
    }
    private fun valid(url: String): Boolean = try {
        val uri = Uri.parse(url)
        uri.scheme == "https" && !uri.host.isNullOrBlank()
    } catch (_: Exception) { false }
    private fun download(url: String) {
        if (!valid(url)) {
            message("Yalnızca HTTPS bağlantıları desteklenir")
            return
        }
        val uri = Uri.parse(url)
        val path = (uri.path ?: "").lowercase()
        if (!listOf(".mp4", ".webm", ".m4v", ".mov", ".mkv").any { path.endsWith(it) }) {
            message("Doğrudan video dosyası bağlantısı gerekli. Korumalı akışlar desteklenmez.")
            return
        }
        try {
            val name = URLUtil.guessFileName(url, null, "video/mp4")
            val request = DownloadManager.Request(uri)
                .setTitle(name)
                .setDescription("Film İndirici")
                .setMimeType("video/mp4")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
            val cookie = CookieManager.getInstance().getCookie(url)
            if (!cookie.isNullOrBlank()) request.addRequestHeader("Cookie", cookie)
            lastDownloadId = (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            message("İndirme başlatıldı: $name")
        } catch (e: Exception) {
            message("İndirme başlatılamadı: ${e.localizedMessage}")
        }
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (browser.canGoBack()) browser.goBack() else super.onBackPressed()
    }
    override fun onDestroy() {
        handler.removeCallbacks(poll)
        browser.destroy()
        super.onDestroy()
    }
}
