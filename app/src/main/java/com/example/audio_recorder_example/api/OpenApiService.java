package com.example.audio_recorder_example.api;

import com.example.audio_recorder_example.Response.OpenApi_response;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface OpenApiService {
    @Multipart
    @POST("/v1/audio/transcriptions")
    Observable<OpenApi_response> uploadFile2(
            @Header("Content-Type") String contentType,
            @Header("Authorization") String authorization,
            @Part MultipartBody.Part file,
            @Part("model") String model);
}
