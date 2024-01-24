package com.example.speech_rec;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.AnimateBuilder;
import com.aldebaran.qi.sdk.builder.AnimationBuilder;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.design.activity.conversationstatus.SpeechBarDisplayStrategy;
import com.aldebaran.qi.sdk.object.actuation.Animate;
import com.aldebaran.qi.sdk.object.actuation.Animation;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends RobotActivity implements RobotLifecycleCallbacks {
    private String AZURE_SUBSCRIPTION_KEY;
    private String AZURE_REGION;
    private String AZURE_OPENAI_URL;
    private String AZURE_OPENAI_KEY;


    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 101;
    private static final int PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 102;



    private AzureSpeechToText azureSpeechToText;
    private Button buttonRecord;
    private boolean isRecording = false;
    // 识别到的文本框
    private TextView recognizedTextView;
    // 机器的回答文本框
    private TextView answerTextView;

//    private final ChatGPT GPT_Bot = new ChatGPT(); OpenAI
    private AzureGPT GPT_Bot;

    private QiContext qicontext;

    //双语说话模块
    private AzureTextToSpeech englishTTS;
    private AzureTextToSpeech chineseTTS;

    private java.util.concurrent.Future<SpeechSynthesisResult> speakResult;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 加载Azure认知服务 API密钥，地区等配置文件
        Properties config = loadConfig();
        AZURE_SUBSCRIPTION_KEY = config.getProperty("AZURE_SUBSCRIPTION_KEY");
        AZURE_REGION = config.getProperty("AZURE_REGION");

        // 加载ChatGPT配置
        AZURE_OPENAI_URL =  config.getProperty("AZURE_OPENAI_URL");
        AZURE_OPENAI_KEY =  config.getProperty("AZURE_OPENAI_KEY");
        GPT_Bot  = new AzureGPT(AZURE_OPENAI_URL, AZURE_OPENAI_KEY);  // Azure


        setContentView(R.layout.activity_main);
        // 隐藏自带对话条
        setSpeechBarDisplayStrategy(SpeechBarDisplayStrategy.IMMERSIVE);
        //识别到的本文
        recognizedTextView = findViewById(R.id.recognized_text_view);
        //机器人回答的内容
        answerTextView = findViewById(R.id.robot_answer_text_view);
        // 语言引擎
        if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.INTERNET}, 1);
        }
        englishTTS = new AzureTextToSpeech(this, Locale.US, AZURE_SUBSCRIPTION_KEY, AZURE_REGION);
        chineseTTS = new AzureTextToSpeech(this, Locale.SIMPLIFIED_CHINESE, AZURE_SUBSCRIPTION_KEY, AZURE_REGION);

        // 录音按钮
        buttonRecord = findViewById(R.id.button_record);
        buttonRecord.setOnClickListener(v -> {
            if (!isRecording) {

                // 防止之前回答刚开始回复，重置状态
                if (isSpeakerPlaying()){
                    chineseTTS.stopSpeaking();
                    englishTTS.stopSpeaking();
                }
                // 开始录音和识别
                azureSpeechToText.startContinuousRecognitionAsync();
                updateRecognizedText("I'm Listening....");
                recognizedTextView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.green));
                recognizedTextView.setTextSize(30);

                buttonRecord.setText(R.string.Button_stop);
                buttonRecord.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.android_red)); // 设置按钮颜色为红色
            } else {
                // 停止录音,识别
                Log.d("MainActivity", "---------Stop Speaking ----------------");
                GPT_Bot.stopReplyFromOpenAI();
                chineseTTS.stopSpeaking();
                englishTTS.stopSpeaking();
                azureSpeechToText.stopContinuousRecognitionAsync();
                updateRecognizedText("Recognized Text:");
                buttonRecord.setText(R.string.Button);
                buttonRecord.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.purple_500)); // 设置按钮颜色为紫色
            }
            isRecording = !isRecording;
        });


        // 注册RobotLifecycleCallbacks
        QiSDK.register(this, this);

    }

    @Override
    protected void onDestroy() {
        // 注销RobotLifecycleCallbacks
        QiSDK.unregister(this, this);
        super.onDestroy();
    }

    // 实现RobotLifecycleCallbacks接口中的方法
    @Override
    public void onRobotFocusGained(QiContext qiContext) {

        // 机器人获取焦点时执行的代码
        this.qicontext = qiContext;
        // 请求录音权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
        // 请求存储权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_EXTERNAL_STORAGE);
        }
        setupAzureSpeechToText();
    }

    @Override
    public void onRobotFocusLost() {
        // 机器人失去焦点时执行的代码
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        // 机器人拒绝焦点时执行的代码
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupAzureSpeechToText();
            } else {
                Toast.makeText(this, "需要录音权限才能使用语音识别", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSIONS_REQUEST_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 外部存储权限已授予，执行相关操作
            } else {
                Toast.makeText(this, "需要外部存储权限以存储配置文件", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupAzureSpeechToText() {

        azureSpeechToText = new AzureSpeechToText(AZURE_SUBSCRIPTION_KEY, AZURE_REGION, new AzureSpeechToText.SpeechToTextListener() {
            @Override
            public void onResult(String result) {
                if(!isRecording) return;
                if(result.length() == 0) return;
                //暂停语音识别。防止听到自己的声音
                azureSpeechToText.pauseRecognition();

                Log.i("MainActivity", "Recognized Text: " + result);
                // 在这里处理识别结果
                updateRecognizedText("Recognized Text: " + result);
                updateAnswerText("I'm Thinking....");


                String answer = GPT_Bot.replyFromOpenAI(result);
                Log.i("MainActivity", "Robot Answer: " + answer);
                updateAnswerText("Answer: " + answer);


                if (isChinese(answer)) {
                    Log.d("AzureTTS", "speak() called Chinese---------------------------------");
                    speakResult = chineseTTS.speak(answer, qicontext, getGestureResources());
                } else {
                    Log.d("AzureTTS", "speak() called English---------------------------------");
                    speakResult = englishTTS.speak(answer, qicontext, getGestureResources());
                }
                try {
                    speakResult.get();
                    Thread.sleep(3000);
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
                // 等待扬声器播放ing
                while (isSpeakerPlaying()){
                    //不想改TTS异步动作链，偷个懒
                }
                chineseTTS.isSpeaking.set(false);
                englishTTS.isSpeaking.set(false);

                if (isRecording) {
                    System.out.println("Resume Listening..........");
                    azureSpeechToText.resumeRecognition();
                    updateRecognizedText("I'm Listening....");
                    runOnUiThread(() -> recognizedTextView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.green)));
                    runOnUiThread(() -> recognizedTextView.setTextSize(30));
                }

            }

            @Override
            public void onError(String errorMessage) {
                Log.e("MainActivity", "Error: " + errorMessage);
                // 在这里处理错误
            }
        });

    }

    private void updateRecognizedText(final String recognizedText) {
        runOnUiThread(() -> recognizedTextView.setText(recognizedText));
        runOnUiThread(() -> recognizedTextView.setTextSize(20));
        runOnUiThread(() -> recognizedTextView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.black)));
    }

    private void updateAnswerText(final String answerText) {
        runOnUiThread(() -> answerTextView.setText(answerText));
    }

    private boolean isChinese(String text) {
        //判断当前需要用的语言
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B) {
                return true;
            }
        }
        return false;
    }
    private List<Integer> getGestureResources() {
        // 获取可用的动作列表
        Field[] fields = R.raw.class.getFields();
        List<Integer> gestureResources = new ArrayList<>();

        for (Field field : fields) {
            String resourceName = field.getName();
            if (resourceName.startsWith("affirmation_") || resourceName.startsWith("enumeration_") || resourceName.startsWith("question_")|| resourceName.startsWith("raise")|| resourceName.startsWith("spread_")|| resourceName.startsWith("exclamation_")) {
                int rawResourceId = getResources().getIdentifier(resourceName, "raw", getPackageName());
                gestureResources.add(rawResourceId);
            }
        }
        return gestureResources;
    }
    public boolean isSpeakerPlaying() {
        // 检查扬声器是否在播放声音
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            return audioManager.isMusicActive();
        }
        return false;
    }
    private Properties loadConfig() {
        //读取配置文件，key, region等信息
        Properties properties = new Properties();
        try {
            InputStream inputStream = null;
            try {
                inputStream = getAssets().open("config.properties");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            properties.load(inputStream);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

}
