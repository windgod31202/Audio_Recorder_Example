package com.example.audio_recorder_example.api;

import com.example.audio_recorder_example.Response.RK_response;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.MultipartBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {

    @Multipart
    @POST("/transcribe")
    Observable<RK_response> uploadFile(@Part MultipartBody.Part file);
}
