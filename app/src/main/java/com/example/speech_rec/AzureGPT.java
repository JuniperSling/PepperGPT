package com.example.speech_rec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;

public class AzureGPT {
    private final List<String> context_list; // 记录openai上下文
    private OkHttpClient client;
    private ExecutorService executorService;
    private Future<String> replyFuture;
    private final String OPENAI_URL;
    private final String OPENAI_KEY;

    public AzureGPT(String AZURE_OPENAI_URL, String AZURE_OPENAI_KEY){
        OPENAI_URL = AZURE_OPENAI_URL;
        OPENAI_KEY = AZURE_OPENAI_KEY;
        client = new OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build();
        executorService =  Executors.newSingleThreadExecutor();
        context_list = new ArrayList<>();
    }


    public String replyFromOpenAI(String question) {
        // 控制上下文长度，进行裁剪
        if (context_list.size() > 6) {
            context_list.remove(0);
            context_list.remove(0);
        }

        context_list.add(String.format("{\"role\": \"user\", \"content\": \"%s\"}", question));

        Callable<String> replyTask = () -> {
            MediaType mediaType = MediaType.parse("application/json");
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
                    .addHeader("api-key", OPENAI_KEY) // key 1
                    .build();

            Response response;
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
                if (context_list.size() > 0) context_list.remove(0);
                return "Error calling Azure API";
            }
        };

        replyFuture = executorService.submit(replyTask);
        try {
            return replyFuture.get(15, TimeUnit.SECONDS); // 设置超时时间
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            replyFuture.cancel(true);
            return "Error calling Azure API";
        }
    }

    // 创建一个停止replyFromOpenAI的方法
    public void stopReplyFromOpenAI() {
        if (replyFuture != null && !replyFuture.isDone()) {
            replyFuture.cancel(true);
        }
    }


}