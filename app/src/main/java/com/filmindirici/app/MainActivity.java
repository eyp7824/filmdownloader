package com.filmindirici.app;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.ViewGroup;
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
    private WebView web;
    private EditText address;
    private TextView status;

    private void download(String url) {
        Uri uri = Uri.parse(url);
        String path = uri.getPath();
        String lower = path == null ? "" : path.toLowerCase(Locale.ROOT);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null ||
            !(lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mov") || lower.endsWith(".m4v"))) {
            status.setText("Doğrudan HTTPS video bağlantısı gerekli (MP4, WebM, MOV).");
            return;
        }
        try {
            String filename = URLUtil.guessFileName(url, null, "video/mp4");
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle(filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
            status.setText("İndirme başlatıldı: " + filename);
        } catch (Exception e) {
            status.setText("Hata: " + e.getMessage());
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 16, 16, 8);
        root.setBackgroundColor(Color.rgb(13, 20, 32));
        TextView title = new TextView(this);
        title.setText("FİLM İNDİRİCİ");
        title.setTextSize(26);
        title.setTextColor(Color.rgb(50, 220, 190));
        root.addView(title);
        address = new EditText(this);
        address.setSingleLine(true);
        address.setTextColor(Color.WHITE);
        address.setHintTextColor(Color.LTGRAY);
        address.setHint("https://...");
        root.addView(address);
        LinearLayout buttons = new LinearLayout(this);
        Button open = new Button(this);
        open.setText("SİTEYİ AÇ");
        open.setOnClickListener(v -> {
            String url = address.getText().toString().trim();
            if (url.startsWith("https://")) web.loadUrl(url);
            else status.setText("HTTPS adresi girin.");
        });
        buttons.addView(open, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button save = new Button(this);
        save.setText("İNDİR");
        save.setOnClickListener(v -> download(address.getText().toString().trim()));
        buttons.addView(save, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(buttons);
        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setText("Doğrudan video dosyası bağlantısını indirin.");
        root.addView(status);
        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                address.setText(url);
            }
        });
        web.setDownloadListener((url, userAgent, disposition, mime, length) -> download(url));
        root.addView(web, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
    }
    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
