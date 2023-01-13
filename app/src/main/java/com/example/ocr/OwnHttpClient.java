package com.example.ocr;

import android.util.Log;

import java.io.File;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OwnHttpClient {
    public Boolean sendRequestWithFile(String serverURL, String imageName, File file) {
        try {
            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", imageName, RequestBody.create(MediaType.parse("image/jpeg"), file))
                    .build();

            Request request = new Request.Builder()
                    .url(serverURL)
                    .post(requestBody)
                    .build();

            OkHttpClient client = new OkHttpClient();
            Call call = client.newCall(request);

            Response response = call.execute();
            if (!response.isSuccessful()) {
                Log.d(OwnHttpClient.class.getSimpleName(), "Unsuccessful");
                Log.d(OwnHttpClient.class.getSimpleName(), Objects.requireNonNull(response.body()).string());
            } else {
                Log.d(OwnHttpClient.class.getSimpleName(), "Success");
                Log.d(OwnHttpClient.class.getSimpleName(), Objects.requireNonNull(response.body()).string());
            }

            return true;
        } catch (Exception ex) {
            Log.d(OwnHttpClient.class.getSimpleName(), "Exception");
            Log.d(OwnHttpClient.class.getSimpleName(), ex.getMessage());
        }
        return false;
    }
}
