package com.example.speech_rec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatGPT{
    private final List<String> context_list; // 记录openai上下文
    private OkHttpClient client;
    private String OPENAI_URL;
    private String OPENAI_KEY;

    public ChatGPT(String CHATGPT_OPENAI_URL, String CHATGPT_OPENAI_KEY){
        OPENAI_URL = CHATGPT_OPENAI_URL;
        OPENAI_KEY = CHATGPT_OPENAI_KEY;
        client = new OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build();
//        executorService =  Executors.newSingleThreadExecutor();
        context_list = new ArrayList<>();
    }

    public String replyFromOpenAI(String question){
        // 控制上下文长度，进行裁剪
        if(context_list.size() > 8){
            context_list.remove(0);
            context_list.remove(0);
        }

        context_list.add(String.format("{\"role\": \"user\", \"content\": \"%s\"}", question));

        MediaType mediaType = MediaType.parse("application/json");
        //你是由微软创新车库开发的机器人pepper，现在请简短回答我的问题
        RequestBody body = RequestBody.create(mediaType, "{" +
                "\"model\": \"gpt-3.5-turbo\"," +
                "\"messages\":" +
                context_list.toString() +
                "}"
        );
        System.out.println(body.toString());

        Request request = new Request.Builder()
                .url(OPENAI_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", OPENAI_KEY)
                .build();

        Response response = null;
        try {
            response = client.newCall(request).execute();
            String responseBody = response.body().string();
            System.out.println(responseBody);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode reply_node = jsonNode.get("choices").get(0).get("message").get("content");
            String reply = reply_node.asText().strip();
            System.out.println(reply);
            context_list.add(String.format("{\"role\": \"assistant\", \"content\": \"%s\"}", reply.replace("\n", "").replace("'", "").replace("\"", "")));
            System.out.println(context_list.toString());
            return reply;
        } catch (IOException e) {
            //回溯，从该纪录中删去没有成功得到回答的提问
            if(context_list.size() > 0) context_list.remove(0);
            return "调用OpenAI出错";
        }
    }
}
