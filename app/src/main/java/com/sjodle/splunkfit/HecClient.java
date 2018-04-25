package com.sjodle.splunkfit;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HecClient {
    private Context context;
    private OkHttpClient httpClient = new OkHttpClient();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    HecClient(Context context) {
        this.context = context;
    }

    public void updateConnectionInfo(String url, String token) {
        SharedPreferences appData = context.getSharedPreferences(Constants.PREFS_FILE_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = appData.edit();
        editor.putString(Constants.PREFS_HEC_URL_KEY, url);
        editor.putString(Constants.PREFS_HEC_TOKEN_KEY, token);
        editor.apply();
    }

    boolean ingest(JSONArray event) {
        OkHttpClient.Builder clientBuilder = httpClient.newBuilder();

        if (Constants.ALLOW_UNTRUSTED_SSL) {
            Log.w("Ingest", "**** Allow untrusted SSL connection ****");
            final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    X509Certificate[] cArrr = new X509Certificate[0];
                    return cArrr;
                }

                @Override
                public void checkServerTrusted(final X509Certificate[] chain,
                                               final String authType) throws CertificateException {
                }

                @Override
                public void checkClientTrusted(final X509Certificate[] chain,
                                               final String authType) throws CertificateException {
                }
            }};

            SSLContext sslContext = null;
            try {
                sslContext = SSLContext.getInstance("SSL");

                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                e.printStackTrace();
            }
            clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);

            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    Log.d("Ingest", "Trust Host :" + hostname);
                    return true;
                }
            };
            clientBuilder.hostnameVerifier(hostnameVerifier);
            httpClient = clientBuilder.build();
        }

        SharedPreferences appData = context.getSharedPreferences(Constants.PREFS_FILE_KEY, Context.MODE_PRIVATE);
        try {
            URL hecUrl = new URL(appData.getString(Constants.PREFS_HEC_URL_KEY, ""));
            hecUrl = new URL(hecUrl, "/services/collector");
            String hecToken = appData.getString(Constants.PREFS_HEC_TOKEN_KEY, "");
            RequestBody requestBody = RequestBody.create(JSON, event.toString());
            Request request = new Request.Builder()
                    .url(hecUrl)
                    .header("Authorization", "Splunk " + hecToken)
                    .post(requestBody)
                    .build();
            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                Log.d("IngestSuccess", response.body().string());
                return true;
            } else {
                Log.e("IngestFailure", response.body().string());
                Log.e("IngestFailure", hecUrl.toString());
                Log.e("IngestFailure", event.toString());
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void ingestTestEvent() {
        JSONObject event = new JSONObject();
        JSONObject body = new JSONObject();
        JSONArray events = new JSONArray();
        try {
            body.put("test", 123);
            event.put("event", body);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        events.put(event);
        ingest(events);
    }
}
