package com.example.ocr;

import android.util.Log;

import java.io.File;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OwnHttpClient {
    public Response sendRequestWithFile(String serverURL, String imageName, File file) {
        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", imageName, RequestBody.create(MediaType.parse("image/jpeg"), file))
                .build();

        return sendRequestWithGivenBody(serverURL, requestBody);
    }

    public Response sendRequestWithBytes(String serverURL, String imageName, byte[] bytes) {
        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", imageName, RequestBody.create(MediaType.parse("image/jpeg"), bytes))
                .build();

        return sendRequestWithGivenBody(serverURL, requestBody);
    }

    private Response sendRequestWithGivenBody(String serverURL, RequestBody requestBody) {
        try {
            Request request = new Request.Builder()
                    .url(serverURL)
                    .post(requestBody)
                    .build();

            OkHttpClient client = new OkHttpClient();
            Call call = client.newCall(request);
            return call.execute();
        }
        catch (Exception ex) {
            Log.d(OwnHttpClient.class.getSimpleName(), "Exception");
            Log.d(OwnHttpClient.class.getSimpleName(), ex.getMessage());
            return null;
        }
    }
}
