package com.example.audio_recorder_example;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
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

    private static final int REQUEST_PERMISSION_CODE = 1000;
    private MediaRecorder mediaRecorder;
    private String filePath , outPath;
    private OpenApiService openApiService;
    private Disposable disposable;
    private TextView rk_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 請求錄音權限
        requestPermission();

        // 初始化 MediaRecorder
        mediaRecorder = new MediaRecorder();

        AudioRecorder();

        // 設定錄音來源和輸出格式
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(filePath);

        rk_text = findViewById(R.id.rk_textView);

        // 設定錄音按鈕的點擊事件
        Button recordButton = findViewById(R.id.startButton);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    // 開始錄音
                    try {
                        mediaRecorder.prepare();
                        mediaRecorder.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // 如果尚未授權錄音權限，再次請求權限
                    requestPermission();
                }
            }
        });

        Button stopButton = findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 停止錄音
                try {
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    setffmpeg();
                } catch (IllegalStateException e) {
                    // 處理錯誤
                    e.printStackTrace();
                }
            }
        });

    }

    private void setffmpeg(){
        // 原始 MP3 文件路徑
        String src = filePath;

        // 轉換後 WAV 文件路徑
        String dst = outPath;

        // 轉換
//        FFmpeg.executeAsync(
//                "-y -i " + src + " -c:a pcm_s16le -ar 44100 -ac 2 " + dst, new ExecuteCallback() {
//            @Override
//            public void apply(final long executionId, final int returnCode) {
//                // 根據returnCode進行處理
//                if (returnCode == RETURN_CODE_SUCCESS) {
//                    // FFmpeg執行成功
//                    Log.e("TAG", "FFmpeg執行成功");
//                } else if (returnCode == RETURN_CODE_CANCEL) {
//                    // 使用者取消了執行
//                    Log.e("TAG", "使用者取消了執行");
//                } else {
//                    // 發生了錯誤
//                }
//            }
//        });

        FFmpeg.executeAsync(
                "-y -i " + src + " -c:a libmp3lame -b:a 96k " + dst, new ExecuteCallback() {
                    @Override
                    public void apply(final long executionId, final int returnCode) {
                        if (returnCode == RETURN_CODE_SUCCESS) {
                            Log.e("TAG", "FFmpeg執行成功");
                            uploadFile(dst);
                        } else if (returnCode == RETURN_CODE_CANCEL) {
                            Log.e("TAG", "使用者取消了執行");
                        } else {
                            Log.e("TAG", "FFmpeg執行失敗，錯誤碼：" + returnCode);
                        }
                    }
                });

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
    private void AudioRecorder() {
        // 獲取外部存儲的目錄
        File externalDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);

        // 檢查目錄是否存在，如果不存在，創建它
        if (!externalDir.exists()) {
            externalDir.mkdirs();
        }

        // 設置錄音的文件路徑
        filePath = new File(externalDir, "recorded_audio.mp3").getAbsolutePath();
        outPath = new File(externalDir, "recorded_audio2.mp3").getAbsolutePath();
//        filePath = new File(externalDir, "recorded_audio.wav").getAbsolutePath();

    }

    private void requestPermission() {
        // 請求錄音權限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // 檢查權限請求的結果
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 如果用戶同意權限，可以進行相應的操作
            }
        }
    }
}