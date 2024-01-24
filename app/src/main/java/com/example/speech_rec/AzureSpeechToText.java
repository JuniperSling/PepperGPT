package com.example.speech_rec;

import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class AzureSpeechToText {

    private static final String TAG = "AzureSpeechToText";
    private final SpeechConfig speechConfig;
    private final AudioConfig audioConfig;
    private SpeechRecognizer recognizer;

    private boolean isRecognitionStarted = false;
    private Executor executor = Executors.newSingleThreadExecutor();

    public interface SpeechToTextListener {
        void onResult(String result);

        void onError(String errorMessage);
    }

    private final SpeechToTextListener listener;

    public AzureSpeechToText(String subscriptionKey, String region, SpeechToTextListener listener) {
        this.listener = listener;
        this.speechConfig = SpeechConfig.fromSubscription(subscriptionKey, region);

        // 设置识别器语言为中英混合模型
        speechConfig.setSpeechRecognitionLanguage("zh-CN");

        this.audioConfig = AudioConfig.fromDefaultMicrophoneInput();
        this.recognizer = new SpeechRecognizer(speechConfig, audioConfig);
    }


    public void startContinuousRecognitionAsync() {
        if (!isRecognitionStarted) {
            recognizer = new SpeechRecognizer(speechConfig, audioConfig);

            recognizer.recognized.addEventListener((o, speechRecognitionEventArgs) -> {
                String recognizedText = speechRecognitionEventArgs.getResult().getText();
                listener.onResult(recognizedText);
            });

            executor.execute(() -> {
                try {
                    recognizer.startContinuousRecognitionAsync().get();
                } catch (InterruptedException | ExecutionException e) {
                    Log.e(TAG, "Error starting recognition: ", e);
                    listener.onError("Error");
                }
            });

            isRecognitionStarted = true;
        }
    }

    public void stopContinuousRecognitionAsync() {
        if (isRecognitionStarted) {
            executor.execute(() -> {
                try {
                    recognizer.stopContinuousRecognitionAsync().get();
                } catch (InterruptedException | ExecutionException e) {
                    Log.e(TAG, "Error stopping recognition: ", e);
                }

                recognizer.close();
                isRecognitionStarted = false;
            });
        }
    }

    public void pauseRecognition() {
        if (isRecognitionStarted) {
            executor.execute(() -> {
                try {
                    recognizer.stopContinuousRecognitionAsync().get();
                } catch (InterruptedException | ExecutionException e) {
                    Log.e(TAG, "Error stopping recognition: ", e);
                }
            });
            isRecognitionStarted = false;
        }
    }

    public void resumeRecognition() {
        if (!isRecognitionStarted) {
            executor.execute(() -> {
                try {
                    recognizer.startContinuousRecognitionAsync().get();
                } catch (InterruptedException | ExecutionException e) {
                    Log.e(TAG, "Error starting recognition: ", e);
                }
            });
            isRecognitionStarted = true;
        }
    }




}
