package com.aqnichol.optishop;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import liboptishop.liboptishop.Liboptishop;
import liboptishop.liboptishop.Response;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webview);

        String filesDir = getFilesDir().getAbsolutePath();

        try {
            copyAssetDir("web-assets", filesDir + "/assets");
        } catch (IOException e) {
            Log.e("optishop", "got exception copying files: " + e);
        }

        Log.d("optishop", "running setup");

        if (!Liboptishop.setupComplete() && Liboptishop.setupError() != "") {
            Liboptishop.startSetup(filesDir+"/assets", filesDir);
            while (!Liboptishop.setupComplete() && Liboptishop.setupError() != "") {
                // Wait here.
            }
        }

        Log.d("optishop", "done setting up");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view,
                                                              WebResourceRequest request) {
                if (!request.getUrl().getHost().equals("optishop.us")) {
                    Log.i("optishop", "shouldn't intercept:" + request.getUrl().getHost());
                    return null;
                }
                Log.i("optishop", "should intercept: " + request.getUrl().getHost());
                Response resp = Liboptishop.handleGet(request.getUrl().toString());
                return new WebResourceResponse(resp.getContentType(), "utf-8",
                        (int)resp.getCode(), "OK", new HashMap<String,String>(),
                        new ByteArrayInputStream(resp.getData()));
            }
        });

        WebSettings webViewSettings = webView.getSettings();
        webViewSettings.setAllowFileAccessFromFileURLs(false);
        webViewSettings.setAllowUniversalAccessFromFileURLs(false);
        webViewSettings.setAllowFileAccess(false);
        webViewSettings.setAllowContentAccess(false);
        webViewSettings.setJavaScriptEnabled(true);
        webView.loadUrl("https://optishop.us");
    }

    private void copyAssetDir(String assetDir, String targetDir) throws IOException {
        File f = new File(targetDir);
        if (!f.exists()) {
            f.mkdir();
        }
        for (String name : getAssets().list(assetDir)) {
            String assetPath = assetDir + "/" + name;
            String targetPath = targetDir + "/" + name;
            Log.i("optishop", "copying " + assetPath + " to " + targetPath);
            // Hacky way to check for a directory.
            if (getAssets().list(assetPath).length == 0) {
                InputStream in = getAssets().open(assetPath);
                OutputStream out = new FileOutputStream(targetPath);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();
                out.close();
            } else {
                copyAssetDir(assetPath, targetPath);
            }
        }
    }
}
