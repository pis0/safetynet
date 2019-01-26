package com.example.piso.safetynet4;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    //PraiaBingoTest
    private static final String API_KEY = "AIzaSyB8nm9-10LmiBH1IkFuW_AbI54TpkBDjb4";

    private byte[] getNonce() {
        return Base64.encode(Long.toString(new Date().getTime()).getBytes(), Base64.DEFAULT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //check google play services
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS)

            //start safetynet api
            SafetyNet.getClient(this).attest(getNonce(), API_KEY)
                    .addOnSuccessListener(this,
                            new OnSuccessListener<SafetyNetApi.AttestationResponse>() {
                                @Override
                                public void onSuccess(SafetyNetApi.AttestationResponse response) {
                                    Log.d("SafetyNet.success", response.getJwsResult());

                                    //get safetynet props result
                                    final String jwsResult = response.getJwsResult();
                                    final String[] jwt = jwsResult.split("\\.");
                                    if (jwt.length == 3) {
                                        String resultText = "RESPONSE: \n\n" + new String(Base64.decode(jwt[1], Base64.DEFAULT));

                                        Log.d("SafetyNet", resultText);
                                        final TextView result = findViewById(R.id.result);
                                        result.setText(resultText);

                                        //server side validation
                                        Thread thread = new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    URL url = new URL("https://www.googleapis.com/androidcheck/v1/attestations/verify?key=" + API_KEY);
                                                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                                    conn.setRequestMethod("POST");
                                                    conn.setRequestProperty("Content-Type", "application/json");
                                                    conn.setRequestProperty("Accept", "application/json");
                                                    conn.setDoOutput(true);
                                                    conn.setDoInput(true);

                                                    JSONObject jo = new JSONObject();
                                                    try {
                                                        jo.put("signedAttestation", jwsResult);
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }

                                                    Log.d("GoogleHttpPost.json", jo.toString());

                                                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                                                    os.writeBytes(jo.toString());
                                                    os.flush();
                                                    os.close();

                                                    Log.d("GoogleHttpPost.status", String.valueOf(conn.getResponseCode()));
                                                    Log.d("GoogleHttpPost.msg", conn.getResponseMessage());

                                                    Scanner scan = new Scanner(conn.getInputStream(), "UTF-8");
                                                    StringBuilder fResult = new StringBuilder();
                                                    while (scan.hasNext())
                                                        fResult.append(scan.next());
                                                    final String fResultt = fResult.toString();
                                                    Log.d("GoogleHttpPost.result", fResult.toString());

                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            result.append("\n\n" + fResultt);
                                                        }
                                                    });


                                                    conn.disconnect();
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                        thread.start();

                                    }
                                }
                            })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if (e instanceof ApiException)
                                Log.d("SafetyNet.error", String.valueOf(((ApiException) e).getStatusCode()));
                            else
                                Log.d("SafetyNet.error", e.getMessage());
                        }
                    });
        else
            Log.d("GPlayServices.error", "Google Play Services app not up to date!");
    }

}
