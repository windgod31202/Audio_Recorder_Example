package com.example.audio_recorder_example;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.example.audio_recorder_example.Response.OpenApi_response;
import com.example.audio_recorder_example.api.ApiClient;
import com.example.audio_recorder_example.api.ApiService;
import com.example.audio_recorder_example.api.OpenApiService;

import java.io.File;
import java.io.IOException;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    private MediaRecorder mediaRecorder;
    private Button startButton;
    private Button stopButton;
//    private Button apichagebutton;
//    private ApiService apiService;
    private OpenApiService openApiService;
    private Disposable disposable;
    private TextView rk_text;
    private String outPath, filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check and request the audio recording permission
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
//        apichagebutton = findViewById(R.id.chageApiButton);
        rk_text = findViewById(R.id.rk_textView);
        
//        apichagebutton.setOnClickListener(v -> {
//            Log.d("YourTag", "Start button clicked");
//            changeAPI();
//        });
        
        if (startButton != null && stopButton != null) {
            startButton.setOnClickListener(v -> {
                Log.d("YourTag", "Start button clicked");
                checkPermissionsAndStartRecording();
            });

            stopButton.setOnClickListener(v -> {
                Log.d("YourTag", "Stop button clicked");
                stopRecording();
            });
        }
    }

//    private void changeAPI() {
//        Boolean isChangeApi = true;
//        if (isChangeApi){
//            ApiClient apiClient = new ApiClient();
//            apiService = apiClient.getApiService();
//        }else {
//            ApiClient apiClient = new ApiClient();
//            openApiService = apiClient.getOpenApiService();
//        }
//    }

    private void checkPermissionsAndStartRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted, start recording
            startRecording();
        } else {
            // Request permission if not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    private void startRecording() {
        Log.d("YourTag", "Permission granted. Starting recording...");
        initializeRecorder();
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            Log.d("YourTag", "Recording started successfully.");
            rk_text.setText("Starting recording...");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("YourTag", "Error starting recording: " + e.getMessage());
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            Log.d("YourTag", "Recording stopped.");

            uploadFile(getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath() + "/audio_record2.mp3");

        } else {
            Log.d("YourTag", "MediaRecorder is null. Cannot stop recording.");
        }
    }

    private void initializeRecorder() {
        File directory = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (directory != null && !directory.exists()) {
            directory.mkdirs();
        }
        filePath = directory.getAbsolutePath() + "/audio_record.mp3";
        outPath = directory.getAbsolutePath() + "/audio_record2.mp3";

        FFmpeg.executeAsync(
                "-y -i " + filePath + " -c:a libmp3lame -b:a 96k " + outPath, new ExecuteCallback() {
            @Override
            public void apply(final long executionId, final int returnCode) {
                if (returnCode == RETURN_CODE_SUCCESS) {
                    Log.e("TAG", "FFmpeg執行成功");
                } else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.e("TAG", "使用者取消了執行");
                } else {
                    Log.e("TAG", "FFmpeg執行失敗，錯誤碼：" + returnCode);
                }
            }
        });

        Log.e("Jay", "initializeRecorder: "+filePath );
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(filePath);
    }
    private void uploadFile(String filePath) {
        ApiClient apiClient = new ApiClient();
        openApiService = apiClient.getApiService();

        File file = new File(filePath);
        RequestBody requestFile = RequestBody.create(MediaType.parse("audio/mp3"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
        Log.e("TAG", "uploadFile: "+filePath );

        openApiService.uploadFile2("multipart/form-data",
                        "Bearer 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",
                        body,
                        "whisper-1")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<OpenApi_response>() {
                    @Override
                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                        disposable = d;
                        Log.d("YourTag", "onSubscribe: "+d.toString());

                    }

                    @Override
                    public void onNext(@io.reactivex.rxjava3.annotations.NonNull OpenApi_response responseBody) {
                        Log.d("YourTag", "onNext: " + responseBody.getText());
                        rk_text.setText(responseBody.getText());
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        Log.d("YourTag", "onError: " + e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        Log.d("YourTag", "onComplete");
                    }
                });
    }

    // Make sure to dispose of the disposable when appropriate
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionToRecordAccepted = true;
                    checkPermissionsAndStartRecording(); // Check permissions again and start recording
                    Log.d("YourTag", "Record audio permission granted.");
                } else {
                    Log.d("YourTag", "Record audio permission not granted.");
                    // Handle the case where permission is not granted
                    finish();
                }
                break;
        }
    }
}

