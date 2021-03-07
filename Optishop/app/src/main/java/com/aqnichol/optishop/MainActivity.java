package com.aqnichol.optishop;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

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

        Liboptishop.startSetup(filesDir+"/assets", filesDir);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view,
                                                              WebResourceRequest request) {
                String host = request.getUrl().getHost();
                if (host.equals("optishop.us")) {
                    Response resp = Liboptishop.handleGet(request.getUrl().toString());
                    return new WebResourceResponse(resp.getContentType(), "utf-8",
                            (int) resp.getCode(), "OK", new HashMap<String, String>(),
                            new ByteArrayInputStream(resp.getData()));
                } else if (host.equals("init.optishop.us")) {
                    if (request.getUrl().getPath().equals("/status")) {
                        return setupStatusData();
                    } else if (request.getUrl().getPath().length() < 2) {
                        // Retry setup if it failed.
                        Liboptishop.startSetup(filesDir + "/assets", filesDir);
                        return setupLoadingPage();
                    }
                } else if (host.equals("error.optishop.us")) {
                    Log.d("optishop", "loading error page");
                    // TODO: redirect to an error page.
                }
                return null;
            }
        });

        WebSettings webViewSettings = webView.getSettings();
        webViewSettings.setAllowFileAccessFromFileURLs(false);
        webViewSettings.setAllowUniversalAccessFromFileURLs(false);
        webViewSettings.setAllowFileAccess(false);
        webViewSettings.setAllowContentAccess(false);
        webViewSettings.setJavaScriptEnabled(true);
        webView.loadUrl("https://init.optishop.us");
    }

    private WebResourceResponse setupLoadingPage() {
        try {
            InputStream in = getAssets().open("loading.html");
            return new WebResourceResponse("text/html", "utf-8", 200,
                    "OK", new HashMap<String,String>(), in);
        } catch (IOException e) {
            return null;
        }
    }

    private WebResourceResponse setupStatusData() {
        JSONObject obj = new JSONObject();
        try {
            obj.accumulate("error", Liboptishop.setupError());
            obj.accumulate("complete", Liboptishop.setupComplete());
            return new WebResourceResponse("application/json", "utf-8",
                    200, "OK", new HashMap<String, String>(),
                    new ByteArrayInputStream(obj.toString().getBytes()));
        } catch (JSONException e) {
            return null;
        }
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
