package com.example.speech_rec;

import android.content.Context;
import android.util.Log;
import android.util.LogPrinter;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.builder.AnimateBuilder;
import com.aldebaran.qi.sdk.builder.AnimationBuilder;
import com.aldebaran.qi.sdk.object.actuation.Animate;
import com.aldebaran.qi.sdk.object.actuation.Animation;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;


public class AzureTextToSpeech {
    private final SpeechSynthesizer synthesizer;
    private Context context;
    private java.util.concurrent.Future<SpeechSynthesisResult> speakFuture;
    public AtomicBoolean isSpeaking = new AtomicBoolean(false);


    public AzureTextToSpeech(Context context, Locale language, String AZURE_SUBSCRIPTION_KEY, String AZURE_REGION) {
        this.context = context;
        SpeechConfig speechConfig = SpeechConfig.fromSubscription(AZURE_SUBSCRIPTION_KEY, AZURE_REGION);
        speechConfig.setSpeechSynthesisLanguage(language.toLanguageTag());
        synthesizer = new SpeechSynthesizer(speechConfig);
    }

    public java.util.concurrent.Future<SpeechSynthesisResult> speak(String text, QiContext qiContext, List<Integer> gestureResources) {
        // 设置为正在说话
        isSpeaking.set(true);
        new Thread(() -> {
            while (isSpeaking.get()) {
                Future<Void> gestureFuture = startRandomGesture(qiContext, gestureResources);
                try {
                    // 延迟，以便在两个连续的手势之间有一个短暂的暂停
                    Thread.sleep(8000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                gestureFuture.requestCancellation();
            }
        }).start();

        speakFuture = synthesizer.StartSpeakingTextAsync(text);

        return speakFuture;

    }

    // 机器人在说话时配合随机的手势动作
    private Future<Void> startRandomGesture(QiContext qiContext, List<Integer> gestureResources) {
        int randomGestureIndex = new Random().nextInt(gestureResources.size());
        int rawResourceId = gestureResources.get(randomGestureIndex);

        Animation myAnimation = AnimationBuilder.with(qiContext)
                .withResources(rawResourceId)
                .build();

        Animate animate = AnimateBuilder.with(qiContext)
                .withAnimation(myAnimation)
                .build();

        return animate.async().run();
    }

    public Void stopSpeaking() {
        // 停止说话
        java.util.concurrent.Future<Void> stopFuture = synthesizer.StopSpeakingAsync();
        isSpeaking.set(false);
        try {
            Log.d("Azure TTS", "-------------stoping Speaking--------------");
            stopFuture.get(); // 等待停止操作完成
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
