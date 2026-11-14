package com.filmindirici.app;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class MainActivity extends Activity {
    private EditText address;
    private WebView browser;
    private TextView status;
    private final int background = Color.rgb(12, 18, 30);
    private final int accent = Color.rgb(29, 210, 185);

    private TextView label(String value, int size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(Color.WHITE);
        view.setTextSize(size);
        view.setPadding(4, 12, 4, 12);
        return view;
    }
    private void notice(String message) {
        status.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    private boolean isHttps(String url) {
        Uri uri = Uri.parse(url);
        return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
    }
    private void download(String url) {
        if (!isHttps(url)) { notice("Geçerli HTTPS video bağlantısı girin."); return; }
        String path = Uri.parse(url).getPath();
        String p = path == null ? "" : path.toLowerCase(Locale.ROOT);
        if (!(p.endsWith(".mp4") || p.endsWith(".webm") || p.endsWith(".m4v") || p.endsWith(".mov"))) {
            notice("Doğrudan MP4, WebM veya MOV video dosyası bağlantısı gerekli.");
            return;
        }
        try {
            String name = URLUtil.guessFileName(url, null, "video/mp4");
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(name);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
            String cookie = CookieManager.getInstance().getCookie(url);
            if (cookie != null && !cookie.isEmpty()) request.addRequestHeader("Cookie", cookie);
            ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
            notice("İndirme başlatıldı: " + name);
        } catch (Exception error) {
            notice("İndirme başlatılamadı: " + error.getMessage());
        }
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(background);
        root.setPadding(18, 12, 18, 8);
        TextView heading = label("🎬 FİLM İNDİRİCİ", 25);
        heading.setTextColor(accent);
        root.addView(heading);
        root.addView(label("Türkçe • Koyu tema • Doğrudan video indirme", 13));
        address = new EditText(this);
        address.setSingleLine(true);
        address.setHint("https://ornek.com/video.mp4");
        address.setHintTextColor(Color.LTGRAY);
        address.setTextColor(Color.WHITE);
        address.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        root.addView(address);
        LinearLayout buttons = new LinearLayout(this);
        Button open = new Button(this);
        open.setText("SİTEYİ AÇ");
        open.setOnClickListener(v -> {
            String url = address.getText().toString().trim();
            if (isHttps(url)) browser.loadUrl(url); else notice("Geçerli HTTPS adresi girin.");
        });
        buttons.addView(open, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button save = new Button(this);
        save.setText("VİDEOYU İNDİR");
        save.setOnClickListener(v -> download(address.getText().toString().trim()));
        buttons.addView(save, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(buttons);
        LinearLayout tools = new LinearLayout(this);
        Button downloads = new Button(this);
        downloads.setText("İNDİRMELER");
        downloads.setOnClickListener(v -> startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)));
        tools.addView(downloads, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button back = new Button(this);
        back.setText("GERİ");
        back.setOnClickListener(v -> { if (browser.canGoBack()) browser.goBack(); });
        tools.addView(back, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(tools);
        status = label("Doğrudan video bağlantılarını indirebilirsiniz.", 13);
        root.addView(status);
        browser = new WebView(this);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setDomStorageEnabled(true);
        browser.getSettings().setAllowFileAccess(false);
        browser.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) { address.setText(url); }
        });
        browser.setDownloadListener((url, userAgent, disposition, mime, length) -> download(url));
        root.addView(browser, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        if (Intent.ACTION_SEND.equals(getIntent().getAction())) {
            String shared = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null) address.setText(shared);
        }
    }
    @Override public void onBackPressed() {
        if (browser != null && browser.canGoBack()) browser.goBack();
        else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        if (browser != null) browser.destroy();
        super.onDestroy();
    }
}
