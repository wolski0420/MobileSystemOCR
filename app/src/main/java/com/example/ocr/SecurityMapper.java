package com.example.ocr;

import android.media.Image;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;

import okhttp3.Response;

public class SecurityMapper {
    private int secLevel;
    private HashMap<Integer, String> urlEndingsMap = new HashMap<Integer, String>(){{
        put(1, "upload");
        put(2, "checksum" );
        put(3, "fernet_in" );
        put(5, "fernet_in_out" );
        put(8, "checksum_fernet_in_out" );
    }};
    private OwnHttpClient httpClient = new OwnHttpClient();
    private SecurityPackage secPackage = new SecurityPackage();
    SecurityMapper(int SecurityLevel){
        this.secLevel = SecurityLevel;
    }

    private String getMinSecUrlEndings(){
        for (int i = this.secLevel; i <= 8; i++) {
            if (this.urlEndingsMap.keySet().contains(i)){
                this.secLevel = i;
                return urlEndingsMap.get(i);
            }
        }
        return "";
    }

    private Response sendRequest(String url,byte[] image){
        //TODO
        Response response =null;
        if(this.secLevel == 1){
            response = httpClient.sendRequestWithBytes(url,"pobrane.png", image);
        } else if (this.secLevel == 2) {
            response = httpClient.sendRequestWithBytesWithHash(url,"pobrane.png", image);
        }else if (this.secLevel == 3) {
            response = httpClient.sendRequestWithBytesEncrypted(url,"pobrane.png", image);
        }
        else if (this.secLevel == 5) {
            response = httpClient.sendRequestWithBytesEncrypted(url,"pobrane.png", image);
        }else{
            response = httpClient.sendRequestWithBytesEncrypted(url,"pobrane.png", image);
        }
        return response;

    }

    private boolean processResponse(Response response) throws IOException {
        //TODO

        if (response != null) {
            if (response.isSuccessful()) {

                Log.d("MainActivity - CloudOCR", "Success");
                if(this.secLevel == 1){
                    Log.d("MainActivity - CloudOCR", response.body().string()); // OK
                } else if (this.secLevel == 2) {
                    Log.d("MainActivity - CloudOCR", response.body().string()); //extract hash and checkit
                }else if (this.secLevel == 3) {
                    Log.d("MainActivity - CloudOCR", response.body().string()); // OK
                }
                else if (this.secLevel == 5) {
                    Log.d("MainActivity - CloudOCR", this.secPackage.decrypt(response.body().string())); //OK
                }else{
                    Log.d("MainActivity - CloudOCR", this.secPackage.decrypt(response.body().string())); // extract hash and checkit
                }
                return true;
            } else {
                Log.d("MainActivity - CloudOCR", "Failure");
                Log.d("MainActivity - CloudOCR", response.body().string());
                return false;
            }
        }
        else {
            Log.d("MainActivity - CloudOCR", "Failure");
            Log.d("MainActivity - CloudOCR", "response null url http://$urlAndPort/fernet_in_out");
            return false;
        }
    }

    public boolean sendWithSecLevel(String urlAndPort, byte[] image){
        String url = "http://" + urlAndPort +"/" + getMinSecUrlEndings();


        Response response = sendRequest(url, image);

        try {
            return processResponse(response);
        } catch (IOException e) {
            return false;
        }
    }

}
